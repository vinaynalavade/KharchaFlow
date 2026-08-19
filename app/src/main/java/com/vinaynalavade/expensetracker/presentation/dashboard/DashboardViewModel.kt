package com.vinaynalavade.expensetracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.usecase.GetFinancialSummaryUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val summary: FinancialSummary = FinancialSummary.EMPTY,
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class DashboardViewModel(
    getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
    getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        getFinancialSummaryUseCase(),
        getTransactionsUseCase.getRecent(5)
    ) { summary, recentList ->
        DashboardUiState(
            summary = summary,
            recentTransactions = recentList,
            isLoading = false
        )
    }.catch { e ->
        emit(DashboardUiState(isLoading = false, error = e.message ?: "Failed to load dashboard"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    class Factory(
        private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
        private val getTransactionsUseCase: GetTransactionsUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(getFinancialSummaryUseCase, getTransactionsUseCase) as T
        }
    }
}
