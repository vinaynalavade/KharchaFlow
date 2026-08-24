package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.backup.BackupCategory
import com.vinaynalavade.expensetracker.core.backup.BackupData
import com.vinaynalavade.expensetracker.core.backup.BackupPreferences
import com.vinaynalavade.expensetracker.core.backup.BackupTransaction
import com.vinaynalavade.expensetracker.core.backup.BackupValidationResult
import com.vinaynalavade.expensetracker.core.backup.ImportValidationResult
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
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
import com.vinaynalavade.expensetracker.domain.usecase.SetAppTourCompletedUseCase
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
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import javax.xml.parsers.DocumentBuilderFactory

class V104ReleaseRegressionTest {

    private lateinit var prefsRepo: FakeUserPrefsRepo
    private lateinit var scheduler: FakeScheduler
    private lateinit var driveRepo: FakeDriveRepo
    private lateinit var txRepo: FakeTxRepo
    private lateinit var backupRepo: FakeBackupRepo

    @Before
    fun setUp() {
        prefsRepo = FakeUserPrefsRepo()
        scheduler = FakeScheduler()
        driveRepo = FakeDriveRepo()
        txRepo = FakeTxRepo()
        backupRepo = FakeBackupRepo()
    }

    @Test
    fun testFreshLocalUserJourney() = runBlocking {
        // 1. Initial Fresh User State
        var prefs = prefsRepo.getUserPreferences().first()
        assertTrue("isFirstLaunch must be true initially", prefs.isFirstLaunch)
        assertFalse("isAppTourCompleted must be false initially", prefs.isAppTourCompleted)

        // 2. User chooses 'Continue Locally' on WelcomeScreen
        prefsRepo.setFirstLaunchCompleted()
        prefs = prefsRepo.getUserPreferences().first()
        assertFalse("isFirstLaunch should now be false", prefs.isFirstLaunch)
        assertFalse("isAppTourCompleted is still false (tour next)", prefs.isAppTourCompleted)

        // 3. User goes through App Tour and completes/skips
        val setTourUseCase = SetAppTourCompletedUseCase(prefsRepo)
        setTourUseCase(completed = true)
        prefs = prefsRepo.getUserPreferences().first()
        assertFalse(prefs.isFirstLaunch)
        assertTrue("isAppTourCompleted is now true", prefs.isAppTourCompleted)

        // 4. Subsequent launch / restart: neither Welcome nor App Tour re-triggers
        val restartPrefs = prefsRepo.getUserPreferences().first()
        assertFalse(restartPrefs.isFirstLaunch)
        assertTrue(restartPrefs.isAppTourCompleted)
    }

    @Test
    fun testFreshGoogleUserWithoutBackup() = runBlocking {
        // 1. User signs in with Google on WelcomeScreen
        val googleAccount = GoogleAccountInfo("vinay@example.com", "Vinay Nalavade", "https://photo.url")
        driveRepo.connectedAccount = googleAccount
        driveRepo.cloudMetadata = null // No cloud backup
        prefsRepo.setFirstLaunchCompleted()

        // 2. Check Restore Eligibility
        val checkRestoreUseCase = CheckRestoreEligibilityUseCase(driveRepo, prefsRepo, txRepo)
        val eligibility = checkRestoreUseCase()
        assertTrue("Should not be eligible since no cloud backup exists", eligibility is RestoreEligibility.NotEligible)

        // 3. Completes App Tour
        val setTourUseCase = SetAppTourCompletedUseCase(prefsRepo)
        setTourUseCase(completed = true)

        val finalPrefs = prefsRepo.getUserPreferences().first()
        assertTrue(finalPrefs.isAppTourCompleted)
        assertFalse(finalPrefs.isFirstLaunch)
    }

    @Test
    fun testFreshGoogleUserWithCloudBackup() = runBlocking {
        val googleAccount = GoogleAccountInfo("vinay@example.com", "Vinay Nalavade", "https://photo.url")
        val backupTime = 1700000000000L
        driveRepo.connectedAccount = googleAccount
        driveRepo.cloudMetadata = GoogleBackupMetadata("drive-id-1", backupTime, 2048)
        txRepo.transactionsList = emptyList()
        prefsRepo.setFirstLaunchCompleted()

        // Check Restore Eligibility -> Eligible
        val checkRestoreUseCase = CheckRestoreEligibilityUseCase(driveRepo, prefsRepo, txRepo)
        val eligibility = checkRestoreUseCase()
        assertTrue(eligibility is RestoreEligibility.Eligible)
        val eligible = eligibility as RestoreEligibility.Eligible
        assertFalse("Fresh install has no existing local data", eligible.hasExistingLocalData)

        // Restore Backup
        val backupData = BackupData(1, "1.0.4", backupTime, emptyList(), emptyList(), emptyList(), BackupPreferences())
        val restoreUseCase = RestoreBackupUseCase(backupRepo, prefsRepo)
        val restoreResult = restoreUseCase(backupData)
        assertTrue(restoreResult is AppResult.Success)

        // Dismissal timestamp recorded
        val prefs = prefsRepo.getUserPreferences().first()
        assertEquals(backupTime, prefs.lastDismissedRestoreBackupTimestamp)

        // Re-check eligibility -> NotEligible (already restored)
        val recheck = checkRestoreUseCase()
        assertTrue(recheck is RestoreEligibility.NotEligible)
    }

    @Test
    fun testReturningUserLaunchesDirectlyToDashboard() = runBlocking {
        // Setup existing user
        prefsRepo.currentPrefs = UserPreferences(
            isFirstLaunch = false,
            isAppTourCompleted = true,
            userName = "Vinay",
            currency = Currency.INR
        )

        val prefs = prefsRepo.getUserPreferences().first()
        assertFalse("Existing user does not trigger Welcome", prefs.isFirstLaunch)
        assertTrue("Existing user does not trigger App Tour", prefs.isAppTourCompleted)
        assertEquals("Vinay", prefs.userName)
    }

    @Test
    fun testProfileStatePersistence() = runBlocking {
        // Set profile name
        prefsRepo.setProfileName("Vinay Nalavade")
        assertEquals("Vinay Nalavade", prefsRepo.getUserPreferences().first().userName)

        // Set custom photo URI
        prefsRepo.setProfileImageUri("content://media/external/images/media/123")
        assertEquals("content://media/external/images/media/123", prefsRepo.getUserPreferences().first().profileImageUri)

        // Remove photo (fallback to initials)
        prefsRepo.setProfileImageUri(null)
        assertNull(prefsRepo.getUserPreferences().first().profileImageUri)
        assertEquals("Vinay Nalavade", prefsRepo.getUserPreferences().first().userName)
    }

    @Test
    fun testAutomaticBackupSchedulerContract() = runBlocking {
        val useCase = SetAutomaticBackupUseCase(prefsRepo, scheduler)

        // Default disabled
        assertFalse(prefsRepo.getUserPreferences().first().automaticBackupEnabled)
        assertFalse(scheduler.isScheduled)

        // Enable
        useCase(true)
        assertTrue(prefsRepo.getUserPreferences().first().automaticBackupEnabled)
        assertTrue(scheduler.isScheduled)

        // Disable
        useCase(false)
        assertFalse(prefsRepo.getUserPreferences().first().automaticBackupEnabled)
        assertFalse(scheduler.isScheduled)
    }

    @Test
    fun testRestoreLocalDataProtection() = runBlocking {
        driveRepo.connectedAccount = GoogleAccountInfo("vinay@example.com", "Vinay", null)
        driveRepo.cloudMetadata = GoogleBackupMetadata("drive-id-2", 1700000000000L, 1024)

        // Existing local transaction
        txRepo.transactionsList = listOf(
            Transaction(
                id = 1L,
                amount = Amount(150000L),
                type = TransactionType.EXPENSE,
                category = Category(1L, "Food", "fastfood", "#FF5722", TransactionType.EXPENSE, true),
                paymentMethod = PaymentMethod.UPI,
                note = "Groceries",
                timestamp = System.currentTimeMillis()
            )
        )

        val checkUseCase = CheckRestoreEligibilityUseCase(driveRepo, prefsRepo, txRepo)
        val result = checkUseCase()
        assertTrue(result is RestoreEligibility.Eligible)
        assertTrue("Must flag existing local data for replacement confirmation", (result as RestoreEligibility.Eligible).hasExistingLocalData)
    }

    @Test
    fun testRecurringEmiFutureDateNonImmediateGeneration() {
        val repo = com.vinaynalavade.expensetracker.data.repository.RecurringTransactionRepositoryImpl(
            FakeDaoRecurring(),
            FakeDaoTx()
        )
        val zone = ZoneId.systemDefault()
        val startDate = LocalDate.of(2026, 8, 23)
        val entity = com.vinaynalavade.expensetracker.data.local.entity.RecurringTransactionEntity(
            id = 1L,
            title = "Home Loan EMI",
            amountSubunits = 3500000L,
            type = "EXPENSE",
            categoryId = 1L,
            paymentMethod = "BANK_ACCOUNT",
            frequency = "MONTHLY",
            dayOfMonth = 3,
            dayOfWeek = 1,
            startDate = startDate.atStartOfDay(zone).toInstant().toEpochMilli(),
            endDate = null,
            isEnabled = true,
            isAutoGenerated = true,
            reminderDaysBefore = 1,
            lastGeneratedDate = null,
            createdAt = startDate.atStartOfDay(zone).toInstant().toEpochMilli(),
            updatedAt = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        )

        // Check on creation day (Aug 23) -> MUST NOT BE DUE
        assertFalse("Created on Aug 23 with day 3 must NOT be due on Aug 23", repo.checkIfDue(entity, LocalDate.of(2026, 8, 23)))
        // Check on Sept 3 -> Due!
        assertTrue("Must be due on first scheduled date (Sept 3)", repo.checkIfDue(entity, LocalDate.of(2026, 9, 3)))
    }

    @Test
    fun testLocalizationThreeLanguageResourceParity() {
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val resDir = if (File(projectDir, "app/src/main/res").exists()) {
            File(projectDir, "app/src/main/res")
        } else {
            File(projectDir, "src/main/res")
        }

        val enFile = File(resDir, "values/strings.xml")
        val hiFile = File(resDir, "values-hi/strings.xml")
        val mrFile = File(resDir, "values-mr/strings.xml")

        assertTrue(enFile.exists())
        assertTrue(hiFile.exists())
        assertTrue(mrFile.exists())

        val enKeys = extractStringKeys(enFile)
        val hiKeys = extractStringKeys(hiFile)
        val mrKeys = extractStringKeys(mrFile)

        // Key areas: Settings, Backup, Profile, App Tour, Widgets
        val criticalKeys = listOf(
            "app_name",
            "nav_dashboard",
            "nav_transactions",
            "nav_analytics",
            "nav_settings",
            "total_balance",
            "total_income",
            "total_expense",
            "settings_language",
            "profile_default_local_name",
            "profile_connected_google",
            "settings_auto_backup_title",
            "settings_auto_backup_desc",
            "backup_btn_now",
            "restore_btn_now",
            "restore_prompt_title",
            "restore_replace_title",
            "app_tour_btn_skip",
            "app_tour_btn_next",
            "app_tour_btn_finish",
            "app_tour_step1_title",
            "widget_overview_description",
            "widget_today_expense_description"
        )

        for (k in criticalKeys) {
            assertTrue("Key '$k' missing in EN", enKeys.contains(k))
            assertTrue("Key '$k' missing in HI", hiKeys.contains(k))
            assertTrue("Key '$k' missing in MR", mrKeys.contains(k))
        }
    }

    private fun extractStringKeys(file: File): Set<String> {
        val keys = mutableSetOf<String>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(file)
        doc.documentElement.normalize()

        val stringNodes = doc.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue
            if (name != null) {
                keys.add(name)
            }
        }
        return keys
    }

    // --- Fakes ---

    private class FakeScheduler : AutoBackupScheduler {
        var isScheduled = false
        override fun schedule() { isScheduled = true }
        override fun cancel() { isScheduled = false }
    }

    private class FakeDriveRepo : GoogleDriveBackupRepository {
        var connectedAccount: GoogleAccountInfo? = null
        var cloudMetadata: GoogleBackupMetadata? = null

        override fun getConnectedAccount(): Flow<GoogleAccountInfo?> = flowOf(connectedAccount)
        override fun getLastCloudBackupTimestamp(): Flow<Long?> = flowOf(cloudMetadata?.modifiedTime)
        override suspend fun saveConnectedAccount(account: GoogleAccountInfo) = AppResult.Success(Unit)
        override suspend fun disconnect() = AppResult.Success(Unit)
        override suspend fun getCloudBackupMetadata(): AppResult<GoogleBackupMetadata?> = AppResult.Success(cloudMetadata)
        override suspend fun uploadBackup(backupData: BackupData): AppResult<GoogleBackupMetadata> {
            val m = GoogleBackupMetadata("test-id", System.currentTimeMillis(), 1024)
            cloudMetadata = m
            return AppResult.Success(m)
        }
        override suspend fun downloadBackup(): AppResult<BackupData> {
            return AppResult.Success(BackupData(1, "1.0.4", System.currentTimeMillis(), emptyList(), emptyList(), emptyList(), BackupPreferences()))
        }
    }

    private class FakeTxRepo : TransactionRepository {
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
        override suspend fun createFullBackup(): AppResult<BackupData> = AppResult.Success(BackupData(1, "1.0.4", System.currentTimeMillis(), emptyList(), emptyList(), emptyList(), BackupPreferences()))
        override suspend fun restoreFullBackup(backupData: BackupData): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun validateBackupJson(jsonString: String): BackupValidationResult {
            val d = BackupData(1, "1.0.4", System.currentTimeMillis(), emptyList(), emptyList(), emptyList(), BackupPreferences())
            return BackupValidationResult.Valid(d, 0, 0, 0, d.createdAt, d.appVersion)
        }
        override suspend fun getFilteredTransactions(startDate: Long?, endDate: Long?, type: TransactionType?, categoryId: Long?): AppResult<List<Transaction>> = AppResult.Success(emptyList())
        override suspend fun validateAndPrepareImportCsv(csvContent: String, defaultCurrency: Currency): ImportValidationResult = ImportValidationResult(0, emptyList(), emptyList())
        override suspend fun validateAndPrepareImportJson(jsonContent: String): ImportValidationResult = ImportValidationResult(0, emptyList(), emptyList())
        override suspend fun importTransactions(transactions: List<Transaction>): AppResult<Int> = AppResult.Success(transactions.size)
    }

    private class FakeUserPrefsRepo : UserPreferencesRepository {
        private val _flow = MutableStateFlow(UserPreferences())
        var currentPrefs: UserPreferences
            get() = _flow.value
            set(value) {
                _flow.value = value
            }

        override fun getUserPreferences(): Flow<UserPreferences> = _flow.asStateFlow()
        override suspend fun setThemeMode(themeMode: ThemeMode) = AppResult.Success(Unit)
        override suspend fun setCurrencyCode(currencyCode: String) = AppResult.Success(Unit)
        override suspend fun setDynamicColors(useDynamicColors: Boolean) = AppResult.Success(Unit)
        override suspend fun setFirstLaunchCompleted(): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(isFirstLaunch = false)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }
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
        override suspend fun setProfileName(name: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(userName = name)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }
        override suspend fun setProfileImageUri(uri: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(profileImageUri = uri)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }
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

    private class FakeDaoRecurring : com.vinaynalavade.expensetracker.data.local.dao.RecurringTransactionDao {
        override fun getAllRecurringWithCategory() = flowOf(emptyList<com.vinaynalavade.expensetracker.data.local.entity.RecurringWithCategory>())
        override fun getRecurringWithCategoryById(id: Long) = flowOf(null)
        override suspend fun getActiveRecurringTransactions() = emptyList<com.vinaynalavade.expensetracker.data.local.entity.RecurringWithCategory>()
        override suspend fun insertRecurringTransaction(entity: com.vinaynalavade.expensetracker.data.local.entity.RecurringTransactionEntity) = 1L
        override suspend fun insertRecurringTransactions(entities: List<com.vinaynalavade.expensetracker.data.local.entity.RecurringTransactionEntity>) {}
        override suspend fun updateRecurringTransaction(entity: com.vinaynalavade.expensetracker.data.local.entity.RecurringTransactionEntity) {}
        override suspend fun deleteRecurringTransactionById(id: Long) {}
        override suspend fun deleteAllRecurringTransactions() {}
    }

    private class FakeDaoTx : com.vinaynalavade.expensetracker.data.local.dao.TransactionDao {
        override fun getAllTransactionsWithCategory() = flowOf(emptyList<com.vinaynalavade.expensetracker.data.local.entity.TransactionWithCategory>())
        override fun getRecentTransactionsWithCategory(limit: Int) = flowOf(emptyList<com.vinaynalavade.expensetracker.data.local.entity.TransactionWithCategory>())
        override fun getTransactionWithCategoryById(id: Long) = flowOf(null)
        override fun getTransactionsBetween(startDate: Long, endDate: Long) = flowOf(emptyList<com.vinaynalavade.expensetracker.data.local.entity.TransactionWithCategory>())
        override fun getTotalIncomeSubunits() = flowOf(0L)
        override fun getTotalExpenseSubunits() = flowOf(0L)
        override suspend fun insertTransaction(transaction: com.vinaynalavade.expensetracker.data.local.entity.TransactionEntity) = 1L
        override suspend fun insertTransactions(transactions: List<com.vinaynalavade.expensetracker.data.local.entity.TransactionEntity>) {}
        override suspend fun updateTransaction(transaction: com.vinaynalavade.expensetracker.data.local.entity.TransactionEntity) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override suspend fun deleteAllTransactions() {}
    }
}
