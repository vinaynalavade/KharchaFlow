package com.vinaynalavade.expensetracker.domain.repository

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for managing user preferences.
 */
interface UserPreferencesRepository {

    fun getUserPreferences(): Flow<UserPreferences>

    suspend fun setThemeMode(themeMode: ThemeMode): AppResult<Unit>

    suspend fun setCurrencyCode(currencyCode: String): AppResult<Unit>

    suspend fun setDynamicColors(useDynamicColors: Boolean): AppResult<Unit>

    suspend fun setFirstLaunchCompleted(): AppResult<Unit>

    suspend fun setOpeningBalance(subunits: Long): AppResult<Unit>

    suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int): AppResult<Unit>

    suspend fun setEmiReminders(enabled: Boolean): AppResult<Unit>

    fun getLastBackupTimestamp(): Flow<Long?>

    suspend fun setLastBackupTimestamp(timestamp: Long): AppResult<Unit>

    suspend fun setAppLockEnabled(enabled: Boolean): AppResult<Unit>

    suspend fun setBiometricEnabled(enabled: Boolean): AppResult<Unit>

    suspend fun setAutoLockDurationSeconds(seconds: Long): AppResult<Unit>

    suspend fun setHideContentInRecents(hide: Boolean): AppResult<Unit>

    suspend fun setNotificationsMasterEnabled(enabled: Boolean): AppResult<Unit>

    suspend fun setBudgetAlertsEnabled(enabled: Boolean): AppResult<Unit>

    suspend fun setMonthlyBudgetLimit(subunits: Long): AppResult<Unit>

    suspend fun setRecurringRemindersEnabled(enabled: Boolean): AppResult<Unit>

    suspend fun setRecurringReminderAdvanceDays(days: Int): AppResult<Unit>

    suspend fun setSavingsGoalNotificationsEnabled(enabled: Boolean): AppResult<Unit>
 
    suspend fun setAppLanguage(languageCode: String): AppResult<Unit>
 
    suspend fun setProfileName(name: String?): AppResult<Unit>

    suspend fun setProfileImageUri(uri: String?): AppResult<Unit>

    suspend fun setAutomaticBackupEnabled(enabled: Boolean): AppResult<Unit>

    suspend fun setLastBackupStatus(status: String?): AppResult<Unit>

    suspend fun setLastBackupError(error: String?): AppResult<Unit>
 
    suspend fun setLastDismissedRestoreBackupTimestamp(timestamp: Long?): AppResult<Unit>

    suspend fun setAppTourCompleted(completed: Boolean = true): AppResult<Unit>
 
    suspend fun setDefaultIncomeSource(source: com.vinaynalavade.expensetracker.domain.model.PaymentMethod): AppResult<Unit>
 
    suspend fun setDefaultExpenseSource(source: com.vinaynalavade.expensetracker.domain.model.PaymentMethod): AppResult<Unit>
}
