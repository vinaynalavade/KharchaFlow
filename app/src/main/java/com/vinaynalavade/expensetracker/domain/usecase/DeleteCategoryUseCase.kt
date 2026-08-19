package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.repository.CategoryRepository
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull

class DeleteCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(categoryId: Long): AppResult<Unit> {
        val category = categoryRepository.getCategoryById(categoryId).firstOrNull()
            ?: return AppResult.Error(AppError.NotFound("Category not found."))

        if (category.isDefault) {
            return AppResult.Error(AppError.ValidationError("Default system categories cannot be deleted."))
        }

        // Check if any transactions reference this category
        val transactions = transactionRepository.getTransactions().firstOrNull() ?: emptyList()
        val hasTransactions = transactions.any { it.category.id == categoryId }

        if (hasTransactions) {
            return AppResult.Error(
                AppError.ValidationError("Cannot delete category '${category.name}' because existing transactions depend on it.")
            )
        }

        return categoryRepository.deleteCategory(categoryId)
    }
}
