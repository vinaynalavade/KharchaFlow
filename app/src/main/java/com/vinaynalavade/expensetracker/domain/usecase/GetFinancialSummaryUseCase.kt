package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * UseCase to retrieve overall and date-ranged financial summaries.
 * Includes the user's starting balance in the current balance calculation.
 */
class GetFinancialSummaryUseCase(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<FinancialSummary> =
        combine(
            transactionRepository.getTransactions(),
            userPreferencesRepository.getUserPreferences()
        ) { transactions, preferences ->
            val startingBalance = preferences.openingBalance
            var incomeSubunits = 0L
            var expenseSubunits = 0L
            for (tx in transactions) {
                if (tx.type == TransactionType.INCOME) {
                    incomeSubunits += tx.amount.subunits
                } else {
                    expenseSubunits += tx.amount.subunits
                }
            }
            val totalIncome = Amount(incomeSubunits)
            val totalExpense = Amount(expenseSubunits)
            val netChange = Amount(incomeSubunits - expenseSubunits)
            val currentBalance = startingBalance + netChange

            FinancialSummary(
                openingBalance = startingBalance,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netChange = netChange,
                currentBalance = currentBalance,
                transactionCount = transactions.size
            )
        }

    fun getByDateRange(startDateEpoch: Long, endDateEpoch: Long): Flow<FinancialSummary> =
        combine(
            transactionRepository.getTransactionsBetween(startDateEpoch, endDateEpoch),
            userPreferencesRepository.getUserPreferences()
        ) { transactions, preferences ->
            val startingBalance = preferences.openingBalance
            var incomeSubunits = 0L
            var expenseSubunits = 0L
            for (tx in transactions) {
                if (tx.type == TransactionType.INCOME) {
                    incomeSubunits += tx.amount.subunits
                } else {
                    expenseSubunits += tx.amount.subunits
                }
            }
            val totalIncome = Amount(incomeSubunits)
            val totalExpense = Amount(expenseSubunits)
            val netChange = Amount(incomeSubunits - expenseSubunits)

            FinancialSummary(
                openingBalance = startingBalance,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netChange = netChange,
                currentBalance = startingBalance + netChange,
                transactionCount = transactions.size
            )
        }
}
