package com.secretbase.app.data.anniversary

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnniversaryNotificationSchedulerTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun recurringReminder_rollsForwardAndAppliesOffset() {
        val item = anniversary(
            date = LocalDate.of(2026, 5, 6),
            repeatYearly = true,
            reminder = AnniversaryReminder.THREE_DAYS_BEFORE,
        )
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 7, 17),
            LocalTime.NOON,
            zone,
        )

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2027, 5, 3), LocalTime.of(9, 0), zone),
            nextAnniversaryReminderTrigger(item, now),
        )
    }

    @Test
    fun pastNonRecurringReminder_isNotScheduled() {
        val item = anniversary(
            date = LocalDate.of(2026, 5, 6),
            repeatYearly = false,
            reminder = AnniversaryReminder.SAME_DAY,
        )
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 7, 17),
            LocalTime.NOON,
            zone,
        )

        assertNull(nextAnniversaryReminderTrigger(item, now))
    }

    @Test
    fun leapDayReminder_fallsBackToFebruary28InCommonYear() {
        val item = anniversary(
            date = LocalDate.of(2024, 2, 29),
            repeatYearly = true,
            reminder = AnniversaryReminder.SAME_DAY,
        )
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 1, 1),
            LocalTime.NOON,
            zone,
        )

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2026, 2, 28), LocalTime.of(9, 0), zone),
            nextAnniversaryReminderTrigger(item, now),
        )
    }

    private fun anniversary(
        date: LocalDate,
        repeatYearly: Boolean,
        reminder: AnniversaryReminder,
    ) = Anniversary(
        id = "test",
        title = "纪念日",
        date = date.atStartOfDay(zone).toInstant().toEpochMilli(),
        repeatYearly = repeatYearly,
        reminderType = reminder,
        createdAt = 0L,
    )
}
