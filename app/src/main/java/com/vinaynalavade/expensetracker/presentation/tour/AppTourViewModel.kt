package com.vinaynalavade.expensetracker.presentation.tour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.domain.usecase.SetAppTourCompletedUseCase
import kotlinx.coroutines.launch

class AppTourViewModel(
    private val setAppTourCompletedUseCase: SetAppTourCompletedUseCase
) : ViewModel() {

    fun onTourCompleted(onFinished: () -> Unit) {
        viewModelScope.launch {
            setAppTourCompletedUseCase(completed = true)
            onFinished()
        }
    }

    fun onTourSkipped(onFinished: () -> Unit) {
        viewModelScope.launch {
            setAppTourCompletedUseCase(completed = true)
            onFinished()
        }
    }

    class Factory(
        private val setAppTourCompletedUseCase: SetAppTourCompletedUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppTourViewModel(setAppTourCompletedUseCase) as T
        }
    }
}
