package com.secretbase.app.data.anniversary

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.secretbase.app.MainActivity
import com.secretbase.app.R
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class AnniversaryNotificationScheduler(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        createNotificationChannel(appContext)
    }

    fun sync(items: List<Anniversary>) {
        val currentIds = items.map(Anniversary::id).toSet()
        val previousIds = preferences.getStringSet(KEY_SCHEDULED_IDS, emptySet()).orEmpty()
        (previousIds - currentIds).forEach(::cancel)
        items.forEach(::schedule)
        preferences.edit().putStringSet(KEY_SCHEDULED_IDS, currentIds).apply()
    }

    fun schedule(item: Anniversary) {
        if (item.reminderType == AnniversaryReminder.NONE) {
            cancel(item.id)
            return
        }
        val triggerAt = nextAnniversaryReminderTrigger(item) ?: run {
            cancel(item.id)
            return
        }
        val delayMillis = Duration.between(ZonedDateTime.now(), triggerAt)
            .toMillis()
            .coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<AnniversaryNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(item.toWorkData())
            .build()
        workManager.enqueueUniqueWork(
            workName(item.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(anniversaryId: String) {
        workManager.cancelUniqueWork(workName(anniversaryId))
    }

    private fun Anniversary.toWorkData(): Data = Data.Builder()
        .putString(AnniversaryNotificationWorker.KEY_ID, id)
        .putString(AnniversaryNotificationWorker.KEY_TITLE, title)
        .putLong(AnniversaryNotificationWorker.KEY_DATE, date)
        .putBoolean(AnniversaryNotificationWorker.KEY_REPEAT_YEARLY, repeatYearly)
        .putString(AnniversaryNotificationWorker.KEY_REMINDER, reminderType.name)
        .putLong(AnniversaryNotificationWorker.KEY_CREATED_AT, createdAt)
        .putString(AnniversaryNotificationWorker.KEY_ICON, iconEmoji)
        .build()

    private companion object {
        const val PREFERENCES_NAME = "anniversary_notification_schedule"
        const val KEY_SCHEDULED_IDS = "scheduled_ids"

        fun workName(id: String) = "anniversary-reminder-$id"
    }
}

internal fun nextAnniversaryReminderTrigger(
    item: Anniversary,
    now: ZonedDateTime = ZonedDateTime.now(),
): ZonedDateTime? {
    if (item.reminderType == AnniversaryReminder.NONE) return null
    val zone = now.zone
    val originalDate = Instant.ofEpochMilli(item.date).atZone(zone).toLocalDate()
    var eventDate = if (item.repeatYearly) originalDate.atYear(now.year) else originalDate
    var trigger = eventDate
        .minusDays(item.reminderType.daysBefore)
        .atTime(ANNIVERSARY_NOTIFICATION_TIME)
        .atZone(zone)
    if (!trigger.isAfter(now) && item.repeatYearly) {
        eventDate = originalDate.atYear(now.year + 1)
        trigger = eventDate
            .minusDays(item.reminderType.daysBefore)
            .atTime(ANNIVERSARY_NOTIFICATION_TIME)
            .atZone(zone)
    }
    return trigger.takeIf { it.isAfter(now) }
}

private val AnniversaryReminder.daysBefore: Long
    get() = when (this) {
        AnniversaryReminder.NONE,
        AnniversaryReminder.SAME_DAY -> 0L
        AnniversaryReminder.ONE_DAY_BEFORE -> 1L
        AnniversaryReminder.THREE_DAYS_BEFORE -> 3L
    }

private fun LocalDate.atYear(year: Int): LocalDate = LocalDate.of(
    year,
    month,
    dayOfMonth.coerceAtMost(YearMonth.of(year, month).lengthOfMonth()),
)

class AnniversaryNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val item = inputData.toAnniversary() ?: return Result.failure()
        createNotificationChannel(applicationContext)

        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (canNotify) {
            NotificationManagerCompat.from(applicationContext).notify(
                item.id.hashCode(),
                buildNotification(applicationContext, item),
            )
        }

        if (item.repeatYearly) {
            AnniversaryNotificationScheduler(applicationContext).schedule(item)
        }
        return Result.success()
    }

    private fun Data.toAnniversary(): Anniversary? {
        val id = getString(KEY_ID) ?: return null
        val title = getString(KEY_TITLE) ?: return null
        val reminder = getString(KEY_REMINDER)
            ?.let { value -> AnniversaryReminder.entries.firstOrNull { it.name == value } }
            ?: return null
        return Anniversary(
            id = id,
            title = title,
            date = getLong(KEY_DATE, 0L),
            repeatYearly = getBoolean(KEY_REPEAT_YEARLY, false),
            reminderType = reminder,
            createdAt = getLong(KEY_CREATED_AT, 0L),
            iconEmoji = getString(KEY_ICON),
        )
    }

    companion object {
        const val KEY_ID = "anniversary_id"
        const val KEY_TITLE = "anniversary_title"
        const val KEY_DATE = "anniversary_date"
        const val KEY_REPEAT_YEARLY = "repeat_yearly"
        const val KEY_REMINDER = "reminder_type"
        const val KEY_CREATED_AT = "created_at"
        const val KEY_ICON = "icon_emoji"
    }
}

private fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        ANNIVERSARY_CHANNEL_ID,
        "纪念日提醒",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "提醒你们即将到来的重要日子"
    }
    context.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(channel)
}

private fun buildNotification(context: Context, item: Anniversary): android.app.Notification {
    val today = LocalDate.now()
    val eventDate = Instant.ofEpochMilli(item.date)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .let { original ->
            if (item.repeatYearly) {
                val thisYear = original.atYear(today.year)
                if (thisYear.isBefore(today)) original.atYear(today.year + 1) else thisYear
            } else {
                original
            }
        }
    val days = ChronoUnit.DAYS.between(today, eventDate).coerceAtLeast(0L)
    val content = if (days == 0L) {
        "今天是${item.title}"
    } else {
        "距离${item.title}还有 $days 天"
    }
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(MainActivity.EXTRA_INITIAL_ROUTE, MainActivity.DESTINATION_ANNIVERSARY)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        item.id.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return NotificationCompat.Builder(context, ANNIVERSARY_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_calendar_nav)
        .setContentTitle(listOfNotNull(item.iconEmoji, item.title).joinToString(" "))
        .setContentText(content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
}

private const val ANNIVERSARY_CHANNEL_ID = "secret_base_anniversary_reminders"
private val ANNIVERSARY_NOTIFICATION_TIME: LocalTime = LocalTime.of(9, 0)
