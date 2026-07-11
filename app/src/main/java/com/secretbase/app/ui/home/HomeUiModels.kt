package com.secretbase.app.ui.home

import androidx.annotation.DrawableRes
import com.secretbase.app.AppActions
import com.secretbase.app.data.BottomNavMessages
import com.secretbase.app.data.FeatureSpec
import com.secretbase.app.data.HomeSnapshot
import com.secretbase.app.data.HomeVisuals
import com.secretbase.app.data.MoodOption
import com.secretbase.app.data.QuickRecordConfig
import com.secretbase.app.data.TopActionMessages
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quickNoteText: String = "",
    val editingMoodUserId: String? = null,
    val payload: HomePayload? = null,
)

data class HomePayload(
    val greeting: String,
    val coupleDisplayName: String,
    val relationship: RelationshipUiModel,
    val visuals: HomeVisuals,
    val quickRecord: QuickRecordConfig,
    val topActionMessages: TopActionMessages,
    val bottomNavMessages: BottomNavMessages,
    val moodOptions: List<MoodOption>,
    val moodCards: List<MoodCardUiModel>,
    val featureCards: List<FeatureCardUiModel>,
    val activities: List<ActivityUiModel>,
    val recentActivityEmptyText: String,
    val recentActivityListMessage: String,
    val messageDotVisible: Boolean,
)

data class RelationshipUiModel(
    val label: String,
    val daysTogether: Int,
    val startDateText: String,
    val anniversaryTitle: String,
    val daysUntilAnniversary: Int,
    val anniversaryCountdownLabel: String,
    val anniversaryProgress: Float,
)

data class HomeUpcomingAnniversary(
    val title: String,
    val eventDate: LocalDate,
    val cycleStartDate: LocalDate,
)

data class MoodCardUiModel(
    val userId: String,
    val displayName: String,
    val moodLabel: String,
    val moodEmoji: String,
    val editable: Boolean,
    @DrawableRes val avatarRes: Int?,
)

data class FeatureCardUiModel(
    val id: String,
    val title: String,
    val summary: String,
    val clickMessage: String,
    @DrawableRes val iconRes: Int?,
)

data class ActivityUiModel(
    val id: String,
    val title: String,
    val relativeTime: String,
    val clickMessage: String,
    @DrawableRes val iconRes: Int?,
)

fun HomeSnapshot.toUiState(
    now: ZonedDateTime,
    quickNoteText: String,
    editingMoodUserId: String? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    upcomingAnniversary: HomeUpcomingAnniversary? = null,
): HomeUiState {
    val relationship = buildRelationshipUi(
        today = now.toLocalDate(),
        upcomingAnniversary = upcomingAnniversary,
    )
    return HomeUiState(
        isLoading = isLoading,
        errorMessage = errorMessage,
        quickNoteText = quickNoteText,
        editingMoodUserId = editingMoodUserId,
        payload = HomePayload(
            greeting = greetingAt(now.hour),
            coupleDisplayName = "${couple.leftName}\u2764\uFE0F${couple.rightName}",
            relationship = relationship,
            visuals = visuals,
            quickRecord = quickRecord,
            topActionMessages = topActionMessages.copy(message = AppActions.OpenMessageWall),
            bottomNavMessages = bottomNavMessages.copy(anniversary = AppActions.OpenAnniversary),
            moodOptions = MoodOption.defaults,
            moodCards = users.map { user ->
                MoodCardUiModel(
                    userId = user.id,
                    displayName = user.name,
                    moodLabel = if (user.showUnrecordedState) "今天还未记录" else user.currentMood.label,
                    moodEmoji = if (user.showUnrecordedState) "..." else user.currentMood.emoji,
                    editable = user.editable,
                    avatarRes = visuals.avatar(user.id),
                )
            },
            featureCards = features.map { feature ->
                FeatureCardUiModel(
                    id = feature.id,
                    title = feature.title,
                    summary = summaryForFeature(feature.id, relationship.daysUntilAnniversary),
                    clickMessage = featureAction(feature),
                    iconRes = visuals.icon(feature.iconSlot),
                )
            },
            activities = recentActivities
                .sortedByDescending { it.timestamp }
                .take(2)
                .map { activity ->
                    ActivityUiModel(
                        id = activity.id,
                        title = activity.title,
                        relativeTime = formatRelativeTime(activity.timestamp, now),
                        clickMessage = activity.clickMessage,
                        iconRes = visuals.icon(activity.iconSlot),
                    )
                },
            recentActivityEmptyText = recentActivityEmptyText,
            recentActivityListMessage = recentActivityListMessage,
            messageDotVisible = stats.messageUnread > 0,
        ),
    )
}

private fun HomeSnapshot.buildRelationshipUi(
    today: LocalDate,
    upcomingAnniversary: HomeUpcomingAnniversary?,
): RelationshipUiModel {
    val startDate = couple.relationshipStartDate
    val anniversaryThisYear = startDate.withYear(today.year)
    val fallbackAnniversary = when {
        today.isBefore(anniversaryThisYear) -> anniversaryThisYear
        today.isAfter(anniversaryThisYear) -> anniversaryThisYear.plusYears(1)
        today.year == startDate.year -> anniversaryThisYear.plusYears(1)
        else -> anniversaryThisYear
    }
    val nextAnniversary = upcomingAnniversary?.eventDate ?: fallbackAnniversary
    val cycleStart = maxOf(
        startDate,
        upcomingAnniversary?.cycleStartDate ?: nextAnniversary.minusYears(1),
    )
    val daysTogether = max(1, ChronoUnit.DAYS.between(startDate, today).toInt() + 1)
    val totalCycleDays = max(1L, ChronoUnit.DAYS.between(cycleStart, nextAnniversary))
    val elapsedCycleDays = ChronoUnit.DAYS.between(cycleStart, today).coerceIn(0, totalCycleDays)
    val remainingDays = ChronoUnit.DAYS.between(today, nextAnniversary).toInt()

    return RelationshipUiModel(
        label = couple.relationshipLabel,
        daysTogether = daysTogether,
        startDateText = "${startDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))} ${startDate.chineseWeekday()}",
        anniversaryTitle = upcomingAnniversary?.title ?: anniversaryTitle(startDate, nextAnniversary),
        daysUntilAnniversary = remainingDays,
        anniversaryCountdownLabel = if (remainingDays == 0) "就是今天" else "还有 $remainingDays 天",
        anniversaryProgress = (elapsedCycleDays.toFloat() / totalCycleDays.toFloat()).coerceIn(0f, 1f),
    )
}

private fun LocalDate.chineseWeekday(): String =
    when (dayOfWeek.value) {
        1 -> "\u661f\u671f\u4e00"
        2 -> "\u661f\u671f\u4e8c"
        3 -> "\u661f\u671f\u4e09"
        4 -> "\u661f\u671f\u56db"
        5 -> "\u661f\u671f\u4e94"
        6 -> "\u661f\u671f\u516d"
        else -> "\u661f\u671f\u65e5"
    }

private fun HomeSnapshot.summaryForFeature(featureId: String, anniversaryDays: Int): String =
    when (featureId) {
        "anniversary" -> if (anniversaryDays == 0) "就是今天" else "${anniversaryDays}天后"
        "album" -> "${stats.albumCount}张"
        "wishlist" -> "${stats.wishCompleted} / ${stats.wishTotal} 完成"
        "messageWall" -> if (stats.messageUnread == 0) "暂无新消息" else "${stats.messageUnread}条新消息"
        "diary" -> if (stats.diaryStreak == 0) "尚未记录" else "已连续${stats.diaryStreak}天"
        "tasks" -> if (stats.activeTasks == 0) "暂无任务" else "${stats.activeTasks}项进行中"
        else -> ""
    }

private fun featureAction(feature: FeatureSpec): String =
    when (feature.id) {
        "messageWall" -> AppActions.OpenMessageWall
        "wishlist" -> AppActions.OpenWishList
        "anniversary" -> AppActions.OpenAnniversary
        else -> feature.clickMessage
    }

private fun greetingAt(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

private fun anniversaryTitle(startDate: LocalDate, nextAnniversary: LocalDate): String {
    val yearCount = ChronoUnit.YEARS.between(startDate, nextAnniversary).toInt()
    return "恋爱${toChineseNumber(max(1, yearCount))}周年"
}

private fun toChineseNumber(value: Int): String {
    val digits = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    return when {
        value < 10 -> digits[value]
        value < 20 -> "十" + if (value == 10) "" else digits[value % 10]
        value < 100 -> digits[value / 10] + "十" + if (value % 10 == 0) "" else digits[value % 10]
        else -> value.toString()
    }
}

private fun formatRelativeTime(instant: Instant, now: ZonedDateTime): String {
    val activityTime = instant.atZone(ZoneId.systemDefault())
    val minutes = ChronoUnit.MINUTES.between(activityTime, now)
    val hours = ChronoUnit.HOURS.between(activityTime, now)
    val days = ChronoUnit.DAYS.between(activityTime.toLocalDate(), now.toLocalDate())
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        activityTime.toLocalDate() == now.toLocalDate() -> "${hours}小时前"
        days == 1L -> "昨天"
        activityTime.year == now.year -> activityTime.format(DateTimeFormatter.ofPattern("M月d日"))
        else -> activityTime.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    }
}
