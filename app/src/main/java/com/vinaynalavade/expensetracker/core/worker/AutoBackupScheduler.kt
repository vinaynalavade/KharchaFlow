package com.vinaynalavade.expensetracker.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

interface AutoBackupScheduler {
    fun schedule()
    fun cancel()
}

class WorkManagerAutoBackupScheduler(
    private val context: Context
) : AutoBackupScheduler {

    companion object {
        const val UNIQUE_AUTO_BACKUP_WORK = "Leaf_AutoBackup"
        private const val LEGACY_AUTO_BACKUP_WORK = "KharchaFlow_AutoBackup"
    }

    override fun schedule() {
        val workManager = WorkManager.getInstance(context)
        // Ensure any legacy scheduled periodic work from previous installations is cancelled
        workManager.cancelUniqueWork(LEGACY_AUTO_BACKUP_WORK)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val autoBackupWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_AUTO_BACKUP_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            autoBackupWorkRequest
        )
    }

    override fun cancel() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(UNIQUE_AUTO_BACKUP_WORK)
        workManager.cancelUniqueWork(LEGACY_AUTO_BACKUP_WORK)
    }
}
