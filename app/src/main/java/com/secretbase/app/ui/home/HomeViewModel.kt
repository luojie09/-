package com.secretbase.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.secretbase.app.data.HomeRepository
import com.secretbase.app.data.HomeSnapshot
import com.secretbase.app.data.MoodOption
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HomeRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(HomeUiState())
    private val _messages = MutableSharedFlow<String>()

    private var latestSnapshot: HomeSnapshot? = null

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
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
            repository.addQuickNote(text)
            _uiState.update { it.copy(quickNoteText = "") }
            emitMessage("已记录到最近动态")
            loadSnapshot(showLoading = false)
        }
    }

    fun onMoodCardClick(userId: String) {
        val user = latestSnapshot?.users?.firstOrNull { it.id == userId } ?: return
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
            repository.saveMood(userId, mood)
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

            runCatching { repository.loadSnapshot() }
                .onSuccess { snapshot ->
                    latestSnapshot = snapshot
                    _uiState.value = snapshot.toUiState(
                        now = ZonedDateTime.now(),
                        quickNoteText = _uiState.value.quickNoteText,
                        editingMoodUserId = _uiState.value.editingMoodUserId,
                    )
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = "加载失败，点击重试",
                        )
                    }
                }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }

    companion object {
        private const val MAX_NOTE_LENGTH = 500
    }
}

