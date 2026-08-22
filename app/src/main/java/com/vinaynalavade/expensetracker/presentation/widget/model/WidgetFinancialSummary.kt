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

data class WidgetFinancialSummary(
    val balance: Amount,
    val monthlyIncome: Amount,
    val monthlyExpense: Amount,
    val latestTransaction: WidgetTransactionSummary? = null,
    val monthLabel: String = ""
)
