package com.vinaynalavade.expensetracker

import android.app.Application
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.di.AppContainer
import com.vinaynalavade.expensetracker.di.DefaultAppContainer
import com.vinaynalavade.expensetracker.presentation.widget.ExpenseTrackerWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseTrackerApp : Application() {

    lateinit var container: AppContainer
        private set

    val isContainerInitialized: Boolean
        get() = ::container.isInitialized

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Initialize Android notification channels
        NotificationHelper.createNotificationChannels(this)

        // Process any due recurring transactions / salary in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = container.processDueRecurringTransactionsUseCase()
                if (result is AppResult.Success && result.data > 0) {
                    ExpenseTrackerWidgetProvider.updateAll(this@ExpenseTrackerApp)
                }
            } catch (_: Exception) {
                // Safe background launch
            }
        }
    }
}
