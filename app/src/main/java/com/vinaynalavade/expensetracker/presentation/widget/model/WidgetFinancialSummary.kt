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
 * Precision-safe financial summary data model tailored for home-screen widgets.
 * Holds Total Balance, Today's Expense, Monthly Income, Monthly Expense, and budget status.
 */
data class WidgetFinancialSummary(
    val balance: Amount = Amount.ZERO,
    val todayExpense: Amount = Amount.ZERO,
    val monthlyExpense: Amount = Amount.ZERO,
    val monthlyIncome: Amount = Amount.ZERO,
    val monthlyBudgetLimit: Amount? = null,
    val remainingBudget: Amount? = null,
    val isOverBudget: Boolean = false,
    val monthLabel: String = "",
    val latestTransaction: WidgetTransactionSummary? = null
)
