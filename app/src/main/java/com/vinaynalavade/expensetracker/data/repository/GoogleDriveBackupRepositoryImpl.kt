package com.vinaynalavade.expensetracker.data.repository

import com.vinaynalavade.expensetracker.core.backup.BackupData
import com.vinaynalavade.expensetracker.core.backup.BackupValidationResult
import com.vinaynalavade.expensetracker.core.backup.JsonBackupParser
import com.vinaynalavade.expensetracker.core.google.GoogleAccountManager
import com.vinaynalavade.expensetracker.core.google.GoogleDriveRestService
import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.data.preferences.UserPreferencesDataStore
import com.vinaynalavade.expensetracker.domain.model.GoogleAccountInfo
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupMetadata
import com.vinaynalavade.expensetracker.domain.repository.BackupRepository
import com.vinaynalavade.expensetracker.domain.repository.GoogleDriveBackupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull

class GoogleDriveBackupRepositoryImpl(
    private val googleAccountManager: GoogleAccountManager,
    private val googleDriveRestService: GoogleDriveRestService,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val backupRepository: BackupRepository
) : GoogleDriveBackupRepository {

    override fun getConnectedAccount(): Flow<GoogleAccountInfo?> {
        return combine(
            userPreferencesDataStore.googleConnectedEmailFlow,
            userPreferencesDataStore.googleConnectedNameFlow,
            userPreferencesDataStore.googleConnectedPhotoUrlFlow
        ) { email, name, photoUrl ->
            if (email.isNullOrBlank()) {
                null
            } else {
                GoogleAccountInfo(
                    email = email,
                    displayName = name,
                    photoUrl = photoUrl
                )
            }
        }
    }

    override fun getLastCloudBackupTimestamp(): Flow<Long?> {
        return userPreferencesDataStore.googleLastBackupTimestampFlow
    }

    override suspend fun saveConnectedAccount(account: GoogleAccountInfo): AppResult<Unit> {
        return try {
            userPreferencesDataStore.setGoogleAccount(
                email = account.email,
                name = account.displayName,
                photoUrl = account.photoUrl
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError("Failed to persist connected account: ${e.message}", e))
        }
    }

    override suspend fun disconnect(): AppResult<Unit> {
        return try {
            googleAccountManager.signOut()
            userPreferencesDataStore.clearGoogleAccount()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError("Failed to disconnect Google account: ${e.message}", e))
        }
    }

    override suspend fun getCloudBackupMetadata(): AppResult<GoogleBackupMetadata?> {
        val account = getConnectedAccount().firstOrNull()
            ?: return AppResult.Error(AppError.UnknownError("No Google account is currently connected."))

        val tokenResult = googleAccountManager.getOAuthAccessToken(account.email)
        if (tokenResult is AppResult.Error) {
            return AppResult.Error(tokenResult.error)
        }
        val token = (tokenResult as AppResult.Success).data

        return googleDriveRestService.findBackupFile(token)
    }

    override suspend fun uploadBackup(backupData: BackupData): AppResult<GoogleBackupMetadata> {
        val account = getConnectedAccount().firstOrNull()
            ?: return AppResult.Error(AppError.UnknownError("Please connect a Google account before backing up."))

        val jsonString = JsonBackupParser.toJson(backupData)
        val validation = backupRepository.validateBackupJson(jsonString)
        if (validation is BackupValidationResult.Invalid) {
            return AppResult.Error(AppError.ValidationError("Generated backup data is invalid: ${validation.errorMessage}"))
        }

        val tokenResult = googleAccountManager.getOAuthAccessToken(account.email)
        if (tokenResult is AppResult.Error) {
            return AppResult.Error(tokenResult.error)
        }
        val token = (tokenResult as AppResult.Success).data

        val existingFileResult = googleDriveRestService.findBackupFile(token)
        val uploadResult = if (existingFileResult is AppResult.Success && existingFileResult.data != null) {
            googleDriveRestService.updateBackupFile(token, existingFileResult.data.fileId, jsonString)
        } else {
            googleDriveRestService.createBackupFile(token, jsonString)
        }

        return when (uploadResult) {
            is AppResult.Success -> {
                userPreferencesDataStore.setGoogleLastBackupTimestamp(uploadResult.data.modifiedTime)
                AppResult.Success(uploadResult.data)
            }
            is AppResult.Error -> AppResult.Error(uploadResult.error)
        }
    }

    override suspend fun downloadBackup(): AppResult<BackupData> {
        val account = getConnectedAccount().firstOrNull()
            ?: return AppResult.Error(AppError.UnknownError("Please connect a Google account before restoring."))

        val tokenResult = googleAccountManager.getOAuthAccessToken(account.email)
        if (tokenResult is AppResult.Error) {
            return AppResult.Error(tokenResult.error)
        }
        val token = (tokenResult as AppResult.Success).data

        val findResult = googleDriveRestService.findBackupFile(token)
        if (findResult is AppResult.Error) {
            return AppResult.Error(findResult.error)
        }
        val fileMetadata = (findResult as AppResult.Success).data
            ?: return AppResult.Error(AppError.NotFound("No KharchaFlow backup was found in this Google account."))

        val downloadResult = googleDriveRestService.downloadBackupFile(token, fileMetadata.fileId)
        if (downloadResult is AppResult.Error) {
            return AppResult.Error(downloadResult.error)
        }
        val jsonContent = (downloadResult as AppResult.Success).data

        val validation = backupRepository.validateBackupJson(jsonContent)
        return when (validation) {
            is BackupValidationResult.Valid -> AppResult.Success(validation.backupData)
            is BackupValidationResult.Invalid -> {
                AppResult.Error(AppError.ValidationError("Your cloud backup could not be restored because it appears to be invalid: ${validation.errorMessage}"))
            }
        }
    }
}
