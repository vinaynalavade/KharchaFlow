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
 * Suppresses daily reminders if any transaction (Expense or Income) was already recorded today.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? ExpenseTrackerApp ?: return
        val container = app.container

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    NotificationHelper.ACTION_DAILY_REMINDER -> {
                        val prefs = container.getUserPreferencesUseCase().firstOrNull()

                        if (prefs != null && prefs.notificationsMasterEnabled && prefs.dailyReminderEnabled) {
                            // 1. Smart Check: Has user recorded any transaction today?
                            val todayStartEpoch = DateTimeUtils.getStartOfDayEpoch(LocalDate.now())
                            val todayEndEpoch = DateTimeUtils.getEndOfDayEpoch(LocalDate.now())

                            val todayTransactions = container.transactionRepository
                                .getTransactionsBetween(todayStartEpoch, todayEndEpoch)
                                .firstOrNull() ?: emptyList()

                            // Suppress notification if at least 1 transaction already recorded today
                            if (todayTransactions.isEmpty()) {
                                NotificationHelper.showDailyReminderNotification(context)
                            }

                            // 2. Schedule next occurrence for tomorrow at the configured time
                            container.dailyReminderScheduler.schedule(
                                prefs.dailyReminderHour,
                                prefs.dailyReminderMinute
                            )
                        } else {
                            container.dailyReminderScheduler.cancel()
                        }
                    }

                    NotificationHelper.ACTION_CHECK_FINANCIAL_REMINDERS,
                    NotificationHelper.ACTION_EMI_REMINDER -> {
                        // Process due recurring transactions, budget alerts, and upcoming bills
                        container.processFinancialRemindersUseCase()

                        val prefs = container.getUserPreferencesUseCase().firstOrNull()
                        if (prefs != null && prefs.notificationsMasterEnabled &&
                            (prefs.budgetAlertsEnabled || prefs.recurringRemindersEnabled)
                        ) {
                            container.dailyReminderScheduler.scheduleFinancialChecks()
                        }
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
