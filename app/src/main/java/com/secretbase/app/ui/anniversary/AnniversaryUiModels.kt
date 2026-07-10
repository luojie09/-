package com.secretbase.app.ui.anniversary

import com.secretbase.app.data.HomeVisuals
import com.secretbase.app.data.anniversary.AnniversaryReminder

data class AnniversaryUiState(
    val isLoading: Boolean = true,
    val visuals: HomeVisuals = HomeVisuals.EMPTY,
    val relationshipDays: Int = 0,
    val relationshipStartText: String = "",
    val items: List<AnniversaryUiModel> = emptyList(),
    val editorVisible: Boolean = false,
    val editingId: String? = null,
    val title: String = "",
    val date: Long? = null,
    val iconEmoji: String = DefaultAnniversaryEmoji,
    val repeatYearly: Boolean = true,
    val reminderType: AnniversaryReminder = AnniversaryReminder.NONE,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

data class AnniversaryUiModel(
    val id: String,
    val title: String,
    val dateText: String,
    val statusText: String,
    val statusTone: AnniversaryStatusTone,
    val repeatLabel: String,
    val reminderType: AnniversaryReminder,
    val iconEmoji: String,
)

enum class AnniversaryStatusTone {
    TODAY,
    UPCOMING,
    PASSED,
    EXPIRED,
}

const val DefaultAnniversaryEmoji: String = "💗"

val AnniversaryEmojiOptions = listOf(
    "💗",
    "🌸",
    "🎂",
    "🎁",
    "✈️",
    "🌊",
    "⭐",
    "🍀",
    "🏠",
    "📷",
    "🍰",
    "🎬",
    "🌙",
    "☀️",
    "💌",
    "🦋",
    "🐑",
    "🐥",
)

fun defaultAnniversaryEmoji(title: String): String =
    when {
        "生日" in title -> "🎂"
        "旅行" in title -> "✈️"
        "约会" in title -> "🌸"
        "见家长" in title -> "🏠"
        "照片" in title || "相册" in title -> "📷"
        "电影" in title -> "🎬"
        "海" in title -> "🌊"
        else -> DefaultAnniversaryEmoji
    }
