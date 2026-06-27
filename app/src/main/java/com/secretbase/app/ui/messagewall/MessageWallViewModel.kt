package com.secretbase.app.ui.messagewall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.secretbase.app.data.HomeRepository
import com.secretbase.app.data.HomeVisuals
import com.secretbase.app.data.message.Message
import com.secretbase.app.data.message.MessageDraft
import com.secretbase.app.data.message.MessageRepository
import com.secretbase.app.data.message.SecretBaseUsers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MessageWallViewModel(
    private val homeRepository: HomeRepository,
    private val messageRepository: MessageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageWallUiState())
    private val _messages = MutableSharedFlow<String>()

    private var visuals: HomeVisuals = HomeVisuals.EMPTY
    private var latestMessages: List<Message> = emptyList()
    private var latestDraft: MessageDraft = MessageDraft()

    val uiState: StateFlow<MessageWallUiState> = _uiState.asStateFlow()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        loadVisuals()
        observeData()
    }

    fun updateDraftText(text: String) {
        viewModelScope.launch {
            messageRepository.updateDraft(
                content = text.take(MAX_MESSAGE_LENGTH),
                imagePaths = latestDraft.imagePaths,
            )
        }
    }

    fun addSelectedImages(imagePaths: List<String>) {
        val merged = (latestDraft.imagePaths + imagePaths)
            .distinct()
            .take(MAX_IMAGE_COUNT)
        viewModelScope.launch {
            messageRepository.updateDraft(
                content = latestDraft.content,
                imagePaths = merged,
            )
            if (merged.size < latestDraft.imagePaths.size + imagePaths.distinct().size) {
                emitMessage("最多选择 9 张图片")
            }
        }
    }

    fun removeSelectedImage(imagePath: String) {
        viewModelScope.launch {
            messageRepository.updateDraft(
                content = latestDraft.content,
                imagePaths = latestDraft.imagePaths.filterNot { it == imagePath },
            )
        }
    }

    fun publishMessage() {
        val draftText = latestDraft.content.trim()
        val draftImages = latestDraft.imagePaths
        if (draftText.isBlank() && draftImages.isEmpty()) {
            emitMessage("文字和图片不能同时为空")
            return
        }

        _uiState.update { it.copy(isPublishing = true) }
        viewModelScope.launch {
            messageRepository.publishMessage(draftText, draftImages)
                .onSuccess {
                    messageRepository.clearDraft()
                    emitMessage("留言已发布")
                }
                .onFailure { error ->
                    emitMessage(error.message ?: "发布失败，请稍后再试")
                }
            _uiState.update { it.copy(isPublishing = false) }
        }
    }

    fun openReplyComposer(messageId: String) {
        _uiState.update {
            it.copy(
                activeReplyMessageId = messageId,
                replyText = "",
            )
        }
        markMessageRead(messageId)
    }

    fun closeReplyComposer() {
        _uiState.update {
            it.copy(
                activeReplyMessageId = null,
                replyText = "",
            )
        }
    }

    fun updateReplyText(text: String) {
        _uiState.update { it.copy(replyText = text.take(MAX_REPLY_LENGTH)) }
    }

    fun sendReply() {
        val messageId = _uiState.value.activeReplyMessageId ?: return
        val replyText = _uiState.value.replyText.trim()
        if (replyText.isBlank()) {
            emitMessage("回复内容不能为空")
            return
        }

        viewModelScope.launch {
            messageRepository.addReply(messageId, replyText)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            activeReplyMessageId = null,
                            replyText = "",
                        )
                    }
                    emitMessage("回复已发送")
                }
                .onFailure { error ->
                    emitMessage(error.message ?: "回复发送失败")
                }
        }
    }

    fun toggleExpandedReplies(messageId: String) {
        _uiState.update { state ->
            val expanded = state.expandedReplyMessageIds.toMutableSet()
            if (!expanded.add(messageId)) {
                expanded.remove(messageId)
            }
            state.copy(expandedReplyMessageIds = expanded)
        }
        markMessageRead(messageId)
    }

    fun startEditing(messageId: String) {
        val message = latestMessages.firstOrNull { it.id == messageId } ?: return
        if (message.authorId != SecretBaseUsers.CURRENT_USER_ID) return
        _uiState.update {
            it.copy(
                editingMessageId = messageId,
                editingText = message.content,
            )
        }
    }

    fun updateEditingText(text: String) {
        _uiState.update { it.copy(editingText = text.take(MAX_MESSAGE_LENGTH)) }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                editingMessageId = null,
                editingText = "",
            )
        }
    }

    fun saveEditing() {
        val messageId = _uiState.value.editingMessageId ?: return
        val text = _uiState.value.editingText.trim()
        if (text.isBlank()) {
            emitMessage("留言内容不能为空")
            return
        }

        viewModelScope.launch {
            messageRepository.updateMessage(messageId, text)
                .onSuccess {
                    cancelEditing()
                    emitMessage("留言已更新")
                }
                .onFailure { error ->
                    emitMessage(error.message ?: "更新失败")
                }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
                .onSuccess { emitMessage("留言已删除") }
                .onFailure { error -> emitMessage(error.message ?: "删除失败") }
        }
    }

    fun deleteReply(replyId: String) {
        viewModelScope.launch {
            messageRepository.deleteReply(replyId)
                .onSuccess { emitMessage("回复已删除") }
                .onFailure { error -> emitMessage(error.message ?: "删除失败") }
        }
    }

    fun markMessageRead(messageId: String) {
        viewModelScope.launch {
            messageRepository.markMessageRead(messageId)
        }
    }

    private fun loadVisuals() {
        viewModelScope.launch {
            runCatching { homeRepository.loadSnapshot().visuals }
                .onSuccess {
                    visuals = it
                    updateUi()
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            visuals = HomeVisuals.EMPTY,
                            errorMessage = "加载留言墙失败",
                        )
                    }
                }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                messageRepository.observeMessages(),
                messageRepository.observeDraft(),
            ) { messages, draft -> messages to draft }
                .collectLatest { (messages, draft) ->
                    latestMessages = messages.sortedByDescending { it.createdAt }
                    latestDraft = draft
                    updateUi()
                }
        }
    }

    private fun updateUi() {
        val expandedIds = _uiState.value.expandedReplyMessageIds
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                unreadCount = latestMessages.sumOf { message ->
                    message.unreadCountFor(SecretBaseUsers.CURRENT_USER_ID)
                },
                draftText = latestDraft.content,
                selectedImages = latestDraft.imagePaths,
                visuals = visuals,
                messages = latestMessages.map { message ->
                    message.toUiModel(
                        visuals = visuals,
                        expanded = expandedIds.contains(message.id),
                    )
                },
            )
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }

    companion object {
        private const val MAX_MESSAGE_LENGTH = 500
        private const val MAX_REPLY_LENGTH = 300
        private const val MAX_IMAGE_COUNT = 9

        fun factory(
            homeRepository: HomeRepository,
            messageRepository: MessageRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MessageWallViewModel(
                    homeRepository = homeRepository,
                    messageRepository = messageRepository,
                ) as T
        }
    }
}

private fun Message.toUiModel(
    visuals: HomeVisuals,
    expanded: Boolean,
): MessageUiModel {
    val isMine = authorId == SecretBaseUsers.CURRENT_USER_ID
    val repliesToShow = if (expanded || replies.size <= DEFAULT_VISIBLE_REPLIES) {
        replies
    } else {
        replies.takeLast(DEFAULT_VISIBLE_REPLIES)
    }

    return MessageUiModel(
        id = id,
        authorId = authorId,
        authorName = authorName,
        avatarRes = visuals.avatar(authorId),
        content = content,
        imagePaths = imagePaths,
        timeText = createdAt.toFriendlyTime(),
        statusText = when {
            isMine && counterpartReadAt == null -> "对方未读"
            isMine && counterpartReadAt != null -> "对方已读"
            !isMine && !isRead -> "未读"
            else -> "已读"
        },
        statusHighlight = if (isMine) counterpartReadAt == null else !isRead,
        isMine = isMine,
        isEdited = updatedAt != null,
        replyCount = replies.size,
        visibleReplies = repliesToShow.map { reply ->
            MessageReplyUiModel(
                id = reply.id,
                authorId = reply.authorId,
                authorName = reply.authorName,
                avatarRes = visuals.avatar(reply.authorId),
                content = reply.content,
                timeText = reply.createdAt.toFriendlyTime(),
                canDelete = reply.authorId == SecretBaseUsers.CURRENT_USER_ID,
            )
        },
        hiddenReplyCount = (replies.size - repliesToShow.size).coerceAtLeast(0),
    )
}

private fun Message.unreadCountFor(currentUserId: String): Int {
    val messageUnread = if (authorId != currentUserId && !isRead) 1 else 0
    val replyUnread = replies.count { reply -> reply.authorId != currentUserId && !reply.isRead }
    return messageUnread + replyUnread
}

private fun Long.toFriendlyTime(): String {
    val created = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
    val now = Instant.now().atZone(ZoneId.systemDefault())
    val duration = Duration.between(created, now)
    return when {
        duration.toMinutes() < 1 -> "刚刚"
        duration.toMinutes() < 60 -> "${duration.toMinutes()} 分钟前"
        created.toLocalDate() == now.toLocalDate() -> created.format(DateTimeFormatter.ofPattern("HH:mm"))
        created.toLocalDate().plusDays(1) == now.toLocalDate() -> "昨天 ${created.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        created.year == now.year -> created.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
        else -> created.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
    }
}

private const val DEFAULT_VISIBLE_REPLIES = 3
