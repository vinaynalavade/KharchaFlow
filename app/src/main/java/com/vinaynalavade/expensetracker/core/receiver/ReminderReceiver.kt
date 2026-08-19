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
 * Receiver invoked by AlarmManager to handle smart daily reminders and process recurring transactions.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? ExpenseTrackerApp ?: return
        val container = app.container

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Process any due recurring transactions / salary / EMIs deterministically
                container.processDueRecurringTransactionsUseCase()

                if (intent.action == NotificationHelper.ACTION_DAILY_REMINDER) {
                    // 2. Smart Check: Has user recorded any transaction today?
                    val todayStartEpoch = DateTimeUtils.getStartOfDayEpoch(LocalDate.now())
                    val recentTransactions = container.getTransactionsUseCase().firstOrNull() ?: emptyList()
                    val hasTransactionsToday = recentTransactions.any { it.timestamp >= todayStartEpoch }

                    if (!hasTransactionsToday) {
                        NotificationHelper.showDailyReminderNotification(context)
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
