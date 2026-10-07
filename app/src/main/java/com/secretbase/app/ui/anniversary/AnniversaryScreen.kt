package com.secretbase.app.ui.anniversary

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.secretbase.app.R
import com.secretbase.app.data.anniversary.AnniversaryReminder
import com.secretbase.app.ui.common.SecretBaseCardSurface
import com.secretbase.app.ui.common.SecretBaseInputSurface
import com.secretbase.app.ui.common.SecretBasePageTopBar
import com.secretbase.app.ui.common.SecretBasePrimaryButton
import com.secretbase.app.ui.common.SecretBaseSnackbarHost
import com.secretbase.app.ui.messagewall.WallIllustration
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmGray
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnniversaryScreen(
    uiState: AnniversaryUiState,
    snackbarHostState: SnackbarHostState,
    bottomBar: @Composable () -> Unit = {},
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onIconChange: (String) -> Unit,
    onRepeatChange: (Boolean) -> Unit,
    onReminderChange: (AnniversaryReminder) -> Unit,
    onDismissEditor: () -> Unit,
    onSaveEditor: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var pendingReminder by remember { mutableStateOf<AnniversaryReminder?>(null) }
    val canSaveEditor = !uiState.isSaving && uiState.title.isNotBlank() && uiState.date != null
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val reminder = pendingReminder
        pendingReminder = null
        if (granted && reminder != null) {
            onReminderChange(reminder)
        } else if (!granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("需要通知权限才能按时提醒纪念日")
            }
        }
    }
    val selectReminder: (AnniversaryReminder) -> Unit = { reminder ->
        val needsPermission = reminder != AnniversaryReminder.NONE &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingReminder = reminder
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onReminderChange(reminder)
        }
    }

    val openDatePicker: (Long?) -> Unit = { initial ->
        val base = Instant.ofEpochMilli(initial ?: System.currentTimeMillis())
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val millis = java.time.LocalDate.of(year, month + 1, day)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                onDateChange(millis)
            },
            base.year,
            base.monthValue - 1,
            base.dayOfMonth,
        ).show()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SecretBaseSnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    SecretBasePageTopBar(
                        title = "纪念日",
                        onBack = onBack,
                        showBackButton = false,
                        titleAlignStart = true,
                        actionIcon = Icons.Outlined.Add,
                        actionDescription = "新增纪念日",
                        onActionClick = onAdd,
                    )
                }
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        AnniversaryHero(
                            relationshipDays = uiState.relationshipDays,
                            relationshipStartText = uiState.relationshipStartText,
                        )
                    }
                }
                if (uiState.items.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            AnniversaryEmptyState(
                                illustrationRes = uiState.visuals.hero.imageRes,
                                onAdd = onAdd,
                            )
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "重要日子",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = InkBlack,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${uiState.items.size} 个 · 长按删除",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmGray,
                            )
                        }
                    }
                    itemsIndexed(uiState.items, key = { _, item -> item.id }) { index, item ->
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            AnniversaryCard(
                                item = item,
                                onEdit = { onEdit(item.id) },
                                onDelete = { pendingDeleteId = item.id },
                            )
                            if (index < uiState.items.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 50.dp)
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

    if (uiState.editorVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissEditor,
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (uiState.editingId == null) "新增纪念日" else "编辑纪念日",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = InkBlack,
                )
                SheetLabel("纪念日名称 *")
                AnniversaryInput(
                    value = uiState.title,
                    placeholder = "请输入纪念日名称",
                    onValueChange = onTitleChange,
                )
                SheetLabel("图标")
                AnniversaryEmojiPicker(
                    selectedEmoji = uiState.iconEmoji,
                    onSelect = onIconChange,
                )
                SheetLabel("日期 *")
                AnniversaryDateField(
                    value = uiState.date?.toDateText().orEmpty(),
                    placeholder = "选择日期",
                    onClick = { openDatePicker(uiState.date) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "每年重复",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = InkBlack,
                        )
                        Text(
                            text = "生日、恋爱纪念日建议开启",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGray,
                        )
                    }
                    Switch(
                        checked = uiState.repeatYearly,
                        onCheckedChange = onRepeatChange,
                    )
                }
                SheetLabel("提醒时间")
                ReminderRow(
                    selected = uiState.reminderType,
                    onSelect = selectReminder,
                )
                SecretBasePrimaryButton(
                    text = if (uiState.isSaving) "保存中..." else "保存",
                    enabled = canSaveEditor,
                    onClick = onSaveEditor,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteId = null
                        onDelete(id)
                    },
                ) {
                    Text("删除", color = CherryPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("取消", color = WarmGray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SurfaceWhite,
            title = { Text("删除这个纪念日？") },
            text = { Text("删除后将无法恢复。", color = WarmGray) },
        )
    }
}

@Composable
private fun AnniversaryHero(
    relationshipDays: Int,
    relationshipStartText: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(192.dp)
            .clipToBounds(),
    ) {
        Image(
            painter = painterResource(R.drawable.anniversary_header_dynamic_note),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.12f
                    scaleY = 1.12f
                },
            contentScale = ContentScale.Fit,
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 100.dp, top = 8.dp, end = 100.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "我们在一起已经",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = WarmGray,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = relationshipDays.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 52.sp,
                        color = Color(0xFF64A1E3),
                        fontWeight = FontWeight.Bold,
                        lineHeight = 56.sp,
                    ),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "天",
                    modifier = Modifier.padding(bottom = 5.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = WarmGray,
                )
            }
            Text(
                text = relationshipStartText,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun AnniversaryCard(
    item: AnniversaryUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onDelete,
                onLongClickLabel = "删除 ${item.title}",
            )
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnniversaryIcon(
            icon = item.iconEmoji,
            tone = item.statusTone,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = InkBlack,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = item.dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
                Text(text = "·", style = MaterialTheme.typography.bodySmall, color = WarmGray)
                Text(
                    text = item.repeatLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            AnniversaryStatusText(item)
        }
    }
}

@Composable
private fun AnniversaryIcon(
    icon: String,
    tone: AnniversaryStatusTone,
) {
    val color = when (tone) {
        AnniversaryStatusTone.TODAY -> Color(0xFFFF6F95)
        AnniversaryStatusTone.UPCOMING -> CherryPink
        AnniversaryStatusTone.PASSED -> Color(0xFF4DB6AC)
        AnniversaryStatusTone.EXPIRED -> Color(0xFFB0BEC5)
    }
    Surface(
        modifier = Modifier.size(38.dp),
        color = color.copy(alpha = 0.14f),
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = anniversarySceneIcon(icon),
                contentDescription = anniversaryIconLabel(icon),
                modifier = Modifier.size(20.dp),
                tint = color,
            )
        }
    }
}

private fun anniversarySceneIcon(icon: String) = when (normalizeAnniversaryIcon(icon)) {
    "together" -> Icons.Outlined.FavoriteBorder
    "date" -> Icons.Outlined.LocalCafe
    "trip" -> Icons.Outlined.Flight
    "birthday" -> Icons.Outlined.Cake
    "gift" -> Icons.Outlined.CardGiftcard
    "home" -> Icons.Outlined.Home
    "photo" -> Icons.Outlined.PhotoCamera
    "movie" -> Icons.Outlined.Movie
    "dinner" -> Icons.Outlined.Restaurant
    "music" -> Icons.Outlined.MusicNote
    "flower" -> Icons.Outlined.LocalFlorist
    "star" -> Icons.Outlined.StarBorder
    else -> Icons.Outlined.FavoriteBorder
}

@Composable
private fun AnniversaryStatusText(item: AnniversaryUiModel) {
    val color = when (item.statusTone) {
        AnniversaryStatusTone.TODAY -> CherryPink
        AnniversaryStatusTone.UPCOMING -> CherryPink
        AnniversaryStatusTone.PASSED -> Color(0xFF4DB6AC)
        AnniversaryStatusTone.EXPIRED -> WarmGray
    }
    Text(
        text = item.statusText,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = color,
    )
}

@Composable
private fun AnniversaryEmptyState(
    illustrationRes: Int?,
    onAdd: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WallIllustration(
                illustrationRes = illustrationRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )
            Text(
                text = "这里还没有纪念日",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = InkBlack,
            )
            Text(
                text = "先把重要的日子收进来吧。",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
            )
            SecretBasePrimaryButton(
                text = "新增纪念日",
                enabled = true,
                onClick = onAdd,
            )
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        color = InkBlack,
    )
}

@Composable
private fun AnniversaryInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    SecretBaseInputSurface(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = InkBlack),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = WarmGray,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun AnniversaryEmojiPicker(
    selectedEmoji: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnniversaryEmojiOptions.chunked(6).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { icon ->
                    val selected = icon == normalizeAnniversaryIcon(selectedEmoji)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { onSelect(icon) },
                        shape = CircleShape,
                        color = if (selected) Color(0xFFFFF0F4) else Color(0xFFFFFBFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) CherryPink.copy(alpha = 0.34f) else Color(0xFFF1E5EA),
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = anniversarySceneIcon(icon),
                                contentDescription = anniversaryIconLabel(icon),
                                modifier = Modifier.size(21.dp),
                                tint = if (selected) CherryPink else WarmGray,
                            )
                        }
                    }
                }
                repeat(6 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AnniversaryDateField(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
) {
    SecretBaseInputSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isBlank()) WarmGray else InkBlack,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.Celebration,
                contentDescription = "选择日期",
                tint = WarmGray,
            )
        }
    }
}

@Composable
private fun ReminderRow(
    selected: AnniversaryReminder,
    onSelect: (AnniversaryReminder) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            AnniversaryReminder.NONE to "不提醒",
            AnniversaryReminder.SAME_DAY to "当天提醒",
            AnniversaryReminder.ONE_DAY_BEFORE to "提前 1 天",
            AnniversaryReminder.THREE_DAYS_BEFORE to "提前 3 天",
        ).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (type, label) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(type) },
                        color = if (selected == type) Color(0xFFFFF1F5) else Color(0xFFF7F3F4),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected == type) CherryPink else WarmGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
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
