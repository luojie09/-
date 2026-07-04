package com.secretbase.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.secretbase.app.AppActions
import com.secretbase.app.data.ActivityRecord
import com.secretbase.app.data.HomeRepository
import com.secretbase.app.data.HomeSnapshot
import com.secretbase.app.data.MoodOption
import com.secretbase.app.data.message.Message
import com.secretbase.app.data.message.MessageRepository
import com.secretbase.app.data.message.SecretBaseUsers
import com.secretbase.app.data.wish.Wish
import com.secretbase.app.data.wish.WishRepository
import com.secretbase.app.data.wish.WishStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val messageRepository: MessageRepository,
    private val wishRepository: WishRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    private val _messages = MutableSharedFlow<String>()

    private var latestBaseSnapshot: HomeSnapshot? = null
    private var latestMessageWall: List<Message> = emptyList()
    private var latestWishes: List<Wish> = emptyList()

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        observeMessageWall()
        observeWishes()
        refresh()
    }

    fun refresh() {
        loadSnapshot(showLoading = true)
    }

    fun updateQuickNote(text: String) {
        _uiState.update { state ->
            state.copy(quickNoteText = text.take(MAX_NOTE_LENGTH))
        }
    }

    fun submitQuickNote() {
        val text = _uiState.value.quickNoteText.trim()
        if (text.isBlank()) {
            emitMessage("写点文字后再记录吧")
            return
        }

        viewModelScope.launch {
            homeRepository.addQuickNote(text)
            _uiState.update { it.copy(quickNoteText = "") }
            emitMessage("已记录到最近动态")
            loadSnapshot(showLoading = false)
        }
    }

    fun onMoodCardClick(userId: String) {
        val user = latestBaseSnapshot?.users?.firstOrNull { it.id == userId } ?: return
        if (!user.editable) {
            emitMessage("当前版本只支持修改自己的心情")
            return
        }

        _uiState.update { it.copy(editingMoodUserId = userId) }
    }

    fun dismissMoodPicker() {
        _uiState.update { it.copy(editingMoodUserId = null) }
    }

    fun updateMood(mood: MoodOption) {
        val userId = _uiState.value.editingMoodUserId ?: return
        viewModelScope.launch {
            homeRepository.saveMood(userId, mood)
            _uiState.update { it.copy(editingMoodUserId = null) }
            emitMessage("今日心情已更新")
            loadSnapshot(showLoading = false)
        }
    }

    fun onPlaceholderAction(message: String) {
        emitMessage(message)
    }

    private fun loadSnapshot(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = true,
                        errorMessage = null,
                        editingMoodUserId = null,
                    )
                }
            }

            try {
                latestBaseSnapshot = homeRepository.loadSnapshot()
                renderUi()
            } catch (_: Throwable) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = "加载失败，点击重试",
                    )
                }
            }
        }
    }

    private fun observeMessageWall() {
        viewModelScope.launch {
            messageRepository.observeMessages().collectLatest { messages ->
                latestMessageWall = messages
                renderUi()
            }
        }
    }

    private fun observeWishes() {
        viewModelScope.launch {
            wishRepository.observeWishes().collectLatest { wishes ->
                latestWishes = wishes
                renderUi()
            }
        }
    }

    private fun renderUi() {
        val snapshot = latestBaseSnapshot ?: return
        val enriched = snapshot.withLiveModules(
            messages = latestMessageWall,
            wishes = latestWishes,
        )
        _uiState.value = enriched.toUiState(
            now = ZonedDateTime.now(),
            quickNoteText = _uiState.value.quickNoteText,
            editingMoodUserId = _uiState.value.editingMoodUserId,
        )
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }

    companion object {
        private const val MAX_NOTE_LENGTH = 500

        fun factory(
            homeRepository: HomeRepository,
            messageRepository: MessageRepository,
            wishRepository: WishRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(
                    homeRepository = homeRepository,
                    messageRepository = messageRepository,
                    wishRepository = wishRepository,
                ) as T
        }
    }
}

private fun HomeSnapshot.withLiveModules(
    messages: List<Message>,
    wishes: List<Wish>,
): HomeSnapshot {
    val unreadCount = messages.sumOf { it.unreadCountFor(SecretBaseUsers.CURRENT_USER_ID) }
    val messageActivities = messages.toActivityRecords(SecretBaseUsers.CURRENT_USER_ID)
    val wishActivities = wishes.toActivityRecords()
    return copy(
        stats = stats.copy(
            messageUnread = unreadCount,
            wishCompleted = wishes.count { it.status == WishStatus.REALIZED },
            wishTotal = wishes.size,
        ),
        recentActivities = (recentActivities + messageActivities + wishActivities)
            .distinctBy(ActivityRecord::id)
            .sortedByDescending(ActivityRecord::timestamp),
        recentActivityListMessage = AppActions.OpenWishList,
    )
}

private fun Message.unreadCountFor(currentUserId: String): Int {
    val messageUnread = if (authorId != currentUserId && !isRead) 1 else 0
    val replyUnread = replies.count { reply -> reply.authorId != currentUserId && !reply.isRead }
    return messageUnread + replyUnread
}

private fun List<Message>.toActivityRecords(currentUserId: String): List<ActivityRecord> =
    flatMap { message ->
        buildList {
            add(
                ActivityRecord(
                    id = "message-activity-${message.id}",
                    title = "${message.authorName}发布了一条新留言",
                    iconSlot = "activityNote",
                    timestamp = Instant.ofEpochMilli(message.createdAt),
                    clickMessage = AppActions.OpenMessageWall,
                ),
            )
            message.replies.forEach { reply ->
                if (message.authorId == currentUserId && reply.authorId != currentUserId) {
                    add(
                        ActivityRecord(
                            id = "reply-activity-${reply.id}",
                            title = "${reply.authorName}回复了你的留言",
                            iconSlot = "messageWallFeature",
                            timestamp = Instant.ofEpochMilli(reply.createdAt),
                            clickMessage = AppActions.OpenMessageWall,
                        ),
                    )
                }
            }
        }
    }

private fun List<Wish>.toActivityRecords(): List<ActivityRecord> =
    mapNotNull { wish ->
        val completion = wish.completion ?: return@mapNotNull null
        ActivityRecord(
            id = "wish-activity-${wish.id}",
            title = "完成了愿望「${wish.title}」",
            iconSlot = "activityChecklist",
            timestamp = Instant.ofEpochMilli(completion.completedAt),
            clickMessage = AppActions.OpenWishList,
        )
    }
