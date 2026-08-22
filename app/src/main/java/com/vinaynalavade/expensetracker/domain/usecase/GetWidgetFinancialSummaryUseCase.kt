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
 * UseCase to retrieve a precision-safe financial snapshot tailored for home screen widgets.
 * Combines opening balance, all-time net change for current balance, and monthly income/expense metrics.
 */
class GetWidgetFinancialSummaryUseCase(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<WidgetFinancialSummary> =
        combine(
            transactionRepository.getTransactions(),
            userPreferencesRepository.getUserPreferences()
        ) { transactions, preferences ->
            val startingBalance = preferences.openingBalance
            var totalAllTimeIncome = 0L
            var totalAllTimeExpense = 0L

            val currentMonth = YearMonth.now()
            val startOfMonthEpoch = DateTimeUtils.getStartOfMonthEpoch(currentMonth)
            val endOfMonthEpoch = DateTimeUtils.getEndOfMonthEpoch(currentMonth)

            var monthlyIncomeSubunits = 0L
            var monthlyExpenseSubunits = 0L

            for (tx in transactions) {
                if (tx.type == TransactionType.INCOME) {
                    totalAllTimeIncome += tx.amount.subunits
                    if (tx.timestamp in startOfMonthEpoch..endOfMonthEpoch) {
                        monthlyIncomeSubunits += tx.amount.subunits
                    }
                } else {
                    totalAllTimeExpense += tx.amount.subunits
                    if (tx.timestamp in startOfMonthEpoch..endOfMonthEpoch) {
                        monthlyExpenseSubunits += tx.amount.subunits
                    }
                }
            }

            val netChange = totalAllTimeIncome - totalAllTimeExpense
            val currentBalance = startingBalance + Amount(netChange)

            val latestTx = transactions.maxByOrNull { it.timestamp }?.let {
                WidgetTransactionSummary(
                    id = it.id,
                    amount = it.amount,
                    type = it.type,
                    categoryName = it.category.name,
                    timestamp = it.timestamp
                )
            }

            val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            val monthLabel = LocalDate.now().format(monthFormatter)

            WidgetFinancialSummary(
                balance = currentBalance,
                monthlyIncome = Amount(monthlyIncomeSubunits),
                monthlyExpense = Amount(monthlyExpenseSubunits),
                latestTransaction = latestTx,
                monthLabel = monthLabel
            )
        }
}
