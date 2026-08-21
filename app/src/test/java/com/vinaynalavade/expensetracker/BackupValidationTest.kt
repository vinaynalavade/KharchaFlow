package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.backup.BackupCategory
import com.vinaynalavade.expensetracker.core.backup.BackupData
import com.vinaynalavade.expensetracker.core.backup.BackupPreferences
import com.vinaynalavade.expensetracker.core.backup.BackupTransaction
import com.vinaynalavade.expensetracker.core.backup.BackupValidationResult
import com.vinaynalavade.expensetracker.core.backup.JsonBackupParser
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.BackupRepository
import com.vinaynalavade.expensetracker.domain.usecase.ValidateBackupUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidationTest {

    private val validCategories = listOf(
        BackupCategory(1L, "Food", "restaurant", "#EF4444", "EXPENSE", true)
    )

    private val validTransactions = listOf(
        BackupTransaction(10L, 50000L, "EXPENSE", 1L, "UPI", "Lunch", 1787000000000L)
    )

    @Test
    fun testValidBackupValidationSucceeds() = runBlocking {
        val backup = BackupData(
            backupVersion = 1,
            appVersion = "1.0.1",
            createdAt = 1787000000000L,
            categories = validCategories,
            transactions = validTransactions,
            preferences = BackupPreferences()
        )
        val json = JsonBackupParser.toJson(backup)
        val repo = FakeBackupRepository()
        val validateUseCase = ValidateBackupUseCase(repo)

        val result = validateUseCase(json)
        assertTrue(result is BackupValidationResult.Valid)
    }

    @Test
    fun testFutureBackupVersionIsRejected() = runBlocking {
        val futureJson = """
            {
                "backupVersion": 99,
                "appVersion": "5.0.0",
                "createdAt": 1787000000000,
                "categories": [],
                "transactions": []
            }
        """.trimIndent()
        val repo = FakeBackupRepository()
        val validateUseCase = ValidateBackupUseCase(repo)

        val result = validateUseCase(futureJson)
        assertTrue(result is BackupValidationResult.Invalid)
        assertTrue((result as BackupValidationResult.Invalid).errorMessage.contains("newer than supported"))
    }

    @Test
    fun testMissingCategoriesIsRejected() = runBlocking {
        val noCategoriesJson = """
            {
                "backupVersion": 1,
                "appVersion": "1.0.1",
                "createdAt": 1787000000000,
                "categories": [],
                "transactions": [
                    { "id": 1, "amountSubunits": 500, "type": "EXPENSE", "categoryId": 1, "paymentMethod": "CASH", "timestamp": 123 }
                ]
            }
        """.trimIndent()
        val repo = FakeBackupRepository()
        val validateUseCase = ValidateBackupUseCase(repo)

        val result = validateUseCase(noCategoriesJson)
        assertTrue(result is BackupValidationResult.Invalid)
    }

    @Test
    fun testOrphanTransactionReferenceIsRejected() = runBlocking {
        val orphanTxBackup = BackupData(
            backupVersion = 1,
            appVersion = "1.0.1",
            createdAt = 1787000000000L,
            categories = listOf(BackupCategory(1L, "Food", "restaurant", "#EF4444", "EXPENSE", true)),
            transactions = listOf(BackupTransaction(10L, 50000L, "EXPENSE", 999L, "UPI", "Lunch", 1787000000000L)), // categoryId 999 does not exist!
            preferences = BackupPreferences()
        )
        val json = JsonBackupParser.toJson(orphanTxBackup)
        val repo = FakeBackupRepository()
        val validateUseCase = ValidateBackupUseCase(repo)

        val result = validateUseCase(json)
        assertTrue(result is BackupValidationResult.Invalid)
        assertTrue((result as BackupValidationResult.Invalid).errorMessage.contains("missing category"))
    }

    @Test
    fun testMalformedJsonIsRejectedSafely() = runBlocking {
        val malformedJson = "{ broken json: [1, 2"
        val repo = FakeBackupRepository()
        val validateUseCase = ValidateBackupUseCase(repo)

        val result = validateUseCase(malformedJson)
        assertTrue(result is BackupValidationResult.Invalid)
    }

    private class FakeBackupRepository : BackupRepository {
        override fun getLastBackupTimestamp(): Flow<Long?> = flowOf(null)
        override suspend fun createFullBackup(): AppResult<BackupData> = AppResult.Error(com.vinaynalavade.expensetracker.core.result.AppError.DatabaseError("Not implemented"))
        override suspend fun validateBackupJson(jsonString: String): BackupValidationResult {
            return try {
                val backup = JsonBackupParser.fromJson(jsonString)
                if (backup.backupVersion > BackupData.CURRENT_VERSION) {
                    return BackupValidationResult.Invalid("Backup version ${backup.backupVersion} is newer than supported version (${BackupData.CURRENT_VERSION}).")
                }
                if (backup.categories.isEmpty()) {
                    return BackupValidationResult.Invalid("Corrupted backup: No categories found in file.")
                }
                val categoryIds = backup.categories.map { it.id }.toSet()
                for (tx in backup.transactions) {
                    if (tx.categoryId !in categoryIds) {
                        return BackupValidationResult.Invalid("Relationship integrity error: Transaction ${tx.id} references missing category ${tx.categoryId}.")
                    }
                }
                BackupValidationResult.Valid(backup, backup.transactions.size, backup.categories.size, backup.recurringTransactions.size, backup.createdAt, backup.appVersion)
            } catch (e: Exception) {
                BackupValidationResult.Invalid(e.message ?: "Invalid JSON")
            }
        }
        override suspend fun restoreFullBackup(backupData: BackupData): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getFilteredTransactions(startDate: Long?, endDate: Long?, type: TransactionType?, categoryId: Long?): AppResult<List<Transaction>> = AppResult.Success(emptyList())
        override suspend fun validateAndPrepareImportCsv(csvContent: String, defaultCurrency: com.vinaynalavade.expensetracker.core.model.Currency): com.vinaynalavade.expensetracker.core.backup.ImportValidationResult = com.vinaynalavade.expensetracker.core.backup.ImportValidationResult(0, emptyList(), emptyList())
        override suspend fun validateAndPrepareImportJson(jsonContent: String): com.vinaynalavade.expensetracker.core.backup.ImportValidationResult = com.vinaynalavade.expensetracker.core.backup.ImportValidationResult(0, emptyList(), emptyList())
        override suspend fun importTransactions(transactions: List<Transaction>): AppResult<Int> = AppResult.Success(transactions.size)
    }
}
