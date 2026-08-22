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
            val themeModeString = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
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

            UserPreferences(
                themeMode = themeMode,
                currency = currency,
                isFirstLaunch = isFirstLaunch,
                useDynamicColors = useDynamicColors,
                openingBalanceSubunits = openingBalanceSubunits,
                dailyReminderEnabled = dailyReminderEnabled,
                dailyReminderHour = dailyReminderHour,
                dailyReminderMinute = dailyReminderMinute,
                emiRemindersEnabled = emiRemindersEnabled
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
}
