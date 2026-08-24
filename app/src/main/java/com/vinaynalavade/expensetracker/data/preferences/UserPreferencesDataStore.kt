package com.vinaynalavade.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vinaynalavade.expensetracker.core.constants.AppConstants
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = AppConstants.PREFERENCES_DATASTORE_NAME
)

class UserPreferencesDataStore(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey(AppConstants.PREF_KEY_THEME_MODE)
        val CURRENCY_CODE = stringPreferencesKey(AppConstants.PREF_KEY_CURRENCY_CODE)
        val IS_FIRST_LAUNCH = booleanPreferencesKey(AppConstants.PREF_KEY_IS_FIRST_LAUNCH)
        val USE_DYNAMIC_COLORS = booleanPreferencesKey(AppConstants.PREF_KEY_DYNAMIC_COLORS)
        val OPENING_BALANCE = longPreferencesKey("pref_opening_balance_subunits")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("pref_daily_reminder_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("pref_daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("pref_daily_reminder_minute")
        val EMI_REMINDERS_ENABLED = booleanPreferencesKey("pref_emi_reminders_enabled")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("pref_last_backup_timestamp")
        val GOOGLE_CONNECTED_EMAIL = stringPreferencesKey("pref_google_connected_email")
        val GOOGLE_CONNECTED_NAME = stringPreferencesKey("pref_google_connected_name")
        val GOOGLE_CONNECTED_PHOTO_URL = stringPreferencesKey("pref_google_connected_photo_url")
        val GOOGLE_LAST_BACKUP_TIMESTAMP = longPreferencesKey("pref_google_last_backup_timestamp")
        val APP_LOCK_ENABLED = booleanPreferencesKey("pref_app_lock_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("pref_biometric_enabled")
        val AUTO_LOCK_DURATION_SECONDS = longPreferencesKey("pref_auto_lock_duration_seconds")
        val HIDE_CONTENT_IN_RECENTS = booleanPreferencesKey("pref_hide_content_in_recents")
        val NOTIFICATIONS_MASTER_ENABLED = booleanPreferencesKey("pref_notifications_master_enabled")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("pref_budget_alerts_enabled")
        val MONTHLY_BUDGET_LIMIT_SUBUNITS = longPreferencesKey("pref_monthly_budget_limit_subunits")
        val RECURRING_REMINDERS_ENABLED = booleanPreferencesKey("pref_recurring_reminders_enabled")
        val RECURRING_REMINDER_ADVANCE_DAYS = intPreferencesKey("pref_recurring_reminder_advance_days")
        val SAVINGS_GOAL_NOTIFICATIONS_ENABLED = booleanPreferencesKey("pref_savings_goal_notifications_enabled")
        val APP_LANGUAGE = stringPreferencesKey("pref_app_language")
        val PROFILE_NAME = stringPreferencesKey("pref_profile_name")
        val PROFILE_IMAGE_URI = stringPreferencesKey("pref_profile_image_uri")
        val AUTOMATIC_BACKUP_ENABLED = booleanPreferencesKey("pref_automatic_backup_enabled")
        val LAST_BACKUP_STATUS = stringPreferencesKey("pref_last_backup_status")
        val LAST_BACKUP_ERROR = stringPreferencesKey("pref_last_backup_error")
        val LAST_DISMISSED_RESTORE_TIMESTAMP = longPreferencesKey("pref_last_dismissed_restore_timestamp")
        val IS_APP_TOUR_COMPLETED = booleanPreferencesKey("pref_is_app_tour_completed")
    }

    val googleConnectedEmailFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.GOOGLE_CONNECTED_EMAIL] }

    val googleConnectedNameFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.GOOGLE_CONNECTED_NAME] }

    val googleConnectedPhotoUrlFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.GOOGLE_CONNECTED_PHOTO_URL] }

    val googleLastBackupTimestampFlow: Flow<Long?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.GOOGLE_LAST_BACKUP_TIMESTAMP] }

    val lastBackupTimestampFlow: Flow<Long?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeModeString = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.LIGHT.name
            val themeMode = ThemeMode.fromString(themeModeString)

            val currencyCode = preferences[PreferencesKeys.CURRENCY_CODE] ?: Currency.DEFAULT.code
            val currency = Currency.fromCode(currencyCode)

            val isFirstLaunch = preferences[PreferencesKeys.IS_FIRST_LAUNCH] ?: true
            val useDynamicColors = preferences[PreferencesKeys.USE_DYNAMIC_COLORS] ?: false
            val openingBalanceSubunits = preferences[PreferencesKeys.OPENING_BALANCE] ?: 0L
            val dailyReminderEnabled = preferences[PreferencesKeys.DAILY_REMINDER_ENABLED] ?: false
            val dailyReminderHour = preferences[PreferencesKeys.DAILY_REMINDER_HOUR] ?: 21
            val dailyReminderMinute = preferences[PreferencesKeys.DAILY_REMINDER_MINUTE] ?: 0
            val emiRemindersEnabled = preferences[PreferencesKeys.EMI_REMINDERS_ENABLED] ?: true
            val appLockEnabled = preferences[PreferencesKeys.APP_LOCK_ENABLED] ?: false
            val biometricEnabled = preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
            val autoLockDurationSeconds = preferences[PreferencesKeys.AUTO_LOCK_DURATION_SECONDS] ?: 0L
            val hideContentInRecents = preferences[PreferencesKeys.HIDE_CONTENT_IN_RECENTS] ?: true
            val notificationsMasterEnabled = preferences[PreferencesKeys.NOTIFICATIONS_MASTER_ENABLED] ?: false
            val budgetAlertsEnabled = preferences[PreferencesKeys.BUDGET_ALERTS_ENABLED] ?: false
            val monthlyBudgetLimitSubunits = preferences[PreferencesKeys.MONTHLY_BUDGET_LIMIT_SUBUNITS] ?: 0L
            val recurringRemindersEnabled = preferences[PreferencesKeys.RECURRING_REMINDERS_ENABLED] ?: false
            val recurringReminderAdvanceDays = preferences[PreferencesKeys.RECURRING_REMINDER_ADVANCE_DAYS] ?: 1
            val savingsGoalNotificationsEnabled = preferences[PreferencesKeys.SAVINGS_GOAL_NOTIFICATIONS_ENABLED] ?: false
            val appLanguage = preferences[PreferencesKeys.APP_LANGUAGE] ?: "SYSTEM"
            val profileName = preferences[PreferencesKeys.PROFILE_NAME]
            val profileImageUri = preferences[PreferencesKeys.PROFILE_IMAGE_URI]
            val automaticBackupEnabled = preferences[PreferencesKeys.AUTOMATIC_BACKUP_ENABLED] ?: false
            val lastBackupStatus = preferences[PreferencesKeys.LAST_BACKUP_STATUS]
            val lastBackupError = preferences[PreferencesKeys.LAST_BACKUP_ERROR]
            val lastDismissedRestoreTimestamp = preferences[PreferencesKeys.LAST_DISMISSED_RESTORE_TIMESTAMP]
            val isAppTourCompleted = preferences[PreferencesKeys.IS_APP_TOUR_COMPLETED] ?: false

            UserPreferences(
                themeMode = themeMode,
                currency = currency,
                isFirstLaunch = isFirstLaunch,
                useDynamicColors = useDynamicColors,
                openingBalanceSubunits = openingBalanceSubunits,
                dailyReminderEnabled = dailyReminderEnabled,
                dailyReminderHour = dailyReminderHour,
                dailyReminderMinute = dailyReminderMinute,
                emiRemindersEnabled = emiRemindersEnabled,
                appLockEnabled = appLockEnabled,
                biometricEnabled = biometricEnabled,
                autoLockDurationSeconds = autoLockDurationSeconds,
                hideContentInRecents = hideContentInRecents,
                notificationsMasterEnabled = notificationsMasterEnabled,
                budgetAlertsEnabled = budgetAlertsEnabled,
                monthlyBudgetLimitSubunits = monthlyBudgetLimitSubunits,
                recurringRemindersEnabled = recurringRemindersEnabled,
                recurringReminderAdvanceDays = recurringReminderAdvanceDays,
                savingsGoalNotificationsEnabled = savingsGoalNotificationsEnabled,
                appLanguage = appLanguage,
                userName = profileName,
                profileImageUri = profileImageUri,
                automaticBackupEnabled = automaticBackupEnabled,
                lastBackupStatus = lastBackupStatus,
                lastBackupError = lastBackupError,
                lastDismissedRestoreBackupTimestamp = lastDismissedRestoreTimestamp,
                isAppTourCompleted = isAppTourCompleted
            )
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setCurrencyCode(currencyCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_CODE] = currencyCode
        }
    }

    suspend fun setDynamicColors(useDynamicColors: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLORS] = useDynamicColors
        }
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] = false
        }
    }

    suspend fun setOpeningBalance(subunits: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPENING_BALANCE] = subunits
        }
    }

    suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.DAILY_REMINDER_HOUR] = hour
            preferences[PreferencesKeys.DAILY_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setEmiReminders(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EMI_REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    suspend fun setGoogleAccount(email: String, name: String?, photoUrl: String?) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GOOGLE_CONNECTED_EMAIL] = email
            if (name != null) {
                preferences[PreferencesKeys.GOOGLE_CONNECTED_NAME] = name
            } else {
                preferences.remove(PreferencesKeys.GOOGLE_CONNECTED_NAME)
            }
            if (photoUrl != null) {
                preferences[PreferencesKeys.GOOGLE_CONNECTED_PHOTO_URL] = photoUrl
            } else {
                preferences.remove(PreferencesKeys.GOOGLE_CONNECTED_PHOTO_URL)
            }
        }
    }

    suspend fun clearGoogleAccount() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.GOOGLE_CONNECTED_EMAIL)
            preferences.remove(PreferencesKeys.GOOGLE_CONNECTED_NAME)
            preferences.remove(PreferencesKeys.GOOGLE_CONNECTED_PHOTO_URL)
            preferences.remove(PreferencesKeys.GOOGLE_LAST_BACKUP_TIMESTAMP)
        }
    }

    suspend fun setGoogleLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GOOGLE_LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setAutoLockDurationSeconds(seconds: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_LOCK_DURATION_SECONDS] = seconds
        }
    }

    suspend fun setHideContentInRecents(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_CONTENT_IN_RECENTS] = hide
        }
    }

    suspend fun setNotificationsMasterEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_MASTER_ENABLED] = enabled
        }
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BUDGET_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setMonthlyBudgetLimit(subunits: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONTHLY_BUDGET_LIMIT_SUBUNITS] = subunits
        }
    }

    suspend fun setRecurringRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECURRING_REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun setRecurringReminderAdvanceDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECURRING_REMINDER_ADVANCE_DAYS] = days
        }
    }

    suspend fun setSavingsGoalNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAVINGS_GOAL_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = languageCode
        }
    }

    suspend fun setProfileName(name: String?) {
        context.dataStore.edit { preferences ->
            if (name != null && name.isNotBlank()) {
                preferences[PreferencesKeys.PROFILE_NAME] = name.trim()
            } else {
                preferences.remove(PreferencesKeys.PROFILE_NAME)
            }
        }
    }

    suspend fun setProfileImageUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri != null && uri.isNotBlank()) {
                preferences[PreferencesKeys.PROFILE_IMAGE_URI] = uri
            } else {
                preferences.remove(PreferencesKeys.PROFILE_IMAGE_URI)
            }
        }
    }

    suspend fun setAutomaticBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTOMATIC_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun setLastBackupStatus(status: String?) {
        context.dataStore.edit { preferences ->
            if (status != null) {
                preferences[PreferencesKeys.LAST_BACKUP_STATUS] = status
            } else {
                preferences.remove(PreferencesKeys.LAST_BACKUP_STATUS)
            }
        }
    }

    suspend fun setLastBackupError(error: String?) {
        context.dataStore.edit { preferences ->
            if (error != null) {
                preferences[PreferencesKeys.LAST_BACKUP_ERROR] = error
            } else {
                preferences.remove(PreferencesKeys.LAST_BACKUP_ERROR)
            }
        }
    }

    suspend fun setLastDismissedRestoreBackupTimestamp(timestamp: Long?) {
        context.dataStore.edit { preferences ->
            if (timestamp != null) {
                preferences[PreferencesKeys.LAST_DISMISSED_RESTORE_TIMESTAMP] = timestamp
            } else {
                preferences.remove(PreferencesKeys.LAST_DISMISSED_RESTORE_TIMESTAMP)
            }
        }
    }

    suspend fun setAppTourCompleted(completed: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_APP_TOUR_COMPLETED] = completed
        }
    }
}
