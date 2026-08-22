package com.vinaynalavade.expensetracker.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

/**
 * Financial Overview AppWidget Provider.
 * Displays live Current Balance, Monthly Income, Monthly Expenses, and direct action triggers.
 */
class ExpenseTrackerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdateManager.updateFinancialSummaryWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetUpdateManager.ACTION_WIDGET_REFRESH) {
            WidgetUpdateManager.refreshAllWidgets(context)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            WidgetUpdateManager.refreshAllWidgets(context)
        }
    }
}
