package com.vinaynalavade.expensetracker.domain.model

enum class TransactionType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income");

    companion object {
        fun fromString(value: String): TransactionType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXPENSE
        }
    }
}
