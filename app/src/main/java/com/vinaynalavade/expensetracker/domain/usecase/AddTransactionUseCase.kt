package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository

/**
 * UseCase to validate and record a financial transaction.
 */
class AddTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): AppResult<Long> {
        if (transaction.amount.isZero || transaction.amount.isNegative) {
            return AppResult.Error(
                AppError.ValidationError("Transaction amount must be greater than zero.", "amount")
            )
        }
        return transactionRepository.insertTransaction(transaction)
    }
}
