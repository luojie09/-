package com.secretbase.app.ui.wishlist

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.secretbase.app.MainBottomTabBar
import com.secretbase.app.data.wish.WishStatus
import com.secretbase.app.ui.home.buildPreviewHomeVisuals
import com.secretbase.app.ui.theme.SecretBaseTheme

@PreviewTest
@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Wish List Normal 393x852")
@Composable
fun WishListScreenshotNormal393() = WishListScreenshotContent(empty = false)

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Wish List Normal 360x800")
@Composable
fun WishListScreenshotNormal360() = WishListScreenshotContent(empty = false)

@PreviewTest
@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Wish List Empty 393x852")
@Composable
fun WishListScreenshotEmpty393() = WishListScreenshotContent(empty = true)

@Composable
private fun WishListScreenshotContent(empty: Boolean) {
    val wishes = if (empty) emptyList() else previewWishes()
    SecretBaseTheme {
        WishListScreen(
            uiState = WishListUiState(
                isLoading = false,
                visuals = buildPreviewHomeVisuals(),
                wishes = wishes,
                selectedStatus = WishStatus.UNREALIZED,
                unrealizedCount = wishes.count { it.status == WishStatus.UNREALIZED },
                realizedCount = wishes.count { it.status == WishStatus.REALIZED },
            ),
            snackbarHostState = remember { SnackbarHostState() },
            bottomBar = {
                MainBottomTabBar(activeRoute = "wish-list", onSelect = {})
            },
            onBack = {},
            onSelectStatus = {},
            onAddWish = {},
            onWishClick = {},
            onEditWish = {},
            onDeleteWish = {},
            onCompleteWish = {},
            onEditorTitleChange = {},
            onEditorDescriptionChange = {},
            onEditorPlannedDateChange = {},
            onEditorCoverChange = {},
            onDismissEditor = {},
            onSaveWish = {},
        )
    }
}

private fun previewWishes(): List<WishUiModel> = listOf(
    WishUiModel(
        id = "wish-preview-1",
        title = "一起去旅行",
        description = "去看海，也去我们从未去过的城市。",
        coverImagePath = null,
        plannedDateText = "2026.12.31",
        createdAtText = "2026.06.18",
        status = WishStatus.UNREALIZED,
        completionDateText = null,
        completionSummary = null,
        completionImagePaths = emptyList(),
    ),
    WishUiModel(
        id = "wish-preview-2",
        title = "拍一组情侣写真",
        description = "把镜头里的笑也好好珍藏。",
        coverImagePath = null,
        plannedDateText = "2026.08.31",
        createdAtText = "2026.06.20",
        status = WishStatus.UNREALIZED,
        completionDateText = null,
        completionSummary = null,
        completionImagePaths = emptyList(),
    ),
    WishUiModel(
        id = "wish-preview-3",
        title = "一起看日出",
        description = "去海边看一场属于我们的清晨。",
        coverImagePath = null,
        plannedDateText = "2026.07.30",
        createdAtText = "2026.06.22",
        status = WishStatus.UNREALIZED,
        completionDateText = null,
        completionSummary = null,
        completionImagePaths = emptyList(),
    ),
)
