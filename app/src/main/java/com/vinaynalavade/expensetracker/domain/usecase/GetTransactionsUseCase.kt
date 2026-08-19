package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UseCase to retrieve transactions reactively.
 */
class GetTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> =
        transactionRepository.getTransactions()

    fun getRecent(limit: Int = 10): Flow<List<Transaction>> =
        transactionRepository.getTransactions().map { it.take(limit) }

    fun getByDateRange(startDateEpoch: Long, endDateEpoch: Long): Flow<List<Transaction>> =
        transactionRepository.getTransactionsBetween(startDateEpoch, endDateEpoch)
}
