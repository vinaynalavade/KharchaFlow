package com.vinaynalavade.expensetracker.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.usecase.AddTransactionUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TransactionFilter(val displayName: String) {
    ALL("All"),
    EXPENSE("Expenses"),
    INCOME("Income")
}

enum class DateRangeFilter(val displayName: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom")
}

data class TransactionSummaryHeader(
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netBalance: Long = 0L,
    val transactionCount: Int = 0
)

data class DailyTransactionGroup(
    val dateHeader: String,
    val totalExpense: Long,
    val totalIncome: Long,
    val transactions: List<Transaction>
)

data class MonthlyTransactionGroup(
    val monthHeader: String,
    val totalExpense: Long,
    val totalIncome: Long,
    val dailyGroups: List<DailyTransactionGroup>
)

data class TransactionFilterParams(
    val filter: TransactionFilter = TransactionFilter.ALL,
    val dateRange: DateRangeFilter = DateRangeFilter.ALL_TIME,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val searchQuery: String = ""
)

data class TransactionsUiState(
    val groups: List<MonthlyTransactionGroup> = emptyList(),
    val summary: TransactionSummaryHeader = TransactionSummaryHeader(),
    val totalTransactionsCount: Int = 0,
    val selectedFilter: TransactionFilter = TransactionFilter.ALL,
    val selectedDateRange: DateRangeFilter = DateRangeFilter.ALL_TIME,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isFilterActive: Boolean = false
)

class TransactionsViewModel(
    getTransactionsUseCase: GetTransactionsUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    initialFilter: TransactionFilter = TransactionFilter.ALL,
    initialSearchQuery: String = ""
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(initialFilter)
    val selectedFilter: StateFlow<TransactionFilter> = _selectedFilter

    private val _selectedDateRange = MutableStateFlow(DateRangeFilter.ALL_TIME)
    val selectedDateRange: StateFlow<DateRangeFilter> = _selectedDateRange

    private val _customStartDate = MutableStateFlow<Long?>(null)
    val customStartDate: StateFlow<Long?> = _customStartDate

    private val _customEndDate = MutableStateFlow<Long?>(null)
    val customEndDate: StateFlow<Long?> = _customEndDate

    private val _searchQuery = MutableStateFlow(initialSearchQuery)
    val searchQuery: StateFlow<String> = _searchQuery

    private val filterParams = combine(
        _selectedFilter,
        _selectedDateRange,
        _customStartDate,
        _customEndDate,
        _searchQuery
    ) { filter, dateRange, customStart, customEnd, query ->
        TransactionFilterParams(
            filter = filter,
            dateRange = dateRange,
            customStartDate = customStart,
            customEndDate = customEnd,
            searchQuery = query
        )
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        getTransactionsUseCase(),
        filterParams
    ) { allTransactions: List<Transaction>, params: TransactionFilterParams ->
        filterAndGroupTransactions(allTransactions, params)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState(isLoading = true)
    )

    fun onFilterSelected(filter: TransactionFilter) {
        _selectedFilter.value = filter
    }

    fun onDateRangeSelected(dateRange: DateRangeFilter) {
        _selectedDateRange.value = dateRange
        if (dateRange != DateRangeFilter.CUSTOM) {
            _customStartDate.value = null
            _customEndDate.value = null
        }
    }

    fun onCustomDateRangeSet(startDateEpoch: Long, endDateEpoch: Long) {
        val startOfSelectedDay = DateTimeUtils.getStartOfDayEpoch(
            DateTimeUtils.epochToLocalDate(startDateEpoch)
        )
        val endOfSelectedDay = DateTimeUtils.getEndOfDayEpoch(
            DateTimeUtils.epochToLocalDate(endDateEpoch)
        )
        _customStartDate.value = startOfSelectedDay
        _customEndDate.value = endOfSelectedDay
        _selectedDateRange.value = DateRangeFilter.CUSTOM
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun resetFilters() {
        _selectedFilter.value = TransactionFilter.ALL
        _selectedDateRange.value = DateRangeFilter.ALL_TIME
        _customStartDate.value = null
        _customEndDate.value = null
        _searchQuery.value = ""
    }

    fun restoreTransaction(transaction: Transaction, onRestored: () -> Unit = {}) {
        viewModelScope.launch {
            addTransactionUseCase(transaction)
            onRestored()
        }
    }

    companion object {
        fun filterAndGroupTransactions(
            allTransactions: List<Transaction>,
            params: TransactionFilterParams
        ): TransactionsUiState {
            val totalCount: Int = allTransactions.size

            // 1. Filter by Type
            val typeFiltered: List<Transaction> = when (params.filter) {
                TransactionFilter.ALL -> allTransactions
                TransactionFilter.EXPENSE -> allTransactions.filter { tx -> tx.type == TransactionType.EXPENSE }
                TransactionFilter.INCOME -> allTransactions.filter { tx -> tx.type == TransactionType.INCOME }
            }

            // 2. Filter by Date Range
            val dateFiltered: List<Transaction> = when (params.dateRange) {
                DateRangeFilter.ALL_TIME -> typeFiltered
                DateRangeFilter.TODAY -> {
                    val start = DateTimeUtils.getStartOfDayEpoch(LocalDate.now())
                    val end = DateTimeUtils.getEndOfDayEpoch(LocalDate.now())
                    typeFiltered.filter { tx -> tx.timestamp in start..end }
                }
                DateRangeFilter.THIS_WEEK -> {
                    val start = DateTimeUtils.getStartOfWeekEpoch(LocalDate.now())
                    val end = DateTimeUtils.getEndOfWeekEpoch(LocalDate.now())
                    typeFiltered.filter { tx -> tx.timestamp in start..end }
                }
                DateRangeFilter.THIS_MONTH -> {
                    val start = DateTimeUtils.getStartOfMonthEpoch(YearMonth.now())
                    val end = DateTimeUtils.getEndOfMonthEpoch(YearMonth.now())
                    typeFiltered.filter { tx -> tx.timestamp in start..end }
                }
                DateRangeFilter.CUSTOM -> {
                    if (params.customStartDate != null && params.customEndDate != null) {
                        typeFiltered.filter { tx -> tx.timestamp in params.customStartDate..params.customEndDate }
                    } else {
                        typeFiltered
                    }
                }
            }

            // 3. Filter by Search Query
            val searchFiltered: List<Transaction> = if (params.searchQuery.isBlank()) {
                dateFiltered
            } else {
                val q = params.searchQuery.trim().lowercase()
                dateFiltered.filter { tx: Transaction ->
                    tx.category.name.lowercase().contains(q) ||
                    (tx.note?.lowercase()?.contains(q) == true) ||
                    tx.paymentMethod.displayName.lowercase().contains(q) ||
                    tx.amount.format().lowercase().contains(q) ||
                    (tx.amount.subunits / 100.0).toString().contains(q)
                }
            }

            // 4. Calculate Live Summary for Filtered Results
            val filteredExpense: Long = searchFiltered
                .filter { tx -> tx.type == TransactionType.EXPENSE }
                .sumOf { tx -> tx.amount.subunits }
            val filteredIncome: Long = searchFiltered
                .filter { tx -> tx.type == TransactionType.INCOME }
                .sumOf { tx -> tx.amount.subunits }
            val netBalance: Long = filteredIncome - filteredExpense

            val summary = TransactionSummaryHeader(
                totalIncome = filteredIncome,
                totalExpense = filteredExpense,
                netBalance = netBalance,
                transactionCount = searchFiltered.size
            )

            // 5. Group by Month and Day (Sorted newest first)
            val sortedTransactions: List<Transaction> = searchFiltered.sortedWith(
                compareByDescending<Transaction> { tx -> tx.timestamp }.thenByDescending { tx -> tx.id }
            )

            val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            val monthGroups: List<MonthlyTransactionGroup> = sortedTransactions
                .groupBy { tx: Transaction ->
                    val localDate = Instant.ofEpochMilli(tx.timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    localDate.format(monthFormatter)
                }
                .map { entry ->
                    val monthStr = entry.key
                    val monthTxList: List<Transaction> = entry.value

                    val monthExpense: Long = monthTxList
                        .filter { tx -> tx.type == TransactionType.EXPENSE }
                        .sumOf { tx -> tx.amount.subunits }
                    val monthIncome: Long = monthTxList
                        .filter { tx -> tx.type == TransactionType.INCOME }
                        .sumOf { tx -> tx.amount.subunits }

                    val dailyGroups: List<DailyTransactionGroup> = monthTxList
                        .groupBy { tx: Transaction -> DateTimeUtils.formatDate(tx.timestamp) }
                        .map { dayEntry ->
                            val dateStr = dayEntry.key
                            val dayTxList: List<Transaction> = dayEntry.value

                            val dayExpense: Long = dayTxList
                                .filter { tx -> tx.type == TransactionType.EXPENSE }
                                .sumOf { tx -> tx.amount.subunits }
                            val dayIncome: Long = dayTxList
                                .filter { tx -> tx.type == TransactionType.INCOME }
                                .sumOf { tx -> tx.amount.subunits }

                            DailyTransactionGroup(
                                dateHeader = dateStr,
                                totalExpense = dayExpense,
                                totalIncome = dayIncome,
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

            val isFilterActive: Boolean = params.filter != TransactionFilter.ALL ||
                params.dateRange != DateRangeFilter.ALL_TIME ||
                params.searchQuery.isNotBlank()

            return TransactionsUiState(
                groups = monthGroups,
                summary = summary,
                totalTransactionsCount = totalCount,
                selectedFilter = params.filter,
                selectedDateRange = params.dateRange,
                customStartDate = params.customStartDate,
                customEndDate = params.customEndDate,
                searchQuery = params.searchQuery,
                isLoading = false,
                isFilterActive = isFilterActive
            )
        }
    }

    class Factory(
        private val getTransactionsUseCase: GetTransactionsUseCase,
        private val addTransactionUseCase: AddTransactionUseCase,
        private val initialFilter: TransactionFilter = TransactionFilter.ALL,
        private val initialSearchQuery: String = ""
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionsViewModel(
                getTransactionsUseCase,
                addTransactionUseCase,
                initialFilter,
                initialSearchQuery
            ) as T
        }
    }
}
