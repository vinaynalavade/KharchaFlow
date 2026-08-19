package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.domain.model.StatementLedgerItem
import com.vinaynalavade.expensetracker.domain.model.StatementReport
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UseCase to compile a comprehensive financial statement with running balance calculations.
 */
class GenerateStatementUseCase(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(
        startEpoch: Long,
        endEpoch: Long,
        periodTitle: String
    ): StatementReport {
        val allTransactions = transactionRepository.getTransactions().first()
        val preferences = userPreferencesRepository.getUserPreferences().first()
        val currency = preferences.currency

        val baseOpeningBalance = preferences.openingBalance

        // 1. Calculate opening balance before statement start date
        var pastIncome = Amount.ZERO
        var pastExpense = Amount.ZERO

        val periodTransactions = mutableListOf<Transaction>()

        for (tx in allTransactions) {
            if (tx.timestamp < startEpoch) {
                if (tx.type == TransactionType.INCOME) {
                    pastIncome += tx.amount
                } else {
                    pastExpense += tx.amount
                }
            } else if (tx.timestamp <= endEpoch) {
                periodTransactions.add(tx)
            }
        }

        val openingBalance = baseOpeningBalance + pastIncome - pastExpense

        // Sort ascending chronologically for running balance ledger
        val sortedAscending = periodTransactions.sortedBy { it.timestamp }

        var runningBalance = openingBalance
        var totalIncome = Amount.ZERO
        var totalExpense = Amount.ZERO

        val ledgerItems = mutableListOf<StatementLedgerItem>()
        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // Add Initial Opening Balance row
        ledgerItems.add(
            StatementLedgerItem(
                dateEpoch = startEpoch,
                dateString = if (startEpoch <= 0L) "Account Start" else dateFormatter.format(Date(startEpoch)),
                description = "Opening Balance",
                categoryName = "—",
                type = null,
                amount = null,
                runningBalance = openingBalance
            )
        )

        for (tx in sortedAscending) {
            if (tx.type == TransactionType.INCOME) {
                runningBalance += tx.amount
                totalIncome += tx.amount
            } else {
                runningBalance -= tx.amount
                totalExpense += tx.amount
            }

            ledgerItems.add(
                StatementLedgerItem(
                    dateEpoch = tx.timestamp,
                    dateString = dateFormatter.format(Date(tx.timestamp)),
                    description = tx.note?.ifBlank { tx.category.name } ?: tx.category.name,
                    categoryName = tx.category.name,
                    type = tx.type,
                    amount = tx.amount,
                    runningBalance = runningBalance
                )
            )
        }

        val closingBalance = openingBalance + totalIncome - totalExpense

        return StatementReport(
            periodTitle = periodTitle,
            currency = currency,
            openingBalance = openingBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            closingBalance = closingBalance,
            ledgerItems = ledgerItems
        )
    }
}
