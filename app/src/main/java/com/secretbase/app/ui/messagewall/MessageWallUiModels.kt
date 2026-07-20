package com.secretbase.app.ui.messagewall

import androidx.annotation.DrawableRes
import com.secretbase.app.data.HomeVisuals

data class MessageWallUiState(
    val isLoading: Boolean = true,
    val messages: List<MessageUiModel> = emptyList(),
    val unreadCount: Int = 0,
    val draftText: String = "",
    val selectedImages: List<String> = emptyList(),
    val activeReplyMessageId: String? = null,
    val replyText: String = "",
    val expandedReplyMessageIds: Set<String> = emptySet(),
    val isPublishing: Boolean = false,
    val editingMessageId: String? = null,
    val editingText: String = "",
    val visuals: HomeVisuals = HomeVisuals.EMPTY,
    val errorMessage: String? = null,
)

data class MessageUiModel(
    val id: String,
    val authorId: String,
    val authorName: String,
    @DrawableRes val avatarRes: Int?,
    val content: String,
    val imagePaths: List<String>,
    val timeText: String,
    val isMine: Boolean,
    val isEdited: Boolean,
    val replyCount: Int,
    val likeCount: Int,
    val isLiked: Boolean,
    val visibleReplies: List<MessageReplyUiModel>,
    val hiddenReplyCount: Int,
    val canReply: Boolean = true,
)

data class MessageReplyUiModel(
    val id: String,
    val authorId: String,
    val authorName: String,
    val replyToAuthorName: String? = null,
    @DrawableRes val avatarRes: Int?,
    val content: String,
    val timeText: String,
    val canDelete: Boolean,
)
