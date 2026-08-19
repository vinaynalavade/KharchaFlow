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
}
