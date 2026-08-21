package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.data.local.entity.RecurringTransactionEntity
import com.vinaynalavade.expensetracker.data.local.entity.TransactionEntity
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.RecurrenceFrequency
import com.vinaynalavade.expensetracker.domain.model.RecurringTransaction
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.usecase.AddTransactionUseCase
import com.vinaynalavade.expensetracker.domain.usecase.UpdateTransactionUseCase
import com.vinaynalavade.expensetracker.presentation.entry.AddTransactionUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentMethodUiIntegrationTest {

    private val testCategory = Category(
        id = 1L,
        name = "Food",
        iconName = "restaurant",
        colorHex = "#EF4444",
        type = TransactionType.EXPENSE,
        isDefault = true
    )

    @Test
    fun testAddTransactionUiStateDefaultPaymentMethodIsCash() {
        val state = AddTransactionUiState(
            transactionType = TransactionType.EXPENSE
        )
        assertEquals(PaymentMethod.CASH, state.selectedPaymentMethod)
    }

    @Test
    fun testAddTransactionUiStateUpdatePaymentMethod() {
        var state = AddTransactionUiState(
            transactionType = TransactionType.EXPENSE
        )

        state = state.copy(selectedPaymentMethod = PaymentMethod.BANK_ACCOUNT)
        assertEquals(PaymentMethod.BANK_ACCOUNT, state.selectedPaymentMethod)

        state = state.copy(selectedPaymentMethod = PaymentMethod.UPI)
        assertEquals(PaymentMethod.UPI, state.selectedPaymentMethod)

        state = state.copy(selectedPaymentMethod = PaymentMethod.CASH)
        assertEquals(PaymentMethod.CASH, state.selectedPaymentMethod)
    }

    @Test
    fun testAddTransactionUseCasePersistsPaymentMethod() = runBlocking {
        val fakeRepo = FakeTransactionRepository()
        val addUseCase = AddTransactionUseCase(fakeRepo)

        val tx = Transaction(
            amount = Amount(15000L),
            type = TransactionType.EXPENSE,
            category = testCategory,
            paymentMethod = PaymentMethod.UPI,
            note = "UPI payment test",
            timestamp = 1000L
        )

        val result = addUseCase(tx)
        assertTrue(result is AppResult.Success)

        val saved = fakeRepo.savedTransactions
        assertEquals(1, saved.size)
        assertEquals(PaymentMethod.UPI, saved[0].paymentMethod)
    }

    @Test
    fun testUpdateTransactionUseCasePreservesPaymentMethod() = runBlocking {
        val fakeRepo = FakeTransactionRepository()
        val addUseCase = AddTransactionUseCase(fakeRepo)
        val updateUseCase = UpdateTransactionUseCase(fakeRepo)

        val originalTx = Transaction(
            id = 42L,
            amount = Amount(50000L),
            type = TransactionType.EXPENSE,
            category = testCategory,
            paymentMethod = PaymentMethod.BANK_ACCOUNT,
            note = "Initial Bank Payment",
            timestamp = 2000L
        )
        addUseCase(originalTx)

        val updatedTx = originalTx.copy(
            note = "Updated note",
            paymentMethod = PaymentMethod.BANK_ACCOUNT
        )
        val updateResult = updateUseCase(updatedTx)
        assertTrue(updateResult is AppResult.Success)

        val saved = fakeRepo.savedTransactions.find { it.id == 42L }
        assertEquals(PaymentMethod.BANK_ACCOUNT, saved?.paymentMethod)
        assertEquals("Updated note", saved?.note)
    }

    @Test
    fun testRecurringTransactionPropagationToGeneratedTransaction() {
        val recurring = RecurringTransaction(
            id = 10L,
            title = "Monthly Fiber Internet",
            amount = Amount(99900L),
            type = TransactionType.EXPENSE,
            category = testCategory,
            paymentMethod = PaymentMethod.UPI,
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = 1
        )

        // Convert recurring to entity
        val entity = RecurringTransactionEntity.fromDomainModel(recurring)
        assertEquals("UPI", entity.paymentMethod)

        // Simulate repository generating transaction entity from recurring item
        val generatedTxEntity = TransactionEntity(
            amountSubunits = entity.amountSubunits,
            type = entity.type,
            categoryId = entity.categoryId,
            paymentMethod = entity.paymentMethod,
            note = entity.note ?: entity.title,
            timestamp = 3000L,
            createdAt = 3000L,
            updatedAt = 3000L
        )

        assertEquals("UPI", generatedTxEntity.paymentMethod)
        assertEquals(99900L, generatedTxEntity.amountSubunits)
    }

    private class FakeTransactionRepository : TransactionRepository {
        val savedTransactions = mutableListOf<Transaction>()
        private val flow = MutableStateFlow<List<Transaction>>(emptyList())

        override fun getTransactions(): Flow<List<Transaction>> = flow

        override fun getTransactionById(id: Long): Flow<Transaction?> =
            flow.map { list -> list.find { it.id == id } }

        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flow.map { list -> list.filter { it.timestamp in startDate..endDate } }

        override fun getFinancialSummary(): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)

        override fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary> =
            flowOf(FinancialSummary.EMPTY)

        override suspend fun insertTransaction(transaction: Transaction): AppResult<Long> {
            val id = if (transaction.id == 0L) (savedTransactions.size + 1).toLong() else transaction.id
            val txWithId = transaction.copy(id = id)
            savedTransactions.add(txWithId)
            flow.value = savedTransactions.toList()
            return AppResult.Success(id)
        }

        override suspend fun updateTransaction(transaction: Transaction): AppResult<Unit> {
            val index = savedTransactions.indexOfFirst { it.id == transaction.id }
            if (index != -1) {
                savedTransactions[index] = transaction
            } else {
                savedTransactions.add(transaction)
            }
            flow.value = savedTransactions.toList()
            return AppResult.Success(Unit)
        }

        override suspend fun deleteTransaction(id: Long): AppResult<Unit> {
            savedTransactions.removeAll { it.id == id }
            flow.value = savedTransactions.toList()
            return AppResult.Success(Unit)
        }
    }
}
