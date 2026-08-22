package com.vinaynalavade.expensetracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.vinaynalavade.expensetracker.domain.model.BudgetThreshold
import com.vinaynalavade.expensetracker.domain.repository.NotificationStateRepository

class NotificationStateRepositoryImpl(
    private val context: Context? = null,
    private val prefsName: String = PREFS_NAME,
    injectedPrefs: SharedPreferences? = null
) : NotificationStateRepository {

    companion object {
        const val PREFS_NAME = "kharchaflow_notification_state"
        private const val PREFIX_BUDGET = "notif_budget_"
        private const val PREFIX_RECURRING = "notif_recurring_"
        private const val PREFIX_GOAL = "notif_goal_"
    }

    private val prefs: SharedPreferences by lazy {
        injectedPrefs ?: requireNotNull(context) { "Context or SharedPreferences required" }
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    override fun hasBudgetThresholdFired(monthKey: String, threshold: BudgetThreshold): Boolean {
        val key = "$PREFIX_BUDGET${monthKey}_${threshold.name}"
        return prefs.getBoolean(key, false)
    }

    override fun markBudgetThresholdFired(monthKey: String, threshold: BudgetThreshold) {
        val key = "$PREFIX_BUDGET${monthKey}_${threshold.name}"
        prefs.edit().putBoolean(key, true).apply()
    }

    override fun hasRecurringReminderFired(dateKey: String, recurringId: Long): Boolean {
        val key = "$PREFIX_RECURRING${dateKey}_$recurringId"
        return prefs.getBoolean(key, false)
    }

    override fun markRecurringReminderFired(dateKey: String, recurringId: Long) {
        val key = "$PREFIX_RECURRING${dateKey}_$recurringId"
        prefs.edit().putBoolean(key, true).apply()
    }

    override fun hasGoalMilestoneFired(goalId: String, milestone: Int): Boolean {
        val key = "$PREFIX_GOAL${goalId}_$milestone"
        return prefs.getBoolean(key, false)
    }

    override fun markGoalMilestoneFired(goalId: String, milestone: Int) {
        val key = "$PREFIX_GOAL${goalId}_$milestone"
        prefs.edit().putBoolean(key, true).apply()
    }

    override fun clearOldNotificationState(currentMonthKey: String, currentDateKey: String) {
        val allKeys = prefs.all.keys
        val editor = prefs.edit()
        var changed = false

        for (key in allKeys) {
            if (key.startsWith(PREFIX_BUDGET)) {
                // Remove keys not matching current month
                if (!key.startsWith("$PREFIX_BUDGET$currentMonthKey")) {
                    editor.remove(key)
                    changed = true
                }
            } else if (key.startsWith(PREFIX_RECURRING)) {
                // Remove keys not matching current date
                if (!key.startsWith("$PREFIX_RECURRING$currentDateKey")) {
                    editor.remove(key)
                    changed = true
                }
            }
        }

        if (changed) {
            editor.apply()
        }
    }
}
