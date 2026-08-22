package com.vinaynalavade.expensetracker.domain.repository

import com.vinaynalavade.expensetracker.core.backup.BackupData
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.GoogleAccountInfo
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing Google Account connectivity and Google Drive appDataFolder backups.
 */
interface GoogleDriveBackupRepository {

    fun getConnectedAccount(): Flow<GoogleAccountInfo?>

    fun getLastCloudBackupTimestamp(): Flow<Long?>

    suspend fun saveConnectedAccount(account: GoogleAccountInfo): AppResult<Unit>

    suspend fun disconnect(): AppResult<Unit>

    suspend fun getCloudBackupMetadata(): AppResult<GoogleBackupMetadata?>

    suspend fun uploadBackup(backupData: BackupData): AppResult<GoogleBackupMetadata>

    suspend fun downloadBackup(): AppResult<BackupData>
}
