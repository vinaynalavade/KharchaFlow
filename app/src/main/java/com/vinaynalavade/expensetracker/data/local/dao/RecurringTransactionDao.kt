package com.vinaynalavade.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vinaynalavade.expensetracker.data.local.entity.RecurringTransactionEntity
import com.vinaynalavade.expensetracker.data.local.entity.RecurringWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Transaction
    @Query("SELECT * FROM recurring_transactions ORDER BY is_enabled DESC, title ASC")
    fun getAllRecurringWithCategory(): Flow<List<RecurringWithCategory>>

    @Transaction
    @Query("SELECT * FROM recurring_transactions WHERE id = :id LIMIT 1")
    fun getRecurringWithCategoryById(id: Long): Flow<RecurringWithCategory?>

    @Transaction
    @Query("SELECT * FROM recurring_transactions WHERE is_enabled = 1")
    suspend fun getActiveRecurringTransactions(): List<RecurringWithCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(entity: RecurringTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransactions(entities: List<RecurringTransactionEntity>)

    @Update
    suspend fun updateRecurringTransaction(entity: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringTransactionById(id: Long)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAllRecurringTransactions()
}
