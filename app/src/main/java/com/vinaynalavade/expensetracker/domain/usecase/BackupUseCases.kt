package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.backup.BackupCategory
import com.vinaynalavade.expensetracker.core.backup.BackupData
import com.vinaynalavade.expensetracker.core.backup.BackupTransaction
import com.vinaynalavade.expensetracker.core.backup.BackupValidationResult
import com.vinaynalavade.expensetracker.core.backup.CsvTransactionHelper
import com.vinaynalavade.expensetracker.core.backup.ImportValidationResult
import com.vinaynalavade.expensetracker.core.backup.JsonBackupParser
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.BackupRepository
import com.vinaynalavade.expensetracker.domain.repository.CategoryRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.firstOrNull

class CreateBackupUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): AppResult<BackupData> {
        return backupRepository.createFullBackup()
    }
}

class ValidateBackupUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(jsonString: String): BackupValidationResult {
        return backupRepository.validateBackupJson(jsonString)
    }
}

class RestoreBackupUseCase(
    private val backupRepository: BackupRepository,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val rescheduleAllRemindersUseCase: RescheduleAllRemindersUseCase? = null
) {
    suspend operator fun invoke(backupData: BackupData): AppResult<Unit> {
        val result = backupRepository.restoreFullBackup(backupData)
        if (result is AppResult.Success) {
            userPreferencesRepository?.setLastDismissedRestoreBackupTimestamp(backupData.createdAt)
            rescheduleAllRemindersUseCase?.invoke()
        }
        return result
    }
}

enum class ExportFormat {
    CSV,
    JSON
}

data class ExportFilterOptions(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val format: ExportFormat = ExportFormat.CSV
)

data class ExportedFileResult(
    val content: String,
    val fileName: String,
    val mimeType: String,
    val transactionCount: Int
)

class ExportTransactionsUseCase(
    private val backupRepository: BackupRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(options: ExportFilterOptions): AppResult<ExportedFileResult> {
        return when (val txResult = backupRepository.getFilteredTransactions(
            options.startDate, options.endDate, options.type, options.categoryId
        )) {
            is AppResult.Success -> {
                val transactions = txResult.data
                val prefs = userPreferencesRepository.getUserPreferences().firstOrNull()
                val currency = prefs?.currency ?: Currency.DEFAULT
                val now = System.currentTimeMillis()
                val timestampStr = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
                )

                if (options.format == ExportFormat.CSV) {
                    val csvText = CsvTransactionHelper.exportToCsv(transactions, currency)
                    val fileName = "KharchaFlow_Transactions_$timestampStr.csv"
                    AppResult.Success(
                        ExportedFileResult(
                            content = csvText,
                            fileName = fileName,
                            mimeType = "text/csv",
                            transactionCount = transactions.size
                        )
                    )
                } else {
                    val categories = categoryRepository.getCategories().firstOrNull() ?: emptyList()
                    val backupCategories = categories.map {
                        BackupCategory(it.id, it.name, it.iconName, it.colorHex, it.type.name, it.isDefault)
                    }
                    val backupTransactions = transactions.map {
                        BackupTransaction(it.id, it.amount.subunits, it.type.name, it.category.id, it.paymentMethod.name, it.note, it.timestamp)
                    }
                    val jsonText = JsonBackupParser.exportTransactionsToJson(backupTransactions, backupCategories)
                    val fileName = "KharchaFlow_Transactions_$timestampStr.json"
                    AppResult.Success(
                        ExportedFileResult(
                            content = jsonText,
                            fileName = fileName,
                            mimeType = "application/json",
                            transactionCount = transactions.size
                        )
                    )
                }
            }
            is AppResult.Error -> AppResult.Error(txResult.error)
        }
    }
}

class ValidateImportUseCase(
    private val backupRepository: BackupRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(fileContent: String, isCsv: Boolean): ImportValidationResult {
        return if (isCsv) {
            val prefs = userPreferencesRepository.getUserPreferences().firstOrNull()
            val currency = prefs?.currency ?: Currency.DEFAULT
            backupRepository.validateAndPrepareImportCsv(fileContent, currency)
        } else {
            backupRepository.validateAndPrepareImportJson(fileContent)
        }
    }
}

class ImportTransactionsUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(transactions: List<Transaction>): AppResult<Int> {
        return backupRepository.importTransactions(transactions)
    }
}
