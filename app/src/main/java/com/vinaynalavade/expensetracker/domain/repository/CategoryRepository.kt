package com.vinaynalavade.expensetracker.domain.repository

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for managing category data.
 */
interface CategoryRepository {

    fun getCategories(): Flow<List<Category>>

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    fun getCategoryById(id: Long): Flow<Category?>

    suspend fun insertCategory(category: Category): AppResult<Long>

    suspend fun updateCategory(category: Category): AppResult<Unit>

    suspend fun deleteCategory(id: Long): AppResult<Unit>
}
