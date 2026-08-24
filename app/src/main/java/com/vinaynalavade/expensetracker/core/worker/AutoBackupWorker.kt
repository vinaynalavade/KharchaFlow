package com.vinaynalavade.expensetracker.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vinaynalavade.expensetracker.ExpenseTrackerApp
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupState
import kotlinx.coroutines.flow.firstOrNull

/**
 * WorkManager CoroutineWorker for executing periodic background backups to Google Drive.
 * Runs only when network connectivity is available.
 */
class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ExpenseTrackerApp ?: return Result.failure()
        val container = app.container

        try {
            val userPrefs = container.getUserPreferencesUseCase().firstOrNull() ?: return Result.success()
            if (!userPrefs.automaticBackupEnabled) {
                return Result.success()
            }

            val googleState = container.getGoogleBackupStateUseCase().firstOrNull()
            if (googleState !is GoogleBackupState.Connected) {
                return Result.success()
            }

            val result = container.performGoogleDriveBackupUseCase()
            return when (result) {
                is AppResult.Success -> {
                    Result.success()
                }
                is AppResult.Error -> {
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            }
        } catch (_: Exception) {
            return Result.failure()
        }
    }
}
