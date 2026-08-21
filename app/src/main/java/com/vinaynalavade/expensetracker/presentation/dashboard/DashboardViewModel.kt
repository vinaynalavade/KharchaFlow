package com.vinaynalavade.expensetracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.domain.model.CategoryAnalysisResult
import com.vinaynalavade.expensetracker.domain.model.FinancialSummary
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.usecase.GetCategoryAnalysisUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetFinancialSummaryUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class DashboardUiState(
    val summary: FinancialSummary = FinancialSummary.EMPTY,
    val recentTransactions: List<Transaction> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val categoryAnalysisType: TransactionType = TransactionType.EXPENSE,
    val categoryAnalysis: CategoryAnalysisResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
    getTransactionsUseCase: GetTransactionsUseCase,
    getCategoryAnalysisUseCase: GetCategoryAnalysisUseCase
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    private val _categoryAnalysisType = MutableStateFlow(TransactionType.EXPENSE)
    val categoryAnalysisType: StateFlow<TransactionType> = _categoryAnalysisType

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedMonth,
        _categoryAnalysisType
    ) { month, type ->
        month to type
    }.flatMapLatest { (month, type) ->
        combine(
            getFinancialSummaryUseCase(),
            getTransactionsUseCase.getRecent(5),
            getCategoryAnalysisUseCase(month, type)
        ) { summary, recentList, analysis ->
            DashboardUiState(
                summary = summary,
                recentTransactions = recentList,
                selectedMonth = month,
                categoryAnalysisType = type,
                categoryAnalysis = analysis,
                isLoading = false
            )
        }
    }.catch { e ->
        emit(DashboardUiState(isLoading = false, error = e.message ?: "Failed to load dashboard"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun onCategoryAnalysisTypeChange(type: TransactionType) {
        _categoryAnalysisType.value = type
    }

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
        private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
        private val getTransactionsUseCase: GetTransactionsUseCase,
        private val getCategoryAnalysisUseCase: GetCategoryAnalysisUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                getFinancialSummaryUseCase,
                getTransactionsUseCase,
                getCategoryAnalysisUseCase
            ) as T
        }
    }
}
