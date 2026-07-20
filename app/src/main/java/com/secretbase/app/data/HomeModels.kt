package com.secretbase.app.data

import androidx.annotation.DrawableRes
import java.time.Instant
import java.time.LocalDate

data class HeroVisualConfig(
    @DrawableRes val imageRes: Int?,
    val gradientStartHex: String,
    val gradientMiddleHex: String,
    val gradientEndHex: String,
    val heightDp: Int,
    val bottomFadeHeightDp: Int,
    val relationshipCardOverlapDp: Int,
)

data class HomeVisuals(
    val hero: HeroVisualConfig,
    @DrawableRes val backgroundOverlayRes: Int?,
    val avatarResByUserId: Map<String, Int>,
    val iconResBySlot: Map<String, Int>,
) {
    fun avatar(userId: String): Int? = avatarResByUserId[userId]

    fun icon(slot: String): Int? = iconResBySlot[slot]

    companion object {
        val EMPTY = HomeVisuals(
            hero = HeroVisualConfig(
                imageRes = null,
                gradientStartHex = "#FFE4EC",
                gradientMiddleHex = "#FFF0F4",
                gradientEndHex = "#FFF4F7",
                heightDp = 250,
                bottomFadeHeightDp = 0,
                relationshipCardOverlapDp = 0,
            ),
            backgroundOverlayRes = null,
            avatarResByUserId = emptyMap(),
            iconResBySlot = emptyMap(),
        )
    }
}

data class CoupleConfig(
    val leftName: String,
    val rightName: String,
    val currentUserId: String,
    val relationshipLabel: String,
    val relationshipStartDate: LocalDate,
)

data class TopActionMessages(
    val message: String,
    val settings: String,
)

data class QuickRecordConfig(
    val placeholder: String,
    val entryMessage: String,
    val galleryMessage: String,
    val cameraMessage: String,
    val calendarMessage: String,
)

data class BottomNavMessages(
    val messageWall: String,
    val wishlist: String,
    val anniversary: String,
    val album: String,
    val profile: String,
)

data class UserMoodState(
    val id: String,
    val name: String,
    val editable: Boolean,
    val currentMood: MoodOption,
    val showUnrecordedState: Boolean,
)

data class HomeStats(
    val albumCount: Int,
    val wishCompleted: Int,
    val wishTotal: Int,
    val messageUnread: Int,
    val diaryStreak: Int,
    val activeTasks: Int,
)

data class FeatureSpec(
    val id: String,
    val title: String,
    val iconSlot: String,
    val clickMessage: String,
)

data class ActivityRecord(
    val id: String,
    val title: String,
    val iconSlot: String,
    val timestamp: Instant,
    val clickMessage: String,
    val detail: String = title,
    val imagePaths: List<String> = emptyList(),
    val sourceAction: String? = clickMessage,
)

data class HomeSnapshot(
    val visuals: HomeVisuals,
    val couple: CoupleConfig,
    val topActionMessages: TopActionMessages,
    val quickRecord: QuickRecordConfig,
    val bottomNavMessages: BottomNavMessages,
    val users: List<UserMoodState>,
    val stats: HomeStats,
    val features: List<FeatureSpec>,
    val recentActivities: List<ActivityRecord>,
    val recentActivityEmptyText: String,
    val recentActivityListMessage: String,
)

enum class MoodOption(val label: String, val emoji: String) {
    Happy("开心", "😊"),
    Calm("平静", "🙂"),
    Tired("疲惫", "🥱"),
    Sad("难过", "😢"),
    Angry("生气", "😠"),
    Missing("想你", "💌");

    companion object {
        val defaults = entries.toList()

        fun fromLabel(label: String): MoodOption? = entries.firstOrNull { it.label == label }
    }
}
