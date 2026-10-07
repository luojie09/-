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

const val DefaultAnniversaryEmoji: String = "together"

val AnniversaryEmojiOptions = listOf(
    "together",
    "date",
    "trip",
    "birthday",
    "gift",
    "home",
    "photo",
    "movie",
    "dinner",
    "music",
    "flower",
    "star",
)

private val LegacyAnniversaryIconAliases = mapOf(
    "💗" to "together",
    "🌸" to "date",
    "🎂" to "birthday",
    "🎁" to "gift",
    "✈️" to "trip",
    "🌊" to "trip",
    "⭐" to "star",
    "🍀" to "flower",
    "🏠" to "home",
    "📷" to "photo",
    "🍰" to "birthday",
    "🎬" to "movie",
    "🌙" to "star",
    "☀️" to "star",
    "💌" to "together",
    "🦋" to "flower",
    "🐑" to "together",
    "🐥" to "together",
)

fun normalizeAnniversaryIcon(value: String?): String =
    LegacyAnniversaryIconAliases[value] ?: value?.takeIf { it in AnniversaryEmojiOptions } ?: DefaultAnniversaryEmoji

fun anniversaryIconLabel(value: String): String = when (normalizeAnniversaryIcon(value)) {
    "together" -> "相伴"
    "date" -> "约会"
    "trip" -> "旅行"
    "birthday" -> "生日"
    "gift" -> "礼物"
    "home" -> "回家"
    "photo" -> "拍照"
    "movie" -> "电影"
    "dinner" -> "晚餐"
    "music" -> "音乐"
    "flower" -> "花"
    "star" -> "星标"
    else -> "相伴"
}

fun defaultAnniversaryEmoji(title: String): String =
    when {
        "生日" in title -> "birthday"
        "旅行" in title || "海" in title -> "trip"
        "约会" in title -> "date"
        "见家长" in title || "回家" in title -> "home"
        "照片" in title || "相册" in title -> "photo"
        "电影" in title -> "movie"
        "吃" in title || "餐" in title -> "dinner"
        "音乐" in title || "演唱会" in title -> "music"
        else -> DefaultAnniversaryEmoji
    }
