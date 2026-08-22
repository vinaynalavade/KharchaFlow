package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.QuickAddTransactionActivity
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.notification.NotificationHelper
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.domain.usecase.GetFinancialSummaryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetIntentRoutingTest {

    @Test
    fun testNotificationHelperConstants() {
        assertEquals("extra_start_route", NotificationHelper.EXTRA_START_ROUTE)
        assertEquals("add_expense", NotificationHelper.ROUTE_ADD_EXPENSE)
        assertEquals("add_income", NotificationHelper.ROUTE_ADD_INCOME)
        assertEquals("transactions", NotificationHelper.ROUTE_TRANSACTIONS)
    }

    @Test
    fun testQuickAddTransactionTypeResolution() {
        assertEquals("extra_transaction_type", QuickAddTransactionActivity.EXTRA_TRANSACTION_TYPE)

        // Simulate intent parsing logic used in QuickAddTransactionActivity
        fun resolveType(rawType: String?, startRoute: String?): TransactionType {
            return when {
                rawType != null -> try {
                    TransactionType.valueOf(rawType.uppercase())
                } catch (_: Exception) {
                    TransactionType.EXPENSE
                }
                startRoute == NotificationHelper.ROUTE_ADD_INCOME -> TransactionType.INCOME
                else -> TransactionType.EXPENSE
            }
        }

        assertEquals(TransactionType.EXPENSE, resolveType("EXPENSE", null))
        assertEquals(TransactionType.INCOME, resolveType("INCOME", null))
        assertEquals(TransactionType.EXPENSE, resolveType("expense", null))
        assertEquals(TransactionType.INCOME, resolveType("income", null))
        assertEquals(TransactionType.INCOME, resolveType(null, NotificationHelper.ROUTE_ADD_INCOME))
        assertEquals(TransactionType.EXPENSE, resolveType(null, NotificationHelper.ROUTE_ADD_EXPENSE))
        assertEquals(TransactionType.EXPENSE, resolveType(null, null))
        assertEquals(TransactionType.EXPENSE, resolveType("INVALID_TYPE", null))
    }

    @Test
    fun testWidgetFinancialSummaryMatchesAppDomainCalculation() = runBlocking {
        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(100000L), // ₹1,000 Income
                type = TransactionType.INCOME,
                category = Category.UNCATEGORIZED,
                paymentMethod = PaymentMethod.BANK_ACCOUNT,
                timestamp = 10000L
            ),
            Transaction(
                id = 2L,
                amount = Amount(25000L), // ₹250 Expense
                type = TransactionType.EXPENSE,
                category = Category.UNCATEGORIZED,
                paymentMethod = PaymentMethod.UPI,
                timestamp = 15000L
            )
        )

        val userPrefs = UserPreferences(openingBalanceSubunits = 500000L) // ₹5,000 Opening Balance

        val fakeTxRepo = object : FakeTxRepository(transactions) {}
        val fakePrefsRepo = object : FakePrefsRepository(userPrefs) {}

        val getFinancialSummaryUseCase = GetFinancialSummaryUseCase(fakeTxRepo, fakePrefsRepo)

        val summary = getFinancialSummaryUseCase().first()

        // Opening = 5000, Income = 1000, Expense = 250 -> Current Balance = 5750 (575000 subunits)
        assertEquals(575000L, summary.currentBalance.subunits)

        // Today range check: between 10000L and 20000L
        val todaySummary = getFinancialSummaryUseCase.getByDateRange(10000L, 20000L).first()
        assertEquals(25000L, todaySummary.totalExpense.subunits)
    }

    private open class FakeTxRepository(private val list: List<Transaction>) : TransactionRepository {
        override fun getTransactions(): Flow<List<Transaction>> = flowOf(list)
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(list.find { it.id == id })
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(list.filter { it.timestamp in startDate..endDate })
        override fun getFinancialSummary(): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override suspend fun insertTransaction(transaction: Transaction) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(1L)
        override suspend fun updateTransaction(transaction: Transaction) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun deleteTransaction(id: Long) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
    }

    private open class FakePrefsRepository(private val prefs: UserPreferences) : UserPreferencesRepository {
        override fun getUserPreferences(): Flow<UserPreferences> = flowOf(prefs)
        override suspend fun setThemeMode(themeMode: com.vinaynalavade.expensetracker.domain.model.ThemeMode) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setCurrencyCode(currencyCode: String) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setDynamicColors(useDynamicColors: Boolean) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setFirstLaunchCompleted() = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setOpeningBalance(subunits: Long) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setEmiReminders(enabled: Boolean) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override fun getLastBackupTimestamp(): Flow<Long?> = flowOf(null)
        override suspend fun setLastBackupTimestamp(timestamp: Long) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setAppLockEnabled(enabled: Boolean) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setBiometricEnabled(enabled: Boolean) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setAutoLockDurationSeconds(seconds: Long) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setHideContentInRecents(hide: Boolean) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setNotificationsMasterEnabled(enabled: Boolean) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setBudgetAlertsEnabled(enabled: Boolean) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setMonthlyBudgetLimit(subunits: Long) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setRecurringRemindersEnabled(enabled: Boolean) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setRecurringReminderAdvanceDays(days: Int) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
        override suspend fun setSavingsGoalNotificationsEnabled(enabled: Boolean) = com.vinaynalavade.expensetracker.core.result.AppResult.Success(Unit)
    }
}
