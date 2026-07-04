package com.secretbase.app.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.secretbase.app.data.HomeRepository
import com.secretbase.app.data.HomeVisuals
import com.secretbase.app.data.wish.Wish
import com.secretbase.app.data.wish.WishCompletion
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class WishListViewModel(
    private val homeRepository: HomeRepository,
    private val wishRepository: WishRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WishListUiState())
    private val _messages = MutableSharedFlow<String>()
    private var latestWishes: List<Wish> = emptyList()
    private var visuals: HomeVisuals = HomeVisuals.EMPTY

    val uiState: StateFlow<WishListUiState> = _uiState.asStateFlow()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        loadVisuals()
        observeWishes()
    }

    fun selectStatus(status: WishStatus) {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun showAddWish() {
        _uiState.update {
            it.copy(
                editorVisible = true,
                editorWishId = null,
                editorTitle = "",
                editorDescription = "",
                editorPlannedDate = null,
                editorCoverImagePath = null,
            )
        }
    }

    fun editWish(wishId: String) {
        val wish = latestWishes.firstOrNull { it.id == wishId } ?: return
        _uiState.update {
            it.copy(
                editorVisible = true,
                editorWishId = wish.id,
                editorTitle = wish.title,
                editorDescription = wish.description,
                editorPlannedDate = wish.plannedDate,
                editorCoverImagePath = wish.coverImagePath,
            )
        }
    }

    fun dismissEditor() {
        _uiState.update { state ->
            state.copy(
                editorVisible = false,
                editorWishId = null,
                editorTitle = "",
                editorDescription = "",
                editorPlannedDate = null,
                editorCoverImagePath = null,
            )
        }
    }

    fun updateEditorTitle(value: String) {
        _uiState.update { it.copy(editorTitle = value.take(TITLE_LIMIT)) }
    }

    fun updateEditorDescription(value: String) {
        _uiState.update { it.copy(editorDescription = value.take(DESCRIPTION_LIMIT)) }
    }

    fun updateEditorPlannedDate(value: Long?) {
        _uiState.update { it.copy(editorPlannedDate = value) }
    }

    fun updateEditorCover(value: String?) {
        _uiState.update { it.copy(editorCoverImagePath = value) }
    }

    fun saveWish() {
        val state = _uiState.value
        val title = state.editorTitle.trim()
        if (title.isBlank()) {
            emitMessage("愿望标题不能为空")
            return
        }
        val wish = Wish(
            id = state.editorWishId ?: "wish-${UUID.randomUUID()}",
            title = title,
            description = state.editorDescription.trim(),
            coverImagePath = state.editorCoverImagePath,
            plannedDate = state.editorPlannedDate,
            createdAt = latestWishes.firstOrNull { it.id == state.editorWishId }?.createdAt ?: System.currentTimeMillis(),
            status = latestWishes.firstOrNull { it.id == state.editorWishId }?.status ?: WishStatus.UNREALIZED,
            completion = latestWishes.firstOrNull { it.id == state.editorWishId }?.completion,
        )
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (state.editorWishId == null) {
                wishRepository.addWish(wish)
            } else {
                wishRepository.updateWish(wish)
            }
            result.onSuccess {
                emitMessage(if (state.editorWishId == null) "愿望已添加" else "愿望已更新")
                dismissEditor()
            }.onFailure { error ->
                emitMessage(error.message ?: "保存失败")
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun deleteWish(wishId: String) {
        viewModelScope.launch {
            wishRepository.deleteWish(wishId)
                .onSuccess { emitMessage("愿望已删除") }
                .onFailure { error -> emitMessage(error.message ?: "删除失败") }
        }
    }

    fun startCompletion(wishId: String) {
        _uiState.update {
            it.copy(
                completionWishId = wishId,
                completionText = "",
                completionImagePaths = emptyList(),
                completionDate = System.currentTimeMillis(),
            )
        }
    }

    fun resetCompletionDraft() {
        _uiState.update {
            it.copy(
                completionWishId = null,
                completionText = "",
                completionImagePaths = emptyList(),
                completionDate = System.currentTimeMillis(),
            )
        }
    }

    fun updateCompletionText(value: String) {
        _uiState.update { it.copy(completionText = value.take(DESCRIPTION_LIMIT)) }
    }

    fun addCompletionImages(paths: List<String>) {
        _uiState.update { state ->
            state.copy(completionImagePaths = (state.completionImagePaths + paths).distinct().take(MAX_IMAGES))
        }
    }

    fun removeCompletionImage(path: String) {
        _uiState.update { state ->
            state.copy(completionImagePaths = state.completionImagePaths.filterNot { it == path })
        }
    }

    fun updateCompletionDate(value: Long) {
        _uiState.update { it.copy(completionDate = value) }
    }

    fun saveCompletion(wishId: String, onSaved: () -> Unit) {
        val state = _uiState.value
        val text = state.completionText.trim()
        val images = state.completionImagePaths
        if (text.isBlank() && images.isEmpty()) {
            emitMessage("完成感想和照片至少填写一种")
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            wishRepository.completeWish(
                wishId = wishId,
                completion = WishCompletion(
                    text = text,
                    imagePaths = images,
                    completedAt = state.completionDate,
                ),
            ).onSuccess {
                emitMessage("愿望已放进已实现清单")
                resetCompletionDraft()
                _uiState.update { it.copy(selectedStatus = WishStatus.REALIZED) }
                onSaved()
            }.onFailure { error ->
                emitMessage(error.message ?: "保存完成记录失败")
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun wishById(wishId: String): WishUiModel? =
        _uiState.value.wishes.firstOrNull { it.id == wishId }

    private fun observeWishes() {
        viewModelScope.launch {
            wishRepository.observeWishes().collectLatest { wishes ->
                latestWishes = wishes
                updateUi()
            }
        }
    }

    private fun loadVisuals() {
        viewModelScope.launch {
            try {
                visuals = homeRepository.loadSnapshot().visuals
                updateUi()
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "加载愿望清单失败") }
            }
        }
    }

    private fun updateUi() {
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                visuals = visuals,
                wishes = latestWishes
                    .sortedWith(compareBy<Wish> { it.status }.thenByDescending { it.createdAt })
                    .map { it.toUiModel() },
                unrealizedCount = latestWishes.count { it.status == WishStatus.UNREALIZED },
                realizedCount = latestWishes.count { it.status == WishStatus.REALIZED },
            )
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }

    companion object {
        private const val TITLE_LIMIT = 50
        private const val DESCRIPTION_LIMIT = 500
        private const val MAX_IMAGES = 9

        fun factory(
            homeRepository: HomeRepository,
            wishRepository: WishRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WishListViewModel(
                    homeRepository = homeRepository,
                    wishRepository = wishRepository,
                ) as T
        }
    }
}

private fun Wish.toUiModel(): WishUiModel =
    WishUiModel(
        id = id,
        title = title,
        description = description,
        coverImagePath = coverImagePath,
        plannedDateText = plannedDate?.toDateText(),
        createdAtText = createdAt.toDateText(),
        status = status,
        completionDateText = completion?.completedAt?.toDateText(),
        completionSummary = completion?.text?.takeIf { it.isNotBlank() },
        completionImagePaths = completion?.imagePaths.orEmpty(),
    )

private fun Long.toDateText(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
