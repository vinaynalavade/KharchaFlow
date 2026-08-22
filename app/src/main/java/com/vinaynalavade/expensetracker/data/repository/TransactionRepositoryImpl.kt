package com.vinaynalavade.expensetracker.data.repository

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.data.local.dao.TransactionDao
import com.vinaynalavade.expensetracker.data.local.entity.TransactionEntity
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsWithCategory().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactionsWithCategory(limit).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getTransactionById(id: Long): Flow<Transaction?> {
        return transactionDao.getTransactionWithCategoryById(id).map { item ->
            item?.toDomainModel()
        }
    }

    override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetween(startDate, endDate).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getFinancialSummary(): Flow<FinancialSummary> {
        return getTransactions().map { transactions ->
            var incomeSubunits = 0L
            var expenseSubunits = 0L
            for (tx in transactions) {
                if (tx.type == TransactionType.INCOME) {
                    incomeSubunits += tx.amount.subunits
                } else {
                    expenseSubunits += tx.amount.subunits
                }
            }
            FinancialSummary(
                totalIncome = Amount(incomeSubunits),
                totalExpense = Amount(expenseSubunits),
                netChange = Amount(incomeSubunits - expenseSubunits),
                currentBalance = Amount(incomeSubunits - expenseSubunits),
                transactionCount = transactions.size
            )
        }
    }

    override fun getFinancialSummaryByDateRange(startDate: Long, endDate: Long): Flow<FinancialSummary> {
        return getTransactionsBetween(startDate, endDate).map { transactions ->
            var incomeSubunits = 0L
            var expenseSubunits = 0L
            for (tx in transactions) {
                if (tx.type == TransactionType.INCOME) {
                    incomeSubunits += tx.amount.subunits
                } else {
                    expenseSubunits += tx.amount.subunits
                }
            }
            FinancialSummary(
                totalIncome = Amount(incomeSubunits),
                totalExpense = Amount(expenseSubunits),
                netChange = Amount(incomeSubunits - expenseSubunits),
                currentBalance = Amount(incomeSubunits - expenseSubunits),
                transactionCount = transactions.size
            )
        }
    }

    override suspend fun insertTransaction(transaction: Transaction): AppResult<Long> {
        return try {
            val entity = TransactionEntity.fromDomainModel(transaction)
            val id = transactionDao.insertTransaction(entity)
            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to insert transaction.", e))
        }
    }

    override suspend fun updateTransaction(transaction: Transaction): AppResult<Unit> {
        return try {
            val entity = TransactionEntity.fromDomainModel(transaction)
            transactionDao.updateTransaction(entity)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to update transaction.", e))
        }
    }

    override suspend fun deleteTransaction(id: Long): AppResult<Unit> {
        return try {
            transactionDao.deleteTransactionById(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.DatabaseError(e.message ?: "Failed to delete transaction.", e))
        }
    }
}
