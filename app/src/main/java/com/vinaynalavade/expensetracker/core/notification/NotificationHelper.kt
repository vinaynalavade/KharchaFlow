package com.vinaynalavade.expensetracker.core.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vinaynalavade.expensetracker.MainActivity
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.core.receiver.ReminderReceiver
import java.util.Calendar

/**
 * Helper for managing Android notification channels, smart reminder notifications, and alarm scheduling.
 */
object NotificationHelper {

    const val CHANNEL_DAILY_REMINDER = "channel_daily_reminder"
    const val CHANNEL_EMI_REMINDER = "channel_emi_reminder"

    const val NOTIFICATION_ID_DAILY = 1001
    const val NOTIFICATION_ID_EMI_BASE = 2000

    const val ACTION_DAILY_REMINDER = "com.vinaynalavade.expensetracker.ACTION_DAILY_REMINDER"
    const val ACTION_EMI_REMINDER = "com.vinaynalavade.expensetracker.ACTION_EMI_REMINDER"

    const val EXTRA_START_ROUTE = "extra_start_route"
    const val ROUTE_ADD_EXPENSE = "add_expense"
    const val ROUTE_ADD_INCOME = "add_income"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY_REMINDER,
                "Daily Expense Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle daily evening reminder to record your financial transactions"
                enableVibration(true)
            }

            val emiChannel = NotificationChannel(
                CHANNEL_EMI_REMINDER,
                "EMI & Recurring Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Due date reminders for scheduled EMIs and recurring transactions"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(dailyChannel)
            notificationManager.createNotificationChannel(emiChannel)
        }
    }

    fun showDailyReminderNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_ROUTE, ROUTE_ADD_EXPENSE)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.reminder_daily_title)
        val content = context.getString(R.string.reminder_daily_content)

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Add Expense",
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DAILY, notification)
        } catch (_: SecurityException) {
            // Handled safely if permission is revoked
        }
    }

    fun showEmiReminderNotification(context: Context, emiTitle: String, amountString: String, daysRemaining: Int, emiId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val notificationId = NOTIFICATION_ID_EMI_BASE + emiId.toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reminderText = if (daysRemaining == 0) {
            "Your EMI '$emiTitle' of $amountString is due today."
        } else {
            "Your EMI '$emiTitle' of $amountString is due in $daysRemaining day${if (daysRemaining > 1) "s" else ""}."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_EMI_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("EMI Due Reminder")
            .setContentText(reminderText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Handled safely if permission is revoked
        }
    }
}
