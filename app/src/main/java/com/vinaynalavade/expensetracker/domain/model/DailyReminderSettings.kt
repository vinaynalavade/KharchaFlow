package com.vinaynalavade.expensetracker.domain.model

/**
 * Strongly typed settings for the daily smart reminder.
 */
data class DailyReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = DEFAULT_MINUTE
) {
    companion object {
        const val DEFAULT_HOUR = 21
        const val DEFAULT_MINUTE = 0
    }
}
