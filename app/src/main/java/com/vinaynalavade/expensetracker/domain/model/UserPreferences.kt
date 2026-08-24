package com.vinaynalavade.expensetracker.domain.model

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency

/**
 * Domain model representing user-configured preferences.
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val currency: Currency = Currency.DEFAULT,
    val isFirstLaunch: Boolean = true,
    val useDynamicColors: Boolean = false,
    val openingBalanceSubunits: Long = 0L,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 21,
    val dailyReminderMinute: Int = 0,
    val emiRemindersEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val autoLockDurationSeconds: Long = 0L,
    val hideContentInRecents: Boolean = true,
    val notificationsMasterEnabled: Boolean = false,
    val budgetAlertsEnabled: Boolean = false,
    val monthlyBudgetLimitSubunits: Long = 0L,
    val recurringRemindersEnabled: Boolean = false,
    val recurringReminderAdvanceDays: Int = 1,
    val savingsGoalNotificationsEnabled: Boolean = false,
    val appLanguage: String = "SYSTEM",
    val userName: String? = null,
    val profileImageUri: String? = null,
    val automaticBackupEnabled: Boolean = false,
    val lastBackupStatus: String? = null,
    val lastBackupError: String? = null,
    val lastDismissedRestoreBackupTimestamp: Long? = null,
    val isAppTourCompleted: Boolean = false
) {
    val openingBalance: Amount
        get() = Amount(openingBalanceSubunits)

    val monthlyBudgetLimit: Amount
        get() = Amount(monthlyBudgetLimitSubunits)
}
