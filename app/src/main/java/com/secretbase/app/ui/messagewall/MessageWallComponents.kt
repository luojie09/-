package com.secretbase.app.ui.messagewall

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.RemoveCircle
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.OutlinePink
import com.secretbase.app.ui.theme.ShadowPink
import com.secretbase.app.ui.theme.SoftPink
import com.secretbase.app.ui.theme.SoftPinkStrong
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmGray

@Composable
fun MessageCard(
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCardOpened,
            ),
        color = Color(0xFFFFFDFC),
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(22.dp),
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
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top,
                ) {
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
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = WarmGray,
                        )
                    }

                    if (message.isMine) {
                        Box {
                            IconButton(
                                modifier = Modifier.size(32.dp),
                                onClick = { showActions = true },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreHoriz,
                                    contentDescription = "更多操作",
                                    tint = WarmGray,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = showActions,
                                onDismissRequest = { showActions = false },
                            ) {
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
                            }
                        }
                    }
                }
            }

            if (message.content.isNotBlank()) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.titleMedium.copy(
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
                    modifier = Modifier.fillMaxWidth(),
                    onImageClick = { index ->
                        viewerIndex = index
                        onImageClick()
                    },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF3EDEF)),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MessageAction(
                    icon = Icons.Outlined.FavoriteBorder,
                    label = "赞",
                    tint = CherryPink,
                    onClick = onCardOpened,
                )
                MessageAction(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = if (message.replyCount > 0) "评论 ${message.replyCount}" else "评论",
                    tint = WarmGray,
                    onClick = onReplyClick,
                )
            }

            ReplySection(
                message = message,
                isReplyActive = isReplyActive,
                replyText = replyText,
                onReplyTextChange = onReplyTextChange,
                onSendReply = onSendReply,
                onCancelReply = onCancelReply,
                onDeleteReply = { confirmDeleteReplyId = it },
                onToggleReplies = onToggleReplies,
            )
        }
    }

    if (confirmDeleteMessage) {
        ConfirmationDialog(
            title = "删除这条留言？",
            message = "删除后，这条留言、它的回复以及图片引用都会一起移除。",
            confirmText = "删除",
            onDismiss = { confirmDeleteMessage = false },
            onConfirm = {
                confirmDeleteMessage = false
                onDeleteMessage()
            },
        )
    }

    confirmDeleteReplyId?.let { replyId ->
        ConfirmationDialog(
            title = "删除这条回复？",
            message = "删除后无法恢复。",
            confirmText = "删除",
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
private fun MessageAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = WarmGray,
        )
    }
}

@Composable
private fun ReplySection(
    message: MessageUiModel,
    isReplyActive: Boolean,
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onSendReply: () -> Unit,
    onCancelReply: () -> Unit,
    onDeleteReply: (String) -> Unit,
    onToggleReplies: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (message.replyCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF2EEF1)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                message.visibleReplies.forEach { reply ->
                    ReplyItem(
                        reply = reply,
                        onDelete = { onDeleteReply(reply.id) },
                    )
                }
                if (message.hiddenReplyCount > 0) {
                    Text(
                        text = "查看全部 ${message.replyCount} 条评论",
                        modifier = Modifier.clickable(onClick = onToggleReplies),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = WarmGray,
                    )
                } else if (message.replyCount > 3) {
                    Text(
                        text = "收起评论",
                        modifier = Modifier.clickable(onClick = onToggleReplies),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = WarmGray,
                    )
                }
            }
        }

        if (isReplyActive) {
            ReplyComposer(
                value = replyText,
                onValueChange = onReplyTextChange,
                onCancel = onCancelReply,
                onSend = onSendReply,
            )
        }
    }
}

@Composable
private fun ReplyItem(
    reply: MessageReplyUiModel,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = reply.authorName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = InkBlack,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = reply.timeText,
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (reply.canDelete) {
                Text(
                    text = "删除",
                    modifier = Modifier.clickable(onClick = onDelete),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = WarmGray,
                )
            }
        }
        Text(
            text = reply.content,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                lineHeight = 25.sp,
            ),
            color = InkBlack,
        )
    }
}

@Composable
private fun ReplyComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFF0EAEE)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                }
            )
            TextButton(onClick = onCancel) {
                Text("取消", color = WarmGray)
            }
            Surface(
                onClick = onSend,
                enabled = value.isNotBlank(),
                shape = CircleShape,
                color = if (value.isNotBlank()) CherryPink else Color(0xFFF1EBEE),
            ) {
                Box(
                    modifier = Modifier.size(34.dp),
                    contentAlignment = Alignment.Center,
                ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "发送回复",
                    tint = if (value.isNotBlank()) SurfaceWhite else WarmGray.copy(alpha = 0.62f),
                    modifier = Modifier.size(16.dp),
                )
                }
            }
        }
    }
}

@Composable
fun DraftInputCard(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "写点想对TA说的话…",
    minHeight: androidx.compose.ui.unit.Dp = 116.dp,
) {
    Surface(
        color = Color(0xFFFDF9FA),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.66f)),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = InkBlack,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 26.sp,
            ),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = WarmGray,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
fun SelectedImageStrip(
    images: List<String>,
    onRemove: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        images.forEach { imagePath ->
            Box {
                MessageMedia(
                    imagePath = imagePath,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(22.dp)),
                )
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp),
                    onClick = { onRemove(imagePath) },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RemoveCircle,
                        contentDescription = "移除图片",
                        tint = CherryPink,
                    )
                }
            }
        }
    }
}

@Composable
fun ComposerImageAction(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = SurfaceWhite.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.68f)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(SoftPink.copy(alpha = 0.42f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = "添加图片",
                    tint = CherryPink,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = "添加照片",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = InkBlack,
            )
        }
    }
}

@Composable
fun PublishPillButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (enabled) CherryPink else Color(0xFFECE6E8),
        shadowElevation = if (enabled) 1.dp else 0.dp,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) CherryPink.copy(alpha = 0.14f) else OutlinePink.copy(alpha = 0.7f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) SurfaceWhite else WarmGray,
            )
        }
    }
}

@Composable
fun WallCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(40.dp),
        color = SurfaceWhite,
        shape = CircleShape,
        shadowElevation = 0.dp,
        border = null,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun WallIllustration(
    illustrationRes: Int?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, OutlinePink.copy(alpha = 0.28f), RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFF8FA),
                        Color(0xFFFFFCFC),
                    ),
                ),
            ),
    ) {
        if (illustrationRes != null) {
            Image(
                painter = painterResource(illustrationRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
fun AvatarBubble(
    avatarRes: Int?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = SurfaceWhite,
        shape = CircleShape,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFF4EEF1)),
    ) {
        if (avatarRes != null) {
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun MessageImageGrid(
    imagePaths: List<String>,
    modifier: Modifier = Modifier,
    onImageClick: (Int) -> Unit,
) {
    if (imagePaths.size == 1) {
        MessageMedia(
            imagePath = imagePaths.first(),
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onImageClick(0) },
        )
    } else {
        val columns = when (imagePaths.size) {
            2 -> 2
            3, 4 -> 2
            else -> 3
        }
        val rows = imagePaths.chunked(columns)

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEachIndexed { columnIndex, imagePath ->
                        val imageIndex = rowIndex * columns + columnIndex
                        MessageMedia(
                            imagePath = imagePath,
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onImageClick(imageIndex) },
                        )
                    }
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f, fill = true))
                    }
                }
            }
        }
    }
}

@Composable
fun MessageMedia(
    imagePath: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    backgroundColor: Color = Color(0xFFFFF7FA),
) {
    val mockPalette = remember(imagePath) { mockPaletteFor(imagePath) }
    if (mockPalette != null) {
        MockSceneImage(
            palette = mockPalette,
            modifier = modifier,
        )
    } else {
        AsyncImage(
            model = imagePath,
            contentDescription = null,
            modifier = modifier.background(backgroundColor),
            contentScale = contentScale,
        )
    }
}

@Composable
private fun MockSceneImage(
    palette: MockPalette,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(palette.start, palette.end),
            ),
        ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = palette.glow.copy(alpha = 0.22f),
                radius = size.minDimension * 0.52f,
                center = Offset(size.width * 0.8f, size.height * 0.2f),
            )
            repeat(7) { index ->
                val fraction = (index + 1) / 8f
                drawCircle(
                    color = palette.spark.copy(alpha = 0.26f),
                    radius = size.minDimension * (0.03f + 0.015f * (index % 3)),
                    center = Offset(
                        size.width * (0.12f + fraction * 0.75f),
                        size.height * (0.18f + (index % 4) * 0.16f),
                    ),
                )
            }
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        palette.overlay,
                    ),
                ),
                topLeft = Offset(0f, size.height * 0.52f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.48f),
            )
        }
        Text(
            text = palette.label,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = SurfaceWhite.copy(alpha = 0.92f),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageImageViewer(
    imagePaths: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit,
) {
    if (imagePaths.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, imagePaths.lastIndex),
        pageCount = { imagePaths.size },
    )
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val previousStatusBarColor = window?.statusBarColor
        val previousNavigationBarColor = window?.navigationBarColor
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatusBars = insetsController?.isAppearanceLightStatusBars
        val previousLightNavigationBars = insetsController?.isAppearanceLightNavigationBars

        window?.statusBarColor = AndroidColor.BLACK
        window?.navigationBarColor = AndroidColor.BLACK
        insetsController?.isAppearanceLightStatusBars = false
        insetsController?.isAppearanceLightNavigationBars = false

        onDispose {
            if (previousStatusBarColor != null) {
                window.statusBarColor = previousStatusBarColor
            }
            if (previousNavigationBarColor != null) {
                window.navigationBarColor = previousNavigationBarColor
            }
            if (previousLightStatusBars != null) {
                insetsController?.isAppearanceLightStatusBars = previousLightStatusBars
            }
            if (previousLightNavigationBars != null) {
                insetsController?.isAppearanceLightNavigationBars = previousLightNavigationBars
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    ZoomableMedia(
                        imagePath = imagePaths[page],
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 22.dp, start = 18.dp, end = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WallCircleButton(
                        icon = Icons.Outlined.Close,
                        contentDescription = "关闭预览",
                        tint = SurfaceWhite,
                        onClick = onDismiss,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${pagerState.currentPage + 1} / ${imagePaths.size}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SurfaceWhite,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(46.dp))
                }
            }
        }
    }
}

@Composable
private fun ZoomableMedia(
    imagePath: String,
    modifier: Modifier = Modifier,
) {
    var scale by remember(imagePath) { mutableStateOf(1f) }
    var offsetX by remember(imagePath) { mutableStateOf(0f) }
    var offsetY by remember(imagePath) { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(imagePath) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2f
                        if (scale == 1f) {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    },
                )
            }
            .pointerInput(imagePath) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedPointers = event.changes.count { it.pressed }
                        if (pressedPointers > 1 || scale > 1f) {
                            val nextScale = (scale * event.calculateZoom()).coerceIn(1f, 4f)
                            val pan = event.calculatePan()
                            scale = nextScale
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                            event.changes.forEach { change ->
                                if (change.pressed) {
                                    change.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        MessageMedia(
            imagePath = imagePath,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
                ,
            contentScale = ContentScale.Fit,
            backgroundColor = Color.Transparent,
        )
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = CherryPink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = WarmGray)
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = SoftPink.copy(alpha = 0.96f),
    )
}

@Composable
fun EditMessageDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("保存", color = CherryPink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = WarmGray)
            }
        },
        title = {
            Text(
                text = "编辑留言",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            )
        },
        text = {
            DraftInputCard(
                value = value,
                onValueChange = onValueChange,
                placeholder = "把这句心里话再润一润…",
                minHeight = 140.dp,
            )
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = SoftPink.copy(alpha = 0.96f),
    )
}

private data class MockPalette(
    val label: String,
    val start: Color,
    val end: Color,
    val glow: Color,
    val spark: Color,
    val overlay: Color,
)

private fun mockPaletteFor(imagePath: String): MockPalette? =
    when (imagePath) {
        "mock://blossom-sky" -> MockPalette(
            label = "樱花",
            start = Color(0xFF7EB5F0),
            end = Color(0xFFFFCDE2),
            glow = Color(0xFFFFF1F6),
            spark = Color(0xFFFFE6F0),
            overlay = Color(0x22FFFFFF),
        )
        "mock://sunset-gold" -> MockPalette(
            label = "日落",
            start = Color(0xFF7A8FD6),
            end = Color(0xFFFFB56B),
            glow = Color(0xFFFFE0B8),
            spark = Color(0xFFFFD6A4),
            overlay = Color(0x3383462C),
        )
        "mock://sunset-shore" -> MockPalette(
            label = "海边",
            start = Color(0xFFF5B48A),
            end = Color(0xFF7EC8E3),
            glow = Color(0xFFFFEDC7),
            spark = Color(0xFFFFD8B5),
            overlay = Color(0x263F6984),
        )
        "mock://wish-paris" -> MockPalette(
            label = "旅程",
            start = Color(0xFF7CC7F7),
            end = Color(0xFFFFC7D7),
            glow = Color(0xFFF7F7FF),
            spark = Color(0xFFE8F5FF),
            overlay = Color(0x2C7F4E68),
        )
        "mock://wish-photo" -> MockPalette(
            label = "写真",
            start = Color(0xFFD8A7B8),
            end = Color(0xFFFFE0C8),
            glow = Color(0xFFFFF3F8),
            spark = Color(0xFFFFE5EF),
            overlay = Color(0x24684A5C),
        )
        "mock://wish-game" -> MockPalette(
            label = "冒险",
            start = Color(0xFF98CC8B),
            end = Color(0xFFCEE6A7),
            glow = Color(0xFFF4FFE9),
            spark = Color(0xFFE3F6C1),
            overlay = Color(0x244C6B49),
        )
        "mock://wish-sunrise",
        "mock://wish-sunrise-1",
        "mock://wish-sunrise-2",
        "mock://wish-sunrise-3" -> MockPalette(
            label = "日出",
            start = Color(0xFF87A7EA),
            end = Color(0xFFFFC6A2),
            glow = Color(0xFFFFE9CA),
            spark = Color(0xFFFFDAB8),
            overlay = Color(0x284A5D8A),
        )
        "mock://wish-cake",
        "mock://wish-cake-1" -> MockPalette(
            label = "蛋糕",
            start = Color(0xFFFFD7E7),
            end = Color(0xFFFFE9B7),
            glow = Color(0xFFFFFAF2),
            spark = Color(0xFFFFE5F0),
            overlay = Color(0x227A5361),
        )
        "mock://wish-movie",
        "mock://wish-movie-1",
        "mock://wish-movie-2" -> MockPalette(
            label = "电影",
            start = Color(0xFF5E536F),
            end = Color(0xFFD39A74),
            glow = Color(0xFFFCE3C9),
            spark = Color(0xFFE8B98B),
            overlay = Color(0x2E2A2338),
        )
        "mock://wish-default" -> MockPalette(
            label = "回忆",
            start = Color(0xFFF1C7D6),
            end = Color(0xFFFDE7C9),
            glow = Color(0xFFFFF6FB),
            spark = Color(0xFFFFE3EE),
            overlay = Color(0x226E5361),
        )
        else -> null
    }
