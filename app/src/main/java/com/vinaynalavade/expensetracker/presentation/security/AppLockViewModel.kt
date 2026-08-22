package com.vinaynalavade.expensetracker.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.security.AppLockManager
import com.vinaynalavade.expensetracker.core.security.PinVerificationResult
import com.vinaynalavade.expensetracker.core.security.SecurePinManager
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.usecase.ChangePinUseCase
import com.vinaynalavade.expensetracker.domain.usecase.DisableAppLockUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetUserPreferencesUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SavePinUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetAppLockEnabledUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetBiometricEnabledUseCase
import com.vinaynalavade.expensetracker.domain.usecase.VerifyPinUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AppLockUiEvent {
    data class ShowMessage(val message: String) : AppLockUiEvent()
    data object UnlockSuccess : AppLockUiEvent()
    data object SetupSuccess : AppLockUiEvent()
    data object ChangePinSuccess : AppLockUiEvent()
    data object DisableSuccess : AppLockUiEvent()
}

class AppLockViewModel(
    getUserPreferencesUseCase: GetUserPreferencesUseCase,
    private val appLockManager: AppLockManager,
    private val securePinManager: SecurePinManager,
    private val verifyPinUseCase: VerifyPinUseCase,
    private val savePinUseCase: SavePinUseCase,
    private val changePinUseCase: ChangePinUseCase,
    private val setAppLockEnabledUseCase: SetAppLockEnabledUseCase,
    private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
    private val disableAppLockUseCase: DisableAppLockUseCase
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = getUserPreferencesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _eventFlow = MutableSharedFlow<AppLockUiEvent>()
    val eventFlow: SharedFlow<AppLockUiEvent> = _eventFlow.asSharedFlow()

    // Unlock Screen State
    private val _enteredPin = MutableStateFlow("")
    val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lockoutSeconds = MutableStateFlow(0L)
    val lockoutSeconds: StateFlow<Long> = _lockoutSeconds.asStateFlow()

    private var countdownJob: Job? = null

    init {
        checkLockout()
    }

    fun checkLockout() {
        val remaining = securePinManager.getLockoutSecondsRemaining()
        if (remaining > 0) {
            startLockoutCountdown(remaining)
        } else {
            _lockoutSeconds.value = 0L
        }
    }

    private fun startLockoutCountdown(seconds: Long) {
        countdownJob?.cancel()
        _lockoutSeconds.value = seconds
        _errorMessage.value = "Too many incorrect attempts. Try again in $seconds seconds."
        countdownJob = viewModelScope.launch {
            var current = seconds
            while (current > 0) {
                _lockoutSeconds.value = current
                _errorMessage.value = "Too many incorrect attempts. Try again in $current seconds."
                delay(1000)
                current--
            }
            _lockoutSeconds.value = 0L
            _errorMessage.value = null
        }
    }

    fun onPinDigit(digit: Char) {
        if (_lockoutSeconds.value > 0) return
        if (_enteredPin.value.length < 4) {
            val newPin = _enteredPin.value + digit
            _enteredPin.value = newPin
            _errorMessage.value = null
            if (newPin.length == 4) {
                verifyUnlockPin(newPin)
            }
        }
    }

    fun onPinDelete() {
        if (_enteredPin.value.isNotEmpty()) {
            _enteredPin.value = _enteredPin.value.dropLast(1)
            _errorMessage.value = null
        }
    }

    fun onPinClear() {
        _enteredPin.value = ""
        _errorMessage.value = null
    }

    private fun verifyUnlockPin(pin: String) {
        when (val result = verifyPinUseCase(pin)) {
            is PinVerificationResult.Success -> {
                _enteredPin.value = ""
                _errorMessage.value = null
                viewModelScope.launch {
                    _eventFlow.emit(AppLockUiEvent.UnlockSuccess)
                }
            }
            is PinVerificationResult.Incorrect -> {
                _enteredPin.value = ""
                if (result.remainingAttempts > 0) {
                    _errorMessage.value = "Incorrect PIN. $result.remainingAttempts attempts remaining."
                } else {
                    _errorMessage.value = "Incorrect PIN. Please try again."
                }
            }
            is PinVerificationResult.LockedOut -> {
                _enteredPin.value = ""
                startLockoutCountdown(result.secondsRemaining)
            }
        }
    }

    fun onBiometricSuccess() {
        appLockManager.unlock()
        _enteredPin.value = ""
        _errorMessage.value = null
        viewModelScope.launch {
            _eventFlow.emit(AppLockUiEvent.UnlockSuccess)
        }
    }

    fun completeSetup(pin: String, enableBiometric: Boolean) {
        viewModelScope.launch {
            val saveResult = savePinUseCase(pin)
            if (saveResult is AppResult.Success) {
                setAppLockEnabledUseCase(true)
                setBiometricEnabledUseCase(enableBiometric)
                _eventFlow.emit(AppLockUiEvent.SetupSuccess)
            } else if (saveResult is AppResult.Error) {
                _eventFlow.emit(AppLockUiEvent.ShowMessage(saveResult.error.message))
            }
        }
    }

    fun updatePin(currentPin: String, newPin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = changePinUseCase(currentPin, newPin)
            if (result is AppResult.Success) {
                _eventFlow.emit(AppLockUiEvent.ChangePinSuccess)
                onSuccess()
            } else if (result is AppResult.Error) {
                onError(result.error.message)
            }
        }
    }

    fun disableAppLock(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = disableAppLockUseCase()
            if (result is AppResult.Success) {
                _eventFlow.emit(AppLockUiEvent.DisableSuccess)
                onSuccess()
            }
        }
    }

    class Factory(
        private val getUserPreferencesUseCase: GetUserPreferencesUseCase,
        private val appLockManager: AppLockManager,
        private val securePinManager: SecurePinManager,
        private val verifyPinUseCase: VerifyPinUseCase,
        private val savePinUseCase: SavePinUseCase,
        private val changePinUseCase: ChangePinUseCase,
        private val setAppLockEnabledUseCase: SetAppLockEnabledUseCase,
        private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
        private val disableAppLockUseCase: DisableAppLockUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppLockViewModel(
                getUserPreferencesUseCase,
                appLockManager,
                securePinManager,
                verifyPinUseCase,
                savePinUseCase,
                changePinUseCase,
                setAppLockEnabledUseCase,
                setBiometricEnabledUseCase,
                disableAppLockUseCase
            ) as T
        }
    }
}
