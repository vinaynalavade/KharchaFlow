package com.vinaynalavade.expensetracker.domain.model

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency

/**
 * Domain model representing user-configured preferences.
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currency: Currency = Currency.DEFAULT,
    val isFirstLaunch: Boolean = true,
    val useDynamicColors: Boolean = false,
    val openingBalanceSubunits: Long = 0L,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 21,
    val dailyReminderMinute: Int = 0,
    val emiRemindersEnabled: Boolean = true
) {
    val openingBalance: Amount
        get() = Amount(openingBalanceSubunits)
}
