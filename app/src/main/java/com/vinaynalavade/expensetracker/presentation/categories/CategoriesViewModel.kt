package com.vinaynalavade.expensetracker.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import com.vinaynalavade.expensetracker.domain.usecase.DeleteCategoryUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetCategoriesUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SaveCategoryUseCase
import com.vinaynalavade.expensetracker.presentation.components.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _selectedType = MutableStateFlow(TransactionType.EXPENSE)
    val selectedType: StateFlow<TransactionType> = _selectedType.asStateFlow()

    private val _dialogError = MutableStateFlow<String?>(null)
    val dialogError: StateFlow<String?> = _dialogError.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<List<Category>>> = _selectedType
        .flatMapLatest { type ->
            getCategoriesUseCase.getByType(type)
        }
        .map { categories ->
            if (categories.isEmpty()) {
                UiState.Empty
            } else {
                UiState.Success(categories)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )

    fun onTypeSelected(type: TransactionType) {
        _selectedType.value = type
        _dialogError.value = null
    }

    fun saveCategory(
        name: String,
        iconName: String,
        colorHex: String,
        type: TransactionType,
        id: Long,
        isDefault: Boolean = false,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _dialogError.value = null
            val category = Category(
                id = id,
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                type = type,
                isDefault = isDefault
            )

            when (val result = saveCategoryUseCase(category)) {
                is AppResult.Success -> {
                    onSuccess()
                }
                is AppResult.Error -> {
                    _dialogError.value = result.error.message
                }
            }
        }
    }

    fun deleteCategory(categoryId: Long, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = deleteCategoryUseCase(categoryId)) {
                is AppResult.Success -> {}
                is AppResult.Error -> onError(result.error.message)
            }
        }
    }

    fun clearDialogError() {
        _dialogError.value = null
    }

    class Factory(
        private val getCategoriesUseCase: GetCategoriesUseCase,
        private val saveCategoryUseCase: SaveCategoryUseCase,
        private val deleteCategoryUseCase: DeleteCategoryUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoriesViewModel(
                getCategoriesUseCase,
                saveCategoryUseCase,
                deleteCategoryUseCase
            ) as T
        }
    }
}
