package com.secretbase.app.ui.messagewall

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secretbase.app.ui.common.SecretBasePageBackground
import com.secretbase.app.ui.common.SecretBasePageTopBar
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmGray

@Composable
fun MessageWallScreen(
    uiState: MessageWallUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenEditor: () -> Unit,
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

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        SecretBasePageBackground {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 36.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    SecretBasePageTopBar(
                        title = "留言墙",
                        onBack = onBack,
                        actionIcon = Icons.Outlined.Edit,
                        actionDescription = "写留言",
                        onActionClick = onOpenEditor,
                    )
                }
                if (uiState.messages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            MessageWallEmptyState(
                                illustrationRes = uiState.visuals.hero.imageRes,
                                onWriteMessage = onOpenEditor,
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.messages,
                        key = MessageUiModel::id,
                    ) { message ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            MinimalMessageCard(
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
                        shadowElevation = 4.dp,
                        tonalElevation = 0.dp,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
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
private fun QuickMessageComposer(
    draftText: String,
    selectedImages: List<String>,
    isPublishing: Boolean,
    onDraftTextChange: (String) -> Unit,
    onAddImages: () -> Unit,
    onRemoveSelectedImage: (String) -> Unit,
    onPublish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canPublish = !isPublishing && (draftText.isNotBlank() || selectedImages.isNotEmpty())

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFFFFDFC),
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFF3ECEF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicTextField(
                value = draftText,
                onValueChange = onDraftTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 180.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = InkBlack,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 28.sp,
                ),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (draftText.isEmpty()) {
                            Text(
                                text = "写下你想说的......",
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

            if (selectedImages.isNotEmpty()) {
                SelectedImageStrip(
                    images = selectedImages,
                    onRemove = onRemoveSelectedImage,
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
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                EditorPhotoAction(onClick = onAddImages)
                EditorPublishButton(
                    text = if (isPublishing) "发布中…" else "发布",
                    enabled = canPublish,
                    onClick = onPublish,
                )
            }
        }
    }
}

@Composable
private fun EditorPhotoAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = "添加照片",
            tint = WarmGray,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "照片",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = WarmGray,
        )
    }
}

@Composable
private fun EditorPublishButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (enabled) CherryPink else Color(0xFFEDE7EA),
        shape = RoundedCornerShape(999.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) CherryPink.copy(alpha = 0.18f) else Color(0xFFE4DDE1),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (enabled) SurfaceWhite else WarmGray,
        )
    }
}

@Composable
fun MessageWallEditorScreen(
    uiState: MessageWallUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDraftTextChange: (String) -> Unit,
    onAddImages: (List<String>) -> Unit,
    onRemoveSelectedImage: (String) -> Unit,
    onPublish: () -> Unit,
) {
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
    ) { innerPadding ->
        SecretBasePageBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding() + 28.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SecretBasePageTopBar(
                    title = "写留言",
                    onBack = onBack,
                )
                QuickMessageComposer(
                    draftText = uiState.draftText,
                    selectedImages = uiState.selectedImages,
                    isPublishing = uiState.isPublishing,
                    onDraftTextChange = onDraftTextChange,
                    onAddImages = { imagePickerLauncher.launch("image/*") },
                    onRemoveSelectedImage = onRemoveSelectedImage,
                    onPublish = onPublish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .weight(1f),
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WallIllustration(
            illustrationRes = illustrationRes,
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp),
        )
        Text(
            text = "这里还没有留言",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = InkBlack,
        )
        Text(
            text = "写下第一句只属于我们的悄悄话吧",
            style = MaterialTheme.typography.bodyMedium,
            color = WarmGray,
        )
        EditorPublishButton(
            text = "写留言",
            enabled = true,
            onClick = onWriteMessage,
        )
    }
}
