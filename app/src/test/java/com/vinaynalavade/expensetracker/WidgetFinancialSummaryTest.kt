package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.domain.usecase.GetWidgetFinancialSummaryUseCase
import com.vinaynalavade.expensetracker.presentation.widget.WidgetUpdateManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class WidgetFinancialSummaryTest {

    private val foodCategory = Category(
        id = 1L,
        name = "Food",
        iconName = "restaurant",
        colorHex = "#EF4444",
        type = TransactionType.EXPENSE
    )

    private val salaryCategory = Category(
        id = 2L,
        name = "Salary",
        iconName = "wallet",
        colorHex = "#10B981",
        type = TransactionType.INCOME
    )

    @Test
    fun testEmptyStateFinancialSummary() = runBlocking {
        val userPrefs = UserPreferences(openingBalanceSubunits = 100000L)
        val fakeTxRepo = FakeTransactionRepository(emptyList())
        val fakePrefsRepo = FakeUserPreferencesRepository(userPrefs)

        val useCase = GetWidgetFinancialSummaryUseCase(fakeTxRepo, fakePrefsRepo)
        val summary = useCase().first()

        assertEquals(100000L, summary.balance.subunits)
        assertEquals(0L, summary.todayExpense.subunits)
        assertEquals(0L, summary.monthlyIncome.subunits)
        assertEquals(0L, summary.monthlyExpense.subunits)
        assertNull(summary.monthlyBudgetLimit)
        assertNull(summary.remainingBudget)
        assertFalse(summary.isOverBudget)
        assertNull(summary.latestTransaction)
    }

    @Test
    fun testTodayAndCurrentMonthExpenseCalculations() = runBlocking {
        val today = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startOfToday = DateTimeUtils.getStartOfDayEpoch(today)
        val startOfMonth = DateTimeUtils.getStartOfMonthEpoch(currentMonth)
        val previousMonthEpoch = startOfMonth - (15 * 24 * 60 * 60 * 1000L)

        // Earlier in the current month (5 days before today, or at start of month if today is early)
        val earlierThisMonthEpoch = if (startOfToday > startOfMonth + 86400000L) {
            startOfToday - 86400000L
        } else {
            startOfMonth + 1000L
        }

        val transactions = listOf(
            // 1. Previous month income: ₹50,000 (excluded from this month & today)
            Transaction(
                id = 1L,
                amount = Amount(5000000L),
                type = TransactionType.INCOME,
                category = salaryCategory,
                timestamp = previousMonthEpoch,
                paymentMethod = PaymentMethod.ACCOUNT
            ),
            // 2. Previous month expense: ₹10,000 (excluded from this month & today)
            Transaction(
                id = 2L,
                amount = Amount(1000000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = previousMonthEpoch + 1000L,
                paymentMethod = PaymentMethod.ACCOUNT
            ),
            // 3. Current month income: ₹35,000 (included in monthly income, excluded from today's expense)
            Transaction(
                id = 3L,
                amount = Amount(3500000L),
                type = TransactionType.INCOME,
                category = salaryCategory,
                timestamp = startOfToday + 1000L,
                paymentMethod = PaymentMethod.ACCOUNT
            ),
            // 4. Earlier this month expense: ₹8,000 (included in monthly expense, excluded from today's expense)
            Transaction(
                id = 4L,
                amount = Amount(800000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = earlierThisMonthEpoch,
                paymentMethod = PaymentMethod.ACCOUNT
            ),
            // 5. Today's expense 1: ₹1,500 (included in both today's expense & monthly expense)
            Transaction(
                id = 5L,
                amount = Amount(150000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = startOfToday + 5000L,
                paymentMethod = PaymentMethod.ACCOUNT
            ),
            // 6. Today's expense 2: ₹2,000 (included in both today's expense & monthly expense)
            Transaction(
                id = 6L,
                amount = Amount(200000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = startOfToday + 6000L,
                paymentMethod = PaymentMethod.CASH
            )
        )

        val userPrefs = UserPreferences(
            monthlyBudgetLimitSubunits = 2000000L // ₹20,000 Budget
        )
        val fakeTxRepo = FakeTransactionRepository(transactions)
        val fakePrefsRepo = FakeUserPreferencesRepository(userPrefs)

        val useCase = GetWidgetFinancialSummaryUseCase(fakeTxRepo, fakePrefsRepo)
        val summary = useCase.forRanges(
            startOfDayEpoch = startOfToday,
            endOfDayEpoch = DateTimeUtils.getEndOfDayEpoch(today),
            startOfMonthEpoch = startOfMonth,
            endOfMonthEpoch = DateTimeUtils.getEndOfMonthEpoch(currentMonth)
        ).first()

        // 0. Total balance = 85,000 income - 21,500 expense = 63,500 (6,350,000 subunits)
        assertEquals(6350000L, summary.balance.subunits)

        // 1. Today's expense = 1,500 + 2,000 = ₹3,500 (350,000 subunits)
        assertEquals(350000L, summary.todayExpense.subunits)

        // 2. Current month expense = 8,000 + 1,500 + 2,000 = ₹11,500 (1,150,000 subunits)
        assertEquals(1150000L, summary.monthlyExpense.subunits)

        // 3. Current month income = ₹35,000 (3,500,000 subunits)
        assertEquals(3500000L, summary.monthlyIncome.subunits)

        // 4. Budget remaining = 20,000 - 11,500 = ₹8,500 (850,000 subunits)
        assertEquals(2000000L, summary.monthlyBudgetLimit?.subunits)
        assertEquals(850000L, summary.remainingBudget?.subunits)
        assertFalse(summary.isOverBudget)

        // 5. Latest transaction = Transaction 6
        assertNotNull(summary.latestTransaction)
        assertEquals(6L, summary.latestTransaction?.id)
        assertEquals(200000L, summary.latestTransaction?.amount?.subunits)
        assertEquals(TransactionType.EXPENSE, summary.latestTransaction?.type)
    }

    @Test
    fun testBudgetStatusCalculations() = runBlocking {
        val today = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startOfToday = DateTimeUtils.getStartOfDayEpoch(today)
        val startOfMonth = DateTimeUtils.getStartOfMonthEpoch(currentMonth)

        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(1500000L), // ₹15,000 Expense
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = startOfToday + 1000L,
                paymentMethod = PaymentMethod.ACCOUNT
            )
        )

        val fakeTxRepo = FakeTransactionRepository(transactions)

        // Case A: Over Budget (Limit ₹10,000, Expense ₹15,000)
        val overBudgetPrefs = UserPreferences(monthlyBudgetLimitSubunits = 1000000L)
        val useCaseA = GetWidgetFinancialSummaryUseCase(fakeTxRepo, FakeUserPreferencesRepository(overBudgetPrefs))
        val summaryA = useCaseA.forRanges(
            startOfDayEpoch = startOfToday,
            endOfDayEpoch = DateTimeUtils.getEndOfDayEpoch(today),
            startOfMonthEpoch = startOfMonth,
            endOfMonthEpoch = DateTimeUtils.getEndOfMonthEpoch(currentMonth)
        ).first()

        assertTrue(summaryA.isOverBudget)
        assertEquals(0L, summaryA.remainingBudget?.subunits)

        // Case B: No Budget Set (Limit 0)
        val noBudgetPrefs = UserPreferences(monthlyBudgetLimitSubunits = 0L)
        val useCaseB = GetWidgetFinancialSummaryUseCase(fakeTxRepo, FakeUserPreferencesRepository(noBudgetPrefs))
        val summaryB = useCaseB.forRanges(
            startOfDayEpoch = startOfToday,
            endOfDayEpoch = DateTimeUtils.getEndOfDayEpoch(today),
            startOfMonthEpoch = startOfMonth,
            endOfMonthEpoch = DateTimeUtils.getEndOfMonthEpoch(currentMonth)
        ).first()

        assertNull(summaryB.monthlyBudgetLimit)
        assertNull(summaryB.remainingBudget)
        assertFalse(summaryB.isOverBudget)
    }

    @Test
    fun testPrecisionSafeFinancialCalculationWithLargeValues() = runBlocking {
        val today = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startOfToday = DateTimeUtils.getStartOfDayEpoch(today)
        val startOfMonth = DateTimeUtils.getStartOfMonthEpoch(currentMonth)

        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(99999999999L), // ₹999,999,999.99
                type = TransactionType.INCOME,
                category = salaryCategory,
                timestamp = startOfToday + 1000L,
                paymentMethod = PaymentMethod.ACCOUNT
            ),
            Transaction(
                id = 2L,
                amount = Amount(123456789L), // ₹1,234,567.89
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = startOfToday + 2000L,
                paymentMethod = PaymentMethod.ACCOUNT
            )
        )

        val userPrefs = UserPreferences(openingBalanceSubunits = 0L)
        val fakeTxRepo = FakeTransactionRepository(transactions)
        val fakePrefsRepo = FakeUserPreferencesRepository(userPrefs)

        val useCase = GetWidgetFinancialSummaryUseCase(fakeTxRepo, fakePrefsRepo)
        val summary = useCase.forRanges(
            startOfDayEpoch = startOfToday,
            endOfDayEpoch = DateTimeUtils.getEndOfDayEpoch(today),
            startOfMonthEpoch = startOfMonth,
            endOfMonthEpoch = DateTimeUtils.getEndOfMonthEpoch(currentMonth)
        ).first()

        assertEquals(123456789L, summary.todayExpense.subunits)
        assertEquals(123456789L, summary.monthlyExpense.subunits)
        assertEquals(99999999999L, summary.monthlyIncome.subunits)
    }

    @Test
    fun testShortcutConstantsAndIntentRouting() {
        assertEquals("extra_start_route", NotificationHelper.EXTRA_START_ROUTE)
        assertEquals("add_expense", NotificationHelper.ROUTE_ADD_EXPENSE)
        assertEquals("add_income", NotificationHelper.ROUTE_ADD_INCOME)
        assertEquals("transactions", NotificationHelper.ROUTE_TRANSACTIONS)
        assertEquals("com.vinaynalavade.expensetracker.ACTION_WIDGET_REFRESH", WidgetUpdateManager.ACTION_WIDGET_REFRESH)
    }

    private class FakeTransactionRepository(private val list: List<Transaction>) : TransactionRepository {
        override fun getTransactions(): Flow<List<Transaction>> = flowOf(list)
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(list.take(limit))
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(list.find { it.id == id })
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(list.filter { it.timestamp in startDate..endDate })
        override fun getFinancialSummary(): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override suspend fun insertTransaction(transaction: Transaction): AppResult<Long> = AppResult.Success(1L)
        override suspend fun updateTransaction(transaction: Transaction): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteTransaction(id: Long): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeUserPreferencesRepository(private val prefs: UserPreferences) : UserPreferencesRepository {
        override fun getUserPreferences(): Flow<UserPreferences> = flowOf(prefs)
        override suspend fun setThemeMode(themeMode: ThemeMode): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setCurrencyCode(currencyCode: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setDynamicColors(useDynamicColors: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setFirstLaunchCompleted(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setOpeningBalance(subunits: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setEmiReminders(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override fun getLastBackupTimestamp(): Flow<Long?> = flowOf(null)
        override suspend fun setLastBackupTimestamp(timestamp: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setAppLockEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setBiometricEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setAutoLockDurationSeconds(seconds: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setHideContentInRecents(hide: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setNotificationsMasterEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setBudgetAlertsEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setMonthlyBudgetLimit(subunits: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setRecurringRemindersEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setRecurringReminderAdvanceDays(days: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setSavingsGoalNotificationsEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setAppLanguage(languageCode: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setProfileName(name: String?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setProfileImageUri(uri: String?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setAutomaticBackupEnabled(enabled: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setLastBackupStatus(status: String?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setLastBackupError(error: String?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setLastDismissedRestoreBackupTimestamp(timestamp: Long?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setAppTourCompleted(completed: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setDefaultIncomeSource(source: PaymentMethod): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun setDefaultExpenseSource(source: PaymentMethod): AppResult<Unit> = AppResult.Success(Unit)
    }
}

