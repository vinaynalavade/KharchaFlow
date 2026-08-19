package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase to retrieve categories by type or all categories.
 */
class GetCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> =
        categoryRepository.getCategories()

    fun getByType(type: TransactionType): Flow<List<Category>> =
        categoryRepository.getCategoriesByType(type)
}
