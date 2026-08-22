package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * UseCase to compute the total expense amount for the current calendar day.
 * Only EXPENSE transactions are counted; INCOME is excluded.
 * Uses the system default timezone via DateTimeUtils for day boundary calculation.
 */
class GetTodayExpenseUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<Amount> {
        val today = LocalDate.now()
        val startOfDay = DateTimeUtils.getStartOfDayEpoch(today)
        val endOfDay = DateTimeUtils.getEndOfDayEpoch(today)

        return transactionRepository.getTransactions().map { transactions ->
            var totalExpenseSubunits = 0L
            for (tx in transactions) {
                if (tx.type == TransactionType.EXPENSE && tx.timestamp in startOfDay..endOfDay) {
                    totalExpenseSubunits += tx.amount.subunits
                }
            }
            Amount(totalExpenseSubunits)
        }
    }

    /**
     * Variant that accepts explicit day boundaries for testability and date-rollover scenarios.
     */
    fun forDateRange(startEpoch: Long, endEpoch: Long): Flow<Amount> {
        return transactionRepository.getTransactions().map { transactions ->
            var totalExpenseSubunits = 0L
            for (tx in transactions) {
                if (tx.type == TransactionType.EXPENSE && tx.timestamp in startEpoch..endEpoch) {
                    totalExpenseSubunits += tx.amount.subunits
                }
            }
            Amount(totalExpenseSubunits)
        }
    }
}
