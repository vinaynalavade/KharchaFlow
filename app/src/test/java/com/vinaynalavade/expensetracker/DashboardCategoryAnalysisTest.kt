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
import com.vinaynalavade.expensetracker.domain.usecase.GetCategoryAnalysisUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetFinancialSummaryUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionsUseCase
import com.vinaynalavade.expensetracker.presentation.dashboard.DashboardViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DashboardCategoryAnalysisTest {

    @Test
    fun testDashboardViewModelAnalysisModeAndMonthSwitching() = runBlocking {
        val now = LocalDate.now()
        val currentYearMonth = YearMonth.now()
        val todayEpoch = DateTimeUtils.getStartOfDayEpoch(now)

        val foodCat = Category(1L, "Food", "restaurant", "#F59E0B", TransactionType.EXPENSE)
        val salaryCat = Category(2L, "Salary", "payments", "#10B981", TransactionType.INCOME)

        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(30000L),
                type = TransactionType.EXPENSE,
                category = foodCat,
                paymentMethod = PaymentMethod.UPI,
                timestamp = todayEpoch
            ),
            Transaction(
                id = 2L,
                amount = Amount(80000L),
                type = TransactionType.INCOME,
                category = salaryCat,
                paymentMethod = PaymentMethod.BANK_ACCOUNT,
                timestamp = todayEpoch
            )
        )

        val fakeTxRepo = FakeTransactionRepository(transactions)
        val fakePrefsRepo = FakePrefsRepository(UserPreferences(openingBalanceSubunits = 100000L))

        val getFinancialSummaryUseCase = GetFinancialSummaryUseCase(fakeTxRepo, fakePrefsRepo)
        val getTransactionsUseCase = GetTransactionsUseCase(fakeTxRepo)
        val getCategoryAnalysisUseCase = GetCategoryAnalysisUseCase(fakeTxRepo)

        val viewModel = DashboardViewModel(
            getFinancialSummaryUseCase,
            getTransactionsUseCase,
            getCategoryAnalysisUseCase
        )

        // 1. Initial Mode should be EXPENSE for current month
        assertEquals(TransactionType.EXPENSE, viewModel.categoryAnalysisType.value)
        assertEquals(currentYearMonth, viewModel.selectedMonth.value)

        val initialExpenseResult = getCategoryAnalysisUseCase(currentYearMonth, TransactionType.EXPENSE).first()
        assertEquals(TransactionType.EXPENSE, initialExpenseResult.type)
        assertEquals(30000L, initialExpenseResult.totalAmount.subunits)

        // 2. Switch to INCOME
        viewModel.onCategoryAnalysisTypeChange(TransactionType.INCOME)
        assertEquals(TransactionType.INCOME, viewModel.categoryAnalysisType.value)

        val incomeResult = getCategoryAnalysisUseCase(currentYearMonth, TransactionType.INCOME).first()
        assertEquals(TransactionType.INCOME, incomeResult.type)
        assertEquals(80000L, incomeResult.totalAmount.subunits)

        // 3. Navigate Previous Month
        viewModel.onPreviousMonth()
        assertEquals(currentYearMonth.minusMonths(1), viewModel.selectedMonth.value)

        val prevMonthResult = getCategoryAnalysisUseCase(currentYearMonth.minusMonths(1), TransactionType.INCOME).first()
        assertEquals(0L, prevMonthResult.totalAmount.subunits)

        // 4. Return to Current Month
        viewModel.onCurrentMonth()
        assertEquals(currentYearMonth, viewModel.selectedMonth.value)
    }

    private class FakeTransactionRepository(private val allTransactions: List<Transaction>) : TransactionRepository {
        override fun getTransactions(): Flow<List<Transaction>> = flowOf(allTransactions)
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(allTransactions.find { it.id == id })
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(allTransactions.filter { it.timestamp in startDate..endDate })
        override fun getFinancialSummary(): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override suspend fun insertTransaction(transaction: Transaction) = AppResult.Success(1L)
        override suspend fun updateTransaction(transaction: Transaction) = AppResult.Success(Unit)
        override suspend fun deleteTransaction(id: Long) = AppResult.Success(Unit)
    }

    private class FakePrefsRepository(private val prefs: UserPreferences) : UserPreferencesRepository {
        override fun getUserPreferences(): Flow<UserPreferences> = flowOf(prefs)
        override suspend fun setThemeMode(themeMode: ThemeMode) = AppResult.Success(Unit)
        override suspend fun setCurrencyCode(currencyCode: String) = AppResult.Success(Unit)
        override suspend fun setDynamicColors(useDynamicColors: Boolean) = AppResult.Success(Unit)
        override suspend fun setFirstLaunchCompleted() = AppResult.Success(Unit)
        override suspend fun setOpeningBalance(subunits: Long) = AppResult.Success(Unit)
        override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) = AppResult.Success(Unit)
        override suspend fun setEmiReminders(enabled: Boolean) = AppResult.Success(Unit)
        override fun getLastBackupTimestamp(): Flow<Long?> = flowOf(null)
        override suspend fun setLastBackupTimestamp(timestamp: Long) = AppResult.Success(Unit)
    }
}
