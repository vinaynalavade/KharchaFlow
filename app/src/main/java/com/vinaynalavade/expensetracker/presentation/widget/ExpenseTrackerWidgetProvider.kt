package com.vinaynalavade.expensetracker.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.vinaynalavade.expensetracker.ExpenseTrackerApp
import com.vinaynalavade.expensetracker.MainActivity
import com.vinaynalavade.expensetracker.QuickAddTransactionActivity
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Financial Overview AppWidget Provider.
 * Displays live Current Balance, Today's Expenses, and direct action triggers.
 */
class ExpenseTrackerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidget(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val overviewIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ExpenseTrackerWidgetProvider::class.java)
            )
            if (overviewIds.isNotEmpty()) {
                updateWidget(context, appWidgetManager, overviewIds)
            }
            QuickAddWidgetProvider.updateAll(context)
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val app = context.applicationContext as? ExpenseTrackerApp ?: return
            val container = app.container

            CoroutineScope(Dispatchers.IO).launch {
                val summary = container.getFinancialSummaryUseCase().firstOrNull()
                val prefs = container.getUserPreferencesUseCase().firstOrNull()
                val currency = prefs?.currency ?: Currency.DEFAULT

                val todayStartEpoch = DateTimeUtils.getStartOfDayEpoch(LocalDate.now())
                val todayEndEpoch = DateTimeUtils.getEndOfDayEpoch(LocalDate.now())
                val todaySummary = container.getFinancialSummaryUseCase.getByDateRange(todayStartEpoch, todayEndEpoch).firstOrNull()

                val balanceStr = summary?.currentBalance?.format(currency) ?: "₹0.00"
                val todayExpenseAmount = todaySummary?.totalExpense ?: Amount.ZERO
                val todayStr = "Today: - ${todayExpenseAmount.format(currency)}"

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

                // 2. Expense tap -> Directly open QuickAddTransactionActivity over launcher
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

                // 3. Income tap -> Open MainActivity navigating directly to Add Income
                val incomeIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
                        setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)
                        setOnClickPendingIntent(R.id.widget_btn_add_expense, expensePendingIntent)
                        setOnClickPendingIntent(R.id.widget_btn_add_income, incomePendingIntent)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
