package com.secretbase.app.ui.messagewall

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.secretbase.app.ui.common.SecretBasePageBackground
import com.secretbase.app.ui.common.SecretBasePageTopBar
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.OutlinePink
import com.secretbase.app.ui.theme.SoftPink
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmGray
import kotlinx.coroutines.launch

@Composable
fun MessageWallScreen(
    uiState: MessageWallUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDraftTextChange: (String) -> Unit,
    onAddImages: (List<String>) -> Unit,
    onRemoveSelectedImage: (String) -> Unit,
    onPublish: () -> Unit,
    onReplyClick: (String) -> Unit,
    onReplyTextChange: (String) -> Unit,
    onSendReply: () -> Unit,
    onCancelReply: () -> Unit,
    onToggleReplies: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onDeleteReply: (String) -> Unit,
    onStartEditing: (String) -> Unit,
    onMarkMessageRead: (String) -> Unit,
    onUpdateEditingText: (String) -> Unit,
    onCancelEditing: () -> Unit,
    onSaveEditing: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onAddImages(uris.map(Uri::toString))
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SecretBasePageTopBar(
                title = "留言墙",
                onBack = onBack,
                actionIcon = Icons.Outlined.Edit,
                actionDescription = "写留言",
                onActionClick = {
                    scope.launch { listState.animateScrollToItem(COMPOSER_INDEX) }
                },
            )
        },
    ) { innerPadding ->
        SecretBasePageBackground {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 10.dp,
                        bottom = innerPadding.calculateBottomPadding() + 32.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        UnreadMessageBanner(
                            unreadCount = uiState.unreadCount,
                            illustrationRes = uiState.visuals.hero.imageRes,
                        )
                    }

                    item {
                        QuickMessageComposer(
                            draftText = uiState.draftText,
                            selectedImages = uiState.selectedImages,
                            isPublishing = uiState.isPublishing,
                            onDraftTextChange = onDraftTextChange,
                            onAddImages = { imagePickerLauncher.launch("image/*") },
                            onRemoveSelectedImage = onRemoveSelectedImage,
                            onPublish = onPublish,
                        )
                    }

                    if (uiState.messages.isEmpty()) {
                        item {
                            MessageWallEmptyState(
                                illustrationRes = uiState.visuals.hero.imageRes,
                                onWriteMessage = {
                                    scope.launch { listState.animateScrollToItem(COMPOSER_INDEX) }
                                },
                            )
                        }
                    } else {
                        items(
                            items = uiState.messages,
                            key = MessageUiModel::id,
                        ) { message ->
                            MessageCard(
                                message = message,
                                isReplyActive = uiState.activeReplyMessageId == message.id,
                                replyText = if (uiState.activeReplyMessageId == message.id) {
                                    uiState.replyText
                                } else {
                                    ""
                                },
                                onReplyClick = {
                                    onReplyClick(message.id)
                                    onMarkMessageRead(message.id)
                                },
                                onReplyTextChange = onReplyTextChange,
                                onSendReply = onSendReply,
                                onCancelReply = onCancelReply,
                                onToggleReplies = { onToggleReplies(message.id) },
                                onDeleteMessage = { onDeleteMessage(message.id) },
                                onDeleteReply = onDeleteReply,
                                onStartEditing = { onStartEditing(message.id) },
                                onImageClick = { onMarkMessageRead(message.id) },
                                onCardOpened = { onMarkMessageRead(message.id) },
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp),
                ) {
                    uiState.errorMessage?.let { message ->
                        Surface(
                            color = SurfaceWhite,
                            shadowElevation = 10.dp,
                            tonalElevation = 0.dp,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = InkBlack,
                            )
                        }
                    }
                }
        }
    }

    if (uiState.editingMessageId != null) {
        EditMessageDialog(
            value = uiState.editingText,
            onValueChange = onUpdateEditingText,
            onDismiss = onCancelEditing,
            onConfirm = onSaveEditing,
        )
    }
}

@Composable
private fun UnreadMessageBanner(
    unreadCount: Int,
    illustrationRes: Int?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite.copy(alpha = 0.96f),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink.copy(alpha = 0.95f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 16.dp, end = 10.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                    color = SoftPink.copy(alpha = 0.56f),
                ) {
                    Text(
                        text = if (unreadCount > 0) "有新的想念" else "今天也很安静",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = CherryPink,
                    )
                }
                Text(
                    text = buildAnnotatedString {
                        append("你有 ")
                        withStyle(
                            style = SpanStyle(
                                color = CherryPink,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                        ) {
                            append(unreadCount.toString())
                        }
                        append(if (unreadCount > 0) " 条新留言未读" else " 条未读留言")
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = InkBlack,
                )
                Text(
                    text = if (unreadCount > 0) {
                        "有些话，想对你说很久了"
                    } else {
                        "今天的留言都看完啦，去写一句新的悄悄话吧"
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                )
            }
            WallIllustration(
                illustrationRes = illustrationRes,
                modifier = Modifier
                    .size(width = 148.dp, height = 92.dp),
            )
        }
    }
}

@Composable
private fun QuickMessageComposer(
    draftText: String,
    selectedImages: List<String>,
    isPublishing: Boolean,
    onDraftTextChange: (String) -> Unit,
    onAddImages: () -> Unit,
    onRemoveSelectedImage: (String) -> Unit,
    onPublish: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite.copy(alpha = 0.98f),
        shadowElevation = 14.dp,
        tonalElevation = 0.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SoftPink.copy(alpha = 0.8f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "今天想留下点什么？",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = InkBlack,
                )
                Text(
                    text = "一句想念、一张照片，都会变成以后再翻看也会心软的小瞬间。",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }
            DraftInputCard(
                value = draftText,
                onValueChange = onDraftTextChange,
            )
            if (selectedImages.isNotEmpty()) {
                SelectedImageStrip(
                    images = selectedImages,
                    onRemove = onRemoveSelectedImage,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ComposerImageAction(onClick = onAddImages)
                PublishPillButton(
                    text = if (isPublishing) "发布中…" else "发布留言",
                    enabled = !isPublishing,
                    onClick = onPublish,
                )
            }
        }
    }
}

@Composable
private fun MessageWallEmptyState(
    illustrationRes: Int?,
    onWriteMessage: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite.copy(alpha = 0.96f),
        shadowElevation = 14.dp,
        tonalElevation = 0.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlinePink.copy(alpha = 0.95f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WallIllustration(
                illustrationRes = illustrationRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
            )
            Text(
                text = "这里还没有留言",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = InkBlack,
            )
            Text(
                text = "写下第一句只属于我们的悄悄话吧",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
            PublishPillButton(
                text = "写留言",
                enabled = true,
                onClick = onWriteMessage,
            )
        }
    }
}

private const val COMPOSER_INDEX = 1
