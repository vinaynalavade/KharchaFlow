package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.presentation.widget.model.WidgetFinancialSummary
import com.vinaynalavade.expensetracker.presentation.widget.model.WidgetTransactionSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UseCase to retrieve a consolidated, precision-safe financial snapshot tailored for home screen widgets.
 * Computes Total Balance (lifetime net change + opening balance), today's expenses, current month's expenses,
 * monthly income, and budget status.
 */
class GetWidgetFinancialSummaryUseCase(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(
        today: LocalDate = LocalDate.now(),
        currentMonth: YearMonth = YearMonth.now()
    ): Flow<WidgetFinancialSummary> {
        val startOfDayEpoch = DateTimeUtils.getStartOfDayEpoch(today)
        val endOfDayEpoch = DateTimeUtils.getEndOfDayEpoch(today)
        val startOfMonthEpoch = DateTimeUtils.getStartOfMonthEpoch(currentMonth)
        val endOfMonthEpoch = DateTimeUtils.getEndOfMonthEpoch(currentMonth)

        return combine(
            transactionRepository.getTransactions(),
            userPreferencesRepository.getUserPreferences()
        ) { transactions, preferences ->
            var lifetimeIncomeSubunits = 0L
            var lifetimeExpenseSubunits = 0L
            var todayExpenseSubunits = 0L
            var monthlyIncomeSubunits = 0L
            var monthlyExpenseSubunits = 0L

            for (tx in transactions) {
                if (tx.type == TransactionType.EXPENSE) {
                    lifetimeExpenseSubunits += tx.amount.subunits
                    if (tx.timestamp in startOfDayEpoch..endOfDayEpoch) {
                        todayExpenseSubunits += tx.amount.subunits
                    }
                    if (tx.timestamp in startOfMonthEpoch..endOfMonthEpoch) {
                        monthlyExpenseSubunits += tx.amount.subunits
                    }
                } else if (tx.type == TransactionType.INCOME) {
                    lifetimeIncomeSubunits += tx.amount.subunits
                    if (tx.timestamp in startOfMonthEpoch..endOfMonthEpoch) {
                        monthlyIncomeSubunits += tx.amount.subunits
                    }
                }
            }

            val openingBalanceSubunits = preferences.openingBalanceSubunits
            val totalBalanceSubunits = openingBalanceSubunits + lifetimeIncomeSubunits - lifetimeExpenseSubunits

            val budgetLimitSubunits = preferences.monthlyBudgetLimitSubunits
            val monthlyBudgetLimit = if (budgetLimitSubunits > 0L) Amount(budgetLimitSubunits) else null
            val remainingBudget = if (budgetLimitSubunits > 0L) {
                val rem = budgetLimitSubunits - monthlyExpenseSubunits
                Amount(if (rem > 0L) rem else 0L)
            } else null
            val isOverBudget = budgetLimitSubunits > 0L && (monthlyExpenseSubunits > budgetLimitSubunits)

            val latestTx = transactions.maxByOrNull { it.timestamp }?.let {
                WidgetTransactionSummary(
                    id = it.id,
                    amount = it.amount,
                    type = it.type,
                    categoryName = it.category.name,
                    timestamp = it.timestamp
                )
            }

            val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())
            val monthLabel = today.format(monthFormatter)

            WidgetFinancialSummary(
                balance = Amount(totalBalanceSubunits),
                todayExpense = Amount(todayExpenseSubunits),
                monthlyExpense = Amount(monthlyExpenseSubunits),
                monthlyIncome = Amount(monthlyIncomeSubunits),
                monthlyBudgetLimit = monthlyBudgetLimit,
                remainingBudget = remainingBudget,
                isOverBudget = isOverBudget,
                monthLabel = monthLabel,
                latestTransaction = latestTx
            )
        }
    }

    /**
     * Explicit epoch-range variant for tests and custom range queries.
     */
    fun forRanges(
        startOfDayEpoch: Long,
        endOfDayEpoch: Long,
        startOfMonthEpoch: Long,
        endOfMonthEpoch: Long,
        monthLabel: String = "This Month"
    ): Flow<WidgetFinancialSummary> =
        combine(
            transactionRepository.getTransactions(),
            userPreferencesRepository.getUserPreferences()
        ) { transactions, preferences ->
            var lifetimeIncomeSubunits = 0L
            var lifetimeExpenseSubunits = 0L
            var todayExpenseSubunits = 0L
            var monthlyIncomeSubunits = 0L
            var monthlyExpenseSubunits = 0L

            for (tx in transactions) {
                if (tx.type == TransactionType.EXPENSE) {
                    lifetimeExpenseSubunits += tx.amount.subunits
                    if (tx.timestamp in startOfDayEpoch..endOfDayEpoch) {
                        todayExpenseSubunits += tx.amount.subunits
                    }
                    if (tx.timestamp in startOfMonthEpoch..endOfMonthEpoch) {
                        monthlyExpenseSubunits += tx.amount.subunits
                    }
                } else if (tx.type == TransactionType.INCOME) {
                    lifetimeIncomeSubunits += tx.amount.subunits
                    if (tx.timestamp in startOfMonthEpoch..endOfMonthEpoch) {
                        monthlyIncomeSubunits += tx.amount.subunits
                    }
                }
            }

            val openingBalanceSubunits = preferences.openingBalanceSubunits
            val totalBalanceSubunits = openingBalanceSubunits + lifetimeIncomeSubunits - lifetimeExpenseSubunits

            val budgetLimitSubunits = preferences.monthlyBudgetLimitSubunits
            val monthlyBudgetLimit = if (budgetLimitSubunits > 0L) Amount(budgetLimitSubunits) else null
            val remainingBudget = if (budgetLimitSubunits > 0L) {
                val rem = budgetLimitSubunits - monthlyExpenseSubunits
                Amount(if (rem > 0L) rem else 0L)
            } else null
            val isOverBudget = budgetLimitSubunits > 0L && (monthlyExpenseSubunits > budgetLimitSubunits)

            val latestTx = transactions.maxByOrNull { it.timestamp }?.let {
                WidgetTransactionSummary(
                    id = it.id,
                    amount = it.amount,
                    type = it.type,
                    categoryName = it.category.name,
                    timestamp = it.timestamp
                )
            }

            WidgetFinancialSummary(
                balance = Amount(totalBalanceSubunits),
                todayExpense = Amount(todayExpenseSubunits),
                monthlyExpense = Amount(monthlyExpenseSubunits),
                monthlyIncome = Amount(monthlyIncomeSubunits),
                monthlyBudgetLimit = monthlyBudgetLimit,
                remainingBudget = remainingBudget,
                isOverBudget = isOverBudget,
                monthLabel = monthLabel,
                latestTransaction = latestTx
            )
        }
}
