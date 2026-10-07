package com.secretbase.app.ui.messagewall

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MinimalMessageCard(
    message: MessageUiModel,
    onReplyClick: () -> Unit,
    onToggleReplies: () -> Unit,
    onDeleteMessage: () -> Unit,
    onDeleteReply: (String) -> Unit,
    onStartEditing: () -> Unit,
    onLikeClick: () -> Unit,
    onImageClick: () -> Unit,
    onCardOpened: () -> Unit,
) {
    var showActions by remember(message.id) { mutableStateOf(false) }
    var confirmDeleteMessage by remember(message.id) { mutableStateOf(false) }
    var confirmDeleteReplyId by remember(message.id) { mutableStateOf<String?>(null) }
    var viewerIndex by remember(message.id) { mutableIntStateOf(-1) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCardOpened,
            ),
        color = SurfaceWhite,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                AvatarBubble(
                    avatarRes = message.avatarRes,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = message.authorName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = InkBlack,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = message.timeText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = WarmGray,
                        )
                        if (message.isEdited) {
                            Text(
                                text = "已编辑",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = WarmGray,
                            )
                        }
                        message.readStatusText?.let { status ->
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (message.isUnread) CherryPink else WarmGray,
                            )
                        }
                    }
                }
                Box {
                    IconButton(
                        modifier = Modifier.size(40.dp),
                        onClick = { showActions = true },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreHoriz,
                            contentDescription = "更多操作",
                            tint = WarmGray.copy(alpha = 0.72f),
                            modifier = Modifier.size(19.dp),
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
                                    onCardOpened()
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
                        fontWeight = FontWeight.Medium,
                    ),
                    color = InkBlack,
                )
            }

            if (message.imagePaths.isNotEmpty()) {
                MessageImageGrid(
                    imagePaths = message.imagePaths,
                    maxVisibleImages = 4,
                    modifier = Modifier.fillMaxWidth(),
                    onImageClick = { index ->
                        viewerIndex = index
                        onImageClick()
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedLikeAction(
                    label = if (message.likeCount > 0) "赞 ${message.likeCount}" else "赞",
                    selected = message.isLiked,
                    onClick = {
                        onLikeClick()
                        onCardOpened()
                    },
                )
                MinimalMessageAction(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = if (message.replyCount > 0) "评论 ${message.replyCount}" else "评论",
                    tint = WarmGray,
                    onClick = {
                        onReplyClick()
                        onCardOpened()
                    },
                )
            }

            FeedReplyPreview(
                message = message,
                onOpenReply = {
                    onReplyClick()
                    onCardOpened()
                },
                onToggleReplies = onToggleReplies,
                onDeleteReply = { confirmDeleteReplyId = it },
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
private fun AnimatedLikeAction(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(
            durationMillis = if (isPressed) 70 else 150,
            easing = FastOutSlowInEasing,
        ),
        label = "like-press-scale",
    )
    val heartColor by animateColorAsState(
        targetValue = if (selected) CherryPink else WarmGray,
        animationSpec = tween(durationMillis = 180),
        label = "like-color",
    )
    val heartScale = remember { Animatable(1f) }
    val burstProgress = remember { Animatable(1f) }
    var previousSelected by remember { mutableStateOf(selected) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(selected) {
        val shouldCelebrate = selected && !previousSelected
        previousSelected = selected
        if (shouldCelebrate) {
            heartScale.snapTo(0.72f)
            burstProgress.snapTo(0f)
            coroutineScope {
                launch {
                    heartScale.animateTo(
                        targetValue = 1f,
                        animationSpec = keyframes {
                            durationMillis = 430
                            1.28f at 140 using LinearOutSlowInEasing
                            0.94f at 270 using FastOutSlowInEasing
                            1f at 430
                        },
                    )
                }
                launch {
                    burstProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 390,
                            easing = LinearOutSlowInEasing,
                        ),
                    )
                }
            }
        } else if (!selected) {
            heartScale.snapTo(1f)
            burstProgress.snapTo(1f)
        }
    }

    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (!selected) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onClick()
                },
            )
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(26.dp)) {
                val progress = burstProgress.value
                if (progress < 1f) {
                    val fade = (1f - progress).coerceIn(0f, 1f)
                    val centerRadius = size.minDimension * (0.2f + 0.3f * progress)
                    drawCircle(
                        color = CherryPink.copy(alpha = fade * 0.42f),
                        radius = centerRadius,
                        style = Stroke(width = 1.4.dp.toPx()),
                    )
                    repeat(8) { index ->
                        val angle = (2.0 * PI * index / 8.0) - (PI / 2.0)
                        val distance = size.minDimension * (0.25f + 0.24f * progress)
                        drawCircle(
                            color = CherryPink.copy(alpha = fade * 0.76f),
                            radius = (1.7.dp.toPx() * fade).coerceAtLeast(0.35.dp.toPx()),
                            center = center + androidx.compose.ui.geometry.Offset(
                                x = (cos(angle) * distance).toFloat(),
                                y = (sin(angle) * distance).toFloat(),
                            ),
                        )
                    }
                }
            }
            Icon(
                imageVector = if (selected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = label,
                tint = heartColor,
                modifier = Modifier
                    .size(18.dp)
                    .scale(pressScale * heartScale.value),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = if (selected) CherryPink else WarmGray,
        )
    }
}

@Composable
private fun MinimalMessageAction(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    selected: Boolean = false,
    iconScale: Float = 1f,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
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
private fun FeedReplyPreview(
    message: MessageUiModel,
    onOpenReply: () -> Unit,
    onToggleReplies: () -> Unit,
    onDeleteReply: (String) -> Unit,
) {
    if (message.replyCount == 0) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFF5F3),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            message.visibleReplies.forEach { reply ->
                MomentCommentLine(
                    reply = reply,
                    onClick = onOpenReply,
                    onDelete = { onDeleteReply(reply.id) },
                )
            }
            if (message.hiddenReplyCount > 0) {
                Text(
                    text = "查看全部 ${message.replyCount} 条评论",
                    modifier = Modifier
                        .clickable(onClick = onToggleReplies)
                        .padding(top = 2.dp, bottom = 1.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = WarmGray,
                )
            } else if (message.replyCount > 3) {
                Text(
                    text = "收起评论",
                    modifier = Modifier
                        .clickable(onClick = onToggleReplies)
                        .padding(top = 2.dp, bottom = 1.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = WarmGray,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MomentCommentLine(
    reply: MessageReplyUiModel,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(reply.authorName)
            }
            append("：")
            append(reply.content)
        },
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (reply.canDelete) onDelete else null,
            )
            .padding(vertical = 1.dp),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Normal,
            lineHeight = 20.sp,
        ),
        color = InkBlack,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageWallReplyBar(
    value: String,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        focusRequester.requestFocus()
        bringIntoViewRequester.bringIntoView()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester),
        color = SurfaceWhite,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 5.dp,
        border = null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "取消评论",
                    tint = WarmGray,
                    modifier = Modifier.size(19.dp),
                )
            }
            Surface(
                modifier = Modifier.weight(1f),
                color = Color(0xFFFFF5F3),
                shape = RoundedCornerShape(16.dp),
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = InkBlack,
                        fontWeight = FontWeight.Medium,
                    ),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (value.isEmpty()) {
                                Text(
                                    text = "评论…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WarmGray,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
            TextButton(
                onClick = onSend,
                enabled = value.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    text = "发送",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (value.isNotBlank()) CherryPink else WarmGray.copy(alpha = 0.56f),
                )
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
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(20.dp),
    )
}
