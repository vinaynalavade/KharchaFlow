package com.vinaynalavade.expensetracker.domain.repository

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for managing recurring transactions and EMIs.
 */
interface RecurringTransactionRepository {

    fun getRecurringTransactions(): Flow<List<RecurringTransaction>>

    fun getRecurringTransactionById(id: Long): Flow<RecurringTransaction?>

    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction): AppResult<Long>

    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction): AppResult<Unit>

    suspend fun deleteRecurringTransaction(id: Long): AppResult<Unit>

    suspend fun processDueOccurrences(): AppResult<Int>
}
