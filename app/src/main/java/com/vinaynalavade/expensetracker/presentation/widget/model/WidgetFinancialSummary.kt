package com.vinaynalavade.expensetracker.presentation.widget.model

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.domain.model.TransactionType

data class WidgetTransactionSummary(
    val id: Long,
    val amount: Amount,
    val type: TransactionType,
    val categoryName: String,
    val timestamp: Long
)

/**
 * Precision-safe financial summary data model tailored for the consolidated home-screen widget.
 * Holds the primary hero metric (today's spending) alongside monthly context and budget tracking.
 */
data class WidgetFinancialSummary(
    val todayExpense: Amount,
    val monthlyExpense: Amount,
    val monthlyIncome: Amount = Amount.ZERO,
    val monthlyBudgetLimit: Amount? = null,
    val remainingBudget: Amount? = null,
    val isOverBudget: Boolean = false,
    val monthLabel: String = "",
    val latestTransaction: WidgetTransactionSummary? = null
)

