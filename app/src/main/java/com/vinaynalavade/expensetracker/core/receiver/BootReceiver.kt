package com.vinaynalavade.expensetracker.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vinaynalavade.expensetracker.ExpenseTrackerApp
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Receiver to restore scheduled alarms when the device reboots.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as? ExpenseTrackerApp ?: return
            val container = app.container

            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = container.getUserPreferencesUseCase().firstOrNull()
                    if (prefs?.dailyReminderEnabled == true) {
                        NotificationHelper.scheduleDailyReminder(
                            context,
                            prefs.dailyReminderHour,
                            prefs.dailyReminderMinute
                        )
                    }
                } catch (_: Exception) {
                    // Safe execution
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
