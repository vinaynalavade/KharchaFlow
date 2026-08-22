package com.vinaynalavade.expensetracker.domain.repository

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository contract for managing transaction data.
 */
interface TransactionRepository {

    fun getTransactions(): Flow<List<Transaction>>

    fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        getTransactions().map { it.take(limit) }

    fun getTransactionById(id: Long): Flow<Transaction?>

    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>>

    fun getFinancialSummary(): Flow<FinancialSummary>

    fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary>

    suspend fun insertTransaction(transaction: Transaction): AppResult<Long>

    suspend fun updateTransaction(transaction: Transaction): AppResult<Unit>

    suspend fun deleteTransaction(id: Long): AppResult<Unit>
}
