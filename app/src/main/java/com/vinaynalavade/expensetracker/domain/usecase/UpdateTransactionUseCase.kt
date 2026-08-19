package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository

class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): AppResult<Unit> {
        if (transaction.amount.isZero || transaction.amount.isNegative) {
            return AppResult.Error(
                AppError.ValidationError("Amount must be greater than zero.", "amount")
            )
        }
        return transactionRepository.updateTransaction(transaction)
    }
}
