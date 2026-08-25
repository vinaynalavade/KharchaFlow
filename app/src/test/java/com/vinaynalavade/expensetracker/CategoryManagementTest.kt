package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.CategoryRepository
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import com.vinaynalavade.expensetracker.domain.usecase.DeleteCategoryUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SaveCategoryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryManagementTest {

    private val defaultFoodCategory = Category(
        id = 10L,
        name = "Food & Dining",
        iconName = "restaurant",
        colorHex = "#F59E0B",
        type = TransactionType.EXPENSE,
        isDefault = true
    )

    private val defaultShoppingCategory = Category(
        id = 11L,
        name = "Shopping",
        iconName = "shopping_bag",
        colorHex = "#EC4899",
        type = TransactionType.EXPENSE,
        isDefault = true
    )

    private val customGymCategory = Category(
        id = 20L,
        name = "Gym Membership",
        iconName = "fitness_center",
        colorHex = "#3B82F6",
        type = TransactionType.EXPENSE,
        isDefault = false
    )

    @Test
    fun testDefaultCategoryCanBeEditedAndPreservesIdAndDefaultFlag() = runBlocking {
        val categories = mutableListOf(defaultFoodCategory, defaultShoppingCategory)
        val fakeCatRepo = FakeCategoryRepository(categories)
        val saveCategoryUseCase = SaveCategoryUseCase(fakeCatRepo)

        // User edits default Food category (changes name to "Groceries & Food", icon to "local_cafe", color to "#10B981")
        val editedCategory = defaultFoodCategory.copy(
            name = "Groceries & Food",
            iconName = "local_cafe",
            colorHex = "#10B981"
        )

        val result = saveCategoryUseCase(editedCategory)

        assertTrue(result is AppResult.Success)
        assertEquals(10L, (result as AppResult.Success).data) // ID is strictly preserved!

        val updatedInRepo = categories.find { it.id == 10L }!!
        assertEquals("Groceries & Food", updatedInRepo.name)
        assertEquals("local_cafe", updatedInRepo.iconName)
        assertEquals("#10B981", updatedInRepo.colorHex)
        assertTrue(updatedInRepo.isDefault) // isDefault flag preserved!
    }

    @Test
    fun testTransactionsRetainLinkageAfterCategoryEdit() = runBlocking {
        val categories = mutableListOf(defaultFoodCategory)
        val fakeCatRepo = FakeCategoryRepository(categories)
        val saveCategoryUseCase = SaveCategoryUseCase(fakeCatRepo)

        // Transaction referencing category ID 10
        val transaction = Transaction(
            id = 1L,
            amount = Amount(50000L),
            type = TransactionType.EXPENSE,
            category = defaultFoodCategory,
            paymentMethod = PaymentMethod.ACCOUNT,
            note = "Dinner",
            timestamp = System.currentTimeMillis()
        )

        // Edit Category 10
        val editedCategory = defaultFoodCategory.copy(name = "Daily Food & Snacks", colorHex = "#EF4444")
        saveCategoryUseCase(editedCategory)

        // Transaction's category ID remains 10 and references the updated category entity
        val updatedCategory = categories.find { it.id == transaction.category.id }!!
        assertEquals("Daily Food & Snacks", updatedCategory.name)
        assertEquals("#EF4444", updatedCategory.colorHex)
        assertEquals(10L, updatedCategory.id)
    }

    @Test
    fun testCaseInsensitiveDuplicateValidationOnCreate() = runBlocking {
        val categories = mutableListOf(defaultFoodCategory)
        val fakeCatRepo = FakeCategoryRepository(categories)
        val saveCategoryUseCase = SaveCategoryUseCase(fakeCatRepo)

        // Try creating new category named "food & dining" in lowercase
        val duplicateCategory = Category(
            id = 0L,
            name = "food & dining",
            iconName = "restaurant",
            colorHex = "#F59E0B",
            type = TransactionType.EXPENSE,
            isDefault = false
        )

        val result = saveCategoryUseCase(duplicateCategory)
        assertTrue(result is AppResult.Error)
    }

    @Test
    fun testEditingCategoryWithoutChangingNameIsAllowed() = runBlocking {
        val categories = mutableListOf(defaultFoodCategory, defaultShoppingCategory)
        val fakeCatRepo = FakeCategoryRepository(categories)
        val saveCategoryUseCase = SaveCategoryUseCase(fakeCatRepo)

        // Edit Food category color only, keeping the same name
        val editColorOnly = defaultFoodCategory.copy(colorHex = "#06B6D4")
        val result = saveCategoryUseCase(editColorOnly)

        assertTrue(result is AppResult.Success)
        assertEquals(10L, (result as AppResult.Success).data)
    }

    @Test
    fun testEditingCategoryNameToAnotherExistingCategoryIsRejected() = runBlocking {
        val categories = mutableListOf(defaultFoodCategory, defaultShoppingCategory)
        val fakeCatRepo = FakeCategoryRepository(categories)
        val saveCategoryUseCase = SaveCategoryUseCase(fakeCatRepo)

        // Try renaming Category 10 (Food) to "Shopping" (which belongs to Category 11)
        val conflictEdit = defaultFoodCategory.copy(name = "SHOPPING")
        val result = saveCategoryUseCase(conflictEdit)

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun testWhitespaceTrimmingDuringSave() = runBlocking {
        val categories = mutableListOf<Category>()
        val fakeCatRepo = FakeCategoryRepository(categories)
        val saveCategoryUseCase = SaveCategoryUseCase(fakeCatRepo)

        val untrimmed = Category(
            id = 0L,
            name = "   Personal Care   ",
            iconName = "spa",
            colorHex = "#8B5CF6",
            type = TransactionType.EXPENSE
        )

        val result = saveCategoryUseCase(untrimmed)
        assertTrue(result is AppResult.Success)
        assertEquals("Personal Care", categories.first().name)
    }

    @Test
    fun testDefaultCategoryDeletionIsProtected() = runBlocking {
        val categories = mutableListOf(defaultFoodCategory)
        val fakeCatRepo = FakeCategoryRepository(categories)
        val fakeTxRepo = FakeTransactionRepository(emptyList())
        val deleteCategoryUseCase = DeleteCategoryUseCase(fakeCatRepo, fakeTxRepo)

        val result = deleteCategoryUseCase(10L)
        assertTrue(result is AppResult.Error)
        assertEquals(1, categories.size) // Not deleted!
    }

    @Test
    fun testCustomCategoryDeletionAllowedOnlyWhenNoTransactionsDependOnIt() = runBlocking {
        val categories = mutableListOf(customGymCategory)
        val fakeCatRepo = FakeCategoryRepository(categories)

        // 1. When a transaction depends on Gym Category -> Deletion is blocked
        val dependentTx = Transaction(
            id = 1L,
            amount = Amount(200000L),
            type = TransactionType.EXPENSE,
            category = customGymCategory,
            paymentMethod = PaymentMethod.ACCOUNT,
            note = null,
            timestamp = System.currentTimeMillis()
        )
        val fakeTxRepoWithTx = FakeTransactionRepository(listOf(dependentTx))
        val deleteUseCase1 = DeleteCategoryUseCase(fakeCatRepo, fakeTxRepoWithTx)

        val blockedResult = deleteUseCase1(20L)
        assertTrue(blockedResult is AppResult.Error)
        assertEquals(1, categories.size)

        // 2. When 0 transactions depend on Gym Category -> Deletion succeeds
        val fakeTxRepoEmpty = FakeTransactionRepository(emptyList())
        val deleteUseCase2 = DeleteCategoryUseCase(fakeCatRepo, fakeTxRepoEmpty)

        val successResult = deleteUseCase2(20L)
        assertTrue(successResult is AppResult.Success)
        assertEquals(0, categories.size)
    }

    private class FakeCategoryRepository(private val list: MutableList<Category>) : CategoryRepository {
        override fun getCategories(): Flow<List<Category>> = flowOf(list)
        override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
            flowOf(list.filter { it.type == type })
        override fun getCategoryById(id: Long): Flow<Category?> = flowOf(list.find { it.id == id })
        override suspend fun insertCategory(category: Category): AppResult<Long> {
            val newId = if (category.id == 0L) (list.maxOfOrNull { it.id } ?: 0L) + 1L else category.id
            val newCat = category.copy(id = newId)
            list.add(newCat)
            return AppResult.Success(newId)
        }
        override suspend fun updateCategory(category: Category): AppResult<Unit> {
            val index = list.indexOfFirst { it.id == category.id }
            if (index != -1) {
                list[index] = category
            }
            return AppResult.Success(Unit)
        }
        override suspend fun deleteCategory(id: Long): AppResult<Unit> {
            list.removeAll { it.id == id && !it.isDefault }
            return AppResult.Success(Unit)
        }
    }

    private class FakeTransactionRepository(private val list: List<Transaction>) : TransactionRepository {
        override fun getTransactions(): Flow<List<Transaction>> = flowOf(list)
        override fun getTransactionById(id: Long): Flow<Transaction?> = flowOf(list.find { it.id == id })
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            flowOf(list.filter { it.timestamp in startDate..endDate })
        override fun getFinancialSummary(): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary> = flowOf(FinancialSummary.EMPTY)
        override suspend fun insertTransaction(transaction: Transaction) = AppResult.Success(1L)
        override suspend fun updateTransaction(transaction: Transaction) = AppResult.Success(Unit)
        override suspend fun deleteTransaction(id: Long) = AppResult.Success(Unit)
    }
}
