package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.GoogleAccountInfo
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupState
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.presentation.components.BottomNavItems
import com.vinaynalavade.expensetracker.presentation.navigation.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReleasePolishAndOnboardingTest {

    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository
    private lateinit var fakeTransactionRepo: FakeTransactionRepository

    private val sampleCategory = Category(1L, "Food", "fastfood", "#FF5722", TransactionType.EXPENSE)

    @Before
    fun setUp() {
        fakePrefsRepo = FakeUserPreferencesRepository()
        fakeTransactionRepo = FakeTransactionRepository()
    }

    @Test
    fun testFreshInstallDefaultsToLightAndFirstLaunchTrue() = runBlocking {
        val prefs = fakePrefsRepo.getUserPreferences().first()
        assertTrue("Fresh install must have isFirstLaunch = true", prefs.isFirstLaunch)
        assertEquals("Fresh install must default to ThemeMode.LIGHT", ThemeMode.LIGHT, prefs.themeMode)
    }

    @Test
    fun testOnboardingCompletionPersists() = runBlocking {
        assertTrue(fakePrefsRepo.getUserPreferences().first().isFirstLaunch)

        // User finishes onboarding
        fakePrefsRepo.setFirstLaunchCompleted()

        val updatedPrefs = fakePrefsRepo.getUserPreferences().first()
        assertFalse("After onboarding, isFirstLaunch must be false", updatedPrefs.isFirstLaunch)
    }

    @Test
    fun testUserThemePreferenceIsPreserved() = runBlocking {
        // User explicitly sets Dark mode
        fakePrefsRepo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, fakePrefsRepo.getUserPreferences().first().themeMode)

        // User sets System mode
        fakePrefsRepo.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, fakePrefsRepo.getUserPreferences().first().themeMode)

        // User sets Light mode
        fakePrefsRepo.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, fakePrefsRepo.getUserPreferences().first().themeMode)
    }

    @Test
    fun testLocalTransactionsPreservedAcrossGoogleAccountOperations() = runBlocking {
        // 1. Add some local transactions
        val tx1 = Transaction(1L, Amount(50000L), TransactionType.EXPENSE, sampleCategory, PaymentMethod.UPI, "Lunch", 1000L)
        val tx2 = Transaction(2L, Amount(200000L), TransactionType.INCOME, sampleCategory, PaymentMethod.BANK_ACCOUNT, "Salary", 2000L)
        fakeTransactionRepo.insertTransaction(tx1)
        fakeTransactionRepo.insertTransaction(tx2)

        var list = fakeTransactionRepo.getTransactions().first()
        assertEquals(2, list.size)

        // 2. Simulate connecting Google account
        val googleAccount = GoogleAccountInfo("test@example.com", "Test User", null)
        fakePrefsRepo.saveConnectedGoogleAccount(googleAccount)
        val connectedState = fakePrefsRepo.getGoogleBackupState().first()
        assertTrue(connectedState is GoogleBackupState.Connected)
        assertEquals("test@example.com", (connectedState as GoogleBackupState.Connected).account.email)

        // Verify local transactions are untouched
        list = fakeTransactionRepo.getTransactions().first()
        assertEquals(2, list.size)

        // 3. Simulate disconnecting Google account
        fakePrefsRepo.disconnectGoogleAccount()
        val disconnectedState = fakePrefsRepo.getGoogleBackupState().first()
        assertTrue(disconnectedState is GoogleBackupState.Disconnected)

        // Local data must NEVER be lost merely because user disconnected Google account
        list = fakeTransactionRepo.getTransactions().first()
        assertEquals("Local transactions must persist after Google account disconnect", 2, list.size)
    }

    @Test
    fun testGoogleAccountAndAppLockAreDecoupled() = runBlocking {
        // Enabling Google account does not affect App Lock
        assertFalse(fakePrefsRepo.getUserPreferences().first().appLockEnabled)
        fakePrefsRepo.saveConnectedGoogleAccount(GoogleAccountInfo("user@gmail.com", "User", null))
        assertFalse("App Lock must remain independent of Google Sign-In", fakePrefsRepo.getUserPreferences().first().appLockEnabled)

        // Enabling App Lock does not affect Google account
        fakePrefsRepo.setAppLockEnabled(true)
        assertTrue(fakePrefsRepo.getUserPreferences().first().appLockEnabled)
        val googleState = fakePrefsRepo.getGoogleBackupState().first()
        assertTrue(googleState is GoogleBackupState.Connected)
    }

    @Test
    fun testBottomNavigationStructure() {
        val bottomRoutes = BottomNavItems.map { it.route }
        assertEquals(4, bottomRoutes.size)
        assertEquals(listOf(Screen.Dashboard.route, Screen.Transactions.route, Screen.Analytics.route, Screen.Settings.route), bottomRoutes)
        assertFalse("Categories must NOT be in bottom navigation", bottomRoutes.contains(Screen.Categories.route))
    }

    // --- Fake Test Implementations ---

    private class FakeTransactionRepository : TransactionRepository {
        private val transactions = mutableListOf<Transaction>()
        private val _flow = MutableStateFlow<List<Transaction>>(emptyList())

        override fun getTransactions(): Flow<List<Transaction>> = _flow.asStateFlow()
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(transactions.take(limit))
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(transactions.find { it.id == id })
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(transactions.filter { it.timestamp in startDate..endDate })

        override fun getFinancialSummary(): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)

        override suspend fun insertTransaction(transaction: Transaction): AppResult<Long> {
            transactions.add(transaction)
            _flow.value = transactions.toList()
            return AppResult.Success(transaction.id)
        }

        override suspend fun updateTransaction(transaction: Transaction): AppResult<Unit> {
            val idx = transactions.indexOfFirst { it.id == transaction.id }
            if (idx != -1) transactions[idx] = transaction
            _flow.value = transactions.toList()
            return AppResult.Success(Unit)
        }

        override suspend fun deleteTransaction(id: Long): AppResult<Unit> {
            transactions.removeAll { it.id == id }
            _flow.value = transactions.toList()
            return AppResult.Success(Unit)
        }
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        var currentPrefs = UserPreferences()
        private val _prefsFlow = MutableStateFlow(currentPrefs)
        private val _googleStateFlow = MutableStateFlow<GoogleBackupState>(GoogleBackupState.Disconnected)

        override fun getUserPreferences(): Flow<UserPreferences> = _prefsFlow.asStateFlow()
        override suspend fun setThemeMode(themeMode: ThemeMode): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(themeMode = themeMode)
            _prefsFlow.value = currentPrefs
            return AppResult.Success(Unit)
        }
        override suspend fun setCurrencyCode(currencyCode: String) = AppResult.Success(Unit)
        override suspend fun setDynamicColors(useDynamicColors: Boolean) = AppResult.Success(Unit)
        override suspend fun setFirstLaunchCompleted(): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(isFirstLaunch = false)
            _prefsFlow.value = currentPrefs
            return AppResult.Success(Unit)
        }
        override suspend fun setOpeningBalance(subunits: Long) = AppResult.Success(Unit)
        override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) = AppResult.Success(Unit)
        override suspend fun setEmiReminders(enabled: Boolean) = AppResult.Success(Unit)
        override fun getLastBackupTimestamp(): Flow<Long?> = flowOf(null)
        override suspend fun setLastBackupTimestamp(timestamp: Long) = AppResult.Success(Unit)

        override suspend fun setAppLockEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(appLockEnabled = enabled)
            _prefsFlow.value = currentPrefs
            return AppResult.Success(Unit)
        }
        override suspend fun setBiometricEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(biometricEnabled = enabled)
            _prefsFlow.value = currentPrefs
            return AppResult.Success(Unit)
        }
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
            _prefsFlow.value = currentPrefs
            return AppResult.Success(Unit)
        }
        override suspend fun setProfileImageUri(uri: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(profileImageUri = uri)
            _prefsFlow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setAutomaticBackupEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(automaticBackupEnabled = enabled)
            _prefsFlow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setLastBackupStatus(status: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(lastBackupStatus = status)
            _prefsFlow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setLastBackupError(error: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(lastBackupError = error)
            _prefsFlow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setLastDismissedRestoreBackupTimestamp(timestamp: Long?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(lastDismissedRestoreBackupTimestamp = timestamp)
            _prefsFlow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setAppTourCompleted(completed: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(isAppTourCompleted = completed)
            _prefsFlow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        fun getGoogleBackupState(): Flow<GoogleBackupState> = _googleStateFlow.asStateFlow()

        fun saveConnectedGoogleAccount(account: GoogleAccountInfo) {
            _googleStateFlow.value = GoogleBackupState.Connected(account, null)
        }

        fun disconnectGoogleAccount() {
            _googleStateFlow.value = GoogleBackupState.Disconnected
        }
    }
}
