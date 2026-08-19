package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository

class SetOpeningBalanceUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(subunits: Long): AppResult<Unit> {
        return userPreferencesRepository.setOpeningBalance(subunits)
    }
}

class SetDailyReminderUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean, hour: Int, minute: Int): AppResult<Unit> {
        return userPreferencesRepository.setDailyReminder(enabled, hour, minute)
    }
}

class SetCurrencyUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(currencyCode: String): AppResult<Unit> {
        return userPreferencesRepository.setCurrencyCode(currencyCode)
    }
}
