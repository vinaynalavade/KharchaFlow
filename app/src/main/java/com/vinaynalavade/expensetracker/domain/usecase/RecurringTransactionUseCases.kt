package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.RecurringTransaction
import com.vinaynalavade.expensetracker.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow

class GetRecurringTransactionsUseCase(
    private val recurringTransactionRepository: RecurringTransactionRepository
) {
    operator fun invoke(): Flow<List<RecurringTransaction>> {
        return recurringTransactionRepository.getRecurringTransactions()
    }
}

class SaveRecurringTransactionUseCase(
    private val recurringTransactionRepository: RecurringTransactionRepository
) {
    suspend operator fun invoke(item: RecurringTransaction): AppResult<Long> {
        if (item.title.isBlank()) {
            return AppResult.Error(AppError.ValidationError("Title cannot be empty.", "title"))
        }
        if (item.amount.isZero || item.amount.isNegative) {
            return AppResult.Error(AppError.ValidationError("Amount must be greater than zero.", "amount"))
        }
        if (item.dayOfMonth !in 1..31) {
            return AppResult.Error(AppError.ValidationError("Day of month must be between 1 and 31.", "dayOfMonth"))
        }

        return if (item.id == 0L) {
            recurringTransactionRepository.insertRecurringTransaction(item)
        } else {
            when (val result = recurringTransactionRepository.updateRecurringTransaction(item)) {
                is AppResult.Success -> AppResult.Success(item.id)
                is AppResult.Error -> AppResult.Error(result.error)
            }
        }
    }
}

class DeleteRecurringTransactionUseCase(
    private val recurringTransactionRepository: RecurringTransactionRepository
) {
    suspend operator fun invoke(id: Long): AppResult<Unit> {
        return recurringTransactionRepository.deleteRecurringTransaction(id)
    }
}

class ProcessDueRecurringTransactionsUseCase(
    private val recurringTransactionRepository: RecurringTransactionRepository
) {
    suspend operator fun invoke(): AppResult<Int> {
        return recurringTransactionRepository.processDueOccurrences()
    }
}
