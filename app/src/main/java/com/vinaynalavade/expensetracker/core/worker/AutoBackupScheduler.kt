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
        const val UNIQUE_AUTO_BACKUP_WORK = "KharchaFlow_AutoBackup"
    }

    override fun schedule() {
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

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_AUTO_BACKUP_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            autoBackupWorkRequest
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_AUTO_BACKUP_WORK)
    }
}
