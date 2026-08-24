package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.worker.AutoBackupScheduler
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository

class SetAutomaticBackupUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val autoBackupScheduler: AutoBackupScheduler
) {
    suspend operator fun invoke(enabled: Boolean): AppResult<Unit> {
        val result = userPreferencesRepository.setAutomaticBackupEnabled(enabled)
        if (result is AppResult.Success) {
            if (enabled) {
                autoBackupScheduler.schedule()
            } else {
                autoBackupScheduler.cancel()
            }
        }
        return result
    }
}
