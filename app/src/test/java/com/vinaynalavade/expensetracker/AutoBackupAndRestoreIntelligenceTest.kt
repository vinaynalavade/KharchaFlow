package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.backup.BackupCategory
import com.vinaynalavade.expensetracker.core.backup.BackupData
import com.vinaynalavade.expensetracker.core.backup.BackupPreferences
import com.vinaynalavade.expensetracker.core.backup.BackupTransaction
import com.vinaynalavade.expensetracker.core.backup.BackupValidationResult
import com.vinaynalavade.expensetracker.core.backup.ImportValidationResult
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.worker.AutoBackupScheduler
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.GoogleAccountInfo
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupMetadata
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.RestoreEligibility
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.BackupRepository
import com.vinaynalavade.expensetracker.domain.repository.GoogleDriveBackupRepository
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.domain.usecase.CheckRestoreEligibilityUseCase
import com.vinaynalavade.expensetracker.domain.usecase.DismissRestorePromptUseCase
import com.vinaynalavade.expensetracker.domain.usecase.RestoreBackupUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetAutomaticBackupUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutoBackupAndRestoreIntelligenceTest {

    private lateinit var fakePrefsRepo: FakeUserPreferencesRepo
    private lateinit var fakeScheduler: FakeAutoBackupScheduler
    private lateinit var fakeGoogleDriveRepo: FakeGoogleDriveBackupRepo
    private lateinit var fakeTxRepo: FakeTransactionRepo
    private lateinit var fakeBackupRepo: FakeBackupRepo

    @Before
    fun setUp() {
        fakePrefsRepo = FakeUserPreferencesRepo()
        fakeScheduler = FakeAutoBackupScheduler()
        fakeGoogleDriveRepo = FakeGoogleDriveBackupRepo()
        fakeTxRepo = FakeTransactionRepo()
        fakeBackupRepo = FakeBackupRepo()
    }

    @Test
    fun testDefaultAutomaticBackupIsDisabled() = runBlocking {
        val prefs = fakePrefsRepo.getUserPreferences().first()
        assertFalse("Automatic backup must default to disabled (false)", prefs.automaticBackupEnabled)
        assertNull("Last backup status must be null initially", prefs.lastBackupStatus)
        assertNull("Last backup error must be null initially", prefs.lastBackupError)
        assertNull("Last dismissed restore timestamp must be null initially", prefs.lastDismissedRestoreBackupTimestamp)
    }

    @Test
    fun testEnablingAutomaticBackupSchedulesWork() = runBlocking {
        val useCase = SetAutomaticBackupUseCase(fakePrefsRepo, fakeScheduler)
        val result = useCase(true)

        assertTrue(result is AppResult.Success)
        val prefs = fakePrefsRepo.getUserPreferences().first()
        assertTrue("Automatic backup must be enabled in preferences", prefs.automaticBackupEnabled)
        assertTrue("Scheduler schedule() must be called", fakeScheduler.isScheduled)
    }

    @Test
    fun testDisablingAutomaticBackupCancelsWork() = runBlocking {
        val useCase = SetAutomaticBackupUseCase(fakePrefsRepo, fakeScheduler)
        // 1. Enable first
        useCase(true)
        assertTrue(fakeScheduler.isScheduled)

        // 2. Disable
        val result = useCase(false)
        assertTrue(result is AppResult.Success)
        val prefs = fakePrefsRepo.getUserPreferences().first()
        assertFalse("Automatic backup must be disabled in preferences", prefs.automaticBackupEnabled)
        assertFalse("Scheduler work must be cancelled", fakeScheduler.isScheduled)
    }

    @Test
    fun testRestoreEligibilityFreshAppWithCloudBackup() = runBlocking {
        // Setup: Connected Google account & Cloud backup metadata available
        fakeGoogleDriveRepo.connectedAccount = GoogleAccountInfo("vinay@example.com", "Vinay Nalavade", null)
        fakeGoogleDriveRepo.cloudMetadata = GoogleBackupMetadata(
            fileId = "drive-file-1",
            modifiedTime = 1700000000000L,
            sizeBytes = 2048
        )
        fakeTxRepo.transactionsList = emptyList()

        val checkUseCase = CheckRestoreEligibilityUseCase(fakeGoogleDriveRepo, fakePrefsRepo, fakeTxRepo)
        val eligibility = checkUseCase()

        assertTrue("Should be eligible for restore prompt", eligibility is RestoreEligibility.Eligible)
        val eligible = eligibility as RestoreEligibility.Eligible
        assertEquals("drive-file-1", eligible.metadata.fileId)
        assertFalse("Fresh app must indicate NO existing data", eligible.hasExistingLocalData)
    }

    @Test
    fun testRestoreEligibilityExistingDataPromptsDestructiveWarning() = runBlocking {
        fakeGoogleDriveRepo.connectedAccount = GoogleAccountInfo("vinay@example.com", "Vinay Nalavade", null)
        fakeGoogleDriveRepo.cloudMetadata = GoogleBackupMetadata(
            fileId = "drive-file-1",
            modifiedTime = 1700000000000L,
            sizeBytes = 2048
        )
        fakeTxRepo.transactionsList = listOf(
            Transaction(
                id = 1L,
                amount = Amount(50000L),
                type = TransactionType.EXPENSE,
                category = Category(1L, "Food", "fastfood", "#FF5722", TransactionType.EXPENSE, true),
                paymentMethod = PaymentMethod.UPI,
                note = "Dinner",
                timestamp = System.currentTimeMillis()
            )
        )

        val checkUseCase = CheckRestoreEligibilityUseCase(fakeGoogleDriveRepo, fakePrefsRepo, fakeTxRepo)
        val eligibility = checkUseCase()

        assertTrue("Should be eligible for restore prompt", eligibility is RestoreEligibility.Eligible)
        val eligible = eligibility as RestoreEligibility.Eligible
        assertTrue("App with transactions must indicate existing data to require destructive replacement confirmation", eligible.hasExistingLocalData)
    }

    @Test
    fun testDismissRestorePromptPreventsRepeatedPrompts() = runBlocking {
        fakeGoogleDriveRepo.connectedAccount = GoogleAccountInfo("vinay@example.com", "Vinay Nalavade", null)
        val backupTime = 1700000000000L
        fakeGoogleDriveRepo.cloudMetadata = GoogleBackupMetadata(
            fileId = "drive-file-1",
            modifiedTime = backupTime,
            sizeBytes = 2048
        )

        val checkUseCase = CheckRestoreEligibilityUseCase(fakeGoogleDriveRepo, fakePrefsRepo, fakeTxRepo)
        val dismissUseCase = DismissRestorePromptUseCase(fakePrefsRepo)

        // 1. Initial check: Eligible
        val initial = checkUseCase()
        assertTrue(initial is RestoreEligibility.Eligible)

        // 2. Dismiss prompt
        val dismissResult = dismissUseCase(backupTime)
        assertTrue(dismissResult is AppResult.Success)

        // 3. Subsequent check: Not Eligible (already dismissed for this backup timestamp)
        val subsequent = checkUseCase()
        assertTrue("Subsequent check must be NotEligible after dismissal", subsequent is RestoreEligibility.NotEligible)
    }

    @Test
    fun testNewerCloudBackupReEnablesRestoreEligibility() = runBlocking {
        fakeGoogleDriveRepo.connectedAccount = GoogleAccountInfo("vinay@example.com", "Vinay Nalavade", null)
        val oldBackupTime = 1700000000000L
        val newerBackupTime = 1700000050000L

        fakeGoogleDriveRepo.cloudMetadata = GoogleBackupMetadata(
            fileId = "drive-file-1",
            modifiedTime = oldBackupTime,
            sizeBytes = 2048
        )

        val checkUseCase = CheckRestoreEligibilityUseCase(fakeGoogleDriveRepo, fakePrefsRepo, fakeTxRepo)
        val dismissUseCase = DismissRestorePromptUseCase(fakePrefsRepo)

        // 1. Dismiss old backup
        dismissUseCase(oldBackupTime)
        assertTrue(checkUseCase() is RestoreEligibility.NotEligible)

        // 2. Newer backup uploaded to Google Drive
        fakeGoogleDriveRepo.cloudMetadata = GoogleBackupMetadata(
            fileId = "drive-file-2",
            modifiedTime = newerBackupTime,
            sizeBytes = 4096
        )

        // 3. Should become eligible again!
        val result = checkUseCase()
        assertTrue("Newer backup must be eligible for restore prompt", result is RestoreEligibility.Eligible)
        assertEquals(newerBackupTime, (result as RestoreEligibility.Eligible).metadata.modifiedTime)
    }

    @Test
    fun testRestoreBackupUpdatesDismissedTimestamp() = runBlocking {
        val testBackupData = BackupData(
            backupVersion = 1,
            appVersion = "1.0.4",
            createdAt = 1700000000000L,
            categories = emptyList(),
            transactions = emptyList(),
            recurringTransactions = emptyList(),
            preferences = BackupPreferences()
        )

        val restoreUseCase = RestoreBackupUseCase(fakeBackupRepo, fakePrefsRepo)
        val result = restoreUseCase(testBackupData)

        assertTrue(result is AppResult.Success)
        val prefs = fakePrefsRepo.getUserPreferences().first()
        assertEquals("Restoring backup must record its timestamp as dismissed/restored", 1700000000000L, prefs.lastDismissedRestoreBackupTimestamp)
    }

    // --- Fakes ---

    private class FakeAutoBackupScheduler : AutoBackupScheduler {
        var isScheduled: Boolean = false

        override fun schedule() {
            isScheduled = true
        }

        override fun cancel() {
            isScheduled = false
        }
    }

    private class FakeGoogleDriveBackupRepo : GoogleDriveBackupRepository {
        var connectedAccount: GoogleAccountInfo? = null
        var cloudMetadata: GoogleBackupMetadata? = null

        override fun getConnectedAccount(): Flow<GoogleAccountInfo?> = flowOf(connectedAccount)
        override fun getLastCloudBackupTimestamp(): Flow<Long?> = flowOf(cloudMetadata?.modifiedTime)
        override suspend fun saveConnectedAccount(account: GoogleAccountInfo) = AppResult.Success(Unit)
        override suspend fun disconnect() = AppResult.Success(Unit)
        override suspend fun getCloudBackupMetadata(): AppResult<GoogleBackupMetadata?> = AppResult.Success(cloudMetadata)
        override suspend fun uploadBackup(backupData: BackupData): AppResult<GoogleBackupMetadata> {
            val meta = GoogleBackupMetadata("test-id", System.currentTimeMillis(), 1024)
            cloudMetadata = meta
            return AppResult.Success(meta)
        }
        override suspend fun downloadBackup(): AppResult<BackupData> {
            return AppResult.Success(
                BackupData(1, "1.0.4", System.currentTimeMillis(), emptyList(), emptyList(), emptyList(), BackupPreferences())
            )
        }
    }

    private class FakeTransactionRepo : TransactionRepository {
        var transactionsList: List<Transaction> = emptyList()

        override fun getTransactions(): Flow<List<Transaction>> = flowOf(transactionsList)
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(transactionsList.find { it.id == id })
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(transactionsList.filter { it.timestamp in startDate..endDate })
        override fun getFinancialSummary(): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override suspend fun insertTransaction(transaction: Transaction): AppResult<Long> = AppResult.Success(1L)
        override suspend fun updateTransaction(transaction: Transaction): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteTransaction(id: Long): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeBackupRepo : BackupRepository {
        override fun getLastBackupTimestamp(): Flow<Long?> = flowOf(null)

        override suspend fun createFullBackup(): AppResult<BackupData> {
            return AppResult.Success(BackupData(1, "1.0.4", System.currentTimeMillis(), emptyList(), emptyList(), emptyList(), BackupPreferences()))
        }

        override suspend fun restoreFullBackup(backupData: BackupData): AppResult<Unit> {
            return AppResult.Success(Unit)
        }

        override suspend fun validateBackupJson(jsonString: String): BackupValidationResult {
            val data = BackupData(1, "1.0.4", System.currentTimeMillis(), emptyList(), emptyList(), emptyList(), BackupPreferences())
            return BackupValidationResult.Valid(data, 0, 0, 0, data.createdAt, data.appVersion)
        }

        override suspend fun getFilteredTransactions(startDate: Long?, endDate: Long?, type: TransactionType?, categoryId: Long?): AppResult<List<Transaction>> {
            return AppResult.Success(emptyList())
        }

        override suspend fun validateAndPrepareImportCsv(csvContent: String, defaultCurrency: Currency): ImportValidationResult {
            return ImportValidationResult(0, emptyList(), emptyList())
        }

        override suspend fun validateAndPrepareImportJson(jsonContent: String): ImportValidationResult {
            return ImportValidationResult(0, emptyList(), emptyList())
        }

        override suspend fun importTransactions(transactions: List<Transaction>): AppResult<Int> {
            return AppResult.Success(transactions.size)
        }
    }

    private class FakeUserPreferencesRepo : UserPreferencesRepository {
        var currentPrefs = UserPreferences()
        private val _flow = MutableStateFlow(currentPrefs)

        override fun getUserPreferences(): Flow<UserPreferences> = _flow.asStateFlow()
        override suspend fun setThemeMode(themeMode: ThemeMode) = AppResult.Success(Unit)
        override suspend fun setCurrencyCode(currencyCode: String) = AppResult.Success(Unit)
        override suspend fun setDynamicColors(useDynamicColors: Boolean) = AppResult.Success(Unit)
        override suspend fun setFirstLaunchCompleted() = AppResult.Success(Unit)
        override suspend fun setOpeningBalance(subunits: Long) = AppResult.Success(Unit)
        override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) = AppResult.Success(Unit)
        override suspend fun setEmiReminders(enabled: Boolean) = AppResult.Success(Unit)
        override fun getLastBackupTimestamp(): Flow<Long?> = flowOf(null)
        override suspend fun setLastBackupTimestamp(timestamp: Long) = AppResult.Success(Unit)
        override suspend fun setAppLockEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setBiometricEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setAutoLockDurationSeconds(seconds: Long) = AppResult.Success(Unit)
        override suspend fun setHideContentInRecents(hide: Boolean) = AppResult.Success(Unit)
        override suspend fun setNotificationsMasterEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setBudgetAlertsEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setMonthlyBudgetLimit(subunits: Long) = AppResult.Success(Unit)
        override suspend fun setRecurringRemindersEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setRecurringReminderAdvanceDays(days: Int) = AppResult.Success(Unit)
        override suspend fun setSavingsGoalNotificationsEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setAppLanguage(languageCode: String) = AppResult.Success(Unit)
        override suspend fun setProfileName(name: String?) = AppResult.Success(Unit)
        override suspend fun setProfileImageUri(uri: String?) = AppResult.Success(Unit)

        override suspend fun setAutomaticBackupEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(automaticBackupEnabled = enabled)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setLastBackupStatus(status: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(lastBackupStatus = status)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setLastBackupError(error: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(lastBackupError = error)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setLastDismissedRestoreBackupTimestamp(timestamp: Long?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(lastDismissedRestoreBackupTimestamp = timestamp)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setAppTourCompleted(completed: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(isAppTourCompleted = completed)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }
    }
}
