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
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.secretbase.app.ui.common.SecretBaseCardSurface
import com.secretbase.app.ui.common.SecretBaseInputSurface
import com.secretbase.app.ui.common.SecretBasePageBackground
import com.secretbase.app.ui.common.SecretBasePageTopBar
import com.secretbase.app.ui.common.SecretBasePrimaryButton
import com.secretbase.app.ui.common.SecretBaseSecondaryButton
import com.secretbase.app.ui.messagewall.ComposerImageAction
import com.secretbase.app.ui.messagewall.DraftInputCard
import com.secretbase.app.ui.messagewall.MessageMedia
import com.secretbase.app.ui.messagewall.SelectedImageStrip
import com.secretbase.app.ui.messagewall.WallIllustration
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.OutlinePink
import com.secretbase.app.ui.theme.SurfaceWhite
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
    val canSaveWish = !uiState.isSaving && uiState.editorTitle.isNotBlank()

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
        SecretBasePageBackground {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 10.dp,
                    bottom = innerPadding.calculateBottomPadding() + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    WishHeroCard(
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
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (uiState.editorWishId == null) "新增愿望" else "编辑愿望",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                SecretBasePrimaryButton(
                    text = if (uiState.isSaving) "保存中…" else "保存",
                    enabled = canSaveWish,
                    onClick = onSaveWish,
                    modifier = Modifier.fillMaxWidth(),
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
        SecretBasePageBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 12.dp,
                        bottom = 28.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SecretBaseCardSurface(
                    shape = RoundedCornerShape(28.dp),
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
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
    val canSaveCompletion = !isSaving && (completionText.isNotBlank() || completionImages.isNotEmpty())
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
        SecretBasePageBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 10.dp,
                        bottom = 28.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SecretBaseCardSurface(
                    shape = RoundedCornerShape(28.dp),
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
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
        topBar = {
            WishTopBar(
                title = "完成记录详情",
                onBack = onBack,
                onMore = false,
                onAdd = {},
            )
        },
    ) { innerPadding ->
        SecretBasePageBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 10.dp,
                        bottom = 28.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SecretBaseCardSurface(
                    shape = RoundedCornerShape(28.dp),
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
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
}

@Composable
private fun WishTopBar(
    title: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onMore: Boolean = true,
) {
    SecretBasePageTopBar(
        title = title,
        onBack = onBack,
        actionIcon = if (onMore) Icons.Outlined.Add else null,
        actionDescription = if (onMore) "新增" else null,
        onActionClick = if (onMore) onAdd else null,
    )
}

@Composable
private fun WishHeroCard(
    unrealizedCount: Int,
    realizedCount: Int,
) {
    SecretBaseCardSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "一起慢慢实现",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = CherryPink,
                )
                Text(
                    text = "未实现 $unrealizedCount · 已实现 $realizedCount",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = InkBlack,
                )
            }
            StatCard("未实现", unrealizedCount, Color(0xFFFFF7FA))
            StatCard("已实现", realizedCount, Color(0xFFFFF7FA))
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    count: Int,
    cardColor: Color = Color(0xFFFFFBFD),
) {
    Surface(
        color = cardColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.38f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = WarmGray)
            Text("$count", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = InkBlack)
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
        color = SurfaceWhite.copy(alpha = 0.74f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.32f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
        color = if (selected) Color(0xFFFFEEF4).copy(alpha = 0.78f) else Color.Transparent,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (selected) CherryPink else WarmGray,
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

    SecretBaseCardSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = wish.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = InkBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    WishStatusPill(wish)
                }
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
                    color = WarmGray.copy(alpha = 0.82f),
                )
                if (wish.status == com.secretbase.app.data.wish.WishStatus.UNREALIZED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        SecondaryActionButton(text = "完成", onClick = onComplete)
                    }
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.MoreHoriz, contentDescription = "更多", tint = WarmGray.copy(alpha = 0.68f))
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
private fun WishStatusPill(wish: WishUiModel) {
    val isUnrealized = wish.status == com.secretbase.app.data.wish.WishStatus.UNREALIZED
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFFFF2F6).copy(alpha = if (isUnrealized) 0.82f else 0.56f),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.28f)),
    ) {
        Text(
            text = if (isUnrealized) "待实现" else "已实现",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isUnrealized) CherryPink else WarmGray,
        )
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
