package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.backup.BackupData
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.GoogleAccountInfo
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupMetadata
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupState
import com.vinaynalavade.expensetracker.domain.repository.BackupRepository
import com.vinaynalavade.expensetracker.domain.repository.GoogleDriveBackupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetGoogleBackupStateUseCase(
    private val googleDriveBackupRepository: GoogleDriveBackupRepository
) {
    operator fun invoke(): Flow<GoogleBackupState> {
        return combine(
            googleDriveBackupRepository.getConnectedAccount(),
            googleDriveBackupRepository.getLastCloudBackupTimestamp()
        ) { account, lastBackupTimestamp ->
            if (account == null) {
                GoogleBackupState.Disconnected
            } else {
                GoogleBackupState.Connected(
                    account = account,
                    lastBackupTimestamp = lastBackupTimestamp,
                    cloudBackupExists = lastBackupTimestamp != null && lastBackupTimestamp > 0L
                )
            }
        }
    }
}

class PerformGoogleDriveBackupUseCase(
    private val backupRepository: BackupRepository,
    private val googleDriveBackupRepository: GoogleDriveBackupRepository
) {
    suspend operator fun invoke(): AppResult<GoogleBackupMetadata> {
        return when (val backupDataResult = backupRepository.createFullBackup()) {
            is AppResult.Success -> {
                googleDriveBackupRepository.uploadBackup(backupDataResult.data)
            }
            is AppResult.Error -> AppResult.Error(backupDataResult.error)
        }
    }
}

class PrepareGoogleDriveRestoreUseCase(
    private val googleDriveBackupRepository: GoogleDriveBackupRepository
) {
    suspend operator fun invoke(): AppResult<BackupData> {
        return googleDriveBackupRepository.downloadBackup()
    }
}

class DisconnectGoogleAccountUseCase(
    private val googleDriveBackupRepository: GoogleDriveBackupRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return googleDriveBackupRepository.disconnect()
    }
}

class SaveConnectedGoogleAccountUseCase(
    private val googleDriveBackupRepository: GoogleDriveBackupRepository
) {
    suspend operator fun invoke(account: GoogleAccountInfo): AppResult<Unit> {
        return googleDriveBackupRepository.saveConnectedAccount(account)
    }
}
