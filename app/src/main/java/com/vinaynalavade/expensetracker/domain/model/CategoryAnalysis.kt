package com.vinaynalavade.expensetracker.domain.model

import com.vinaynalavade.expensetracker.core.model.Amount

/**
 * Analytical breakdown item representing total spending or income for a specific category.
 */
data class CategoryAnalysis(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val amount: Amount,
    val percentage: Float,
    val transactionCount: Int
)

/**
 * Container result for category distribution analysis of a specific transaction type in a date range.
 */
data class CategoryAnalysisResult(
    val type: TransactionType,
    val totalAmount: Amount,
    val categories: List<CategoryAnalysis>
) {
    val isEmpty: Boolean
        get() = categories.isEmpty() || totalAmount.isZero

    companion object {
        fun empty(type: TransactionType) = CategoryAnalysisResult(
            type = type,
            totalAmount = Amount.ZERO,
            categories = emptyList()
        )
    }
}
