package com.secretbase.app.ui.anniversary

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.secretbase.app.MainBottomTabBar
import com.secretbase.app.data.anniversary.AnniversaryReminder
import com.secretbase.app.ui.home.buildPreviewHomeVisuals
import com.secretbase.app.ui.theme.SecretBaseTheme

@PreviewTest
@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Anniversary Normal 393x852")
@Composable
fun AnniversaryScreenshotNormal393() = AnniversaryScreenshotContent(empty = false)

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Anniversary Normal 360x800")
@Composable
fun AnniversaryScreenshotNormal360() = AnniversaryScreenshotContent(empty = false)

@PreviewTest
@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Anniversary Empty 393x852")
@Composable
fun AnniversaryScreenshotEmpty393() = AnniversaryScreenshotContent(empty = true)

@Composable
private fun AnniversaryScreenshotContent(empty: Boolean) {
    SecretBaseTheme {
        AnniversaryScreen(
            uiState = AnniversaryUiState(
                isLoading = false,
                visuals = buildPreviewHomeVisuals(),
                relationshipDays = 73,
                relationshipStartText = "2026.05.06",
                items = if (empty) emptyList() else previewAnniversaries(),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            bottomBar = {
                MainBottomTabBar(activeRoute = "anniversary", onSelect = {})
            },
            onBack = {},
            onAdd = {},
            onEdit = {},
            onDelete = {},
            onTitleChange = {},
            onDateChange = {},
            onIconChange = {},
            onRepeatChange = {},
            onReminderChange = {},
            onDismissEditor = {},
            onSaveEditor = {},
        )
    }
}

private fun previewAnniversaries(): List<AnniversaryUiModel> = listOf(
    AnniversaryUiModel(
        id = "anniversary-preview-1",
        title = "生日 - 小羊",
        dateText = "2023.08.15",
        statusText = "还有 26 天",
        statusTone = AnniversaryStatusTone.UPCOMING,
        repeatLabel = "每年重复",
        reminderType = AnniversaryReminder.ONE_DAY_BEFORE,
        iconEmoji = "🎂",
    ),
    AnniversaryUiModel(
        id = "anniversary-preview-2",
        title = "第一次约会",
        dateText = "2023.01.15",
        statusText = "还有 179 天",
        statusTone = AnniversaryStatusTone.UPCOMING,
        repeatLabel = "每年重复",
        reminderType = AnniversaryReminder.SAME_DAY,
        iconEmoji = "🌸",
    ),
    AnniversaryUiModel(
        id = "anniversary-preview-3",
        title = "第一次旅行",
        dateText = "2023.05.20",
        statusText = "还有 304 天",
        statusTone = AnniversaryStatusTone.UPCOMING,
        repeatLabel = "每年重复",
        reminderType = AnniversaryReminder.THREE_DAYS_BEFORE,
        iconEmoji = "✈️",
    ),
)
