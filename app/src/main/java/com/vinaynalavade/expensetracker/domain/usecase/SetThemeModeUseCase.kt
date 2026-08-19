package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository

/**
 * UseCase to update the application theme mode.
 */
class SetThemeModeUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(themeMode: ThemeMode): AppResult<Unit> =
        userPreferencesRepository.setThemeMode(themeMode)
}
