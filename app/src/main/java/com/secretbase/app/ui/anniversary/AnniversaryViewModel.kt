package com.secretbase.app.ui.anniversary

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.secretbase.app.data.HomeRepository
import com.secretbase.app.data.HomeVisuals
import com.secretbase.app.data.anniversary.Anniversary
import com.secretbase.app.data.anniversary.AnniversaryNotificationScheduler
import com.secretbase.app.data.anniversary.AnniversaryReminder
import com.secretbase.app.data.anniversary.AnniversaryRepository
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

class AnniversaryViewModel(
    private val homeRepository: HomeRepository,
    private val anniversaryRepository: AnniversaryRepository,
    private val notificationScheduler: AnniversaryNotificationScheduler? = null,
    private val todayProvider: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnniversaryUiState())
    private val _messages = MutableSharedFlow<String>()
    private var latestItems: List<Anniversary> = emptyList()
    private var relationshipStart: LocalDate = todayProvider()
    private var visuals: HomeVisuals = HomeVisuals.EMPTY

    val uiState: StateFlow<AnniversaryUiState> = _uiState.asStateFlow()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        loadSnapshot()
        observeAnniversaries()
    }

    fun showAddEditor() {
        _uiState.update {
            it.copy(
                editorVisible = true,
                editingId = null,
                title = "",
                date = System.currentTimeMillis(),
                iconEmoji = DefaultAnniversaryEmoji,
                repeatYearly = true,
                reminderType = AnniversaryReminder.NONE,
            )
        }
    }

    fun editItem(id: String) {
        val item = latestItems.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                editorVisible = true,
                editingId = item.id,
                title = item.title,
                date = item.date,
                iconEmoji = item.iconEmoji ?: defaultAnniversaryEmoji(item.title),
                repeatYearly = item.repeatYearly,
                reminderType = item.reminderType,
            )
        }
    }

    fun dismissEditor() {
        _uiState.update {
            it.copy(
                editorVisible = false,
                editingId = null,
                title = "",
                date = null,
                iconEmoji = DefaultAnniversaryEmoji,
                repeatYearly = true,
                reminderType = AnniversaryReminder.NONE,
            )
        }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value.take(50)) }
    }

    fun updateDate(value: Long) {
        _uiState.update { it.copy(date = value) }
    }

    fun updateIconEmoji(value: String) {
        _uiState.update { it.copy(iconEmoji = value.takeIf { emoji -> emoji in AnniversaryEmojiOptions } ?: DefaultAnniversaryEmoji) }
    }

    fun toggleRepeat(value: Boolean) {
        _uiState.update { it.copy(repeatYearly = value) }
    }

    fun updateReminder(value: AnniversaryReminder) {
        _uiState.update { it.copy(reminderType = value) }
    }

    fun saveEditor() {
        val state = _uiState.value
        val title = state.title.trim()
        val date = state.date
        if (title.isBlank()) {
            emitMessage("纪念日名称不能为空")
            return
        }
        if (date == null) {
            emitMessage("请选择日期")
            return
        }
        val item = Anniversary(
            id = state.editingId ?: "anniversary-${UUID.randomUUID()}",
            title = title,
            date = date,
            repeatYearly = state.repeatYearly,
            reminderType = state.reminderType,
            createdAt = latestItems.firstOrNull { it.id == state.editingId }?.createdAt ?: System.currentTimeMillis(),
            iconEmoji = state.iconEmoji.ifBlank { defaultAnniversaryEmoji(title) },
        )
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (state.editingId == null) {
                anniversaryRepository.addAnniversary(item)
            } else {
                anniversaryRepository.updateAnniversary(item)
            }
            result.onSuccess {
                emitMessage(if (state.editingId == null) "纪念日已添加" else "纪念日已更新")
                dismissEditor()
            }.onFailure { error ->
                emitMessage(error.message ?: "保存失败")
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            anniversaryRepository.deleteAnniversary(id)
                .onSuccess { emitMessage("纪念日已删除") }
                .onFailure { error -> emitMessage(error.message ?: "删除失败") }
        }
    }

    private fun loadSnapshot() {
        viewModelScope.launch {
            try {
                val snapshot = homeRepository.loadSnapshot()
                relationshipStart = snapshot.couple.relationshipStartDate
                visuals = snapshot.visuals
                updateUi()
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "加载纪念日失败，请稍后重试") }
            }
        }
    }

    private fun observeAnniversaries() {
        viewModelScope.launch {
            anniversaryRepository.observeAnniversaries().collectLatest { items ->
                runCatching {
                    latestItems = items
                    notificationScheduler?.sync(items)
                    updateUi()
                }.onFailure { error ->
                    Log.e(TAG, "Failed to render anniversary state", error)
                    _uiState.update { it.copy(isLoading = false, errorMessage = "加载纪念日失败，请稍后重试") }
                }
            }
        }
    }

    private fun updateUi() {
        val today = todayProvider()
        val relationshipDays = ChronoUnit.DAYS.between(relationshipStart, today).toInt() + 1
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                visuals = visuals,
                relationshipDays = relationshipDays.coerceAtLeast(1),
                relationshipStartText = relationshipStart.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                items = latestItems
                    .sortedWith(compareBy<Anniversary> { anniversarySortBucket(it, today) }
                        .thenBy { anniversaryDistance(it, today) })
                    .map { it.toUiModel(today) },
            )
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }

    companion object {
        private const val TAG = "AnniversaryViewModel"

        fun factory(
            homeRepository: HomeRepository,
            anniversaryRepository: AnniversaryRepository,
            notificationScheduler: AnniversaryNotificationScheduler? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AnniversaryViewModel(
                    homeRepository = homeRepository,
                    anniversaryRepository = anniversaryRepository,
                    notificationScheduler = notificationScheduler,
                ) as T
        }
    }
}

private fun Anniversary.toUiModel(today: LocalDate): AnniversaryUiModel {
    val eventDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
    val effectiveDate = effectiveAnniversaryDate(eventDate, repeatYearly, today)
    val days = ChronoUnit.DAYS.between(today, effectiveDate).toInt()
    val (statusText, tone) = when {
        days == 0 -> "就是今天" to AnniversaryStatusTone.TODAY
        days > 0 -> "还有 $days 天" to AnniversaryStatusTone.UPCOMING
        repeatYearly -> "已过去 ${kotlin.math.abs(days)} 天" to AnniversaryStatusTone.PASSED
        else -> "已过期（不重复）" to AnniversaryStatusTone.EXPIRED
    }
    return AnniversaryUiModel(
        id = id,
        title = title,
        dateText = eventDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
        statusText = statusText,
        statusTone = tone,
        repeatLabel = if (repeatYearly) "每年重复" else "不重复",
        reminderType = reminderType,
        iconEmoji = iconEmoji ?: defaultAnniversaryEmoji(title),
    )
}

private fun anniversarySortBucket(item: Anniversary, today: LocalDate): Int {
    val eventDate = Instant.ofEpochMilli(item.date).atZone(ZoneId.systemDefault()).toLocalDate()
    val effectiveDate = effectiveAnniversaryDate(eventDate, item.repeatYearly, today)
    val days = ChronoUnit.DAYS.between(today, effectiveDate).toInt()
    return when {
        days == 0 -> 0
        days > 0 -> 1
        item.repeatYearly -> 2
        else -> 3
    }
}

private fun anniversaryDistance(item: Anniversary, today: LocalDate): Int {
    val eventDate = Instant.ofEpochMilli(item.date).atZone(ZoneId.systemDefault()).toLocalDate()
    val effectiveDate = effectiveAnniversaryDate(eventDate, item.repeatYearly, today)
    return kotlin.math.abs(ChronoUnit.DAYS.between(today, effectiveDate).toInt())
}

internal fun effectiveAnniversaryDate(
    originalDate: LocalDate,
    repeatYearly: Boolean,
    today: LocalDate,
): LocalDate {
    if (!repeatYearly) return originalDate
    val thisYear = originalDate.atYear(today.year)
    return if (thisYear.isBefore(today)) originalDate.atYear(today.year + 1) else thisYear
}

private fun LocalDate.atYear(year: Int): LocalDate = LocalDate.of(
    year,
    month,
    dayOfMonth.coerceAtMost(YearMonth.of(year, month).lengthOfMonth()),
)
