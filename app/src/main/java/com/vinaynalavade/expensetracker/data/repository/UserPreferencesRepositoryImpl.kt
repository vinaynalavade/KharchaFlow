package com.vinaynalavade.expensetracker.data.repository

import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.data.preferences.UserPreferencesDataStore
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class UserPreferencesRepositoryImpl(
    private val dataStore: UserPreferencesDataStore
) : UserPreferencesRepository {

    override fun getUserPreferences(): Flow<UserPreferences> {
        return dataStore.userPreferencesFlow
    }

    override suspend fun setThemeMode(themeMode: ThemeMode): AppResult<Unit> {
        return try {
            dataStore.setThemeMode(themeMode)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.PreferencesError(e.message ?: "Failed to set theme mode.", e))
        }
    }

    override suspend fun setCurrencyCode(currencyCode: String): AppResult<Unit> {
        return try {
            dataStore.setCurrencyCode(currencyCode)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.PreferencesError(e.message ?: "Failed to set currency code.", e))
        }
    }

    override suspend fun setDynamicColors(useDynamicColors: Boolean): AppResult<Unit> {
        return try {
            dataStore.setDynamicColors(useDynamicColors)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.PreferencesError(e.message ?: "Failed to set dynamic colors.", e))
        }
    }

    override suspend fun setFirstLaunchCompleted(): AppResult<Unit> {
        return try {
            dataStore.setFirstLaunchCompleted()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.PreferencesError(e.message ?: "Failed to set first launch.", e))
        }
    }

    override suspend fun setOpeningBalance(subunits: Long): AppResult<Unit> {
        return try {
            dataStore.setOpeningBalance(subunits)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.PreferencesError(e.message ?: "Failed to set opening balance.", e))
        }
    }

    override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int): AppResult<Unit> {
        return try {
            dataStore.setDailyReminder(enabled, hour, minute)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.PreferencesError(e.message ?: "Failed to set daily reminder.", e))
        }
    }

    override suspend fun setEmiReminders(enabled: Boolean): AppResult<Unit> {
        return try {
            dataStore.setEmiReminders(enabled)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.PreferencesError(e.message ?: "Failed to set EMI reminders.", e))
        }
    }
}
