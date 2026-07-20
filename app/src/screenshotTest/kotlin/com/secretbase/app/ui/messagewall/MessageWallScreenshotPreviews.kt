package com.secretbase.app.ui.messagewall

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.secretbase.app.MainBottomTabBar
import com.secretbase.app.R
import com.secretbase.app.ui.home.buildPreviewHomeVisuals
import com.secretbase.app.ui.theme.SecretBaseTheme

@PreviewTest
@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Message Wall Normal 393x852")
@Composable
fun MessageWallScreenshotNormal393() = MessageWallScreenshotContent(empty = false)

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Message Wall Normal 360x800")
@Composable
fun MessageWallScreenshotNormal360() = MessageWallScreenshotContent(empty = false)

@PreviewTest
@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Message Wall Empty 393x852")
@Composable
fun MessageWallScreenshotEmpty393() = MessageWallScreenshotContent(empty = true)

@Composable
private fun MessageWallScreenshotContent(empty: Boolean) {
    SecretBaseTheme {
        MessageWallScreen(
            uiState = MessageWallUiState(
                isLoading = false,
                messages = if (empty) emptyList() else previewMessages(),
                unreadCount = if (empty) 0 else 1,
                visuals = buildPreviewHomeVisuals(),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            bottomBar = {
                MainBottomTabBar(activeRoute = "message-wall", onSelect = {})
            },
            onBack = {},
            onOpenEditor = {},
            onReplyClick = {},
            onReplyTextChange = {},
            onSendReply = {},
            onCancelReply = {},
            onToggleReplies = {},
            onDeleteMessage = {},
            onDeleteReply = {},
            onStartEditing = {},
            onToggleLike = {},
            onMarkMessageRead = {},
            onUpdateEditingText = {},
            onCancelEditing = {},
            onSaveEditing = {},
        )
    }
}

private fun previewMessages(): List<MessageUiModel> = listOf(
    MessageUiModel(
        id = "message-preview-1",
        authorId = "sheep",
        authorName = "小羊",
        avatarRes = R.drawable.avatar_sheep,
        content = "今天看到一朵很美的樱花，第一时间就想分享给你。",
        imagePaths = listOf("mock://blossom-sky"),
        timeText = "今天 15:24",
        isMine = true,
        isEdited = false,
        replyCount = 2,
        likeCount = 1,
        isLiked = true,
        visibleReplies = listOf(
            MessageReplyUiModel(
                id = "reply-preview-1",
                authorId = "chick",
                authorName = "小耶",
                replyToAuthorName = "小羊",
                avatarRes = R.drawable.avatar_chick,
                content = "好想和你一起去看。",
                timeText = "今天 15:30",
                canDelete = false,
            ),
            MessageReplyUiModel(
                id = "reply-preview-2",
                authorId = "sheep",
                authorName = "小羊",
                replyToAuthorName = "小耶",
                avatarRes = R.drawable.avatar_sheep,
                content = "周末一起去呀。",
                timeText = "今天 15:32",
                canDelete = true,
            ),
        ),
        hiddenReplyCount = 0,
    ),
    MessageUiModel(
        id = "message-preview-2",
        authorId = "chick",
        authorName = "小耶",
        avatarRes = R.drawable.avatar_chick,
        content = "回家路上给你带了小蛋糕，记得开门收惊喜。",
        imagePaths = emptyList(),
        timeText = "昨天 20:08",
        isMine = false,
        isEdited = false,
        replyCount = 0,
        likeCount = 0,
        isLiked = false,
        visibleReplies = emptyList(),
        hiddenReplyCount = 0,
    ),
)
