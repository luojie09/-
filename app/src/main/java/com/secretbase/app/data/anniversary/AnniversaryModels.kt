package com.secretbase.app.data.anniversary

import kotlinx.serialization.Serializable

@Serializable
data class Anniversary(
    val id: String,
    val title: String,
    val date: Long,
    val repeatYearly: Boolean,
    val reminderType: AnniversaryReminder,
    val createdAt: Long,
    val iconEmoji: String? = null,
)

@Serializable
enum class AnniversaryReminder {
    NONE,
    SAME_DAY,
    ONE_DAY_BEFORE,
    THREE_DAYS_BEFORE,
}
