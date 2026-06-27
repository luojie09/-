package com.secretbase.app.ui.wishlist

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.secretbase.app.ui.messagewall.ComposerImageAction
import com.secretbase.app.ui.messagewall.DraftInputCard
import com.secretbase.app.ui.messagewall.MessageMedia
import com.secretbase.app.ui.messagewall.PublishPillButton
import com.secretbase.app.ui.messagewall.SelectedImageStrip
import com.secretbase.app.ui.messagewall.WallCircleButton
import com.secretbase.app.ui.messagewall.WallIllustration
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.OutlinePink
import com.secretbase.app.ui.theme.SoftPink
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmBackground
import com.secretbase.app.ui.theme.WarmBackgroundTop
import com.secretbase.app.ui.theme.WarmGray
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishListScreen(
    uiState: WishListUiState,
    snackbarHostState: SnackbarHostState,
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
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        onEditorCoverChange(uri?.toString())
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

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            WishTopBar(
                title = "愿望清单",
                onBack = onBack,
                onAdd = onAddWish,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(WarmBackgroundTop, WarmBackground))),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 10.dp,
                    bottom = innerPadding.calculateBottomPadding() + 26.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    WishHeroCard(
                        illustrationRes = uiState.visuals.hero.imageRes,
                        unrealizedCount = uiState.unrealizedCount,
                        realizedCount = uiState.realizedCount,
                    )
                }
                item {
                    WishStatusTabs(
                        selectedStatus = uiState.selectedStatus,
                        onSelectStatus = onSelectStatus,
                    )
                }
                if (visibleWishes.isEmpty()) {
                    item {
                        WishEmptyState(
                            illustrationRes = uiState.visuals.hero.imageRes,
                            isRealized = uiState.selectedStatus == com.secretbase.app.data.wish.WishStatus.REALIZED,
                            onCreateWish = onAddWish,
                        )
                    }
                } else {
                    items(visibleWishes, key = WishUiModel::id) { wish ->
                        WishCard(
                            wish = wish,
                            onClick = { onWishClick(wish.id) },
                            onEdit = { onEditWish(wish.id) },
                            onDelete = { onDeleteWish(wish.id) },
                            onComplete = { onCompleteWish(wish.id) },
                        )
                    }
                }
            }
        }
    }

    if (uiState.editorVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissEditor,
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (uiState.editorWishId == null) "新增愿望" else "编辑愿望",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = InkBlack,
                )
                FieldLabel("愿望标题 *")
                DraftInputCard(
                    value = uiState.editorTitle,
                    onValueChange = onEditorTitleChange,
                    placeholder = "请输入愿望标题（最多50字）",
                    minHeight = 72.dp,
                )
                CounterText(uiState.editorTitle.length, 50)
                FieldLabel("想法说明（可选）")
                DraftInputCard(
                    value = uiState.editorDescription,
                    onValueChange = onEditorDescriptionChange,
                    placeholder = "写下你的想法吧…（最多500字）",
                    minHeight = 150.dp,
                )
                CounterText(uiState.editorDescription.length, 500)
                FieldLabel("计划日期（可选）")
                DateField(
                    value = uiState.editorPlannedDate?.toDateText() ?: "",
                    placeholder = "选择计划日期",
                    onClick = { datePicker(uiState.editorPlannedDate) },
                )
                FieldLabel("封面图片（可选）")
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
                PublishPillButton(
                    text = if (uiState.isSaving) "保存中…" else "保存",
                    enabled = !uiState.isSaving,
                    onClick = onSaveWish,
                )
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
        topBar = {
            WishTopBar(
                title = "愿望详情",
                onBack = onBack,
                onMore = false,
                onAdd = {},
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(WarmBackgroundTop, WarmBackground)))
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = SurfaceWhite.copy(alpha = 0.98f),
                shadowElevation = 16.dp,
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.95f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (wish.coverImagePath != null) {
                        MessageMedia(
                            imagePath = wish.coverImagePath,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(198.dp)
                                .background(Color(0xFFFFF7FA), RoundedCornerShape(24.dp)),
                        )
                    } else {
                        WallIllustration(
                            illustrationRes = illustrationRes,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(198.dp),
                        )
                    }
                    Text(
                        text = wish.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = InkBlack,
                    )
                    Text(
                        text = wish.description.ifBlank { "这一条愿望还没写下更多说明，但已经足够让人期待。" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkBlack,
                    )
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
                PublishPillButton(
                    text = "完成愿望",
                    enabled = true,
                    onClick = onComplete,
                )
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
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) onAddImages(uris.map(Uri::toString))
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
        topBar = {
            WishTopBar(
                title = "完成记录",
                onBack = onBack,
                onMore = false,
                onAdd = {},
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(WarmBackgroundTop, WarmBackground)))
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 10.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = SurfaceWhite.copy(alpha = 0.98f),
                shadowElevation = 16.dp,
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.95f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WallIllustration(
                        illustrationRes = illustrationRes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                    )
                    Text(
                        text = "太棒了！",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = InkBlack,
                    )
                    Text(
                        text = "记录下这份美好的时刻吧",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmGray,
                    )
                    Text(
                        text = wish.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CherryPink,
                    )
                }
            }
            FieldLabel("完成感想")
            DraftInputCard(
                value = completionText,
                onValueChange = onCompletionTextChange,
                placeholder = "写下完成这个愿望的感受吧…",
                minHeight = 140.dp,
            )
            CounterText(completionText.length, 500)
            FieldLabel("完成照片（最多9张）")
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
            PublishPillButton(
                text = if (isSaving) "保存中…" else "保存完成记录",
                enabled = !isSaving,
                onClick = onSave,
            )
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
        topBar = {
            WishTopBar(
                title = "完成记录详情",
                onBack = onBack,
                onMore = false,
                onAdd = {},
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(WarmBackgroundTop, WarmBackground)))
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 10.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = SurfaceWhite.copy(alpha = 0.98f),
                shadowElevation = 16.dp,
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.95f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    WallIllustration(
                        illustrationRes = illustrationRes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(158.dp),
                    )
                    Text(
                        text = wish.title,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = InkBlack,
                    )
                    InfoBlock("原始愿望", wish.description.ifBlank { "没有额外说明" })
                    WishMetaLine("创建时间", wish.createdAtText)
                    WishMetaLine("计划日期", wish.plannedDateText ?: "尚未设定")
                    WishMetaLine("完成日期", wish.completionDateText ?: "刚刚实现")
                    InfoBlock(
                        "完成记录",
                        wish.completionSummary ?: "这次实现得太开心了，所以把心情都留在照片里啦。",
                    )
                    if (wish.completionImagePaths.isNotEmpty()) {
                        WishPhotoGrid(imagePaths = wish.completionImagePaths)
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WallCircleButton(
            icon = Icons.Outlined.ArrowBack,
            contentDescription = "返回",
            tint = InkBlack,
            onClick = onBack,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = InkBlack,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (onMore) {
            WallCircleButton(
                icon = Icons.Outlined.Add,
                contentDescription = "新增",
                tint = InkBlack,
                onClick = onAdd,
            )
        } else {
            Spacer(modifier = Modifier.size(46.dp))
        }
    }
}

@Composable
private fun WishHeroCard(
    illustrationRes: Int?,
    unrealizedCount: Int,
    realizedCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite.copy(alpha = 0.97f),
        shadowElevation = 14.dp,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.95f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "有你在身边",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = CherryPink,
            )
            Text(
                text = "愿望都会慢慢实现",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = InkBlack,
            )
            WallIllustration(
                illustrationRes = illustrationRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                StatCard("未实现", unrealizedCount, Modifier.weight(1f))
                StatCard("已实现", realizedCount, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = 2.dp),
        color = Color(0xFFFFFBFD),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = WarmGray)
            Text("$count", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = InkBlack)
        }
    }
}

@Composable
private fun WishStatusTabs(
    selectedStatus: com.secretbase.app.data.wish.WishStatus,
    onSelectStatus: (com.secretbase.app.data.wish.WishStatus) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite.copy(alpha = 0.95f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.8f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            WishTabChip(
                text = "未实现",
                selected = selectedStatus == com.secretbase.app.data.wish.WishStatus.UNREALIZED,
                modifier = Modifier.weight(1f),
                onClick = { onSelectStatus(com.secretbase.app.data.wish.WishStatus.UNREALIZED) },
            )
            WishTabChip(
                text = "已实现",
                selected = selectedStatus == com.secretbase.app.data.wish.WishStatus.REALIZED,
                modifier = Modifier.weight(1f),
                onClick = { onSelectStatus(com.secretbase.app.data.wish.WishStatus.REALIZED) },
            )
        }
    }
}

@Composable
private fun WishTabChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) CherryPink else Color(0xFFF7F2F4),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (selected) SurfaceWhite else WarmGray,
        )
    }
}

@Composable
private fun WishCard(
    wish: WishUiModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
) {
    var showMenu by remember(wish.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember(wish.id) { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = SurfaceWhite.copy(alpha = 0.98f),
        shadowElevation = 14.dp,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.9f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MessageMedia(
                imagePath = wish.coverImagePath ?: "mock://wish-default",
                modifier = Modifier
                    .size(width = 76.dp, height = 76.dp)
                    .background(Color(0xFFFFF7FA), RoundedCornerShape(20.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = wish.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = InkBlack,
                )
                Text(
                    text = when (wish.status) {
                        com.secretbase.app.data.wish.WishStatus.UNREALIZED -> wish.description.ifBlank { "把这个愿望轻轻放在未来吧" }
                        com.secretbase.app.data.wish.WishStatus.REALIZED -> wish.completionSummary ?: "完成时的欢喜，已经被悄悄留住啦。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when (wish.status) {
                        com.secretbase.app.data.wish.WishStatus.UNREALIZED -> wish.plannedDateText ?: wish.createdAtText
                        com.secretbase.app.data.wish.WishStatus.REALIZED -> "完成于 ${wish.completionDateText ?: wish.createdAtText}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Outlined.MoreHoriz, contentDescription = "更多", tint = WarmGray)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = CherryPink) },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        },
                    )
                }
            }
        }
    }

    if (wish.status == com.secretbase.app.data.wish.WishStatus.UNREALIZED) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            SecondaryActionButton(text = "完成愿望", onClick = onComplete)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("删除", color = CherryPink) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消", color = WarmGray) }
            },
            title = { Text("删除这个愿望？") },
            text = { Text("删除后将无法恢复。", color = WarmGray) },
        )
    }
}

@Composable
private fun WishEmptyState(
    illustrationRes: Int?,
    isRealized: Boolean,
    onCreateWish: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite.copy(alpha = 0.97f),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.95f)),
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
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = InkBlack,
            )
            Text(
                text = if (isRealized) "等愿望一个个发光，再回来收藏它们。" else "先写下一个小小的期待吧。",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
            if (!isRealized) {
                PublishPillButton(text = "新增愿望", enabled = true, onClick = onCreateWish)
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        color = InkBlack,
    )
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
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Color(0xFFFFFBFD),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.82f)),
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
        color = Color(0xFFFFFBFD),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.8f)),
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
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = SurfaceWhite,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.85f)),
        shadowElevation = 4.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = InkBlack,
        )
    }
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
    Surface(
        color = Color(0xFFFFFBFD),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.75f)),
    ) {
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
