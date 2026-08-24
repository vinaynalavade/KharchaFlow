package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.RestoreEligibility
import com.vinaynalavade.expensetracker.domain.repository.GoogleDriveBackupRepository
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CheckRestoreEligibilityUseCase(
    private val googleDriveBackupRepository: GoogleDriveBackupRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(): RestoreEligibility {
        val account = googleDriveBackupRepository.getConnectedAccount().firstOrNull()
            ?: return RestoreEligibility.NotEligible

        val metadataResult = googleDriveBackupRepository.getCloudBackupMetadata()
        if (metadataResult !is AppResult.Success || metadataResult.data == null) {
            return RestoreEligibility.NotEligible
        }

        val cloudMetadata = metadataResult.data
        val userPrefs = userPreferencesRepository.getUserPreferences().firstOrNull()
        val lastDismissedTimestamp = userPrefs?.lastDismissedRestoreBackupTimestamp ?: 0L

        // If the cloud backup's modifiedTime is not newer than our dismissed timestamp, do not prompt
        if (cloudMetadata.modifiedTime <= lastDismissedTimestamp) {
            return RestoreEligibility.NotEligible
        }

        val transactions = transactionRepository.getTransactions().firstOrNull() ?: emptyList()
        val hasExistingLocalData = transactions.isNotEmpty() || (userPrefs?.openingBalanceSubunits ?: 0L) > 0L

        val formattedDate = try {
            val instant = Instant.ofEpochMilli(cloudMetadata.modifiedTime)
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).format(zonedDateTime)
        } catch (_: Exception) {
            ""
        }

        return RestoreEligibility.Eligible(
            metadata = cloudMetadata,
            formattedDate = formattedDate,
            hasExistingLocalData = hasExistingLocalData
        )
    }
}
