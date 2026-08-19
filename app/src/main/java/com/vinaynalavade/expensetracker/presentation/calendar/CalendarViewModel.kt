package com.vinaynalavade.expensetracker.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class DayTransactionSummary(
    val date: LocalDate,
    val incomeSubunits: Long = 0L,
    val expenseSubunits: Long = 0L,
    val transactions: List<Transaction> = emptyList()
) {
    val hasIncome: Boolean get() = incomeSubunits > 0L
    val hasExpense: Boolean get() = expenseSubunits > 0L
    val incomeAmount: Amount get() = Amount.fromSubunits(incomeSubunits)
    val expenseAmount: Amount get() = Amount.fromSubunits(expenseSubunits)
    val netChangeAmount: Amount get() = Amount.fromSubunits(incomeSubunits - expenseSubunits)
}

data class CalendarUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val daysInMonth: Map<LocalDate, DayTransactionSummary> = emptyMap(),
    val selectedDayTransactions: List<Transaction> = emptyList(),
    val selectedDayIncome: Amount = Amount.ZERO,
    val selectedDayExpense: Amount = Amount.ZERO,
    val selectedDayNetChange: Amount = Amount.ZERO,
    val isLoading: Boolean = false
)

class CalendarViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val uiState: StateFlow<CalendarUiState> = combine(
        getTransactionsUseCase(),
        _selectedMonth,
        _selectedDate
    ) { allTransactions, month, selectedDate ->
        val zoneId = ZoneId.systemDefault()
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        val startEpoch = DateTimeUtils.getStartOfDayEpoch(monthStart, zoneId)
        val endEpoch = DateTimeUtils.getEndOfDayEpoch(monthEnd, zoneId)

        // Filter transactions for the selected month
        val monthTransactions = allTransactions.filter { it.timestamp in startEpoch..endEpoch }

        // Group by local date
        val transactionsByDate = monthTransactions.groupBy { tx ->
            DateTimeUtils.epochToLocalDate(tx.timestamp, zoneId)
        }

        // Build DayTransactionSummary for every day in the month
        val daySummaries = mutableMapOf<LocalDate, DayTransactionSummary>()
        var currentDay = monthStart
        while (!currentDay.isAfter(monthEnd)) {
            val dayTxs = transactionsByDate[currentDay] ?: emptyList()
            var incomeSubunits = 0L
            var expenseSubunits = 0L
            for (tx in dayTxs) {
                if (tx.type == TransactionType.INCOME) {
                    incomeSubunits += tx.amount.subunits
                } else {
                    expenseSubunits += tx.amount.subunits
                }
            }
            daySummaries[currentDay] = DayTransactionSummary(
                date = currentDay,
                incomeSubunits = incomeSubunits,
                expenseSubunits = expenseSubunits,
                transactions = dayTxs.sortedByDescending { it.timestamp }
            )
            currentDay = currentDay.plusDays(1)
        }

        val selectedDaySummary = daySummaries[selectedDate] ?: DayTransactionSummary(selectedDate)

        CalendarUiState(
            selectedMonth = month,
            selectedDate = selectedDate,
            daysInMonth = daySummaries,
            selectedDayTransactions = selectedDaySummary.transactions,
            selectedDayIncome = selectedDaySummary.incomeAmount,
            selectedDayExpense = selectedDaySummary.expenseAmount,
            selectedDayNetChange = selectedDaySummary.netChangeAmount,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(isLoading = true)
    )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        if (YearMonth.from(date) != _selectedMonth.value) {
            _selectedMonth.value = YearMonth.from(date)
        }
    }

    fun onPreviousMonth() {
        val newMonth = _selectedMonth.value.minusMonths(1)
        _selectedMonth.value = newMonth
        // Adjust selected date to be within new month
        val currentDay = _selectedDate.value.dayOfMonth
        val maxDay = newMonth.lengthOfMonth()
        _selectedDate.value = newMonth.atDay(minOf(currentDay, maxDay))
    }

    fun onNextMonth() {
        val newMonth = _selectedMonth.value.plusMonths(1)
        _selectedMonth.value = newMonth
        val currentDay = _selectedDate.value.dayOfMonth
        val maxDay = newMonth.lengthOfMonth()
        _selectedDate.value = newMonth.atDay(minOf(currentDay, maxDay))
    }

    fun onGoToToday() {
        val today = LocalDate.now()
        _selectedMonth.value = YearMonth.from(today)
        _selectedDate.value = today
    }

    class Factory(
        private val getTransactionsUseCase: GetTransactionsUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalendarViewModel(getTransactionsUseCase) as T
        }
    }
}
