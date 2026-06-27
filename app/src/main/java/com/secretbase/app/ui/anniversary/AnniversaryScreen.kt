package com.secretbase.app.ui.anniversary

import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secretbase.app.data.anniversary.AnniversaryReminder
import com.secretbase.app.ui.messagewall.PublishPillButton
import com.secretbase.app.ui.messagewall.WallCircleButton
import com.secretbase.app.ui.messagewall.WallIllustration
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.OutlinePink
import com.secretbase.app.ui.theme.SurfaceWhite
import com.secretbase.app.ui.theme.WarmBackground
import com.secretbase.app.ui.theme.WarmBackgroundTop
import com.secretbase.app.ui.theme.WarmGray
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnniversaryScreen(
    uiState: AnniversaryUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onRepeatChange: (Boolean) -> Unit,
    onReminderChange: (AnniversaryReminder) -> Unit,
    onDismissEditor: () -> Unit,
    onSaveEditor: () -> Unit,
) {
    val context = LocalContext.current
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val openDatePicker: (Long?) -> Unit = { initial ->
        val base = Instant.ofEpochMilli(initial ?: System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDate()
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WallCircleButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = InkBlack,
                    onClick = onBack,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "纪念日",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = InkBlack,
                )
                Spacer(modifier = Modifier.weight(1f))
                WallCircleButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = "新增纪念日",
                    tint = InkBlack,
                    onClick = onAdd,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(WarmBackgroundTop, WarmBackground))),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 10.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AnniversaryHero(
                    illustrationRes = uiState.visuals.hero.imageRes,
                    relationshipDays = uiState.relationshipDays,
                    relationshipStartText = uiState.relationshipStartText,
                )
            }
            if (uiState.items.isEmpty()) {
                item {
                    AnniversaryEmptyState(
                        illustrationRes = uiState.visuals.hero.imageRes,
                        onAdd = onAdd,
                    )
                }
            } else {
                items(uiState.items, key = AnniversaryUiModel::id) { item ->
                    AnniversaryCard(
                        item = item,
                        onEdit = { onEdit(item.id) },
                        onDelete = { pendingDeleteId = item.id },
                    )
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
                    text = if (uiState.editingId == null) "新增纪念日" else "编辑纪念日",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = InkBlack,
                )
                SheetLabel("纪念日名称 *")
                AnniversaryInput(uiState.title, "请输入纪念日名称", onTitleChange)
                SheetLabel("日期 *")
                AnniversaryDateField(
                    value = uiState.date?.toDateText() ?: "",
                    placeholder = "选择日期",
                    onClick = { openDatePicker(uiState.date) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("每年重复", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = InkBlack)
                        Text("生日、纪念日这类推荐开启", style = MaterialTheme.typography.bodySmall, color = WarmGray)
                    }
                    Switch(checked = uiState.repeatYearly, onCheckedChange = onRepeatChange)
                }
                SheetLabel("提醒时间")
                ReminderRow(
                    selected = uiState.reminderType,
                    onSelect = onReminderChange,
                )
                PublishPillButton(
                    text = if (uiState.isSaving) "保存中…" else "保存",
                    enabled = !uiState.isSaving,
                    onClick = onSaveEditor,
                )
            }
        }
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteId = null
                    onDelete(id)
                }) { Text("删除", color = CherryPink) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("取消", color = WarmGray) }
            },
            title = { Text("删除这个纪念日？") },
            text = { Text("删除后将无法恢复。", color = WarmGray) },
        )
    }
}

@Composable
private fun AnniversaryHero(
    illustrationRes: Int?,
    relationshipDays: Int,
    relationshipStartText: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite.copy(alpha = 0.98f),
        shadowElevation = 16.dp,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.95f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WallIllustration(
                illustrationRes = illustrationRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(158.dp),
            )
            Text("我们在一起已经", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = WarmGray)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$relationshipDays", style = MaterialTheme.typography.headlineLarge.copy(color = CherryPink, fontWeight = FontWeight.ExtraBold))
                Spacer(modifier = Modifier.width(8.dp))
                Text("天", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = InkBlack)
            }
            Text("从 $relationshipStartText 开始", style = MaterialTheme.typography.bodyMedium, color = WarmGray)
        }
    }
}

@Composable
private fun AnniversaryCard(
    item: AnniversaryUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite.copy(alpha = 0.98f),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.9f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnniversaryIcon(item.statusTone)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = InkBlack)
                Text(item.dateText, style = MaterialTheme.typography.bodySmall, color = WarmGray)
                Text(item.repeatLabel, style = MaterialTheme.typography.bodySmall, color = WarmGray)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnniversaryStatusPill(item)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniAction(Icons.Outlined.Edit, "编辑", onEdit)
                    MiniAction(Icons.Outlined.DeleteOutline, "删除", onDelete)
                }
            }
        }
    }
}

@Composable
private fun AnniversaryIcon(tone: AnniversaryStatusTone) {
    val (icon, color) = when (tone) {
        AnniversaryStatusTone.TODAY -> Icons.Outlined.Favorite to Color(0xFFFF6F95)
        AnniversaryStatusTone.UPCOMING -> Icons.Outlined.Flight to Color(0xFFFFA726)
        AnniversaryStatusTone.PASSED -> Icons.Outlined.Groups to Color(0xFF4DB6AC)
        AnniversaryStatusTone.EXPIRED -> Icons.Outlined.Cake to Color(0xFFB0BEC5)
    }
    Surface(
        modifier = Modifier.size(44.dp),
        color = color.copy(alpha = 0.14f),
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color)
        }
    }
}

@Composable
private fun AnniversaryStatusPill(item: AnniversaryUiModel) {
    val color = when (item.statusTone) {
        AnniversaryStatusTone.TODAY -> CherryPink
        AnniversaryStatusTone.UPCOMING -> Color(0xFFFFA726)
        AnniversaryStatusTone.PASSED -> Color(0xFF4DB6AC)
        AnniversaryStatusTone.EXPIRED -> WarmGray
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = item.statusText,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

@Composable
private fun MiniAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = Color(0xFFFFFBFD),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.72f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = label, tint = WarmGray, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = WarmGray)
        }
    }
}

@Composable
private fun AnniversaryEmptyState(
    illustrationRes: Int?,
    onAdd: () -> Unit,
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
                    .height(140.dp),
            )
            Text("这里还没有纪念日", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = InkBlack)
            Text("先把重要的日子收进来吧。", style = MaterialTheme.typography.bodyMedium, color = WarmGray)
            PublishPillButton(text = "新增纪念日", enabled = true, onClick = onAdd)
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = InkBlack)
}

@Composable
private fun AnniversaryInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFFBFD),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, OutlinePink.copy(alpha = 0.82f)),
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = InkBlack),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = WarmGray)
                }
                inner()
            },
        )
    }
}

@Composable
private fun AnniversaryDateField(
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
            Icon(Icons.Outlined.Celebration, contentDescription = "选择日期", tint = WarmGray)
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
            AnniversaryReminder.ONE_DAY_BEFORE to "提前1天",
            AnniversaryReminder.THREE_DAYS_BEFORE to "提前3天",
        ).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (type, label) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(type) },
                        color = if (selected == type) CherryPink else Color(0xFFF7F2F4),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (selected == type) SurfaceWhite else WarmGray,
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
