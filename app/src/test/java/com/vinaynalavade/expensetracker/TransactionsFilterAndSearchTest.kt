package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.usecase.AddTransactionUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionsUseCase
import com.vinaynalavade.expensetracker.presentation.transactions.DateRangeFilter
import com.vinaynalavade.expensetracker.presentation.transactions.TransactionFilter
import com.vinaynalavade.expensetracker.presentation.transactions.TransactionFilterParams
import com.vinaynalavade.expensetracker.presentation.transactions.TransactionsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionsFilterAndSearchTest {

    private val foodCategory = Category(
        id = 1L,
        name = "Food & Dining",
        iconName = "restaurant",
        colorHex = "#EF4444",
        type = TransactionType.EXPENSE,
        isDefault = true
    )

    private val salaryCategory = Category(
        id = 2L,
        name = "Salary",
        iconName = "account_balance_wallet",
        colorHex = "#10B981",
        type = TransactionType.INCOME,
        isDefault = true
    )

    private val shoppingCategory = Category(
        id = 3L,
        name = "Shopping",
        iconName = "shopping_cart",
        colorHex = "#8B5CF6",
        type = TransactionType.EXPENSE,
        isDefault = true
    )

    private lateinit var sampleTransactions: List<Transaction>

    @Before
    fun setUp() {
        val todayStart = DateTimeUtils.getStartOfDayEpoch()
        val todayNoon = todayStart + 12 * 3600000L // 12:00 PM today
        val yesterday = todayStart - 12 * 3600000L // Yesterday
        val twoMonthsAgo = todayStart - 60 * 24 * 60 * 60 * 1000L

        sampleTransactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(50000), // ₹500
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = todayNoon,
                paymentMethod = PaymentMethod.ACCOUNT,
                note = "Lunch with team"
            ),
            Transaction(
                id = 2L,
                amount = Amount(5000000), // ₹50,000
                type = TransactionType.INCOME,
                category = salaryCategory,
                timestamp = todayNoon - 3600000L, // 11:00 AM today
                paymentMethod = PaymentMethod.ACCOUNT,
                note = "Monthly salary"
            ),
            Transaction(
                id = 3L,
                amount = Amount(150000), // ₹1,500
                type = TransactionType.EXPENSE,
                category = shoppingCategory,
                timestamp = yesterday,
                paymentMethod = PaymentMethod.CASH,
                note = "Groceries supermarket"
            ),
            Transaction(
                id = 4L,
                amount = Amount(80000), // ₹800
                type = TransactionType.EXPENSE,
                category = foodCategory,
                timestamp = twoMonthsAgo,
                paymentMethod = PaymentMethod.ACCOUNT,
                note = "Old restaurant dinner"
            )
        )
    }

    @Test
    fun testGetRecentTransactionsLimitsTo3() = runBlocking {
        val fakeRepo = FakeTransactionRepository(sampleTransactions)
        val getTransactionsUseCase = GetTransactionsUseCase(fakeRepo)

        val recent = getTransactionsUseCase.getRecent(3).first()
        assertEquals(3, recent.size)
        assertEquals(1L, recent[0].id)
        assertEquals(2L, recent[1].id)
        assertEquals(3L, recent[2].id)
    }

    @Test
    fun testViewModelStateMutationsAndReset() {
        val fakeRepo = FakeTransactionRepository(sampleTransactions)
        val getTransactionsUseCase = GetTransactionsUseCase(fakeRepo)
        val addTransactionUseCase = AddTransactionUseCase(fakeRepo)

        val viewModel = TransactionsViewModel(getTransactionsUseCase, addTransactionUseCase)

        assertEquals(TransactionFilter.ALL, viewModel.selectedFilter.value)
        assertEquals(DateRangeFilter.ALL_TIME, viewModel.selectedDateRange.value)
        assertEquals("", viewModel.searchQuery.value)

        viewModel.onFilterSelected(TransactionFilter.EXPENSE)
        assertEquals(TransactionFilter.EXPENSE, viewModel.selectedFilter.value)

        viewModel.onDateRangeSelected(DateRangeFilter.THIS_MONTH)
        assertEquals(DateRangeFilter.THIS_MONTH, viewModel.selectedDateRange.value)

        viewModel.onSearchQueryChanged("Food")
        assertEquals("Food", viewModel.searchQuery.value)

        val start = System.currentTimeMillis() - 10000L
        val end = System.currentTimeMillis()
        viewModel.onCustomDateRangeSet(start, end)
        assertEquals(DateRangeFilter.CUSTOM, viewModel.selectedDateRange.value)
        val expectedStart = DateTimeUtils.getStartOfDayEpoch(DateTimeUtils.epochToLocalDate(start))
        val expectedEnd = DateTimeUtils.getEndOfDayEpoch(DateTimeUtils.epochToLocalDate(end))
        assertEquals(expectedStart, viewModel.customStartDate.value)
        assertEquals(expectedEnd, viewModel.customEndDate.value)

        viewModel.resetFilters()
        assertEquals(TransactionFilter.ALL, viewModel.selectedFilter.value)
        assertEquals(DateRangeFilter.ALL_TIME, viewModel.selectedDateRange.value)
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(null, viewModel.customStartDate.value)
        assertEquals(null, viewModel.customEndDate.value)
    }

    @Test
    fun testDefaultStateShowsAllTransactionsSortedNewestFirst() {
        val params = TransactionFilterParams(
            filter = TransactionFilter.ALL,
            dateRange = DateRangeFilter.ALL_TIME,
            searchQuery = ""
        )

        val state = TransactionsViewModel.filterAndGroupTransactions(sampleTransactions, params)

        assertEquals(4, state.totalTransactionsCount)
        assertEquals(TransactionFilter.ALL, state.selectedFilter)
        assertEquals(DateRangeFilter.ALL_TIME, state.selectedDateRange)
        assertFalse(state.isFilterActive)

        // Summary
        assertEquals(5000000L, state.summary.totalIncome)
        assertEquals(50000L + 150000L + 80000L, state.summary.totalExpense)
        assertEquals(5000000L - (50000L + 150000L + 80000L), state.summary.netBalance)
        assertEquals(4, state.summary.transactionCount)
    }

    @Test
    fun testFilterByExpensesOnly() {
        val params = TransactionFilterParams(
            filter = TransactionFilter.EXPENSE,
            dateRange = DateRangeFilter.ALL_TIME,
            searchQuery = ""
        )

        val state = TransactionsViewModel.filterAndGroupTransactions(sampleTransactions, params)

        assertEquals(TransactionFilter.EXPENSE, state.selectedFilter)
        assertTrue(state.isFilterActive)
        assertEquals(3, state.summary.transactionCount)
        assertEquals(0L, state.summary.totalIncome)
        assertEquals(280000L, state.summary.totalExpense)
    }

    @Test
    fun testFilterByIncomeOnly() {
        val params = TransactionFilterParams(
            filter = TransactionFilter.INCOME,
            dateRange = DateRangeFilter.ALL_TIME,
            searchQuery = ""
        )

        val state = TransactionsViewModel.filterAndGroupTransactions(sampleTransactions, params)

        assertEquals(TransactionFilter.INCOME, state.selectedFilter)
        assertTrue(state.isFilterActive)
        assertEquals(1, state.summary.transactionCount)
        assertEquals(5000000L, state.summary.totalIncome)
        assertEquals(0L, state.summary.totalExpense)
    }

    @Test
    fun testFilterByDateRangeToday() {
        val params = TransactionFilterParams(
            filter = TransactionFilter.ALL,
            dateRange = DateRangeFilter.TODAY,
            searchQuery = ""
        )

        val state = TransactionsViewModel.filterAndGroupTransactions(sampleTransactions, params)

        assertEquals(DateRangeFilter.TODAY, state.selectedDateRange)
        assertTrue(state.isFilterActive)
        // Only tx1 and tx2 are from today
        assertEquals(2, state.summary.transactionCount)
    }

    @Test
    fun testSearchQueryMatchingNote() {
        val params = TransactionFilterParams(
            filter = TransactionFilter.ALL,
            dateRange = DateRangeFilter.ALL_TIME,
            searchQuery = "supermarket"
        )

        val state = TransactionsViewModel.filterAndGroupTransactions(sampleTransactions, params)

        assertTrue(state.isFilterActive)
        assertEquals(1, state.summary.transactionCount)
        assertEquals(150000L, state.summary.totalExpense)
    }

    @Test
    fun testSearchQueryMatchingCategory() {
        val params = TransactionFilterParams(
            filter = TransactionFilter.ALL,
            dateRange = DateRangeFilter.ALL_TIME,
            searchQuery = "Food"
        )

        val state = TransactionsViewModel.filterAndGroupTransactions(sampleTransactions, params)

        assertEquals(2, state.summary.transactionCount)
    }

    @Test
    fun testMultiCriteriaFilterCombination() {
        val params = TransactionFilterParams(
            filter = TransactionFilter.EXPENSE,
            dateRange = DateRangeFilter.THIS_MONTH,
            searchQuery = "Lunch"
        )

        val state = TransactionsViewModel.filterAndGroupTransactions(sampleTransactions, params)

        assertEquals(1, state.summary.transactionCount)
        assertEquals(50000L, state.summary.totalExpense)
        assertEquals(1L, state.groups.first().dailyGroups.first().transactions.first().id)
    }

    @Test
    fun testCustomDateRangeFiltering() {
        val todayStart = DateTimeUtils.getStartOfDayEpoch()
        val start = todayStart - 24 * 3600000L // Start of yesterday
        val end = todayStart - 1L              // End of yesterday

        val params = TransactionFilterParams(
            filter = TransactionFilter.ALL,
            dateRange = DateRangeFilter.CUSTOM,
            customStartDate = start,
            customEndDate = end,
            searchQuery = ""
        )

        val state = TransactionsViewModel.filterAndGroupTransactions(sampleTransactions, params)

        assertEquals(DateRangeFilter.CUSTOM, state.selectedDateRange)
        assertTrue(state.isFilterActive)
        // Tx3 (yesterday) falls in this window
        assertEquals(1, state.summary.transactionCount)
        assertEquals(3L, state.groups.first().dailyGroups.first().transactions.first().id)
    }

    private class FakeTransactionRepository(
        private val list: List<Transaction>
    ) : TransactionRepository {
        override fun getTransactions(): Flow<List<Transaction>> = flowOf(list)
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(list.take(limit))
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(list.find { it.id == id })
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(list.filter { it.timestamp in startDate..endDate })
        override fun getFinancialSummary(): Flow<FinancialSummary> = flowOf(
            FinancialSummary(
                totalIncome = Amount(list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.subunits }),
                totalExpense = Amount(list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.subunits }),
                netChange = Amount(0),
                currentBalance = Amount(0),
                transactionCount = list.size
            )
        )
        override fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary> = getFinancialSummary()
        override suspend fun insertTransaction(transaction: Transaction): AppResult<Long> = AppResult.Success(1L)
        override suspend fun updateTransaction(transaction: Transaction): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteTransaction(id: Long): AppResult<Unit> = AppResult.Success(Unit)
    }
}
