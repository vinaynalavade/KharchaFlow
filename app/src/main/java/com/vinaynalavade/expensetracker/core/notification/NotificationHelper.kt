package com.vinaynalavade.expensetracker.core.notification

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
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.domain.model.BudgetThreshold
import com.vinaynalavade.expensetracker.domain.model.NotificationChannelType

/**
 * Production-ready helper for managing Android notification channels, notification builders, and action routing.
 */
object NotificationHelper {

    const val CHANNEL_DAILY_REMINDER = "channel_daily_reminder"
    const val CHANNEL_BUDGET_ALERTS = "channel_budget_alerts"
    const val CHANNEL_PAYMENT_REMINDERS = "channel_payment_reminders"
    const val CHANNEL_SAVINGS_GOALS = "channel_savings_goals"
    const val CHANNEL_FINANCIAL_INSIGHTS = "channel_financial_insights"

    const val NOTIFICATION_ID_DAILY = 1001
    const val NOTIFICATION_ID_BUDGET_BASE = 2000
    const val NOTIFICATION_ID_RECURRING_BASE = 3000
    const val NOTIFICATION_ID_GOAL_BASE = 4000

    const val ACTION_DAILY_REMINDER = "com.vinaynalavade.expensetracker.ACTION_DAILY_REMINDER"
    const val ACTION_CHECK_FINANCIAL_REMINDERS = "com.vinaynalavade.expensetracker.ACTION_CHECK_FINANCIAL_REMINDERS"
    const val ACTION_EMI_REMINDER = "com.vinaynalavade.expensetracker.ACTION_EMI_REMINDER"

    const val EXTRA_START_ROUTE = "extra_start_route"
    const val ROUTE_ADD_EXPENSE = "add_expense"
    const val ROUTE_ADD_INCOME = "add_income"
    const val ROUTE_TRANSACTIONS = "transactions"
    const val ROUTE_RECURRING = "recurring_transactions"
    const val ROUTE_DASHBOARD = "dashboard"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_DAILY_REMINDER,
                    NotificationChannelType.DAILY_REMINDER.channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = NotificationChannelType.DAILY_REMINDER.description
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_BUDGET_ALERTS,
                    NotificationChannelType.BUDGET_ALERTS.channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = NotificationChannelType.BUDGET_ALERTS.description
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_PAYMENT_REMINDERS,
                    NotificationChannelType.PAYMENT_REMINDERS.channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = NotificationChannelType.PAYMENT_REMINDERS.description
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_SAVINGS_GOALS,
                    NotificationChannelType.SAVINGS_GOALS.channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = NotificationChannelType.SAVINGS_GOALS.description
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_FINANCIAL_INSIGHTS,
                    NotificationChannelType.FINANCIAL_INSIGHTS.channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = NotificationChannelType.FINANCIAL_INSIGHTS.description
                    enableVibration(false)
                }
            )

            for (channel in channels) {
                notificationManager.createNotificationChannel(channel)
            }
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
            // Handled safely
        }
    }

    fun showBudgetAlertNotification(
        context: Context,
        threshold: BudgetThreshold,
        spentAmount: Amount,
        budgetLimit: Amount,
        currency: Currency
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_ROUTE, ROUTE_TRANSACTIONS)
        }

        val notificationId = NOTIFICATION_ID_BUDGET_BASE + threshold.percentage

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (threshold) {
            BudgetThreshold.FIFTY -> "Budget Alert: 50% Reached"
            BudgetThreshold.SEVENTY_FIVE -> "Budget Warning: 75% Reached"
            BudgetThreshold.NINETY -> "Budget Critical: 90% Reached"
            BudgetThreshold.HUNDRED -> "Monthly Budget Limit Reached (100%)"
            BudgetThreshold.OVER_BUDGET -> "Over Budget Alert!"
        }

        val content = when (threshold) {
            BudgetThreshold.OVER_BUDGET -> "You have exceeded your monthly budget of ${budgetLimit.format(currency)} by spending ${spentAmount.format(currency)}."
            else -> "You have spent ${spentAmount.format(currency)} of your ${budgetLimit.format(currency)} monthly limit (${threshold.label})."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "View Transactions",
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Handled safely
        }
    }

    fun showRecurringPaymentNotification(
        context: Context,
        title: String,
        amountString: String,
        daysRemaining: Int,
        recurringId: Long
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_ROUTE, ROUTE_RECURRING)
        }

        val notificationId = NOTIFICATION_ID_RECURRING_BASE + (recurringId % 1000).toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reminderText = if (daysRemaining == 0) {
            "Your payment '$title' of $amountString is due today."
        } else {
            "Your payment '$title' of $amountString is due in $daysRemaining day${if (daysRemaining > 1) "s" else ""}."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_PAYMENT_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Payment Reminder")
            .setContentText(reminderText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminderText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "View Recurring",
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Handled safely
        }
    }

    fun showGoalMilestoneNotification(
        context: Context,
        title: String,
        percentage: Int,
        goalId: Long = 1L
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_ROUTE, ROUTE_DASHBOARD)
        }

        val notificationId = NOTIFICATION_ID_GOAL_BASE + (goalId % 1000).toInt() + percentage

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val milestoneText = if (percentage >= 100) {
            "Congratulations! You have completed your goal '$title'."
        } else {
            "You have achieved $percentage% of your goal '$title'."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SAVINGS_GOALS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Goal Milestone Achieved")
            .setContentText(milestoneText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(milestoneText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Handled safely
        }
    }
}
