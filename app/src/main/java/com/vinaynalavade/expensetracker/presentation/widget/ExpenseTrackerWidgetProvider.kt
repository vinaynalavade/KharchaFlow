package com.vinaynalavade.expensetracker.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.vinaynalavade.expensetracker.ExpenseTrackerApp
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

class ExpenseTrackerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidget(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ExpenseTrackerWidgetProvider::class.java)
            )
            updateWidget(context, appWidgetManager, ids)
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val app = context.applicationContext as? ExpenseTrackerApp ?: return
            val container = app.container

            CoroutineScope(Dispatchers.IO).launch {
                val summary = container.getFinancialSummaryUseCase().firstOrNull()
                val prefs = container.getUserPreferencesUseCase().firstOrNull()
                val currency = prefs?.currency ?: com.vinaynalavade.expensetracker.core.model.Currency.DEFAULT

                val todayStartEpoch = DateTimeUtils.getStartOfDayEpoch(LocalDate.now())
                val allTx = container.getTransactionsUseCase().firstOrNull() ?: emptyList()
                val todayExpenseSubunits = allTx
                    .filter { it.type == TransactionType.EXPENSE && it.timestamp >= todayStartEpoch }
                    .sumOf { it.amount.subunits }

                val balanceStr = summary?.currentBalance?.format(currency) ?: "₹0.00"
                val todayStr = "Today: - ${Amount(todayExpenseSubunits).format(currency)}"

                // Intent for Add Expense
                val expenseIntent = Intent(context, com.vinaynalavade.expensetracker.QuickAddTransactionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(NotificationHelper.EXTRA_START_ROUTE, NotificationHelper.ROUTE_ADD_EXPENSE)
                }
                val expensePendingIntent = PendingIntent.getActivity(
                    context,
                    101,
                    expenseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Intent for Add Income
                val incomeIntent = Intent(context, com.vinaynalavade.expensetracker.QuickAddTransactionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(NotificationHelper.EXTRA_START_ROUTE, NotificationHelper.ROUTE_ADD_INCOME)
                }
                val incomePendingIntent = PendingIntent.getActivity(
                    context,
                    102,
                    incomeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_expense_tracker).apply {
                        setTextViewText(R.id.widget_total_balance, balanceStr)
                        setTextViewText(R.id.widget_today_expense, todayStr)
                        setOnClickPendingIntent(R.id.widget_btn_add_expense, expensePendingIntent)
                        setOnClickPendingIntent(R.id.widget_btn_add_income, incomePendingIntent)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
