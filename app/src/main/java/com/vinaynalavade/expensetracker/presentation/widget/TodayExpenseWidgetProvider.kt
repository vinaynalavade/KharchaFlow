package com.vinaynalavade.expensetracker.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

/**
 * Today's Expense AppWidget Provider.
 * Displays a single glanceable metric: total expense spending for the current calendar day.
 */
class TodayExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdateManager.updateTodayExpenseWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetUpdateManager.ACTION_TODAY_WIDGET_REFRESH) {
            WidgetUpdateManager.refreshAllWidgets(context)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            WidgetUpdateManager.refreshAllWidgets(context)
        }
    }
}
