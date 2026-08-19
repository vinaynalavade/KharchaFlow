package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionByIdUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(id: Long): Flow<Transaction?> {
        return transactionRepository.getTransactionById(id)
    }
}
