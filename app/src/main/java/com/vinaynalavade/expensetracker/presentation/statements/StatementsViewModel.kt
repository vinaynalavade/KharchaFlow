package com.vinaynalavade.expensetracker.presentation.statements

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.utils.DateTimeUtils
import com.vinaynalavade.expensetracker.core.utils.PdfUtils
import com.vinaynalavade.expensetracker.domain.model.StatementReport
import com.vinaynalavade.expensetracker.domain.usecase.GenerateStatementUseCase
import com.vinaynalavade.expensetracker.presentation.components.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class StatementPeriodOption(val title: String) {
    CURRENT_MONTH("Current Month"),
    PREVIOUS_MONTH("Previous Month"),
    LAST_3_MONTHS("Last 3 Months"),
    YEAR_TO_DATE("Year to Date"),
    ALL_TIME("All Time")
}

class StatementsViewModel(
    private val generateStatementUseCase: GenerateStatementUseCase
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(StatementPeriodOption.CURRENT_MONTH)
    val selectedPeriod: StateFlow<StatementPeriodOption> = _selectedPeriod.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<StatementReport>>(UiState.Loading)
    val uiState: StateFlow<UiState<StatementReport>> = _uiState.asStateFlow()

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    init {
        loadStatement()
    }

    fun onPeriodSelected(period: StatementPeriodOption) {
        _selectedPeriod.value = period
        loadStatement()
    }

    private fun loadStatement() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val (startEpoch, endEpoch, title) = calculatePeriodBounds(_selectedPeriod.value)
                val report = generateStatementUseCase(startEpoch, endEpoch, title)
                _uiState.value = UiState.Success(report)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to generate statement report.")
            }
        }
    }

    fun generateAndSharePdf(context: Context, onPdfReady: (File) -> Unit) {
        val currentReport = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val pdfFile = PdfUtils.generateStatementPdf(context, currentReport)
                _isGeneratingPdf.value = false
                onPdfReady(pdfFile)
            } catch (e: Exception) {
                _isGeneratingPdf.value = false
            }
        }
    }

    private fun calculatePeriodBounds(period: StatementPeriodOption): Triple<Long, Long, String> {
        val today = LocalDate.now()
        val monthFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

        return when (period) {
            StatementPeriodOption.CURRENT_MONTH -> {
                val ym = YearMonth.now()
                val start = ym.atDay(1)
                val end = ym.atEndOfMonth()
                Triple(
                    DateTimeUtils.getStartOfDayEpoch(start),
                    DateTimeUtils.getEndOfDayEpoch(end),
                    "${start.format(monthFormatter)} – ${end.format(monthFormatter)}"
                )
            }
            StatementPeriodOption.PREVIOUS_MONTH -> {
                val ym = YearMonth.now().minusMonths(1)
                val start = ym.atDay(1)
                val end = ym.atEndOfMonth()
                Triple(
                    DateTimeUtils.getStartOfDayEpoch(start),
                    DateTimeUtils.getEndOfDayEpoch(end),
                    "${start.format(monthFormatter)} – ${end.format(monthFormatter)}"
                )
            }
            StatementPeriodOption.LAST_3_MONTHS -> {
                val start = today.minusMonths(3)
                Triple(
                    DateTimeUtils.getStartOfDayEpoch(start),
                    DateTimeUtils.getEndOfDayEpoch(today),
                    "${start.format(monthFormatter)} – ${today.format(monthFormatter)}"
                )
            }
            StatementPeriodOption.YEAR_TO_DATE -> {
                val start = LocalDate.of(today.year, 1, 1)
                Triple(
                    DateTimeUtils.getStartOfDayEpoch(start),
                    DateTimeUtils.getEndOfDayEpoch(today),
                    "${start.format(monthFormatter)} – ${today.format(monthFormatter)}"
                )
            }
            StatementPeriodOption.ALL_TIME -> {
                Triple(
                    0L,
                    System.currentTimeMillis(),
                    "Complete Transaction Ledger"
                )
            }
        }
    }

    class Factory(
        private val generateStatementUseCase: GenerateStatementUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatementsViewModel(generateStatementUseCase) as T
        }
    }
}
