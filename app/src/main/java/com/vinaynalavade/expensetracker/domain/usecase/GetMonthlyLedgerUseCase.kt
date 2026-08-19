package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.CategorySpending
import com.vinaynalavade.expensetracker.domain.model.MonthlyLedgerSummary
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth

/**
 * Calculates continuous financial continuity, opening balance carry-forward,
 * income, expenses, and closing balance for any requested YearMonth.
 */
class GetMonthlyLedgerUseCase(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(yearMonth: YearMonth): Flow<MonthlyLedgerSummary> {
        val startOfMonthEpoch = DateTimeUtils.getStartOfDayEpoch(yearMonth.atDay(1))
        val endOfMonthEpoch = DateTimeUtils.getEndOfDayEpoch(yearMonth.atEndOfMonth())

        return combine(
            transactionRepository.getTransactions(),
            userPreferencesRepository.getUserPreferences()
        ) { allTransactions, preferences ->
            val baseOpeningBalance = preferences.openingBalance

            // 1. Calculate past carry-forward before the current month starts
            var pastIncome = Amount.ZERO
            var pastExpense = Amount.ZERO

            val monthTransactions = mutableListOf<Transaction>()
            var monthIncome = Amount.ZERO
            var monthExpense = Amount.ZERO

            val expenseCategoryMap = mutableMapOf<Long, Pair<com.vinaynalavade.expensetracker.domain.model.Category, MutableList<Amount>>>()
            val incomeCategoryMap = mutableMapOf<Long, Pair<com.vinaynalavade.expensetracker.domain.model.Category, MutableList<Amount>>>()

            for (tx in allTransactions) {
                if (tx.timestamp < startOfMonthEpoch) {
                    if (tx.type == TransactionType.INCOME) {
                        pastIncome += tx.amount
                    } else {
                        pastExpense += tx.amount
                    }
                } else if (tx.timestamp <= endOfMonthEpoch) {
                    monthTransactions.add(tx)
                    if (tx.type == TransactionType.INCOME) {
                        monthIncome += tx.amount
                        val entry = incomeCategoryMap.getOrPut(tx.category.id) { tx.category to mutableListOf() }
                        entry.second.add(tx.amount)
                    } else {
                        monthExpense += tx.amount
                        val entry = expenseCategoryMap.getOrPut(tx.category.id) { tx.category to mutableListOf() }
                        entry.second.add(tx.amount)
                    }
                }
            }

            val openingBalance = baseOpeningBalance + pastIncome - pastExpense
            val closingBalance = openingBalance + monthIncome - monthExpense
            val netChange = monthIncome - monthExpense

            // Compute category breakdowns
            val expenseBreakdown = expenseCategoryMap.values.map { (cat, amounts) ->
                val total = amounts.fold(Amount.ZERO) { acc, a -> acc + a }
                val pct = if (monthExpense.subunits > 0) total.subunits.toFloat() / monthExpense.subunits.toFloat() else 0f
                CategorySpending(category = cat, totalAmount = total, percentageOfTotal = pct, transactionCount = amounts.size)
            }.sortedByDescending { it.totalAmount.subunits }

            val incomeBreakdown = incomeCategoryMap.values.map { (cat, amounts) ->
                val total = amounts.fold(Amount.ZERO) { acc, a -> acc + a }
                val pct = if (monthIncome.subunits > 0) total.subunits.toFloat() / monthIncome.subunits.toFloat() else 0f
                CategorySpending(category = cat, totalAmount = total, percentageOfTotal = pct, transactionCount = amounts.size)
            }.sortedByDescending { it.totalAmount.subunits }

            MonthlyLedgerSummary(
                yearMonth = yearMonth,
                openingBalance = openingBalance,
                totalIncome = monthIncome,
                totalExpense = monthExpense,
                netChange = netChange,
                closingBalance = closingBalance,
                transactions = monthTransactions.sortedByDescending { it.timestamp },
                expenseBreakdown = expenseBreakdown,
                incomeBreakdown = incomeBreakdown
            )
        }
    }
}
