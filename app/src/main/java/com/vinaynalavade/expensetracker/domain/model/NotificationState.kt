package com.vinaynalavade.expensetracker.domain.model

enum class BudgetThreshold(val percentage: Int, val label: String) {
    FIFTY(50, "50%"),
    SEVENTY_FIVE(75, "75%"),
    NINETY(90, "90%"),
    HUNDRED(100, "100%"),
    OVER_BUDGET(101, "Over Budget");

    companion object {
        fun fromPercentage(percentage: Int): BudgetThreshold? {
            return entries.firstOrNull { it.percentage == percentage }
        }
    }
}

enum class RecurringReminderAdvance(val days: Int, val label: String) {
    ON_DUE_DATE(0, "On due date"),
    ONE_DAY_BEFORE(1, "1 day before"),
    THREE_DAYS_BEFORE(3, "3 days before"),
    SEVEN_DAYS_BEFORE(7, "7 days before");

    companion object {
        fun fromDays(days: Int): RecurringReminderAdvance {
            return entries.firstOrNull { it.days == days } ?: ONE_DAY_BEFORE
        }
    }
}

enum class NotificationChannelType(val channelId: String, val channelName: String, val description: String) {
    DAILY_REMINDER(
        "channel_daily_reminder",
        "Daily Expense Reminders",
        "Gentle daily reminder to record your financial transactions"
    ),
    BUDGET_ALERTS(
        "channel_budget_alerts",
        "Budget & Spending Alerts",
        "Alerts when reaching or exceeding monthly spending budgets"
    ),
    PAYMENT_REMINDERS(
        "channel_payment_reminders",
        "Bill & Payment Reminders",
        "Due date alerts for upcoming recurring transactions, subscriptions, and EMIs"
    ),
    SAVINGS_GOALS(
        "channel_savings_goals",
        "Savings & Goal Milestones",
        "Updates on savings progress and goal completion"
    ),
    FINANCIAL_INSIGHTS(
        "channel_financial_insights",
        "Financial Insights & Summary",
        "Periodic financial summaries and monthly budget insights"
    )
}
