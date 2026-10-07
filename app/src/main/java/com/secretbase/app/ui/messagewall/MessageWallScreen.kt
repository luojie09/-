package com.secretbase.app.ui.messagewall

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secretbase.app.ui.common.SecretBasePageTopBar
import com.secretbase.app.ui.common.SecretBaseSnackbarHost
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.data.local.PendingMediaStore
import kotlinx.coroutines.launch
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmGray

@Composable
private fun MessageWallPageBackground(
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        content = content,
    )
}

@Composable
fun MessageWallScreen(
    uiState: MessageWallUiState,
    snackbarHostState: SnackbarHostState,
    bottomBar: @Composable () -> Unit = {},
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
    onToggleLike: (String) -> Unit,
    onMarkMessageRead: (String) -> Unit,
    onUpdateEditingText: (String) -> Unit,
    onCancelEditing: () -> Unit,
    onSaveEditing: () -> Unit,
) {
    val listState = rememberLazyListState()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SecretBaseSnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        MessageWallPageBackground {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 36.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    SecretBasePageTopBar(
                        title = "留言墙",
                        onBack = onBack,
                        showBackButton = false,
                        titleAlignStart = true,
                        actionIcon = Icons.Outlined.Edit,
                        actionDescription = "写留言",
                        onActionClick = onOpenEditor,
                    )
                }
                if (uiState.isLoading) {
                    item {
                        MessageWallLoadingState(
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
                if (!uiState.isLoading && uiState.messages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            MessageWallEmptyState(
                                illustrationRes = uiState.visuals.hero.imageRes,
                                onWriteMessage = onOpenEditor,
                            )
                        }
                    }
                } else if (!uiState.isLoading) {
                    items(
                        items = uiState.messages,
                        key = MessageUiModel::id,
                    ) { message ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            MinimalMessageCard(
                                message = message,
                                onReplyClick = {
                                    onReplyClick(message.id)
                                    onMarkMessageRead(message.id)
                                },
                                onToggleReplies = { onToggleReplies(message.id) },
                                onDeleteMessage = { onDeleteMessage(message.id) },
                                onDeleteReply = onDeleteReply,
                                onStartEditing = { onStartEditing(message.id) },
                                onLikeClick = { onToggleLike(message.id) },
                                onImageClick = { onMarkMessageRead(message.id) },
                                onCardOpened = { onMarkMessageRead(message.id) },
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.errorMessage != null && uiState.activeReplyMessageId == null,
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

            AnimatedVisibility(
                visible = uiState.activeReplyMessageId != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = innerPadding.calculateBottomPadding() + 8.dp,
                    ),
            ) {
                MessageWallReplyBar(
                    value = uiState.replyText,
                    onValueChange = onReplyTextChange,
                    onCancel = onCancelReply,
                    onSend = onSendReply,
                )
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
    onDraftTextChange: (String) -> Unit,
    onAddImages: () -> Unit,
    onRemoveSelectedImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editorValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = draftText,
                selection = TextRange(draftText.length),
            ),
        )
    }

    LaunchedEffect(draftText) {
        if (draftText != editorValue.text) {
            editorValue = TextFieldValue(
                text = draftText,
                selection = TextRange(draftText.length),
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BasicTextField(
            value = editorValue,
            onValueChange = { nextValue ->
                editorValue = nextValue
                onDraftTextChange(nextValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = InkBlack,
                fontWeight = FontWeight.Medium,
                lineHeight = 26.sp,
            ),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (editorValue.text.isEmpty()) {
                        Text(
                            text = "这一刻的想法...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = WarmGray,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                    innerTextField()
                }
            }
        )

        EditorSelectedImageGrid(
            images = selectedImages,
            onRemove = onRemoveSelectedImage,
            onAdd = onAddImages,
        )
    }
}

@Composable
private fun MessageEditorTopBar(
    canPublish: Boolean,
    isPublishing: Boolean,
    onBack: () -> Unit,
    onPublish: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "返回",
                tint = InkBlack,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = "写留言",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = InkBlack,
        )
        Text(
            text = if (isPublishing) "发表中" else "发表",
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(
                    enabled = canPublish && !isPublishing,
                    onClick = onPublish,
                )
                .padding(horizontal = 12.dp, vertical = 13.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (canPublish && !isPublishing) CherryPink else WarmGray.copy(alpha = 0.55f),
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
    val context = LocalContext.current
    val mediaScope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            mediaScope.launch {
                runCatching { PendingMediaStore.importAll(context, uris) }
                    .onSuccess(onAddImages)
                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "图片读取失败") }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SecretBaseSnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        MessageWallPageBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                MessageEditorTopBar(
                    canPublish = uiState.draftText.isNotBlank() || uiState.selectedImages.isNotEmpty(),
                    isPublishing = uiState.isPublishing,
                    onBack = onBack,
                    onPublish = onPublish,
                )
                QuickMessageComposer(
                    draftText = uiState.draftText,
                    selectedImages = uiState.selectedImages,
                    onDraftTextChange = onDraftTextChange,
                    onAddImages = { imagePickerLauncher.launch("image/*") },
                    onRemoveSelectedImage = onRemoveSelectedImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .padding(bottom = innerPadding.calculateBottomPadding()),
                )
            }
        }
    }
}

@Composable
private fun MessageWallLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(2) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFFCFB),
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFF4ECEC), RoundedCornerShape(999.dp)),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(12.dp)
                                    .background(Color(0xFFF1E9E9), RoundedCornerShape(999.dp)),
                            )
                            Box(
                                modifier = Modifier
                                    .width(108.dp)
                                    .height(9.dp)
                                    .background(Color(0xFFF5EEEE), RoundedCornerShape(999.dp)),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(Color(0xFFF2EAEA), RoundedCornerShape(999.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(14.dp)
                            .background(Color(0xFFF5EEEE), RoundedCornerShape(999.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorSelectedImageGrid(
    images: List<String>,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val cells = buildList<String?> {
        addAll(images)
        if (images.size < 9) add(null)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cells.chunked(3).forEach { rowImages ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowImages.forEach { imagePath ->
                    if (imagePath == null) {
                        Surface(
                            onClick = onAdd,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            color = Color(0xFFF5F1F2),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 0.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = "添加照片",
                                    tint = WarmGray,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        ) {
                            MessageMedia(
                                imagePath = imagePath,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                            Surface(
                                onClick = { onRemove(imagePath) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(28.dp),
                                shape = RoundedCornerShape(999.dp),
                                color = Color.Black.copy(alpha = 0.48f),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "移除图片",
                                        tint = SurfaceWhite,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                repeat(3 - rowImages.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
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
        Surface(
            onClick = onWriteMessage,
            color = CherryPink,
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 0.dp,
        ) {
            Text(
                text = "写留言",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = SurfaceWhite,
            )
        }
    }
}
