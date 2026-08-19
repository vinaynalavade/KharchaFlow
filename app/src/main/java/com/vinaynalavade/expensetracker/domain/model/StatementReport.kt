package com.vinaynalavade.expensetracker.domain.model

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency

data class StatementLedgerItem(
    val dateEpoch: Long,
    val dateString: String,
    val description: String,
    val categoryName: String,
    val type: TransactionType?,
    val amount: Amount?,
    val runningBalance: Amount
)

data class StatementReport(
    val periodTitle: String,
    val currency: Currency,
    val openingBalance: Amount,
    val totalIncome: Amount,
    val totalExpense: Amount,
    val closingBalance: Amount,
    val ledgerItems: List<StatementLedgerItem>
)
