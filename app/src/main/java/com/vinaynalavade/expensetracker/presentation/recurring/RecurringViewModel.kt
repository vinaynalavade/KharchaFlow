package com.vinaynalavade.expensetracker.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.RecurringTransaction
import com.vinaynalavade.expensetracker.domain.usecase.DeleteRecurringTransactionUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetCategoriesUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetRecurringTransactionsUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SaveRecurringTransactionUseCase
import com.vinaynalavade.expensetracker.presentation.components.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecurringScreenData(
    val items: List<RecurringTransaction>,
    val categories: List<Category>
)

class RecurringViewModel(
    private val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    private val saveRecurringTransactionUseCase: SaveRecurringTransactionUseCase,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    val uiState: StateFlow<UiState<RecurringScreenData>> = combine(
        getRecurringTransactionsUseCase(),
        getCategoriesUseCase()
    ) { items, categories ->
        UiState.Success(RecurringScreenData(items = items, categories = categories))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    fun saveRecurring(item: RecurringTransaction, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (saveRecurringTransactionUseCase(item)) {
                is AppResult.Success -> onSuccess()
                is AppResult.Error -> {}
            }
        }
    }

    fun toggleEnabled(item: RecurringTransaction, isEnabled: Boolean) {
        viewModelScope.launch {
            saveRecurringTransactionUseCase(item.copy(isEnabled = isEnabled))
        }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch {
            deleteRecurringTransactionUseCase(id)
        }
    }

    class Factory(
        private val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
        private val saveRecurringTransactionUseCase: SaveRecurringTransactionUseCase,
        private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
        private val getCategoriesUseCase: GetCategoriesUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecurringViewModel(
                getRecurringTransactionsUseCase,
                saveRecurringTransactionUseCase,
                deleteRecurringTransactionUseCase,
                getCategoriesUseCase
            ) as T
        }
    }
}
