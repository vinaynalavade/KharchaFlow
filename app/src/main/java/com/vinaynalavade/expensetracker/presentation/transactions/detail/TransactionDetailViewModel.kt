package com.vinaynalavade.expensetracker.presentation.transactions.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.usecase.DeleteTransactionUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TransactionDetailUiState {
    data object Loading : TransactionDetailUiState
    data class Success(val transaction: Transaction) : TransactionDetailUiState
    data class Error(val message: String) : TransactionDetailUiState
}

class TransactionDetailViewModel(
    private val transactionId: Long,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransactionDetailUiState>(TransactionDetailUiState.Loading)
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            getTransactionByIdUseCase(transactionId).collectLatest { tx ->
                if (tx != null) {
                    _uiState.value = TransactionDetailUiState.Success(tx)
                } else {
                    _uiState.value = TransactionDetailUiState.Error("Transaction not found.")
                }
            }
        }
    }

    fun deleteTransaction(onSuccess: (Transaction) -> Unit) {
        val current = (_uiState.value as? TransactionDetailUiState.Success)?.transaction ?: return
        viewModelScope.launch {
            when (val result = deleteTransactionUseCase(transactionId)) {
                is AppResult.Success -> onSuccess(current)
                is AppResult.Error -> {
                    _uiState.update { TransactionDetailUiState.Error(result.error.message) }
                }
            }
        }
    }

    class Factory(
        private val transactionId: Long,
        private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
        private val deleteTransactionUseCase: DeleteTransactionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionDetailViewModel(
                transactionId,
                getTransactionByIdUseCase,
                deleteTransactionUseCase
            ) as T
        }
    }
}
