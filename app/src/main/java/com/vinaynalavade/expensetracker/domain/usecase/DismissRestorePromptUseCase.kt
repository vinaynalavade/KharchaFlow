package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository

class DismissRestorePromptUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(backupModifiedTime: Long): AppResult<Unit> {
        return userPreferencesRepository.setLastDismissedRestoreBackupTimestamp(backupModifiedTime)
    }
}
