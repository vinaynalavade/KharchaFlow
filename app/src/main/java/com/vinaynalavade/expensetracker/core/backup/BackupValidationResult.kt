package com.vinaynalavade.expensetracker.core.backup

/**
 * Result of validating a full backup file before restoring.
 */
sealed class BackupValidationResult {
    data class Valid(
        val backupData: BackupData,
        val transactionCount: Int,
        val categoryCount: Int,
        val recurringCount: Int,
        val createdAt: Long,
        val appVersion: String
    ) : BackupValidationResult()

    data class Invalid(
        val errorMessage: String
    ) : BackupValidationResult()
}

/**
 * Result of validating an imported CSV or JSON transaction file before importing.
 */
data class ImportValidationResult(
    val totalRows: Int,
    val validTransactions: List<com.vinaynalavade.expensetracker.domain.model.Transaction>,
    val issues: List<String>
) {
    val isValid: Boolean get() = validTransactions.isNotEmpty()
}
