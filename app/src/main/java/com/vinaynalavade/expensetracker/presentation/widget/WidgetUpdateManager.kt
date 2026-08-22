package com.vinaynalavade.expensetracker.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.vinaynalavade.expensetracker.ExpenseTrackerApp
import com.vinaynalavade.expensetracker.MainActivity
import com.vinaynalavade.expensetracker.QuickAddTransactionActivity
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Centralized widget management and update engine for KharchaFlow.
 * Decouples widget rendering, data fetching, and intent binding from Android AppWidgetProvider lifecycle.
 */
object WidgetUpdateManager {

    const val ACTION_WIDGET_REFRESH = "com.vinaynalavade.expensetracker.ACTION_WIDGET_REFRESH"

    /**
     * Triggers a refresh for all active widget instances (Overview & Quick Add).
     */
    fun refreshAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // 1. Update Financial Summary Widgets
        val overviewIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, ExpenseTrackerWidgetProvider::class.java)
        )
        if (overviewIds.isNotEmpty()) {
            updateFinancialSummaryWidgets(context, appWidgetManager, overviewIds)
        }

        // 2. Update Quick Add Widgets
        val quickAddIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, QuickAddWidgetProvider::class.java)
        )
        if (quickAddIds.isNotEmpty()) {
            updateQuickAddWidgets(context, appWidgetManager, quickAddIds)
        }
    }

    /**
     * Updates all active Financial Summary widget instances with live domain data.
     */
    fun updateFinancialSummaryWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as? ExpenseTrackerApp ?: return
        val container = app.container

        CoroutineScope(Dispatchers.IO).launch {
            val summary = container.getWidgetFinancialSummaryUseCase().firstOrNull()
            val prefs = container.getUserPreferencesUseCase().firstOrNull()
            val currency = prefs?.currency ?: Currency.DEFAULT

            val balanceStr = summary?.balance?.format(currency) ?: "₹0.00"
            val incomeStr = "+${summary?.monthlyIncome?.format(currency) ?: "₹0.00"}"
            val expenseStr = "-${summary?.monthlyExpense?.format(currency) ?: "₹0.00"}"
            val monthLabelStr = summary?.monthLabel ?: "This Month"

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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
                    setTextViewText(R.id.widget_monthly_income, incomeStr)
                    setTextViewText(R.id.widget_monthly_expense, expenseStr)
                    setTextViewText(R.id.widget_month_label, monthLabelStr)

                    setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)
                    setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)
                    setOnClickPendingIntent(R.id.widget_btn_add_expense, expensePendingIntent)
                    setOnClickPendingIntent(R.id.widget_btn_add_income, incomePendingIntent)
                }
                appWidgetManager.updateAppWidget(widgetId, views)
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
    }
}
