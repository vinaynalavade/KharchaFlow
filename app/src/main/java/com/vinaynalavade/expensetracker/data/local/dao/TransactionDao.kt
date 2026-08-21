package com.vinaynalavade.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vinaynalavade.expensetracker.data.local.entity.TransactionEntity
import com.vinaynalavade.expensetracker.data.local.entity.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionWithCategoryById(id: Long): Flow<TransactionWithCategory?>

    @Transaction
    @Query("SELECT * FROM transactions WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp DESC, id DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>>

    @Query("SELECT COALESCE(SUM(amount_subunits), 0) FROM transactions WHERE type = 'INCOME'")
    fun getTotalIncomeSubunits(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount_subunits), 0) FROM transactions WHERE type = 'EXPENSE'")
    fun getTotalExpenseSubunits(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
