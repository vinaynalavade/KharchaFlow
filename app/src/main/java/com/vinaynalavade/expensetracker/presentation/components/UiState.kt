package com.vinaynalavade.expensetracker.presentation.components

/**
 * Standard UI state model for screens.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}
