package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.CategoryAnalysis
import com.vinaynalavade.expensetracker.domain.model.CategoryAnalysisResult
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth

/**
 * UseCase to aggregate transactions into structured, month-aware category analysis distributions.
 */
class GetCategoryAnalysisUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(
        yearMonth: YearMonth,
        type: TransactionType
    ): Flow<CategoryAnalysisResult> {
        val startEpoch = DateTimeUtils.getStartOfDayEpoch(yearMonth.atDay(1))
        val endEpoch = DateTimeUtils.getEndOfDayEpoch(yearMonth.atEndOfMonth())

        return transactionRepository.getTransactionsBetween(startEpoch, endEpoch).map { transactions ->
            val filtered = transactions.filter { it.type == type }
            val totalSubunits = filtered.sumOf { it.amount.subunits }

            val categoryMap = mutableMapOf<Long, Pair<Category, MutableList<Amount>>>()
            for (tx in filtered) {
                val entry = categoryMap.getOrPut(tx.category.id) { tx.category to mutableListOf() }
                entry.second.add(tx.amount)
            }

            val categoryAnalysisList = categoryMap.values.map { (cat, amounts) ->
                val catSubunits = amounts.sumOf { it.subunits }
                val percentage = if (totalSubunits > 0L) {
                    (catSubunits.toFloat() / totalSubunits.toFloat()) * 100f
                } else {
                    0f
                }

                CategoryAnalysis(
                    categoryId = cat.id,
                    categoryName = cat.name,
                    categoryIcon = cat.iconName,
                    categoryColor = cat.colorHex,
                    amount = Amount(catSubunits),
                    percentage = percentage,
                    transactionCount = amounts.size
                )
            }.sortedByDescending { it.amount.subunits }

            CategoryAnalysisResult(
                type = type,
                totalAmount = Amount(totalSubunits),
                categories = categoryAnalysisList
            )
        }
    }
}
