package com.vinaynalavade.expensetracker.presentation.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.usecase.AddTransactionUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetCategoriesUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetTransactionByIdUseCase
import com.vinaynalavade.expensetracker.domain.usecase.UpdateTransactionUseCase
import com.vinaynalavade.expensetracker.domain.validation.TransactionValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddTransactionUiState(
    val transactionType: TransactionType,
    val isEditMode: Boolean = false,
    val editTransactionId: Long? = null,
    val originalCreatedAt: Long = System.currentTimeMillis(),
    val amountInput: String = "",
    val selectedCategory: Category? = null,
    val availableCategories: List<Category> = emptyList(),
    val selectedDateEpoch: Long = System.currentTimeMillis(),
    val note: String = "",
    val amountError: String? = null,
    val categoryError: String? = null,
    val generalError: String? = null,
    val isSaving: Boolean = false,
    val isSaveSuccess: Boolean = false
) {
    val hasUnsavedChanges: Boolean
        get() = amountInput.isNotBlank() || note.isNotBlank()

    val isFormValid: Boolean
        get() {
            val amountValid = TransactionValidator.validateAmount(amountInput).first
            val categoryValid = selectedCategory != null
            return amountValid && categoryValid
        }
}

class AddTransactionViewModel(
    private val transactionType: TransactionType,
    private val editTransactionId: Long?,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val currency: Currency = Currency.DEFAULT
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddTransactionUiState(
            transactionType = transactionType,
            isEditMode = editTransactionId != null,
            editTransactionId = editTransactionId
        )
    )
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        loadCategoriesAndInitialData()
    }

    private fun loadCategoriesAndInitialData() {
        viewModelScope.launch {
            // If editing, preload the transaction first
            if (editTransactionId != null) {
                val tx = getTransactionByIdUseCase(editTransactionId).firstOrNull()
                if (tx != null) {
                    _uiState.update {
                        it.copy(
                            transactionType = tx.type,
                            amountInput = tx.amount.toInputString(currency),
                            selectedCategory = tx.category,
                            selectedDateEpoch = tx.timestamp,
                            note = tx.note ?: "",
                            originalCreatedAt = tx.createdAt
                        )
                    }
                }
            }

            // Load categories for the active transaction type
            val typeToLoad = _uiState.value.transactionType
            getCategoriesUseCase.getByType(typeToLoad).collectLatest { categories ->
                _uiState.update { state ->
                    val selected = state.selectedCategory?.let { current ->
                        categories.find { it.id == current.id } ?: current
                    } ?: categories.firstOrNull()

                    state.copy(
                        availableCategories = categories,
                        selectedCategory = selected
                    )
                }
            }
        }
    }

    fun onAmountChange(newAmount: String) {
        _uiState.update {
            it.copy(
                amountInput = newAmount,
                amountError = null,
                generalError = null
            )
        }
    }

    fun onCategorySelect(category: Category) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                categoryError = null,
                generalError = null
            )
        }
    }

    fun onDateSelect(epochMillis: Long) {
        _uiState.update {
            it.copy(
                selectedDateEpoch = epochMillis,
                generalError = null
            )
        }
    }

    fun onNoteChange(newNote: String) {
        _uiState.update {
            it.copy(
                note = newNote,
                generalError = null
            )
        }
    }

    fun saveTransaction(onSuccess: (String) -> Unit) {
        val currentState = _uiState.value
        if (currentState.isSaving) return

        val validation = TransactionValidator.validateTransaction(
            amountInput = currentState.amountInput,
            category = currentState.selectedCategory,
            expectedType = currentState.transactionType,
            currency = currency
        )

        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    amountError = validation.amountError,
                    categoryError = validation.categoryError
                )
            }
            return
        }

        val amount = validation.parsedAmount ?: return
        val category = currentState.selectedCategory ?: return

        _uiState.update { it.copy(isSaving = true, generalError = null) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val transaction = Transaction(
                id = currentState.editTransactionId ?: 0L,
                amount = amount,
                type = currentState.transactionType,
                category = category,
                note = currentState.note.trim().ifBlank { null },
                timestamp = currentState.selectedDateEpoch,
                createdAt = if (currentState.isEditMode) currentState.originalCreatedAt else now,
                updatedAt = now
            )

            val result = if (currentState.isEditMode) {
                updateTransactionUseCase(transaction)
            } else {
                when (val insertResult = addTransactionUseCase(transaction)) {
                    is AppResult.Success -> AppResult.Success(Unit)
                    is AppResult.Error -> AppResult.Error(insertResult.error)
                }
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSaveSuccess = true
                        )
                    }
                    val msg = if (currentState.isEditMode) {
                        "Transaction updated successfully"
                    } else {
                        if (currentState.transactionType == TransactionType.INCOME) {
                            "Income of ${amount.format(currency)} recorded"
                        } else {
                            "Expense of ${amount.format(currency)} recorded"
                        }
                    }
                    onSuccess(msg)
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            generalError = result.error.message
                        )
                    }
                }
            }
        }
    }

    class Factory(
        private val transactionType: TransactionType = TransactionType.EXPENSE,
        private val editTransactionId: Long? = null,
        private val addTransactionUseCase: AddTransactionUseCase,
        private val updateTransactionUseCase: UpdateTransactionUseCase,
        private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
        private val getCategoriesUseCase: GetCategoriesUseCase,
        private val currency: Currency = Currency.DEFAULT
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddTransactionViewModel(
                transactionType = transactionType,
                editTransactionId = editTransactionId,
                addTransactionUseCase = addTransactionUseCase,
                updateTransactionUseCase = updateTransactionUseCase,
                getTransactionByIdUseCase = getTransactionByIdUseCase,
                getCategoriesUseCase = getCategoriesUseCase,
                currency = currency
            ) as T
        }
    }
}
