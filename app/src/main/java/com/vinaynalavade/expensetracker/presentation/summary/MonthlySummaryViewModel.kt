package com.vinaynalavade.expensetracker.presentation.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.domain.model.MonthlyLedgerSummary
import com.vinaynalavade.expensetracker.domain.usecase.GetMonthlyLedgerUseCase
import com.vinaynalavade.expensetracker.presentation.components.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

class MonthlySummaryViewModel(
    private val getMonthlyLedgerUseCase: GetMonthlyLedgerUseCase
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<MonthlyLedgerSummary>> = _selectedMonth
        .flatMapLatest { yearMonth ->
            getMonthlyLedgerUseCase(yearMonth)
        }
        .map { ledgerSummary ->
            UiState.Success(ledgerSummary)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )

    fun onPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun onCurrentMonth() {
        _selectedMonth.value = YearMonth.now()
    }

    class Factory(
        private val getMonthlyLedgerUseCase: GetMonthlyLedgerUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MonthlySummaryViewModel(getMonthlyLedgerUseCase) as T
        }
    }
}
