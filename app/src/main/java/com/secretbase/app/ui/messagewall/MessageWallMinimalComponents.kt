package com.secretbase.app.ui.messagewall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.SoftPink
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmGray

@Composable
fun MinimalMessageCard(
    message: MessageUiModel,
    isReplyActive: Boolean,
    replyText: String,
    onReplyClick: () -> Unit,
    onReplyTextChange: (String) -> Unit,
    onSendReply: () -> Unit,
    onCancelReply: () -> Unit,
    onToggleReplies: () -> Unit,
    onDeleteMessage: () -> Unit,
    onDeleteReply: (String) -> Unit,
    onStartEditing: () -> Unit,
    onImageClick: () -> Unit,
    onCardOpened: () -> Unit,
) {
    var showActions by remember(message.id) { mutableStateOf(false) }
    var confirmDeleteMessage by remember(message.id) { mutableStateOf(false) }
    var confirmDeleteReplyId by remember(message.id) { mutableStateOf<String?>(null) }
    var viewerIndex by remember(message.id) { mutableIntStateOf(-1) }
    var liked by remember(message.id) { mutableStateOf(false) }
    val likeScale by animateFloatAsState(
        targetValue = if (liked) 1.16f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "message-like-scale",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCardOpened,
            ),
        color = Color(0xFFFFFDFC),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFF3ECEF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                AvatarBubble(
                    avatarRes = message.avatarRes,
                    modifier = Modifier.size(42.dp),
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = message.authorName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = InkBlack,
                    )
                    Text(
                        text = buildString {
                            append(message.timeText)
                            if (message.isEdited) {
                                append(" · 已编辑")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = WarmGray,
                    )
                }
                Box {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = { showActions = true },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreHoriz,
                            contentDescription = "更多操作",
                            tint = WarmGray.copy(alpha = 0.46f),
                            modifier = Modifier.size(10.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = showActions,
                        onDismissRequest = { showActions = false },
                    ) {
                        if (message.isMine) {
                            DropdownMenuItem(
                                text = { Text("编辑留言") },
                                onClick = {
                                    showActions = false
                                    onStartEditing()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("删除留言", color = CherryPink) },
                                onClick = {
                                    showActions = false
                                    confirmDeleteMessage = true
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("回复留言") },
                                onClick = {
                                    showActions = false
                                    onReplyClick()
                                },
                            )
                        }
                    }
                }
            }

            if (message.content.isNotBlank()) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = InkBlack,
                )
            }

            if (message.imagePaths.isNotEmpty()) {
                MessageImageGrid(
                    imagePaths = message.imagePaths,
                    modifier = Modifier.fillMaxWidth(),
                    onImageClick = { index ->
                        viewerIndex = index
                        onImageClick()
                    },
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MinimalMessageAction(
                    icon = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = "赞",
                    tint = if (liked) CherryPink else WarmGray,
                    selected = liked,
                    iconScale = likeScale,
                    onClick = {
                        liked = !liked
                        onCardOpened()
                    },
                )
                MinimalMessageAction(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = if (message.replyCount > 0) "评论 ${message.replyCount}" else "评论",
                    tint = WarmGray,
                    onClick = onReplyClick,
                )
            }

            MinimalReplySection(
                message = message,
                isReplyActive = isReplyActive,
                replyText = replyText,
                onReplyTextChange = onReplyTextChange,
                onSendReply = onSendReply,
                onCancelReply = onCancelReply,
                onDeleteReply = { confirmDeleteReplyId = it },
                onToggleReplies = onToggleReplies,
                onReplyClick = onReplyClick,
            )
        }
    }

    if (confirmDeleteMessage) {
        MinimalConfirmationDialog(
            title = "删除这条留言？",
            message = "删除后，这条留言和它的回复都会一起移除。",
            onDismiss = { confirmDeleteMessage = false },
            onConfirm = {
                confirmDeleteMessage = false
                onDeleteMessage()
            },
        )
    }

    confirmDeleteReplyId?.let { replyId ->
        MinimalConfirmationDialog(
            title = "删除这条评论？",
            message = "删除后无法恢复。",
            onDismiss = { confirmDeleteReplyId = null },
            onConfirm = {
                confirmDeleteReplyId = null
                onDeleteReply(replyId)
            },
        )
    }

    if (viewerIndex >= 0) {
        MessageImageViewer(
            imagePaths = message.imagePaths,
            initialPage = viewerIndex,
            onDismiss = { viewerIndex = -1 },
        )
    }
}

@Composable
private fun MinimalMessageAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    selected: Boolean = false,
    iconScale: Float = 1f,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier
                .size(18.dp)
                .scale(iconScale),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = if (selected) CherryPink else WarmGray,
        )
    }
}

@Composable
private fun MinimalReplySection(
    message: MessageUiModel,
    isReplyActive: Boolean,
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onSendReply: () -> Unit,
    onCancelReply: () -> Unit,
    onDeleteReply: (String) -> Unit,
    onToggleReplies: () -> Unit,
    onReplyClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (message.replyCount > 0) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, bottom = 6.dp, start = 2.dp)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFEFE7EB)),
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    message.visibleReplies.forEach { reply ->
                        MinimalReplyItem(
                            reply = reply,
                            onDelete = { onDeleteReply(reply.id) },
                            onClick = onReplyClick,
                        )
                    }
                    if (message.hiddenReplyCount > 0) {
                        ReplyTogglePill(
                            text = "查看全部 ${message.replyCount} 条评论",
                            onClick = onToggleReplies,
                        )
                    } else if (message.replyCount > 3) {
                        ReplyTogglePill(
                            text = "收起评论",
                            onClick = onToggleReplies,
                        )
                    }
                }
            }
        }

        if (isReplyActive) {
            MinimalReplyComposer(
                value = replyText,
                onValueChange = onReplyTextChange,
                onCancel = onCancelReply,
                onSend = onSendReply,
            )
        }
    }
}

@Composable
private fun MinimalReplyItem(
    reply: MessageReplyUiModel,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = reply.authorName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = InkBlack,
            )
            reply.replyToAuthorName?.let { targetName ->
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "回复 $targetName",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = CherryPink.copy(alpha = 0.78f),
                )
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = reply.timeText,
                style = MaterialTheme.typography.labelSmall,
                color = WarmGray,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (reply.canDelete) {
                Text(
                    text = "删除",
                    modifier = Modifier.clickable(onClick = onDelete),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = WarmGray,
                )
            }
        }
        Text(
            text = reply.content,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp,
            ),
            color = InkBlack,
        )
    }
}

@Composable
private fun ReplyTogglePill(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = WarmGray,
        )
    }
}

@Composable
private fun MinimalReplyComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = Color(0xFFFAF7F8),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF1EAEE)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = InkBlack,
                    fontWeight = FontWeight.Medium,
                ),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isEmpty()) {
                            Text(
                                text = "回复…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = WarmGray,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            TextButton(onClick = onCancel) {
                Text("取消", color = WarmGray)
            }
            Surface(
                onClick = onSend,
                enabled = value.isNotBlank(),
                shape = CircleShape,
                color = if (value.isNotBlank()) CherryPink else Color(0xFFF0EAED),
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "发送回复",
                        tint = if (value.isNotBlank()) SurfaceWhite else WarmGray.copy(alpha = 0.62f),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalConfirmationDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = CherryPink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = WarmGray)
            }
        },
        containerColor = SoftPink.copy(alpha = 0.96f),
        shape = RoundedCornerShape(16.dp),
    )
}
