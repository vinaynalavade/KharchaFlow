package com.vinaynalavade.expensetracker.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vinaynalavade.expensetracker.ExpenseTrackerApp
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Receiver invoked by AlarmManager to handle smart daily reminders and recurring transaction processing.
 * Suppresses notifications if any transaction (Expense or Income) was already recorded today.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? ExpenseTrackerApp ?: return
        val container = app.container

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Process any due recurring transactions / salary deterministically
                container.processDueRecurringTransactionsUseCase()

                if (intent.action == NotificationHelper.ACTION_DAILY_REMINDER) {
                    val prefs = container.getUserPreferencesUseCase().firstOrNull()

                    if (prefs?.dailyReminderEnabled == true) {
                        // 2. Smart Check: Has user recorded any transaction today (between 00:00 and 23:59)?
                        val todayStartEpoch = DateTimeUtils.getStartOfDayEpoch(LocalDate.now())
                        val todayEndEpoch = DateTimeUtils.getEndOfDayEpoch(LocalDate.now())

                        val todayTransactions = container.transactionRepository
                            .getTransactionsBetween(todayStartEpoch, todayEndEpoch)
                            .firstOrNull() ?: emptyList()

                        // Suppress notification if at least 1 transaction already recorded today
                        if (todayTransactions.isEmpty()) {
                            NotificationHelper.showDailyReminderNotification(context)
                        }

                        // 3. Schedule next occurrence for tomorrow at the configured time
                        container.dailyReminderScheduler.schedule(
                            prefs.dailyReminderHour,
                            prefs.dailyReminderMinute
                        )
                    } else {
                        // Reminder is disabled, ensure cancelled
                        container.dailyReminderScheduler.cancel()
                    }
                }
            } catch (_: Exception) {
                // Fail silently without crashing
            } finally {
                pendingResult.finish()
            }
        }
    }
}
