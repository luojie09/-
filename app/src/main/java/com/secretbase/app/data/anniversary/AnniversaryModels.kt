package com.secretbase.app.data.anniversary

data class Anniversary(
    val id: String,
    val title: String,
    val date: Long,
    val repeatYearly: Boolean,
    val reminderType: AnniversaryReminder,
    val createdAt: Long,
)

enum class AnniversaryReminder {
    NONE,
    SAME_DAY,
    ONE_DAY_BEFORE,
    THREE_DAYS_BEFORE,
}
