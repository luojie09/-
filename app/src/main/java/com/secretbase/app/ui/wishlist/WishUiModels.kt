package com.secretbase.app.ui.wishlist

import androidx.annotation.DrawableRes
import com.secretbase.app.data.HomeVisuals
import com.secretbase.app.data.wish.WishStatus

data class WishListUiState(
    val isLoading: Boolean = true,
    val visuals: HomeVisuals = HomeVisuals.EMPTY,
    val wishes: List<WishUiModel> = emptyList(),
    val selectedStatus: WishStatus = WishStatus.UNREALIZED,
    val unrealizedCount: Int = 0,
    val realizedCount: Int = 0,
    val editorVisible: Boolean = false,
    val editorWishId: String? = null,
    val editorTitle: String = "",
    val editorDescription: String = "",
    val editorPlannedDate: Long? = null,
    val editorCoverImagePath: String? = null,
    val completionWishId: String? = null,
    val completionText: String = "",
    val completionImagePaths: List<String> = emptyList(),
    val completionDate: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

data class WishUiModel(
    val id: String,
    val title: String,
    val description: String,
    val coverImagePath: String?,
    val plannedDateText: String?,
    val createdAtText: String,
    val status: WishStatus,
    val completionDateText: String?,
    val completionSummary: String?,
    val completionImagePaths: List<String>,
)
