package com.vinaynalavade.expensetracker.domain.repository

import com.vinaynalavade.expensetracker.core.backup.BackupData
import com.vinaynalavade.expensetracker.core.backup.BackupValidationResult
import com.vinaynalavade.expensetracker.core.backup.ImportValidationResult
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for full application backups, restorations, and transaction exports/imports.
 */
interface BackupRepository {

    fun getLastBackupTimestamp(): Flow<Long?>

    suspend fun createFullBackup(): AppResult<BackupData>

    suspend fun validateBackupJson(jsonString: String): BackupValidationResult

    suspend fun restoreFullBackup(backupData: BackupData): AppResult<Unit>

    suspend fun getFilteredTransactions(
        startDate: Long? = null,
        endDate: Long? = null,
        type: TransactionType? = null,
        categoryId: Long? = null
    ): AppResult<List<Transaction>>

    suspend fun validateAndPrepareImportCsv(
        csvContent: String,
        defaultCurrency: Currency
    ): ImportValidationResult

    suspend fun validateAndPrepareImportJson(
        jsonContent: String
    ): ImportValidationResult

    suspend fun importTransactions(
        transactions: List<Transaction>
    ): AppResult<Int>
}
