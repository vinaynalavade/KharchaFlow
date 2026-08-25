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
import com.vinaynalavade.expensetracker.domain.usecase.GetCategoryAnalysisUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GetCategoryAnalysisUseCaseTest {

    private val foodCategory = Category(id = 1L, name = "Food", iconName = "restaurant", colorHex = "#F59E0B", type = TransactionType.EXPENSE)
    private val transportCategory = Category(id = 2L, name = "Transport", iconName = "directions_car", colorHex = "#3B82F6", type = TransactionType.EXPENSE)
    private val shoppingCategory = Category(id = 3L, name = "Shopping", iconName = "shopping_bag", colorHex = "#EC4899", type = TransactionType.EXPENSE)
    private val salaryCategory = Category(id = 4L, name = "Salary", iconName = "payments", colorHex = "#10B981", type = TransactionType.INCOME)
    private val freelanceCategory = Category(id = 5L, name = "Freelance", iconName = "work", colorHex = "#6366F1", type = TransactionType.INCOME)

    @Test
    fun testExpenseCategoryGroupingAndPercentageCalculation() = runBlocking {
        val aug2026 = YearMonth.of(2026, 8)
        val epochMidAug = DateTimeUtils.getStartOfDayEpoch(LocalDate.of(2026, 8, 15))

        val transactions = listOf(
            // Food: ₹4,000 (400000 subunits)
            Transaction(
                id = 1L,
                amount = Amount(400000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                paymentMethod = PaymentMethod.ACCOUNT,
                timestamp = epochMidAug
            ),
            // Transport: ₹3,000 (300000 subunits)
            Transaction(
                id = 2L,
                amount = Amount(300000L),
                type = TransactionType.EXPENSE,
                category = transportCategory,
                paymentMethod = PaymentMethod.CASH,
                timestamp = epochMidAug
            ),
            // Shopping: ₹3,000 (300000 subunits)
            Transaction(
                id = 3L,
                amount = Amount(300000L),
                type = TransactionType.EXPENSE,
                category = shoppingCategory,
                paymentMethod = PaymentMethod.ACCOUNT,
                timestamp = epochMidAug
            ),
            // Income (Salary: ₹50,000) - should NOT be in Expense analysis!
            Transaction(
                id = 4L,
                amount = Amount(5000000L),
                type = TransactionType.INCOME,
                category = salaryCategory,
                paymentMethod = PaymentMethod.ACCOUNT,
                timestamp = epochMidAug
            )
        )

        val fakeRepo = FakeTransactionRepository(transactions)
        val useCase = GetCategoryAnalysisUseCase(fakeRepo)

        val result = useCase(aug2026, TransactionType.EXPENSE).first()

        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(1000000L, result.totalAmount.subunits) // ₹10,000 total expense
        assertEquals(3, result.categories.size)

        // 1. Food: ₹4,000 (40%)
        val food = result.categories[0]
        assertEquals("Food", food.categoryName)
        assertEquals(400000L, food.amount.subunits)
        assertEquals(40.0f, food.percentage, 0.01f)
        assertEquals(1, food.transactionCount)

        // 2. Transport: ₹3,000 (30%)
        val transport = result.categories[1]
        assertEquals(300000L, transport.amount.subunits)
        assertEquals(30.0f, transport.percentage, 0.01f)

        // 3. Shopping: ₹3,000 (30%)
        val shopping = result.categories[2]
        assertEquals(300000L, shopping.amount.subunits)
        assertEquals(30.0f, shopping.percentage, 0.01f)
    }

    @Test
    fun testIncomeCategoryAnalysis() = runBlocking {
        val aug2026 = YearMonth.of(2026, 8)
        val epochMidAug = DateTimeUtils.getStartOfDayEpoch(LocalDate.of(2026, 8, 10))

        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(8000000L), // ₹80,000 Salary
                type = TransactionType.INCOME,
                category = salaryCategory,
                paymentMethod = PaymentMethod.ACCOUNT,
                timestamp = epochMidAug
            ),
            Transaction(
                id = 2L,
                amount = Amount(2000000L), // ₹20,000 Freelance
                type = TransactionType.INCOME,
                category = freelanceCategory,
                paymentMethod = PaymentMethod.ACCOUNT,
                timestamp = epochMidAug
            ),
            Transaction(
                id = 3L,
                amount = Amount(500000L), // ₹5,000 Expense (ignored in income analysis)
                type = TransactionType.EXPENSE,
                category = foodCategory,
                paymentMethod = PaymentMethod.CASH,
                timestamp = epochMidAug
            )
        )

        val fakeRepo = FakeTransactionRepository(transactions)
        val useCase = GetCategoryAnalysisUseCase(fakeRepo)

        val result = useCase(aug2026, TransactionType.INCOME).first()

        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(10000000L, result.totalAmount.subunits) // ₹100,000 total income
        assertEquals(2, result.categories.size)

        assertEquals("Salary", result.categories[0].categoryName)
        assertEquals(80.0f, result.categories[0].percentage, 0.01f)

        assertEquals("Freelance", result.categories[1].categoryName)
        assertEquals(20.0f, result.categories[1].percentage, 0.01f)
    }

    @Test
    fun testEmptyTransactionsHandlingAndZeroDivisionSafety() = runBlocking {
        val aug2026 = YearMonth.of(2026, 8)
        val fakeRepo = FakeTransactionRepository(emptyList())
        val useCase = GetCategoryAnalysisUseCase(fakeRepo)

        val result = useCase(aug2026, TransactionType.EXPENSE).first()

        assertTrue(result.isEmpty)
        assertEquals(0L, result.totalAmount.subunits)
        assertTrue(result.categories.isEmpty())
    }

    @Test
    fun testDateRangeBoundaryFiltering() = runBlocking {
        val aug2026 = YearMonth.of(2026, 8)
        val july31 = DateTimeUtils.getEndOfDayEpoch(LocalDate.of(2026, 7, 31))
        val aug15 = DateTimeUtils.getStartOfDayEpoch(LocalDate.of(2026, 8, 15))
        val sep01 = DateTimeUtils.getStartOfDayEpoch(LocalDate.of(2026, 9, 1))

        val transactions = listOf(
            Transaction(id = 1L, amount = Amount(50000L), type = TransactionType.EXPENSE, category = foodCategory, paymentMethod = PaymentMethod.CASH, timestamp = july31),
            Transaction(id = 2L, amount = Amount(70000L), type = TransactionType.EXPENSE, category = foodCategory, paymentMethod = PaymentMethod.CASH, timestamp = aug15),
            Transaction(id = 3L, amount = Amount(90000L), type = TransactionType.EXPENSE, category = foodCategory, paymentMethod = PaymentMethod.CASH, timestamp = sep01)
        )

        val fakeRepo = FakeTransactionRepository(transactions)
        val useCase = GetCategoryAnalysisUseCase(fakeRepo)

        val result = useCase(aug2026, TransactionType.EXPENSE).first()

        // Only August 15 should be included
        assertEquals(70000L, result.totalAmount.subunits)
        assertEquals(1, result.categories.size)
        assertEquals(70000L, result.categories[0].amount.subunits)
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
}
