package com.vinaynalavade.expensetracker.domain.model

import com.vinaynalavade.expensetracker.core.model.Amount

/**
 * Aggregated domain model for financial dashboard and statistics.
 */
data class FinancialSummary(
    val openingBalance: Amount = Amount.ZERO,
    val totalIncome: Amount = Amount.ZERO,
    val totalExpense: Amount = Amount.ZERO,
    val netChange: Amount = Amount.ZERO,
    val currentBalance: Amount = Amount.ZERO,
    val transactionCount: Int = 0
) {
    companion object {
        val EMPTY = FinancialSummary()
    }
}
