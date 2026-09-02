package com.vinaynalavade.expensetracker.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.vinaynalavade.expensetracker.ExpenseTrackerApp
import com.vinaynalavade.expensetracker.MainActivity
import com.vinaynalavade.expensetracker.QuickAddTransactionActivity
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.di.AppContainer
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Centralized widget management and update engine for KharchaFlow.
 * Decouples widget rendering, data fetching, and intent binding from Android AppWidgetProvider lifecycle.
 *
 * Supports three independent widget types:
 * 1. Total Balance Overview Widget ([ExpenseTrackerWidgetProvider])
 * 2. Today's Expense Glanceable Widget ([TodayExpenseWidgetProvider])
 * 3. Quick Add Action Widget ([QuickAddWidgetProvider])
 *
 * Design constraints for RemoteViews-based widgets:
 * - All layout XML must use only RemoteViews-compatible views (FrameLayout, LinearLayout,
 *   RelativeLayout, GridLayout, TextView, ImageView, Button, etc.). Bare `View` is NOT supported.
 * - Widget providers may be invoked before Application.onCreate() completes; container access
 *   must be guarded with [getContainerSafely].
 * - Flow collection uses one-shot [firstOrNull] to avoid leaking subscriptions.
 * - Each widget type is updated independently so one failure cannot block others.
 */
object WidgetUpdateManager {

    private const val TAG = "WidgetUpdateManager"

    const val ACTION_WIDGET_REFRESH = "com.vinaynalavade.expensetracker.ACTION_WIDGET_REFRESH"
    const val ACTION_TODAY_WIDGET_REFRESH = "com.vinaynalavade.expensetracker.ACTION_TODAY_WIDGET_REFRESH"

    /**
     * Safely obtains the [AppContainer] from the application context.
     * Returns null if the application is not yet initialized (e.g., widget update triggered
     * before Application.onCreate() completes after process restart).
     */
    private fun getContainerSafely(context: Context): AppContainer? {
        val app = context.applicationContext as? ExpenseTrackerApp ?: return null
        if (!app.isContainerInitialized) {
            Log.w(TAG, "AppContainer not yet initialized; widget update deferred.")
            return null
        }
        return app.container
    }

    /**
     * Triggers a refresh for all active widget instances (Total Balance, Today's Expense & Quick Add).
     * Each widget type is updated independently — failure in one does not block others.
     */
    fun refreshAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // 1. Update Total Balance Overview Widgets
        try {
            val balanceIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ExpenseTrackerWidgetProvider::class.java)
            )
            if (balanceIds.isNotEmpty()) {
                updateFinancialSummaryWidgets(context, appWidgetManager, balanceIds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh Total Balance Overview widgets", e)
        }

        // 2. Update Today's Expense Glanceable Widgets
        try {
            val todayIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TodayExpenseWidgetProvider::class.java)
            )
            if (todayIds.isNotEmpty()) {
                updateTodayExpenseWidgets(context, appWidgetManager, todayIds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh Today's Expense widgets", e)
        }

        // 3. Update Quick Add Widgets
        try {
            val quickAddIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, QuickAddWidgetProvider::class.java)
            )
            if (quickAddIds.isNotEmpty()) {
                updateQuickAddWidgets(context, appWidgetManager, quickAddIds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh Quick Add widgets", e)
        }
    }

    /**
     * Updates all active Total Balance widget instances with live domain data.
     * Displays Total Balance as the hero primary metric, alongside Monthly Income & Expenses.
     * When App Lock is active and locked, redacts financial values to prevent privacy leaks.
     */
    fun updateFinancialSummaryWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val container = getContainerSafely(context) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summary = container.getWidgetFinancialSummaryUseCase().firstOrNull()
                val prefs = container.getUserPreferencesUseCase().firstOrNull()
                val currency = prefs?.currency ?: Currency.DEFAULT
                val isLocked = container.appLockManager.isLocked(prefs ?: UserPreferences())

                val balanceStr = if (isLocked) "••••••" else (summary?.balance?.format(currency) ?: "₹0.00")
                val monthlyIncomeStr = if (isLocked) "+••••" else "+${summary?.monthlyIncome?.format(currency) ?: "₹0.00"}"
                val monthlyExpenseStr = if (isLocked) "-••••" else "-${summary?.monthlyExpense?.format(currency) ?: "₹0.00"}"
                val monthLabelStr = if (isLocked) "Protected" else (summary?.monthLabel?.ifBlank { "This Month" } ?: "This Month")

                // 1. Root tap -> Open MainActivity (Dashboard)
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val mainPendingIntent = PendingIntent.getActivity(
                    context,
                    100,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 2. Expense tap -> Open QuickAddTransactionActivity
                val expenseIntent = Intent(context, QuickAddTransactionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(QuickAddTransactionActivity.EXTRA_TRANSACTION_TYPE, TransactionType.EXPENSE.name)
                    putExtra(NotificationHelper.EXTRA_START_ROUTE, NotificationHelper.ROUTE_ADD_EXPENSE)
                }
                val expensePendingIntent = PendingIntent.getActivity(
                    context,
                    101,
                    expenseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 3. Income tap -> Open QuickAddTransactionActivity
                val incomeIntent = Intent(context, QuickAddTransactionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(QuickAddTransactionActivity.EXTRA_TRANSACTION_TYPE, TransactionType.INCOME.name)
                    putExtra(NotificationHelper.EXTRA_START_ROUTE, NotificationHelper.ROUTE_ADD_INCOME)
                }
                val incomePendingIntent = PendingIntent.getActivity(
                    context,
                    102,
                    incomeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 4. Refresh icon tap -> Broadcast to update widget
                val refreshIntent = Intent(context, ExpenseTrackerWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_REFRESH
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    103,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_expense_tracker).apply {
                        setTextViewText(R.id.widget_total_balance, balanceStr)
                        setTextViewText(R.id.widget_monthly_income, monthlyIncomeStr)
                        setTextViewText(R.id.widget_monthly_expense, monthlyExpenseStr)
                        setTextViewText(R.id.widget_month_label, monthLabelStr)

                        setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)
                        setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)
                        setOnClickPendingIntent(R.id.widget_btn_add_expense, expensePendingIntent)
                        setOnClickPendingIntent(R.id.widget_btn_add_income, incomePendingIntent)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating Total Balance widgets", e)
            }
        }
    }

    /**
     * Updates all active Today's Expense widget instances with live domain data.
     * Displays Today's spending as a single glanceable metric.
     * When App Lock is active and locked, redacts the expense metric.
     */
    fun updateTodayExpenseWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val container = getContainerSafely(context) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val todayExpense = container.getTodayExpenseUseCase().firstOrNull()
                val prefs = container.getUserPreferencesUseCase().firstOrNull()
                val currency = prefs?.currency ?: Currency.DEFAULT
                val isLocked = container.appLockManager.isLocked(prefs ?: UserPreferences())

                val todayExpenseStr = if (isLocked) "••••••" else (todayExpense?.format(currency) ?: "₹0.00")

                // 1. Root tap -> Open MainActivity (Dashboard)
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val mainPendingIntent = PendingIntent.getActivity(
                    context,
                    300,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 2. Refresh icon tap -> Broadcast to update widget
                val refreshIntent = Intent(context, TodayExpenseWidgetProvider::class.java).apply {
                    action = ACTION_TODAY_WIDGET_REFRESH
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    301,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_today_expense).apply {
                        setTextViewText(R.id.today_widget_amount, todayExpenseStr)

                        setOnClickPendingIntent(R.id.today_widget_root, mainPendingIntent)
                        setOnClickPendingIntent(R.id.today_widget_btn_refresh, refreshPendingIntent)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating Today's Expense widgets", e)
            }
        }
    }

    /**
     * Updates Quick Add widget instances with instant transaction entry action buttons.
     */
    fun updateQuickAddWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                200,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val expenseIntent = Intent(context, QuickAddTransactionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(QuickAddTransactionActivity.EXTRA_TRANSACTION_TYPE, TransactionType.EXPENSE.name)
                putExtra(NotificationHelper.EXTRA_START_ROUTE, NotificationHelper.ROUTE_ADD_EXPENSE)
            }
            val expensePendingIntent = PendingIntent.getActivity(
                context,
                201,
                expenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val incomeIntent = Intent(context, QuickAddTransactionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(QuickAddTransactionActivity.EXTRA_TRANSACTION_TYPE, TransactionType.INCOME.name)
                putExtra(NotificationHelper.EXTRA_START_ROUTE, NotificationHelper.ROUTE_ADD_INCOME)
            }
            val incomePendingIntent = PendingIntent.getActivity(
                context,
                202,
                incomeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_quick_add).apply {
                    setOnClickPendingIntent(R.id.quick_widget_root, mainPendingIntent)
                    setOnClickPendingIntent(R.id.quick_widget_btn_expense, expensePendingIntent)
                    setOnClickPendingIntent(R.id.quick_widget_btn_income, incomePendingIntent)
                }
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Quick Add widgets", e)
        }
    }
}
