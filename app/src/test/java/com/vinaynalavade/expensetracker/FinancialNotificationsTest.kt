package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.backup.BackupData
import com.vinaynalavade.expensetracker.core.backup.BackupPreferences
import com.vinaynalavade.expensetracker.core.backup.JsonBackupParser
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.BudgetThreshold
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.RecurrenceFrequency
import com.vinaynalavade.expensetracker.domain.model.RecurringReminderAdvance
import com.vinaynalavade.expensetracker.domain.model.RecurringTransaction
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.NotificationStateRepository
import com.vinaynalavade.expensetracker.domain.repository.RecurringTransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.domain.usecase.CheckBudgetThresholdsUseCase
import com.vinaynalavade.expensetracker.domain.usecase.CheckUpcomingRecurringPaymentsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FinancialNotificationsTest {

    private lateinit var fakeTransactionRepo: FakeTransactionRepository
    private lateinit var fakeRecurringRepo: FakeRecurringRepository
    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository
    private lateinit var fakeStateRepo: FakeNotificationStateRepository

    private val sampleCategory = Category(1L, "Food", "fastfood", "#FF5722", TransactionType.EXPENSE)

    @Before
    fun setUp() {
        fakeTransactionRepo = FakeTransactionRepository()
        fakeRecurringRepo = FakeRecurringRepository()
        fakePrefsRepo = FakeUserPreferencesRepository()
        fakeStateRepo = FakeNotificationStateRepository()
    }

    @Test
    fun testDefaultPreferencesHaveNotificationsOff() {
        val prefs = UserPreferences()
        assertFalse("Master notifications must be OFF by default", prefs.notificationsMasterEnabled)
        assertFalse("Daily reminder must be OFF by default", prefs.dailyReminderEnabled)
        assertFalse("Budget alerts must be OFF by default", prefs.budgetAlertsEnabled)
        assertFalse("Recurring reminders must be OFF by default", prefs.recurringRemindersEnabled)
        assertFalse("Savings goal milestones must be OFF by default", prefs.savingsGoalNotificationsEnabled)
        assertEquals(0L, prefs.monthlyBudgetLimitSubunits)
        assertEquals(1, prefs.recurringReminderAdvanceDays)
    }

    @Test
    fun testCheckBudgetThresholdsReturnsEmptyWhenDisabled() = runBlocking {
        fakePrefsRepo.setNotificationsMasterEnabled(false)
        fakePrefsRepo.setBudgetAlertsEnabled(true)
        fakePrefsRepo.setMonthlyBudgetLimit(1000000L) // 10,000 INR

        val useCase = CheckBudgetThresholdsUseCase(
            transactionRepository = fakeTransactionRepo,
            userPreferencesRepository = fakePrefsRepo,
            notificationStateRepository = fakeStateRepo
        )

        val result = useCase.invoke(LocalDate.of(2026, 8, 23))
        assertTrue("No alerts when master toggle is OFF", result.isEmpty())
    }

    @Test
    fun testCheckBudgetThresholdsDetectsCrossingCorrectly() = runBlocking {
        fakePrefsRepo.setNotificationsMasterEnabled(true)
        fakePrefsRepo.setBudgetAlertsEnabled(true)
        fakePrefsRepo.setMonthlyBudgetLimit(1000000L) // 10,000.00 INR

        val testDate = LocalDate.of(2026, 8, 23)
        val timestamp = DateTimeUtils.getStartOfDayEpoch(testDate)

        // 1. Spend ₹4,000 (40%) -> No threshold
        fakeTransactionRepo.addTransaction(
            Transaction(1L, Amount(400000L), TransactionType.EXPENSE, sampleCategory, PaymentMethod.UPI, null, timestamp)
        )

        val useCase = CheckBudgetThresholdsUseCase(fakeTransactionRepo, fakePrefsRepo, fakeStateRepo)
        var triggered = useCase(testDate)
        assertTrue("40% spent should not trigger 50% threshold", triggered.isEmpty())

        // 2. Spend another ₹1,000 -> Total ₹5,000 (50%) -> Triggers FIFTY
        fakeTransactionRepo.addTransaction(
            Transaction(2L, Amount(100000L), TransactionType.EXPENSE, sampleCategory, PaymentMethod.UPI, null, timestamp)
        )
        triggered = useCase(testDate)
        assertEquals(listOf(BudgetThreshold.FIFTY), triggered)

        // 3. Repeating check immediately should prevent duplicate alert
        val duplicateCheck = useCase(testDate)
        assertTrue("Same threshold must not fire twice in same month", duplicateCheck.isEmpty())

        // 4. Spend another ₹2,500 -> Total ₹7,500 (75%) -> Triggers SEVENTY_FIVE
        fakeTransactionRepo.addTransaction(
            Transaction(3L, Amount(250000L), TransactionType.EXPENSE, sampleCategory, PaymentMethod.UPI, null, timestamp)
        )
        triggered = useCase(testDate)
        assertEquals(listOf(BudgetThreshold.SEVENTY_FIVE), triggered)

        // 5. Spend another ₹1,500 -> Total ₹9,000 (90%) -> Triggers NINETY
        fakeTransactionRepo.addTransaction(
            Transaction(4L, Amount(150000L), TransactionType.EXPENSE, sampleCategory, PaymentMethod.UPI, null, timestamp)
        )
        triggered = useCase(testDate)
        assertEquals(listOf(BudgetThreshold.NINETY), triggered)

        // 6. Spend another ₹1,000 -> Total ₹10,000 (100%) -> Triggers HUNDRED
        fakeTransactionRepo.addTransaction(
            Transaction(5L, Amount(100000L), TransactionType.EXPENSE, sampleCategory, PaymentMethod.UPI, null, timestamp)
        )
        triggered = useCase(testDate)
        assertEquals(listOf(BudgetThreshold.HUNDRED), triggered)

        // 7. Spend another ₹500 -> Total ₹10,500 (Over Budget) -> Triggers OVER_BUDGET
        fakeTransactionRepo.addTransaction(
            Transaction(6L, Amount(50000L), TransactionType.EXPENSE, sampleCategory, PaymentMethod.UPI, null, timestamp)
        )
        triggered = useCase(testDate)
        assertEquals(listOf(BudgetThreshold.OVER_BUDGET), triggered)
    }

    @Test
    fun testMonthlyThresholdResetForNewMonth() = runBlocking {
        fakePrefsRepo.setNotificationsMasterEnabled(true)
        fakePrefsRepo.setBudgetAlertsEnabled(true)
        fakePrefsRepo.setMonthlyBudgetLimit(1000000L) // 10,000 INR

        val augustDate = LocalDate.of(2026, 8, 23)
        fakeTransactionRepo.addTransaction(
            Transaction(1L, Amount(600000L), TransactionType.EXPENSE, sampleCategory, PaymentMethod.CASH, null, DateTimeUtils.getStartOfDayEpoch(augustDate))
        )

        val useCase = CheckBudgetThresholdsUseCase(fakeTransactionRepo, fakePrefsRepo, fakeStateRepo)
        val augustTriggered = useCase(augustDate)
        assertEquals(listOf(BudgetThreshold.FIFTY), augustTriggered)

        // Switch to next month (September)
        val septemberDate = LocalDate.of(2026, 9, 5)
        fakeTransactionRepo.addTransaction(
            Transaction(2L, Amount(550000L), TransactionType.EXPENSE, sampleCategory, PaymentMethod.CASH, null, DateTimeUtils.getStartOfDayEpoch(septemberDate))
        )

        val septemberTriggered = useCase(septemberDate)
        assertEquals("New month should trigger 50% threshold again", listOf(BudgetThreshold.FIFTY), septemberTriggered)
    }

    @Test
    fun testCheckUpcomingRecurringPaymentsTiming() = runBlocking {
        fakePrefsRepo.setNotificationsMasterEnabled(true)
        fakePrefsRepo.setRecurringRemindersEnabled(true)
        fakePrefsRepo.setRecurringReminderAdvanceDays(1) // 1 day before

        // Monthly recurring rent due on 25th of the month
        val rent = RecurringTransaction(
            id = 42L,
            title = "Apartment Rent",
            amount = Amount(1500000L), // ₹15,000
            type = TransactionType.EXPENSE,
            category = sampleCategory,
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = 25,
            reminderDaysBefore = 1 // 1 day before
        )
        fakeRecurringRepo.insertRecurringTransaction(rent)

        val useCase = CheckUpcomingRecurringPaymentsUseCase(fakeRecurringRepo, fakePrefsRepo, fakeStateRepo)

        // 1. Checked on August 20 (5 days before) -> No alert
        var alerts = useCase(LocalDate.of(2026, 8, 20))
        assertTrue("No alert 5 days before when 1 day configured", alerts.isEmpty())

        // 2. Checked on August 24 (1 day before) -> Alert triggered!
        alerts = useCase(LocalDate.of(2026, 8, 24))
        assertEquals(1, alerts.size)
        assertEquals(42L, alerts[0].first.id)
        assertEquals(1, alerts[0].second)

        // 3. Second check on August 24 -> Prevent duplicate!
        val duplicateAlerts = useCase(LocalDate.of(2026, 8, 24))
        assertTrue("Duplicate recurring alerts on same day must be prevented", duplicateAlerts.isEmpty())
    }

    @Test
    fun testBackupPreferencesIncludesNotificationSettings() {
        val backupPrefs = BackupPreferences(
            openingBalanceSubunits = 500000L,
            currencyCode = "INR",
            themeMode = "DARK",
            dailyReminderEnabled = true,
            dailyReminderHour = 20,
            dailyReminderMinute = 30,
            emiRemindersEnabled = true,
            notificationsMasterEnabled = true,
            budgetAlertsEnabled = true,
            monthlyBudgetLimitSubunits = 2500000L,
            recurringRemindersEnabled = true,
            recurringReminderAdvanceDays = 3,
            savingsGoalNotificationsEnabled = true
        )

        val backupData = BackupData(
            categories = emptyList(),
            transactions = emptyList(),
            recurringTransactions = emptyList(),
            preferences = backupPrefs
        )

        val json = JsonBackupParser.toJson(backupData)
        assertTrue(json.contains("\"notificationsMasterEnabled\": true"))
        assertTrue(json.contains("\"budgetAlertsEnabled\": true"))
        assertTrue(json.contains("\"monthlyBudgetLimitSubunits\": 2500000"))
        assertTrue(json.contains("\"recurringReminderAdvanceDays\": 3"))

        val parsed = JsonBackupParser.fromJson(json)
        assertEquals(true, parsed.preferences.notificationsMasterEnabled)
        assertEquals(true, parsed.preferences.budgetAlertsEnabled)
        assertEquals(2500000L, parsed.preferences.monthlyBudgetLimitSubunits)
        assertEquals(3, parsed.preferences.recurringReminderAdvanceDays)
        assertEquals(true, parsed.preferences.savingsGoalNotificationsEnabled)
    }

    @Test
    fun testNotificationRoutesAndChannelsConstants() {
        assertEquals("add_expense", NotificationHelper.ROUTE_ADD_EXPENSE)
        assertEquals("add_income", NotificationHelper.ROUTE_ADD_INCOME)
        assertEquals("transactions", NotificationHelper.ROUTE_TRANSACTIONS)
        assertEquals("recurring_transactions", NotificationHelper.ROUTE_RECURRING)
        assertEquals("dashboard", NotificationHelper.ROUTE_DASHBOARD)

        assertEquals("channel_daily_reminder", NotificationHelper.CHANNEL_DAILY_REMINDER)
        assertEquals("channel_budget_alerts", NotificationHelper.CHANNEL_BUDGET_ALERTS)
        assertEquals("channel_payment_reminders", NotificationHelper.CHANNEL_PAYMENT_REMINDERS)
        assertEquals("channel_savings_goals", NotificationHelper.CHANNEL_SAVINGS_GOALS)
    }

    // --- Fake Test Implementations ---

    private class FakeTransactionRepository : TransactionRepository {
        private val transactions = mutableListOf<Transaction>()
        private val _flow = MutableStateFlow<List<Transaction>>(emptyList())

        fun addTransaction(tx: Transaction) {
            transactions.add(tx)
            _flow.value = transactions.toList()
        }

        override fun getTransactions(): Flow<List<Transaction>> = _flow.asStateFlow()
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(transactions.take(limit))
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(transactions.find { it.id == id })
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(transactions.filter { it.timestamp in startDate..endDate })

        override fun getFinancialSummary(): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override suspend fun insertTransaction(transaction: Transaction): AppResult<Long> {
            addTransaction(transaction)
            return AppResult.Success(transaction.id)
        }
        override suspend fun updateTransaction(transaction: Transaction): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteTransaction(id: Long): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeRecurringRepository : RecurringTransactionRepository {
        private val list = mutableListOf<RecurringTransaction>()
        private val _flow = MutableStateFlow<List<RecurringTransaction>>(emptyList())

        override fun getRecurringTransactions(): Flow<List<RecurringTransaction>> = _flow.asStateFlow()
        override fun getRecurringTransactionById(id: Long): Flow<RecurringTransaction?> = flowOf(list.find { it.id == id })
        override suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction): AppResult<Long> {
            list.add(recurringTransaction)
            _flow.value = list.toList()
            return AppResult.Success(recurringTransaction.id)
        }
        override suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteRecurringTransaction(id: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun processDueOccurrences(): AppResult<Int> = AppResult.Success(0)
    }

    private class FakeNotificationStateRepository : NotificationStateRepository {
        private val budgetFired = mutableSetOf<String>()
        private val recurringFired = mutableSetOf<String>()
        private val goalFired = mutableSetOf<String>()

        override fun hasBudgetThresholdFired(monthKey: String, threshold: BudgetThreshold): Boolean {
            return budgetFired.contains("${monthKey}_${threshold.name}")
        }

        override fun markBudgetThresholdFired(monthKey: String, threshold: BudgetThreshold) {
            budgetFired.add("${monthKey}_${threshold.name}")
        }

        override fun hasRecurringReminderFired(dateKey: String, recurringId: Long): Boolean {
            return recurringFired.contains("${dateKey}_$recurringId")
        }

        override fun markRecurringReminderFired(dateKey: String, recurringId: Long) {
            recurringFired.add("${dateKey}_$recurringId")
        }

        override fun hasGoalMilestoneFired(goalId: String, milestone: Int): Boolean {
            return goalFired.contains("${goalId}_$milestone")
        }

        override fun markGoalMilestoneFired(goalId: String, milestone: Int) {
            goalFired.add("${goalId}_$milestone")
        }

        override fun clearOldNotificationState(currentMonthKey: String, currentDateKey: String) {
            budgetFired.removeAll { !it.startsWith(currentMonthKey) }
            recurringFired.removeAll { !it.startsWith(currentDateKey) }
        }
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
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

        override suspend fun setNotificationsMasterEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(notificationsMasterEnabled = enabled)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setBudgetAlertsEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(budgetAlertsEnabled = enabled)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setMonthlyBudgetLimit(subunits: Long): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(monthlyBudgetLimitSubunits = subunits)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setRecurringRemindersEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(recurringRemindersEnabled = enabled)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setRecurringReminderAdvanceDays(days: Int): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(recurringReminderAdvanceDays = days)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setSavingsGoalNotificationsEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(savingsGoalNotificationsEnabled = enabled)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }
    }
}
