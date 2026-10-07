package com.secretbase.app.ui.wishlist

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.secretbase.app.R
import com.secretbase.app.ui.common.SecretBaseCardSurface
import com.secretbase.app.ui.common.SecretBaseInputSurface
import com.secretbase.app.ui.common.SecretBasePageTopBar
import com.secretbase.app.ui.common.SecretBasePrimaryButton
import com.secretbase.app.ui.common.SecretBaseSecondaryButton
import com.secretbase.app.ui.common.SecretBaseSnackbarHost
import com.secretbase.app.ui.messagewall.ComposerImageAction
import com.secretbase.app.ui.messagewall.DraftInputCard
import com.secretbase.app.ui.messagewall.MessageMedia
import com.secretbase.app.ui.messagewall.SelectedImageStrip
import com.secretbase.app.ui.messagewall.WallIllustration
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.data.local.PendingMediaStore
import kotlinx.coroutines.launch
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmGray
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
private fun WishPageBackground(
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishListScreen(
    uiState: WishListUiState,
    snackbarHostState: SnackbarHostState,
    bottomBar: @Composable () -> Unit = {},
    onBack: () -> Unit,
    onSelectStatus: (com.secretbase.app.data.wish.WishStatus) -> Unit,
    onAddWish: () -> Unit,
    onWishClick: (String) -> Unit,
    onEditWish: (String) -> Unit,
    onDeleteWish: (String) -> Unit,
    onCompleteWish: (String) -> Unit,
    onEditorTitleChange: (String) -> Unit,
    onEditorDescriptionChange: (String) -> Unit,
    onEditorPlannedDateChange: (Long?) -> Unit,
    onEditorCoverChange: (String?) -> Unit,
    onDismissEditor: () -> Unit,
    onSaveWish: () -> Unit,
) {
    val context = LocalContext.current
    val mediaScope = rememberCoroutineScope()
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            onEditorCoverChange(null)
        } else {
            mediaScope.launch {
                runCatching { PendingMediaStore.import(context, uri) }
                    .onSuccess(onEditorCoverChange)
                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "图片读取失败") }
            }
        }
    }

    val datePicker = remember<(Long?) -> Unit> {
        { initial ->
            val base = Instant.ofEpochMilli(initial ?: System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDate()
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val millis = java.time.LocalDate.of(year, month + 1, day)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    onEditorPlannedDateChange(millis)
                },
                base.year,
                base.monthValue - 1,
                base.dayOfMonth,
            ).show()
        }
    }

    val visibleWishes = uiState.wishes.filter { it.status == uiState.selectedStatus }
    val canSaveWish = !uiState.isSaving && uiState.editorTitle.isNotBlank()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SecretBaseSnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        WishPageBackground {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    WishTopBar(
                        title = "愿望清单",
                        onBack = onBack,
                        onAdd = onAddWish,
                        showBackButton = false,
                        titleAlignStart = true,
                    )
                }
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        WishSummaryCard(
                            unrealizedCount = uiState.unrealizedCount,
                            realizedCount = uiState.realizedCount,
                        )
                    }
                }
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        WishStatusTabs(
                            selectedStatus = uiState.selectedStatus,
                            unrealizedCount = uiState.unrealizedCount,
                            realizedCount = uiState.realizedCount,
                            onSelectStatus = onSelectStatus,
                        )
                    }
                }
                if (visibleWishes.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            WishEmptyState(
                                illustrationRes = uiState.visuals.hero.imageRes,
                                isRealized = uiState.selectedStatus == com.secretbase.app.data.wish.WishStatus.REALIZED,
                                onCreateWish = onAddWish,
                            )
                        }
                    }
                } else {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                        ) {
                            visibleWishes.forEachIndexed { index, wish ->
                                WishCard(
                                    wish = wish,
                                    onClick = { onWishClick(wish.id) },
                                    onComplete = { onCompleteWish(wish.id) },
                                )
                                if (index < visibleWishes.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 46.dp)
                                            .height(1.dp)
                                            .background(Color(0xFFE7E2E4)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.editorVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissEditor,
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismissEditor) {
                        Text("取消", color = WarmGray)
                    }
                    Text(
                        text = if (uiState.editorWishId == null) "新增愿望" else "编辑愿望",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = InkBlack,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    TextButton(
                        enabled = canSaveWish,
                        onClick = onSaveWish,
                    ) {
                        Text(
                            text = if (uiState.isSaving) "保存中" else "保存",
                            color = if (canSaveWish) CherryPink else WarmGray.copy(alpha = 0.5f),
                        )
                    }
                }
                WishEditorTextField(
                    value = uiState.editorTitle,
                    onValueChange = onEditorTitleChange,
                    placeholder = "想一起做什么？",
                    minHeight = 56.dp,
                    singleLine = true,
                )
                if (uiState.editorTitle.length >= 40) {
                    CounterText(uiState.editorTitle.length, 50)
                }
                WishEditorTextField(
                    value = uiState.editorDescription,
                    onValueChange = onEditorDescriptionChange,
                    placeholder = "补充一点想法（可选）",
                    minHeight = 118.dp,
                )
                if (uiState.editorDescription.length >= 450) {
                    CounterText(uiState.editorDescription.length, 500)
                }
                FieldLabel("计划日期")
                DateField(
                    value = uiState.editorPlannedDate?.toDateText() ?: "",
                    placeholder = "暂不设置",
                    onClick = { datePicker(uiState.editorPlannedDate) },
                )
                FieldLabel("封面")
                if (uiState.editorCoverImagePath != null) {
                    Box {
                        MessageMedia(
                            imagePath = uiState.editorCoverImagePath,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(146.dp)
                                .background(Color(0xFFFFF7FA), RoundedCornerShape(22.dp)),
                        )
                        IconButton(
                            modifier = Modifier.align(Alignment.TopEnd),
                            onClick = { onEditorCoverChange(null) },
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "移除封面", tint = CherryPink)
                        }
                    }
                } else {
                    CoverPickerCard(onClick = { coverPicker.launch("image/*") })
                }
            }
        }
    }
}

@Composable
fun WishDetailScreen(
    wish: WishUiModel,
    illustrationRes: Int?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        WishPageBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = innerPadding.calculateTopPadding(),
                        bottom = 28.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WishTopBar(
                    title = "愿望详情",
                    onBack = onBack,
                    onMore = false,
                    onAdd = {},
                )
                SecretBaseCardSurface(
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (!wish.coverImagePath.isNullOrBlank()) {
                            MessageMedia(
                                imagePath = wish.coverImagePath,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(198.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFFFF5F1), RoundedCornerShape(24.dp)),
                            )
                        }
                        Text(
                            text = wish.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = InkBlack,
                        )
                        if (wish.description.isNotBlank()) {
                            Text(
                                text = wish.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = InkBlack,
                            )
                        }
                        WishMetaLine("计划日期", wish.plannedDateText ?: "尚未设定")
                        WishMetaLine("创建时间", wish.createdAtText)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SecondaryActionButton(
                        text = "编辑",
                        modifier = Modifier.weight(1f),
                        onClick = onEdit,
                    )
                    SecretBasePrimaryButton(
                        text = "完成愿望",
                        enabled = true,
                        onClick = onComplete,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun WishCompletionScreen(
    wish: WishUiModel,
    illustrationRes: Int?,
    completionText: String,
    completionImages: List<String>,
    completionDate: Long,
    isSaving: Boolean,
    onBack: () -> Unit,
    onCompletionTextChange: (String) -> Unit,
    onAddImages: (List<String>) -> Unit,
    onRemoveImage: (String) -> Unit,
    onCompletionDateChange: (Long) -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val mediaScope = rememberCoroutineScope()
    val canSaveCompletion = !isSaving && (completionText.isNotBlank() || completionImages.isNotEmpty())
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            mediaScope.launch {
                runCatching { PendingMediaStore.importAll(context, uris) }
                    .onSuccess(onAddImages)
            }
        }
    }
    val datePicker = remember<(Long) -> Unit> {
        { initial ->
            val base = Instant.ofEpochMilli(initial).atZone(ZoneId.systemDefault()).toLocalDate()
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val millis = java.time.LocalDate.of(year, month + 1, day)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    onCompletionDateChange(millis)
                },
                base.year,
                base.monthValue - 1,
                base.dayOfMonth,
            ).show()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        WishPageBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = innerPadding.calculateTopPadding(),
                        bottom = 28.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WishTopBar(
                    title = "完成记录",
                    onBack = onBack,
                    onMore = false,
                    onAdd = {},
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceWhite,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            color = Color(0xFFEAF6EE),
                            shape = CircleShape,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4A8A68),
                                    modifier = Modifier.size(23.dp),
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "记录愿望完成",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmGray,
                            )
                            Text(
                                text = wish.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = InkBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                FieldLabel("完成感想")
                DraftInputCard(
                    value = completionText,
                    onValueChange = onCompletionTextChange,
                    placeholder = "写下完成这个愿望的感受吧…",
                    minHeight = 140.dp,
                )
                if (completionText.length >= 450) {
                    CounterText(completionText.length, 500)
                }
                FieldLabel("照片")
                if (completionImages.isNotEmpty()) {
                    SelectedImageStrip(
                        images = completionImages,
                        onRemove = onRemoveImage,
                    )
                }
                ComposerImageAction(onClick = { picker.launch("image/*") })
                FieldLabel("完成日期")
                DateField(
                    value = completionDate.toDateText(),
                    placeholder = "选择完成日期",
                    onClick = { datePicker(completionDate) },
                )
                SecretBasePrimaryButton(
                    text = if (isSaving) "保存中…" else "保存完成记录",
                    enabled = canSaveCompletion,
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun WishCompletionDetailScreen(
    wish: WishUiModel,
    illustrationRes: Int?,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        WishPageBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = innerPadding.calculateTopPadding(),
                        bottom = 28.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WishTopBar(
                    title = "完成记录详情",
                    onBack = onBack,
                    onMore = false,
                    onAdd = {},
                )
                SecretBaseCardSurface(
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4A8A68),
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = "已实现",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF4A8A68),
                            )
                        }
                        Text(
                            text = wish.title,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = InkBlack,
                        )
                        if (wish.description.isNotBlank()) {
                            InfoBlock("最初的想法", wish.description)
                        }
                        WishMetaLine("创建时间", wish.createdAtText)
                        WishMetaLine("计划日期", wish.plannedDateText ?: "尚未设定")
                        WishMetaLine("完成日期", wish.completionDateText ?: "刚刚实现")
                        if (!wish.completionSummary.isNullOrBlank()) {
                            InfoBlock("完成记录", wish.completionSummary)
                        }
                        if (wish.completionImagePaths.isNotEmpty()) {
                            WishPhotoGrid(imagePaths = wish.completionImagePaths)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WishTopBar(
    title: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onMore: Boolean = true,
    showBackButton: Boolean = true,
    titleAlignStart: Boolean = false,
) {
    SecretBasePageTopBar(
        title = title,
        onBack = onBack,
        showBackButton = showBackButton,
        titleAlignStart = titleAlignStart,
        actionIcon = if (onMore) Icons.Outlined.Add else null,
        actionDescription = if (onMore) "新增" else null,
        onActionClick = if (onMore) onAdd else null,
    )
}

@Composable
private fun WishSummaryCard(
    unrealizedCount: Int,
    realizedCount: Int,
) {
    val totalCount = unrealizedCount + realizedCount
    val progress = if (totalCount == 0) 0f else realizedCount.toFloat() / totalCount
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "一起实现的愿望",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = InkBlack,
        )
        Text(
            text = "$realizedCount 个已实现 · $unrealizedCount 个待完成",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            color = WarmGray,
        )
        WishProgressIndicator(progress = progress)
    }
}

@Composable
private fun WishProgressIndicator(progress: Float) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val markerProgress = clampedProgress.coerceIn(0.06f, 0.94f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .height(3.dp)
                .background(Color(0xFFE8E3E5), RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedProgress)
                    .height(3.dp)
                    .background(CherryPink, RoundedCornerShape(999.dp)),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(markerProgress)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Image(
                painter = painterResource(R.drawable.wish_progress_marker_portrait),
                contentDescription = "愿望完成进度",
                modifier = Modifier
                    .size(40.dp)
                    .offset(x = 20.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun WishStatusTabs(
    selectedStatus: com.secretbase.app.data.wish.WishStatus,
    unrealizedCount: Int,
    realizedCount: Int,
    onSelectStatus: (com.secretbase.app.data.wish.WishStatus) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        WishTabChip(
            text = "想实现 $unrealizedCount",
            selected = selectedStatus == com.secretbase.app.data.wish.WishStatus.UNREALIZED,
            onClick = { onSelectStatus(com.secretbase.app.data.wish.WishStatus.UNREALIZED) },
        )
        WishTabChip(
            text = "已实现 $realizedCount",
            selected = selectedStatus == com.secretbase.app.data.wish.WishStatus.REALIZED,
            onClick = { onSelectStatus(com.secretbase.app.data.wish.WishStatus.REALIZED) },
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun WishTabChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) CherryPink else WarmGray,
        )
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .background(if (selected) CherryPink else Color.Transparent),
        )
    }
}

@Composable
private fun WishCard(
    wish: WishUiModel,
    onClick: () -> Unit,
    onComplete: () -> Unit,
) {
    val isUnrealized = wish.status == com.secretbase.app.data.wish.WishStatus.UNREALIZED
    val descriptionText = when (wish.status) {
        com.secretbase.app.data.wish.WishStatus.UNREALIZED -> wish.description.ifBlank { "把这个愿望轻轻放在未来吧" }
        com.secretbase.app.data.wish.WishStatus.REALIZED -> wish.completionSummary ?: "完成时的欢喜，已经被悄悄留住啦。"
    }
    val metaText = when (wish.status) {
        com.secretbase.app.data.wish.WishStatus.UNREALIZED -> wish.plannedDateText ?: wish.createdAtText
        com.secretbase.app.data.wish.WishStatus.REALIZED -> "完成于 ${wish.completionDateText ?: wish.createdAtText}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WishLeadingIcon(
            isUnrealized = isUnrealized,
            onClick = onComplete,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 1.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = wish.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isUnrealized) InkBlack else WarmGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (descriptionText.isNotBlank()) {
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = if (isUnrealized) Icons.Outlined.CalendarMonth else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = WarmGray.copy(alpha = 0.72f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = if (isUnrealized) "计划 $metaText" else metaText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = WarmGray,
                )
            }
        }
        if (!wish.coverImagePath.isNullOrBlank()) {
            MessageMedia(
                imagePath = wish.coverImagePath,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF7F8), RoundedCornerShape(12.dp)),
            )
        }
    }
}

@Composable
private fun WishLeadingIcon(
    isUnrealized: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (isUnrealized) CherryPink else Color(0xFF4A8A68)
    Surface(
        onClick = onClick,
        enabled = isUnrealized,
        modifier = Modifier.size(34.dp),
        shape = CircleShape,
        color = if (isUnrealized) Color.Transparent else tint,
        border = if (isUnrealized) androidx.compose.foundation.BorderStroke(1.8.dp, CherryPink.copy(alpha = 0.62f)) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isUnrealized) {
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = "记录愿望完成",
                    tint = CherryPink.copy(alpha = 0.48f),
                    modifier = Modifier.size(15.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "已实现",
                    tint = SurfaceWhite,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun WishEmptyState(
    illustrationRes: Int?,
    isRealized: Boolean,
    onCreateWish: () -> Unit,
) {
    SecretBaseCardSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WallIllustration(
                illustrationRes = illustrationRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp),
            )
            Text(
                text = if (isRealized) "这里还没有实现记录" else "这里还没有新的愿望",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = InkBlack,
            )
            Text(
                text = if (isRealized) "等愿望一个个发光，再回来收藏它们。" else "先写下一个小小的期待吧。",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
            if (!isRealized) {
                SecretBasePrimaryButton(text = "新增愿望", enabled = true, onClick = onCreateWish)
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        color = InkBlack,
    )
}

@Composable
private fun WishEditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minHeight: androidx.compose.ui.unit.Dp,
    singleLine: Boolean = false,
) {
    SecretBaseInputSurface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        shape = RoundedCornerShape(20.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = InkBlack,
                fontWeight = FontWeight.SemiBold,
            ),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge.copy(
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
private fun CounterText(current: Int, max: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = "$current/$max",
            style = MaterialTheme.typography.bodySmall,
            color = WarmGray,
        )
    }
}

@Composable
private fun DateField(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
) {
    SecretBaseInputSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isBlank()) WarmGray else InkBlack,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.CalendarMonth, contentDescription = "选择日期", tint = WarmGray)
        }
    }
}

@Composable
private fun CoverPickerCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(92.dp)
            .clickable(onClick = onClick),
        color = SurfaceWhite,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 1.dp,
        border = null,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Photo, contentDescription = null, tint = WarmGray)
            Text("添加封面", style = MaterialTheme.typography.bodySmall, color = WarmGray)
        }
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    modifier: Modifier = Modifier.wrapContentWidth(),
    onClick: () -> Unit,
) {
    SecretBaseSecondaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun WishMetaLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = WarmGray)
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = InkBlack)
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    SecretBaseInputSurface(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = WarmGray)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = InkBlack)
        }
    }
}

@Composable
private fun WishPhotoGrid(imagePaths: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        imagePaths.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { path ->
                    MessageMedia(
                        imagePath = path,
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .background(Color(0xFFFFF7FA), RoundedCornerShape(20.dp)),
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun Long.toDateText(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
