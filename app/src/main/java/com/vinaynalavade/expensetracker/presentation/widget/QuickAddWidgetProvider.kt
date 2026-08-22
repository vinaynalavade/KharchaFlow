package com.vinaynalavade.expensetracker.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * Quick Add Widget Provider.
 * Provides instant shortcut buttons to launch quick transaction recording directly from launcher.
 */
class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdateManager.updateQuickAddWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAll(context: Context) {
            WidgetUpdateManager.refreshAllWidgets(context)
        }
    }
}
