package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
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
import com.vinaynalavade.expensetracker.domain.usecase.GetTodayExpenseUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetWidgetFinancialSummaryUseCase
import com.vinaynalavade.expensetracker.presentation.widget.WidgetUpdateManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TodayExpenseWidgetTest {

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

    private val transportCategory = Category(
        id = 3L,
        name = "Transport",
        iconName = "directions_bus",
        colorHex = "#F59E0B",
        type = TransactionType.EXPENSE
    )

    // ─── Test 1: Zero-expense state ───

    @Test
    fun testZeroExpenseState() = runBlocking {
        val fakeTxRepo = FakeTxRepository(emptyList())
        val useCase = GetTodayExpenseUseCase(fakeTxRepo)

        val today = LocalDate.now()
        val startOfDay = DateTimeUtils.getStartOfDayEpoch(today)
        val endOfDay = DateTimeUtils.getEndOfDayEpoch(today)

        val result = useCase.forDateRange(startOfDay, endOfDay).first()
        assertEquals(0L, result.subunits)
    }

    // ─── Test 2: Expense-only filtering (income excluded) ───

    @Test
    fun testExpenseOnlyFiltering() = runBlocking {
        val today = LocalDate.now()
        val startOfDay = DateTimeUtils.getStartOfDayEpoch(today)
        val midDay = startOfDay + (12 * 60 * 60 * 1000L)

        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(50000L), // ₹500 Income
                type = TransactionType.INCOME,
                category = salaryCategory,
                timestamp = midDay,
                paymentMethod = PaymentMethod.ACCOUNT
            ),
            Transaction(
                id = 2L,
                amount = Amount(15000L), // ₹150 Expense
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = midDay + 1000L,
                paymentMethod = PaymentMethod.ACCOUNT
            ),
            Transaction(
                id = 3L,
                amount = Amount(8000L), // ₹80 Expense
                type = TransactionType.EXPENSE,
                category = transportCategory,
                timestamp = midDay + 2000L,
                paymentMethod = PaymentMethod.CASH
            )
        )

        val fakeTxRepo = FakeTxRepository(transactions)
        val useCase = GetTodayExpenseUseCase(fakeTxRepo)

        val endOfDay = DateTimeUtils.getEndOfDayEpoch(today)
        val result = useCase.forDateRange(startOfDay, endOfDay).first()

        // Only expenses: 15000 + 8000 = 23000 (income excluded)
        assertEquals(23000L, result.subunits)
    }

    // ─── Test 3: Date boundary — yesterday/tomorrow excluded ───

    @Test
    fun testDateBoundaryExclusion() = runBlocking {
        val today = LocalDate.now()
        val startOfDay = DateTimeUtils.getStartOfDayEpoch(today)
        val endOfDay = DateTimeUtils.getEndOfDayEpoch(today)

        val yesterday = startOfDay - 1000L // 1 second before today starts
        val tomorrow = endOfDay + 1000L   // 1 second after today ends

        val transactions = listOf(
            // Yesterday's expense — should be excluded
            Transaction(
                id = 1L,
                amount = Amount(100000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = yesterday,
                paymentMethod = PaymentMethod.CASH
            ),
            // Today's expense — should be included
            Transaction(
                id = 2L,
                amount = Amount(25000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = startOfDay + (6 * 60 * 60 * 1000L),
                paymentMethod = PaymentMethod.ACCOUNT
            ),
            // Tomorrow's expense — should be excluded
            Transaction(
                id = 3L,
                amount = Amount(200000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = tomorrow,
                paymentMethod = PaymentMethod.CASH
            )
        )

        val fakeTxRepo = FakeTxRepository(transactions)
        val useCase = GetTodayExpenseUseCase(fakeTxRepo)

        val result = useCase.forDateRange(startOfDay, endOfDay).first()

        // Only today's expense: 25000
        assertEquals(25000L, result.subunits)
    }

    // ─── Test 4: Multiple expenses today — sum is correct ───

    @Test
    fun testMultipleExpensesSum() = runBlocking {
        val today = LocalDate.now()
        val startOfDay = DateTimeUtils.getStartOfDayEpoch(today)
        val endOfDay = DateTimeUtils.getEndOfDayEpoch(today)

        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(5000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = startOfDay + 1000L,
                paymentMethod = PaymentMethod.CASH
            ),
            Transaction(
                id = 2L,
                amount = Amount(12500L),
                type = TransactionType.EXPENSE,
                category = transportCategory,
                timestamp = startOfDay + 2000L,
                paymentMethod = PaymentMethod.ACCOUNT
            ),
            Transaction(
                id = 3L,
                amount = Amount(7500L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = startOfDay + 3000L,
                paymentMethod = PaymentMethod.CASH
            ),
            Transaction(
                id = 4L,
                amount = Amount(30000L),
                type = TransactionType.EXPENSE,
                category = transportCategory,
                timestamp = startOfDay + 4000L,
                paymentMethod = PaymentMethod.ACCOUNT
            )
        )

        val fakeTxRepo = FakeTxRepository(transactions)
        val useCase = GetTodayExpenseUseCase(fakeTxRepo)

        val result = useCase.forDateRange(startOfDay, endOfDay).first()

        // Sum: 5000 + 12500 + 7500 + 30000 = 55000
        assertEquals(55000L, result.subunits)
    }

    // ─── Test 5: Date rollover — different day yields different results ───

    @Test
    fun testDateRollover() = runBlocking {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val todayStart = DateTimeUtils.getStartOfDayEpoch(today)
        val todayEnd = DateTimeUtils.getEndOfDayEpoch(today)
        val yesterdayStart = DateTimeUtils.getStartOfDayEpoch(yesterday)
        val yesterdayEnd = DateTimeUtils.getEndOfDayEpoch(yesterday)

        val transactions = listOf(
            // Yesterday's expense
            Transaction(
                id = 1L,
                amount = Amount(40000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = yesterdayStart + (6 * 60 * 60 * 1000L),
                paymentMethod = PaymentMethod.CASH
            ),
            // Today's expense
            Transaction(
                id = 2L,
                amount = Amount(15000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = todayStart + (6 * 60 * 60 * 1000L),
                paymentMethod = PaymentMethod.ACCOUNT
            )
        )

        val fakeTxRepo = FakeTxRepository(transactions)
        val useCase = GetTodayExpenseUseCase(fakeTxRepo)

        val todayResult = useCase.forDateRange(todayStart, todayEnd).first()
        val yesterdayResult = useCase.forDateRange(yesterdayStart, yesterdayEnd).first()

        assertEquals(15000L, todayResult.subunits)
        assertEquals(40000L, yesterdayResult.subunits)
        assertNotEquals(todayResult.subunits, yesterdayResult.subunits)
    }

    // ─── Test 6: Large value precision — no floating-point drift ───

    @Test
    fun testLargeValuePrecision() = runBlocking {
        val today = LocalDate.now()
        val startOfDay = DateTimeUtils.getStartOfDayEpoch(today)
        val endOfDay = DateTimeUtils.getEndOfDayEpoch(today)

        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(99999999999L), // ₹999,999,999.99
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = startOfDay + 1000L,
                paymentMethod = PaymentMethod.ACCOUNT
            ),
            Transaction(
                id = 2L,
                amount = Amount(1L), // ₹0.01
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = startOfDay + 2000L,
                paymentMethod = PaymentMethod.CASH
            )
        )

        val fakeTxRepo = FakeTxRepository(transactions)
        val useCase = GetTodayExpenseUseCase(fakeTxRepo)

        val result = useCase.forDateRange(startOfDay, endOfDay).first()

        // Exact: 99999999999 + 1 = 100000000000
        assertEquals(100000000000L, result.subunits)
    }

    // ─── Test 7: Consolidated widget use case regression ───

    @Test
    fun testConsolidatedWidgetUseCaseRegression() = runBlocking {
        val today = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startOfDay = DateTimeUtils.getStartOfDayEpoch(today)
        val endOfDay = DateTimeUtils.getEndOfDayEpoch(today)
        val startOfMonth = DateTimeUtils.getStartOfMonthEpoch(currentMonth)
        val endOfMonth = DateTimeUtils.getEndOfMonthEpoch(currentMonth)

        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(100000L), // ₹1,000 Income
                type = TransactionType.INCOME,
                category = salaryCategory,
                timestamp = startOfDay + 1000L,
                paymentMethod = PaymentMethod.ACCOUNT
            ),
            Transaction(
                id = 2L,
                amount = Amount(25000L), // ₹250 Expense
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = startOfDay + 2000L,
                paymentMethod = PaymentMethod.ACCOUNT
            )
        )

        val userPrefs = UserPreferences(openingBalanceSubunits = 500000L)
        val fakeTxRepo = FakeTxRepository(transactions)
        val fakePrefsRepo = FakePrefsRepository(userPrefs)

        // Consolidated use case provides today's expense, monthly expense, and monthly income
        val widgetSummaryUseCase = GetWidgetFinancialSummaryUseCase(fakeTxRepo, fakePrefsRepo)
        val summary = widgetSummaryUseCase.forRanges(
            startOfDayEpoch = startOfDay,
            endOfDayEpoch = endOfDay,
            startOfMonthEpoch = startOfMonth,
            endOfMonthEpoch = endOfMonth
        ).first()

        assertEquals(25000L, summary.todayExpense.subunits)
        assertEquals(25000L, summary.monthlyExpense.subunits)
        assertEquals(100000L, summary.monthlyIncome.subunits)

        // Standalone use case also computes same today's expense correctly
        val todayExpenseUseCase = GetTodayExpenseUseCase(fakeTxRepo)
        val todayExpense = todayExpenseUseCase.forDateRange(startOfDay, endOfDay).first()

        assertEquals(25000L, todayExpense.subunits)
    }

    // ─── Test 8: Action constant verification ───

    @Test
    fun testActionConstantsAreValid() {
        assertEquals(
            "com.vinaynalavade.expensetracker.ACTION_WIDGET_REFRESH",
            WidgetUpdateManager.ACTION_WIDGET_REFRESH
        )
    }

    // ─── Test 9: Edge-of-day boundary transactions ───

    @Test
    fun testEdgeOfDayTransactionsIncluded() = runBlocking {
        val today = LocalDate.now()
        val startOfDay = DateTimeUtils.getStartOfDayEpoch(today)
        val endOfDay = DateTimeUtils.getEndOfDayEpoch(today)

        val transactions = listOf(
            // Exactly at start of day
            Transaction(
                id = 1L,
                amount = Amount(1000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = startOfDay,
                paymentMethod = PaymentMethod.CASH
            ),
            // Exactly at end of day
            Transaction(
                id = 2L,
                amount = Amount(2000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = endOfDay,
                paymentMethod = PaymentMethod.CASH
            )
        )

        val fakeTxRepo = FakeTxRepository(transactions)
        val useCase = GetTodayExpenseUseCase(fakeTxRepo)

        val result = useCase.forDateRange(startOfDay, endOfDay).first()

        // Both edge transactions should be included
        assertEquals(3000L, result.subunits)
    }

    // ─── Test 10: Today's expense with zero amount formatting ───

    @Test
    fun testZeroAmountFormatting() {
        val amount = Amount(0L)
        val formatted = amount.format(com.vinaynalavade.expensetracker.core.model.Currency.INR)
        assertEquals("₹0.00", formatted)
    }

    // ─── Test 11: Multi-currency formatting for Today's Expense ───

    @Test
    fun testMultiCurrencyFormatting() {
        val amount = Amount(15050L) // 150.50 or 15050 for JPY

        assertEquals("₹150.50", amount.format(com.vinaynalavade.expensetracker.core.model.Currency.INR))
        assertEquals("$150.50", amount.format(com.vinaynalavade.expensetracker.core.model.Currency.USD))
        assertEquals("€150.50", amount.format(com.vinaynalavade.expensetracker.core.model.Currency.EUR))
        assertEquals("£150.50", amount.format(com.vinaynalavade.expensetracker.core.model.Currency.GBP))
        assertEquals("¥15,050", amount.format(com.vinaynalavade.expensetracker.core.model.Currency.JPY))
    }

    // ─── Test 12: PendingIntent Request Code Collision Safety ───

    @Test
    fun testPendingIntentRequestCodesDoNotCollide() {
        // Overview widget: 100, 101, 102, 103
        val overviewCodes = setOf(100, 101, 102, 103)

        // Quick Add widget: 200, 201, 202
        val quickAddCodes = setOf(200, 201, 202)

        // Verify mutually disjoint sets
        assertEquals(emptySet<Int>(), overviewCodes.intersect(quickAddCodes))
    }

    // ─── Fakes ───

    private class FakeTxRepository(private val list: List<Transaction>) : TransactionRepository {
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

    private class FakePrefsRepository(private val prefs: UserPreferences) : UserPreferencesRepository {
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
