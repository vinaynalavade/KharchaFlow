package com.vinaynalavade.expensetracker.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vinaynalavade.expensetracker.ExpenseTrackerApp
import com.vinaynalavade.expensetracker.presentation.widget.WidgetUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receiver to restore scheduled alarms and update widgets when the device reboots,
 * app updates, or system time/timezone/date changes.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                val app = context.applicationContext as? ExpenseTrackerApp ?: return
                val container = app.container

                val pendingResult = goAsync()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        container.rescheduleAllRemindersUseCase()
                        WidgetUpdateManager.refreshAllWidgets(context)
                    } catch (_: Exception) {
                        // Safe execution
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
