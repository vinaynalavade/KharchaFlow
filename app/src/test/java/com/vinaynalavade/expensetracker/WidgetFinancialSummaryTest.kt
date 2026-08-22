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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        val userPrefs = UserPreferences(openingBalanceSubunits = 100000L) // ₹1,000 Opening Balance
        val fakeTxRepo = FakeTransactionRepository(emptyList())
        val fakePrefsRepo = FakeUserPreferencesRepository(userPrefs)

        val useCase = GetWidgetFinancialSummaryUseCase(fakeTxRepo, fakePrefsRepo)
        val summary = useCase().first()

        assertEquals(100000L, summary.balance.subunits)
        assertEquals(0L, summary.monthlyIncome.subunits)
        assertEquals(0L, summary.monthlyExpense.subunits)
        assertNull(summary.latestTransaction)
    }

    @Test
    fun testCurrentMonthAndAllTimeCalculations() = runBlocking {
        val currentMonth = YearMonth.now()
        val currentMonthMidEpoch = DateTimeUtils.getStartOfMonthEpoch(currentMonth) + (10 * 24 * 60 * 60 * 1000L)
        val previousMonthEpoch = DateTimeUtils.getStartOfMonthEpoch(currentMonth) - (15 * 24 * 60 * 60 * 1000L)

        val transactions = listOf(
            // Previous month income: ₹50,000
            Transaction(
                id = 1L,
                amount = Amount(5000000L),
                type = TransactionType.INCOME,
                category = salaryCategory,
                timestamp = previousMonthEpoch,
                paymentMethod = PaymentMethod.BANK_ACCOUNT
            ),
            // Previous month expense: ₹10,000
            Transaction(
                id = 2L,
                amount = Amount(1000000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = previousMonthEpoch + 1000L,
                paymentMethod = PaymentMethod.UPI
            ),
            // Current month income: ₹35,000
            Transaction(
                id = 3L,
                amount = Amount(3500000L),
                type = TransactionType.INCOME,
                category = salaryCategory,
                timestamp = currentMonthMidEpoch,
                paymentMethod = PaymentMethod.BANK_ACCOUNT
            ),
            // Current month expense: ₹12,500
            Transaction(
                id = 4L,
                amount = Amount(1250000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = currentMonthMidEpoch + 5000L,
                paymentMethod = PaymentMethod.UPI
            )
        )

        // Starting balance: ₹5,000
        val userPrefs = UserPreferences(openingBalanceSubunits = 500000L)
        val fakeTxRepo = FakeTransactionRepository(transactions)
        val fakePrefsRepo = FakeUserPreferencesRepository(userPrefs)

        val useCase = GetWidgetFinancialSummaryUseCase(fakeTxRepo, fakePrefsRepo)
        val summary = useCase().first()

        // 1. Current balance = 5,000 + (50,000 - 10,000) + (35,000 - 12,500) = 5,000 + 40,000 + 22,500 = ₹67,500 (6,750,000 subunits)
        assertEquals(6750000L, summary.balance.subunits)

        // 2. Current month income only = ₹35,000 (3,500,000 subunits)
        assertEquals(3500000L, summary.monthlyIncome.subunits)

        // 3. Current month expense only = ₹12,500 (1,250,000 subunits)
        assertEquals(1250000L, summary.monthlyExpense.subunits)

        // 4. Latest transaction = Transaction 4
        assertNotNull(summary.latestTransaction)
        assertEquals(4L, summary.latestTransaction?.id)
        assertEquals(1250000L, summary.latestTransaction?.amount?.subunits)
        assertEquals(TransactionType.EXPENSE, summary.latestTransaction?.type)
        assertEquals("Food", summary.latestTransaction?.categoryName)
    }

    @Test
    fun testPrecisionSafeFinancialCalculationWithLargeValues() = runBlocking {
        val currentMonth = YearMonth.now()
        val now = DateTimeUtils.getStartOfMonthEpoch(currentMonth) + 10000L

        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(99999999999L), // ₹999,999,999.99
                type = TransactionType.INCOME,
                category = salaryCategory,
                timestamp = now,
                paymentMethod = PaymentMethod.BANK_ACCOUNT
            ),
            Transaction(
                id = 2L,
                amount = Amount(123456789L), // ₹1,234,567.89
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = now + 1000L,
                paymentMethod = PaymentMethod.UPI
            )
        )

        val userPrefs = UserPreferences(openingBalanceSubunits = 0L)
        val fakeTxRepo = FakeTransactionRepository(transactions)
        val fakePrefsRepo = FakeUserPreferencesRepository(userPrefs)

        val useCase = GetWidgetFinancialSummaryUseCase(fakeTxRepo, fakePrefsRepo)
        val summary = useCase().first()

        assertEquals(99999999999L - 123456789L, summary.balance.subunits)
        assertEquals(99999999999L, summary.monthlyIncome.subunits)
        assertEquals(123456789L, summary.monthlyExpense.subunits)
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
    }
}
