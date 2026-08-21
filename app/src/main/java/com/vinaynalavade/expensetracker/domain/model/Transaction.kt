package com.vinaynalavade.expensetracker.domain.model

import com.vinaynalavade.expensetracker.core.model.Amount

/**
 * Domain model representing a financial transaction (Expense or Income).
 */
data class Transaction(
    val id: Long = 0L,
    val amount: Amount,
    val type: TransactionType,
    val category: Category,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String? = null,
    val timestamp: Long,
    val createdAt: Long = timestamp,
    val updatedAt: Long = timestamp
)
