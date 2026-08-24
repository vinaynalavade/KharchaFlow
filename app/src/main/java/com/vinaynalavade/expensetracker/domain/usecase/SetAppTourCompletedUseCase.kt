package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository

class SetAppTourCompletedUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(completed: Boolean = true): AppResult<Unit> {
        return userPreferencesRepository.setAppTourCompleted(completed)
    }
}
