package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * UseCase to create or update categories with strict case-insensitive duplicate protection.
 */
class SaveCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): AppResult<Long> {
        val canonicalName = category.name.trim()
        if (canonicalName.isBlank()) {
            return AppResult.Error(AppError.ValidationError("Category name cannot be empty.", "name"))
        }

        // Enforce duplicate protection for the same transaction type
        val existingCategories = categoryRepository.getCategoriesByType(category.type).firstOrNull() ?: emptyList()
        val isDuplicate = existingCategories.any {
            it.id != category.id && it.name.trim().equals(canonicalName, ignoreCase = true)
        }

        if (isDuplicate) {
            return AppResult.Error(
                AppError.ValidationError(
                    "A ${category.type.displayName.lowercase()} category named '$canonicalName' already exists.",
                    "name"
                )
            )
        }

        val sanitizedCategory = category.copy(name = canonicalName)
        return if (sanitizedCategory.id == 0L) {
            categoryRepository.insertCategory(sanitizedCategory)
        } else {
            when (val updateResult = categoryRepository.updateCategory(sanitizedCategory)) {
                is AppResult.Success -> AppResult.Success(sanitizedCategory.id)
                is AppResult.Error -> AppResult.Error(updateResult.error)
            }
        }
    }
}
