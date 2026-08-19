package com.vinaynalavade.expensetracker.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.vinaynalavade.expensetracker.QuickAddTransactionActivity
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper

class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val expenseIntent = Intent(context, QuickAddTransactionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
                setOnClickPendingIntent(R.id.quick_widget_btn_expense, expensePendingIntent)
                setOnClickPendingIntent(R.id.quick_widget_btn_income, incomePendingIntent)
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
