package com.vinaynalavade.expensetracker.data.repository

import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.data.local.dao.CategoryDao
import com.vinaynalavade.expensetracker.data.local.entity.CategoryEntity
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list ->
            list.map { mapToDomain(it) }
        }
    }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type.name).map { list ->
            list.map { mapToDomain(it) }
        }
    }

    override fun getCategoryById(id: Long): Flow<Category?> {
        return categoryDao.getCategoryById(id).map { entity ->
            entity?.let { mapToDomain(it) }
        }
    }

    override suspend fun insertCategory(category: Category): AppResult<Long> {
        return try {
            val entity = mapToEntity(category)
            val id = categoryDao.insertCategory(entity)
            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to insert category.", e))
        }
    }

    override suspend fun updateCategory(category: Category): AppResult<Unit> {
        return try {
            val entity = mapToEntity(category)
            categoryDao.updateCategory(entity)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to update category.", e))
        }
    }

    override suspend fun deleteCategory(id: Long): AppResult<Unit> {
        return try {
            categoryDao.deleteCategoryById(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete category.", e))
        }
    }

    private fun mapToDomain(entity: CategoryEntity): Category {
        return Category(
            id = entity.id,
            name = entity.name,
            iconName = entity.iconName,
            colorHex = entity.colorHex,
            type = TransactionType.fromString(entity.type),
            isDefault = entity.isDefault
        )
    }

    private fun mapToEntity(category: Category): CategoryEntity {
        return CategoryEntity(
            id = category.id,
            name = category.name,
            iconName = category.iconName,
            colorHex = category.colorHex,
            type = category.type.name,
            isDefault = category.isDefault
        )
    }
}
