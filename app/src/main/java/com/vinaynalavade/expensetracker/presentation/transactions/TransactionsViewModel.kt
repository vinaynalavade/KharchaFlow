package com.vinaynalavade.expensetracker.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.usecase.AddTransactionUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionsUseCase
import com.vinaynalavade.expensetracker.presentation.components.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TransactionFilter(val displayName: String) {
    ALL("All"),
    EXPENSE("Expenses"),
    INCOME("Income")
}

data class DailyTransactionGroup(
    val dateHeader: String,
    val transactions: List<Transaction>
)

data class MonthlyTransactionGroup(
    val monthHeader: String,
    val totalExpense: Long,
    val totalIncome: Long,
    val dailyGroups: List<DailyTransactionGroup>
)

class TransactionsViewModel(
    getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedFilter: StateFlow<TransactionFilter> = _selectedFilter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val uiState: StateFlow<UiState<List<MonthlyTransactionGroup>>> = combine(
        getTransactionsUseCase(),
        _selectedFilter,
        _searchQuery
    ) { transactions, filter, query ->
        // 1. Filter by type
        val typeFiltered = when (filter) {
            TransactionFilter.ALL -> transactions
            TransactionFilter.EXPENSE -> transactions.filter { it.type == TransactionType.EXPENSE }
            TransactionFilter.INCOME -> transactions.filter { it.type == TransactionType.INCOME }
        }

        // 2. Filter by search query
        val searchFiltered = if (query.isBlank()) {
            typeFiltered
        } else {
            val q = query.trim().lowercase()
            typeFiltered.filter {
                it.category.name.lowercase().contains(q) ||
                (it.note?.lowercase()?.contains(q) == true)
            }
        }

        if (searchFiltered.isEmpty()) {
            UiState.Empty
        } else {
            // Group by Month ("August 2026")
            val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            val monthGroups = searchFiltered
                .groupBy { tx ->
                    val localDate = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                    localDate.format(monthFormatter)
                }
                .map { (monthStr, monthTxList) ->
                    val monthExpense = monthTxList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.subunits }
                    val monthIncome = monthTxList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.subunits }

                    // Group by Day within the month
                    val dailyGroups = monthTxList
                        .groupBy { DateTimeUtils.formatDate(it.timestamp) }
                        .map { (dateStr, dayTxList) ->
                            DailyTransactionGroup(
                                dateHeader = dateStr,
                                transactions = dayTxList
                            )
                        }

                    MonthlyTransactionGroup(
                        monthHeader = monthStr,
                        totalExpense = monthExpense,
                        totalIncome = monthIncome,
                        dailyGroups = dailyGroups
                    )
                }

            UiState.Success(monthGroups)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    fun onFilterSelected(filter: TransactionFilter) {
        _selectedFilter.value = filter
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun restoreTransaction(transaction: Transaction, onRestored: () -> Unit = {}) {
        viewModelScope.launch {
            addTransactionUseCase(transaction)
            onRestored()
        }
    }

    class Factory(
        private val getTransactionsUseCase: GetTransactionsUseCase,
        private val addTransactionUseCase: AddTransactionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionsViewModel(getTransactionsUseCase, addTransactionUseCase) as T
        }
    }
}
