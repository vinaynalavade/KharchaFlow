package com.vinaynalavade.expensetracker.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.vinaynalavade.expensetracker.core.receiver.ReminderReceiver
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Interface contract for scheduling, cancelling, and recalculating daily reminders.
 */
interface DailyReminderScheduler {
    fun schedule(hour: Int, minute: Int)
    fun cancel()
    fun reschedule()
    fun calculateNextTriggerMillis(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long
}

/**
 * Modern Android AlarmManager implementation using setAndAllowWhileIdle with self-rescheduling.
 */
class AlarmDailyReminderScheduler(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) : DailyReminderScheduler {

    override fun calculateNextTriggerMillis(hour: Int, minute: Int, nowMillis: Long): Long {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        var scheduled = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        // If time has already passed for today, schedule for tomorrow
        if (!scheduled.isAfter(now)) {
            scheduled = scheduled.plusDays(1)
        }

        return scheduled.toInstant().toEpochMilli()
    }

    override fun schedule(hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMillis = calculateNextTriggerMillis(hour, minute)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationHelper.ACTION_DAILY_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationHelper.NOTIFICATION_ID_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Safe fallback if alarm restriction applies
        }
    }

    override fun cancel() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationHelper.ACTION_DAILY_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationHelper.NOTIFICATION_ID_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    override fun reschedule() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = userPreferencesRepository.getUserPreferences().firstOrNull()
                if (prefs?.dailyReminderEnabled == true) {
                    schedule(prefs.dailyReminderHour, prefs.dailyReminderMinute)
                } else {
                    cancel()
                }
            } catch (_: Exception) {
                // Safe failure
            }
        }
    }
}
