package com.vinaynalavade.expensetracker.domain.model

/**
 * Domain model representing an Expense or Income Category.
 */
data class Category(
    val id: Long = 0L,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: TransactionType,
    val isDefault: Boolean = false
) {
    companion object {
        val UNCATEGORIZED = Category(
            id = 0L,
            name = "Other",
            iconName = "category",
            colorHex = "#64748B",
            type = TransactionType.EXPENSE,
            isDefault = true
        )
    }
}
