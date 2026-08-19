package com.vinaynalavade.expensetracker

import android.app.Application
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.di.AppContainer
import com.vinaynalavade.expensetracker.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseTrackerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Initialize Android notification channels
        NotificationHelper.createNotificationChannels(this)

        // Process any due recurring transactions / salary in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.processDueRecurringTransactionsUseCase()
            } catch (_: Exception) {
                // Safe background launch
            }
        }
    }
}
