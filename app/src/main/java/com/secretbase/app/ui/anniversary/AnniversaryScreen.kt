package com.secretbase.app.ui.anniversary

import android.app.DatePickerDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secretbase.app.data.anniversary.AnniversaryReminder
import com.secretbase.app.ui.common.SecretBaseCardSurface
import com.secretbase.app.ui.common.SecretBaseInputSurface
import com.secretbase.app.ui.common.SecretBaseMiniActionButton
import com.secretbase.app.ui.common.SecretBasePageBackground
import com.secretbase.app.ui.common.SecretBasePageTopBar
import com.secretbase.app.ui.common.SecretBasePrimaryButton
import com.secretbase.app.ui.messagewall.WallIllustration
import com.secretbase.app.ui.theme.CherryPink
import com.secretbase.app.ui.theme.InkBlack
import com.secretbase.app.ui.theme.SurfaceWhite
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
    val canSaveEditor = !uiState.isSaving && uiState.title.isNotBlank() && uiState.date != null

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SecretBasePageTopBar(
                title = "纪念日",
                onBack = onBack,
                actionIcon = Icons.Outlined.Add,
                actionDescription = "新增纪念日",
                onActionClick = onAdd,
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
                    bottom = 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AnniversaryHero(
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
                    onSelect = onReminderChange,
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
    SecretBaseCardSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "我们已经一起走过",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = CherryPink.copy(alpha = 0.82f),
                )
                Text(
                    text = "从 $relationshipStartText 开始",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = relationshipDays.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = InkBlack,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 42.sp,
                    ),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "天",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = WarmGray,
                )
            }
        }
    }
}

@Composable
private fun AnniversaryCard(
    item: AnniversaryUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    SecretBaseCardSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AnniversaryIcon(item.statusTone)
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = InkBlack,
                )
                Text(
                    text = item.dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFF9F4F6),
                ) {
                    Text(
                        text = item.repeatLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = WarmGray,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AnniversaryStatusPill(item)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    SecretBaseMiniActionButton(
                        icon = Icons.Outlined.Edit,
                        label = "编辑",
                        onClick = onEdit,
                        showLabel = false,
                    )
                    SecretBaseMiniActionButton(
                        icon = Icons.Outlined.DeleteOutline,
                        label = "删除",
                        onClick = onDelete,
                        showLabel = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnniversaryIcon(tone: AnniversaryStatusTone) {
    val (icon, color) = when (tone) {
        AnniversaryStatusTone.TODAY -> Icons.Outlined.Favorite to Color(0xFFFF6F95)
        AnniversaryStatusTone.UPCOMING -> Icons.Outlined.Flight to CherryPink
        AnniversaryStatusTone.PASSED -> Icons.Outlined.Groups to Color(0xFF4DB6AC)
        AnniversaryStatusTone.EXPIRED -> Icons.Outlined.Cake to Color(0xFFB0BEC5)
    }
    Surface(
        modifier = Modifier.size(42.dp),
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
        AnniversaryStatusTone.UPCOMING -> CherryPink
        AnniversaryStatusTone.PASSED -> Color(0xFF4DB6AC)
        AnniversaryStatusTone.EXPIRED -> WarmGray
    }
    Surface(
        color = color.copy(alpha = if (item.statusTone == AnniversaryStatusTone.UPCOMING) 0.12f else 0.1f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = item.statusText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
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
