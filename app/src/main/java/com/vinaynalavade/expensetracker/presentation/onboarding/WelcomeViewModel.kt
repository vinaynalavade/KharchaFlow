package com.vinaynalavade.expensetracker.presentation.onboarding

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.google.GoogleAccountManager
import com.vinaynalavade.expensetracker.core.google.GoogleAuthVerificationResult
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.GoogleAccountInfo
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.domain.usecase.SaveConnectedGoogleAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WelcomeUiState {
    data object Idle : WelcomeUiState
    data class Loading(val message: String) : WelcomeUiState
    data class ConsentRequired(val consentIntent: Intent) : WelcomeUiState
    data class Error(val message: String) : WelcomeUiState
}

class WelcomeViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val googleAccountManager: GoogleAccountManager,
    private val saveConnectedGoogleAccountUseCase: SaveConnectedGoogleAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<WelcomeUiState>(WelcomeUiState.Idle)
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    private var pendingAccount: GoogleAccountInfo? = null

    fun getGoogleSignInIntent(): Intent {
        return googleAccountManager.getSignInIntent()
    }

    fun onGoogleSignInResult(data: Intent?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = WelcomeUiState.Loading("Signing in with Google Account...")
            when (val result = googleAccountManager.parseSignInResult(data)) {
                is AppResult.Success -> {
                    verifyAndCompleteOnboarding(result.data, onComplete)
                }
                is AppResult.Error -> {
                    _uiState.value = WelcomeUiState.Error(result.error.message)
                }
            }
        }
    }

    private suspend fun verifyAndCompleteOnboarding(account: GoogleAccountInfo, onComplete: () -> Unit) {
        _uiState.value = WelcomeUiState.Loading("Verifying Google Drive authorization...")
        when (val authResult = googleAccountManager.verifyDriveAuthorization(account)) {
            is GoogleAuthVerificationResult.Verified -> {
                pendingAccount = null
                saveConnectedGoogleAccountUseCase(authResult.account)
                userPreferencesRepository.setFirstLaunchCompleted()
                _uiState.value = WelcomeUiState.Idle
                onComplete()
            }
            is GoogleAuthVerificationResult.ConsentRequired -> {
                pendingAccount = authResult.account
                _uiState.value = WelcomeUiState.ConsentRequired(authResult.consentIntent)
            }
            is GoogleAuthVerificationResult.Error -> {
                // If drive verification fails due to offline/permission, save basic account anyway so local usage proceeds
                pendingAccount = null
                saveConnectedGoogleAccountUseCase(account)
                userPreferencesRepository.setFirstLaunchCompleted()
                _uiState.value = WelcomeUiState.Idle
                onComplete()
            }
        }
    }

    fun onConsentResult(resultCode: Int, onComplete: () -> Unit) {
        val account = pendingAccount
        if (account == null) {
            _uiState.value = WelcomeUiState.Idle
            return
        }

        if (resultCode == android.app.Activity.RESULT_OK) {
            viewModelScope.launch {
                pendingAccount = null
                saveConnectedGoogleAccountUseCase(account)
                userPreferencesRepository.setFirstLaunchCompleted()
                _uiState.value = WelcomeUiState.Idle
                onComplete()
            }
        } else {
            pendingAccount = null
            // Even if Drive consent is skipped, allow the user to continue locally
            viewModelScope.launch {
                saveConnectedGoogleAccountUseCase(account)
                userPreferencesRepository.setFirstLaunchCompleted()
                _uiState.value = WelcomeUiState.Idle
                onComplete()
            }
        }
    }

    fun onContinueLocally(onComplete: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.setFirstLaunchCompleted()
            _uiState.value = WelcomeUiState.Idle
            onComplete()
        }
    }

    fun clearError() {
        _uiState.value = WelcomeUiState.Idle
    }

    class Factory(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val googleAccountManager: GoogleAccountManager,
        private val saveConnectedGoogleAccountUseCase: SaveConnectedGoogleAccountUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WelcomeViewModel(
                userPreferencesRepository,
                googleAccountManager,
                saveConnectedGoogleAccountUseCase
            ) as T
        }
    }
}
