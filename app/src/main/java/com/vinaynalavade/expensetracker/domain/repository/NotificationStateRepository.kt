package com.vinaynalavade.expensetracker.domain.repository

import com.vinaynalavade.expensetracker.domain.model.BudgetThreshold

/**
 * Repository interface to persist and query notification delivery states.
 * Guarantees no duplicate notifications are sent for budget thresholds, recurring reminders, or goal milestones.
 */
interface NotificationStateRepository {
    fun hasBudgetThresholdFired(monthKey: String, threshold: BudgetThreshold): Boolean
    fun markBudgetThresholdFired(monthKey: String, threshold: BudgetThreshold)
    fun hasRecurringReminderFired(dateKey: String, recurringId: Long): Boolean
    fun markRecurringReminderFired(dateKey: String, recurringId: Long)
    fun hasGoalMilestoneFired(goalId: String, milestone: Int): Boolean
    fun markGoalMilestoneFired(goalId: String, milestone: Int)
    fun clearOldNotificationState(currentMonthKey: String, currentDateKey: String)
}
