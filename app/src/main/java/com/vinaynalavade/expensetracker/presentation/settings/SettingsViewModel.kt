package com.vinaynalavade.expensetracker.presentation.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.google.GoogleAccountManager
import com.vinaynalavade.expensetracker.core.google.GoogleAuthVerificationResult
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.notification.DailyReminderScheduler
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.security.SecurePinManager
import com.vinaynalavade.expensetracker.domain.model.GoogleAccountInfo
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupState
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.domain.usecase.DisableAppLockUseCase
import com.vinaynalavade.expensetracker.domain.usecase.DisconnectGoogleAccountUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetGoogleBackupStateUseCase
import com.vinaynalavade.expensetracker.domain.usecase.GetUserPreferencesUseCase
import com.vinaynalavade.expensetracker.domain.usecase.RescheduleAllRemindersUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SaveConnectedGoogleAccountUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetAppLockEnabledUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetAutoLockDurationUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetBiometricEnabledUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetCurrencyUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetDailyReminderUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetHideContentInRecentsUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetOpeningBalanceUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.vinaynalavade.expensetracker.core.backup.BackupValidationResult
import com.vinaynalavade.expensetracker.core.backup.JsonBackupParser
import com.vinaynalavade.expensetracker.domain.model.RestoreEligibility
import com.vinaynalavade.expensetracker.domain.usecase.CheckRestoreEligibilityUseCase
import com.vinaynalavade.expensetracker.domain.usecase.DismissRestorePromptUseCase
import com.vinaynalavade.expensetracker.domain.usecase.PerformGoogleDriveBackupUseCase
import com.vinaynalavade.expensetracker.domain.usecase.PrepareGoogleDriveRestoreUseCase
import com.vinaynalavade.expensetracker.domain.usecase.RestoreBackupUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetAutomaticBackupUseCase
import com.vinaynalavade.expensetracker.domain.usecase.ValidateBackupUseCase

sealed interface AccountActionState {
    data object Idle : AccountActionState
    data class Loading(val message: String) : AccountActionState
    data class ConsentRequired(val consentIntent: Intent) : AccountActionState
    data class Message(val message: String, val isError: Boolean = false) : AccountActionState
}

class SettingsViewModel(
    getUserPreferencesUseCase: GetUserPreferencesUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setCurrencyUseCase: SetCurrencyUseCase,
    private val setOpeningBalanceUseCase: SetOpeningBalanceUseCase,
    private val setDailyReminderUseCase: SetDailyReminderUseCase,
    private val dailyReminderScheduler: DailyReminderScheduler,
    private val setAppLockEnabledUseCase: SetAppLockEnabledUseCase,
    private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
    private val setAutoLockDurationUseCase: SetAutoLockDurationUseCase,
    private val setHideContentInRecentsUseCase: SetHideContentInRecentsUseCase,
    private val disableAppLockUseCase: DisableAppLockUseCase,
    private val securePinManager: SecurePinManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val rescheduleAllRemindersUseCase: RescheduleAllRemindersUseCase,
    getGoogleBackupStateUseCase: GetGoogleBackupStateUseCase,
    private val disconnectGoogleAccountUseCase: DisconnectGoogleAccountUseCase,
    private val googleAccountManager: GoogleAccountManager,
    private val saveConnectedGoogleAccountUseCase: SaveConnectedGoogleAccountUseCase,
    private val setAutomaticBackupUseCase: SetAutomaticBackupUseCase,
    private val checkRestoreEligibilityUseCase: CheckRestoreEligibilityUseCase,
    private val dismissRestorePromptUseCase: DismissRestorePromptUseCase,
    private val performGoogleDriveBackupUseCase: PerformGoogleDriveBackupUseCase,
    private val prepareGoogleDriveRestoreUseCase: PrepareGoogleDriveRestoreUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val validateBackupUseCase: ValidateBackupUseCase
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = getUserPreferencesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val googleBackupState: StateFlow<GoogleBackupState> = getGoogleBackupStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GoogleBackupState.Disconnected
        )

    private val _accountActionState = MutableStateFlow<AccountActionState>(AccountActionState.Idle)
    val accountActionState: StateFlow<AccountActionState> = _accountActionState.asStateFlow()

    private val _restorePromptEligibility = MutableStateFlow<RestoreEligibility.Eligible?>(null)
    val restorePromptEligibility: StateFlow<RestoreEligibility.Eligible?> = _restorePromptEligibility.asStateFlow()

    private val _showReplaceConfirmation = MutableStateFlow(false)
    val showReplaceConfirmation: StateFlow<Boolean> = _showReplaceConfirmation.asStateFlow()

    private val _isManualBackupRunning = MutableStateFlow(false)
    val isManualBackupRunning: StateFlow<Boolean> = _isManualBackupRunning.asStateFlow()

    private val _isManualRestoreRunning = MutableStateFlow(false)
    val isManualRestoreRunning: StateFlow<Boolean> = _isManualRestoreRunning.asStateFlow()

    private var pendingGoogleAccount: GoogleAccountInfo? = null

    fun getGoogleSignInIntent(): Intent {
        return googleAccountManager.getSignInIntent()
    }

    fun onGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _accountActionState.value = AccountActionState.Loading("Signing in with Google Account...")
            when (val result = googleAccountManager.parseSignInResult(data)) {
                is AppResult.Success -> {
                    verifyAndConnectGoogleAccount(result.data)
                }
                is AppResult.Error -> {
                    pendingGoogleAccount = null
                    _accountActionState.value = AccountActionState.Message(result.error.message, isError = true)
                }
            }
        }
    }

    private suspend fun verifyAndConnectGoogleAccount(accountInfo: GoogleAccountInfo) {
        _accountActionState.value = AccountActionState.Loading("Verifying Google Drive authorization...")
        when (val authResult = googleAccountManager.verifyDriveAuthorization(accountInfo)) {
            is GoogleAuthVerificationResult.Verified -> {
                pendingGoogleAccount = null
                saveConnectedGoogleAccountUseCase(authResult.account)
                _accountActionState.value = AccountActionState.Message("Google Account connected successfully!")
                // Check if a cloud backup is available for this account
                checkForRestoreEligibility()
            }
            is GoogleAuthVerificationResult.ConsentRequired -> {
                pendingGoogleAccount = authResult.account
                _accountActionState.value = AccountActionState.ConsentRequired(authResult.consentIntent)
            }
            is GoogleAuthVerificationResult.Error -> {
                pendingGoogleAccount = null
                _accountActionState.value = AccountActionState.Message(authResult.message, isError = true)
            }
        }
    }

    fun onConsentResult(resultCode: Int) {
        val account = pendingGoogleAccount
        if (account == null) {
            _accountActionState.value = AccountActionState.Idle
            return
        }

        if (resultCode == android.app.Activity.RESULT_OK) {
            viewModelScope.launch {
                verifyAndConnectGoogleAccount(account)
            }
        } else {
            pendingGoogleAccount = null
            _accountActionState.value = AccountActionState.Message("Google Drive permission was not granted.", isError = true)
        }
    }

    fun checkForRestoreEligibility() {
        viewModelScope.launch {
            val eligibility = checkRestoreEligibilityUseCase()
            if (eligibility is RestoreEligibility.Eligible) {
                _restorePromptEligibility.value = eligibility
            }
        }
    }

    fun onAutomaticBackupToggled(enabled: Boolean) {
        viewModelScope.launch {
            val googleState = googleBackupState.value
            if (enabled && googleState !is GoogleBackupState.Connected) {
                _accountActionState.value = AccountActionState.Message(
                    "Please connect your Google account to enable automatic backup.",
                    isError = true
                )
                return@launch
            }
            setAutomaticBackupUseCase(enabled)
        }
    }

    fun onBackupNowClick() {
        if (_isManualBackupRunning.value) return
        viewModelScope.launch {
            val googleState = googleBackupState.value
            if (googleState !is GoogleBackupState.Connected) {
                _accountActionState.value = AccountActionState.Message(
                    "Please connect a Google account before backing up.",
                    isError = true
                )
                return@launch
            }

            _isManualBackupRunning.value = true
            _accountActionState.value = AccountActionState.Loading("Backing up data to Google Drive...")
            when (val result = performGoogleDriveBackupUseCase()) {
                is AppResult.Success -> {
                    _isManualBackupRunning.value = false
                    _accountActionState.value = AccountActionState.Message("Backup completed successfully to Google Drive!")
                }
                is AppResult.Error -> {
                    _isManualBackupRunning.value = false
                    _accountActionState.value = AccountActionState.Message(result.error.message, isError = true)
                }
            }
        }
    }

    fun onRestorePromptAccepted() {
        val prompt = _restorePromptEligibility.value ?: return
        if (prompt.hasExistingLocalData) {
            _showReplaceConfirmation.value = true
        } else {
            executeCloudRestore()
        }
    }

    fun onConfirmReplaceAndRestore() {
        _showReplaceConfirmation.value = false
        executeCloudRestore()
    }

    fun onCancelReplaceConfirmation() {
        _showReplaceConfirmation.value = false
    }

    private fun executeCloudRestore() {
        val prompt = _restorePromptEligibility.value
        _restorePromptEligibility.value = null
        viewModelScope.launch {
            _isManualRestoreRunning.value = true
            _accountActionState.value = AccountActionState.Loading("Downloading backup from Google Drive...")
            when (val result = prepareGoogleDriveRestoreUseCase()) {
                is AppResult.Success -> {
                    val backupData = result.data
                    val validation = validateBackupUseCase(JsonBackupParser.toJson(backupData))
                    if (validation is BackupValidationResult.Valid) {
                        _accountActionState.value = AccountActionState.Loading("Restoring financial records...")
                        when (val restoreResult = restoreBackupUseCase(validation.backupData)) {
                            is AppResult.Success -> {
                                _isManualRestoreRunning.value = false
                                if (prompt != null) {
                                    dismissRestorePromptUseCase(prompt.metadata.modifiedTime)
                                }
                                _accountActionState.value = AccountActionState.Message("Cloud backup restored successfully!")
                            }
                            is AppResult.Error -> {
                                _isManualRestoreRunning.value = false
                                _accountActionState.value = AccountActionState.Message(restoreResult.error.message, isError = true)
                            }
                        }
                    } else if (validation is BackupValidationResult.Invalid) {
                        _isManualRestoreRunning.value = false
                        _accountActionState.value = AccountActionState.Message(validation.errorMessage, isError = true)
                    }
                }
                is AppResult.Error -> {
                    _isManualRestoreRunning.value = false
                    _accountActionState.value = AccountActionState.Message(result.error.message, isError = true)
                }
            }
        }
    }

    fun onDismissRestorePrompt(dontAskAgain: Boolean) {
        val prompt = _restorePromptEligibility.value
        _restorePromptEligibility.value = null
        if (prompt != null) {
            viewModelScope.launch {
                dismissRestorePromptUseCase(prompt.metadata.modifiedTime)
            }
        }
    }

    fun disconnectGoogleAccount() {
        viewModelScope.launch {
            _accountActionState.value = AccountActionState.Loading("Disconnecting Google Account...")
            when (val result = disconnectGoogleAccountUseCase()) {
                is AppResult.Success -> {
                    _accountActionState.value = AccountActionState.Message("Google Account disconnected.")
                }
                is AppResult.Error -> {
                    _accountActionState.value = AccountActionState.Message(result.error.message, isError = true)
                }
            }
        }
    }

    fun clearAccountActionMessage() {
        _accountActionState.value = AccountActionState.Idle
    }

    fun onThemeModeSelected(themeMode: ThemeMode) {
        viewModelScope.launch {
            setThemeModeUseCase(themeMode)
        }
    }

    fun onCurrencySelected(currency: Currency) {
        viewModelScope.launch {
            setCurrencyUseCase(currency.code)
        }
    }

    fun onOpeningBalanceChanged(subunits: Long) {
        viewModelScope.launch {
            setOpeningBalanceUseCase(subunits)
        }
    }

    fun onNotificationsMasterToggled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsMasterEnabled(enabled)
            rescheduleAllRemindersUseCase()
        }
    }

    fun onDailyReminderToggled(enabled: Boolean) {
        val currentHour = userPreferences.value.dailyReminderHour
        val currentMinute = userPreferences.value.dailyReminderMinute
        viewModelScope.launch {
            setDailyReminderUseCase(enabled, currentHour, currentMinute)
            rescheduleAllRemindersUseCase()
        }
    }

    fun onReminderTimeSelected(hour: Int, minute: Int) {
        val isEnabled = userPreferences.value.dailyReminderEnabled
        viewModelScope.launch {
            setDailyReminderUseCase(isEnabled, hour, minute)
            if (isEnabled && userPreferences.value.notificationsMasterEnabled) {
                dailyReminderScheduler.schedule(hour, minute)
            }
        }
    }

    fun onBudgetAlertsToggled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBudgetAlertsEnabled(enabled)
            rescheduleAllRemindersUseCase()
        }
    }

    fun onMonthlyBudgetLimitChanged(subunits: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setMonthlyBudgetLimit(subunits)
        }
    }

    fun onRecurringRemindersToggled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setRecurringRemindersEnabled(enabled)
            rescheduleAllRemindersUseCase()
        }
    }

    fun onRecurringReminderAdvanceDaysSelected(days: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setRecurringReminderAdvanceDays(days)
        }
    }

    fun onSavingsGoalNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSavingsGoalNotificationsEnabled(enabled)
        }
    }

    fun onLanguageSelected(languageCode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setAppLanguage(languageCode)
        }
    }

    fun onProfileNameChanged(name: String?) {
        viewModelScope.launch {
            userPreferencesRepository.setProfileName(name)
        }
    }

    fun onProfileImageSelected(uriString: String?) {
        viewModelScope.launch {
            userPreferencesRepository.setProfileImageUri(uriString)
        }
    }

    fun onRemoveProfileImage() {
        viewModelScope.launch {
            userPreferencesRepository.setProfileImageUri(null)
        }
    }

    fun onBiometricToggled(enabled: Boolean) {
        viewModelScope.launch {
            setBiometricEnabledUseCase(enabled)
        }
    }

    fun onAutoLockDurationSelected(seconds: Long) {
        viewModelScope.launch {
            setAutoLockDurationUseCase(seconds)
        }
    }

    fun onHideContentInRecentsToggled(hide: Boolean) {
        viewModelScope.launch {
            setHideContentInRecentsUseCase(hide)
        }
    }

    fun onDefaultIncomeSourceSelected(source: PaymentMethod) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultIncomeSource(source)
        }
    }

    fun onDefaultExpenseSourceSelected(source: PaymentMethod) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultExpenseSource(source)
        }
    }

    fun verifyAndDisableAppLock(
        pin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = disableAppLockUseCase(pin)) {
                is AppResult.Success -> {
                    onSuccess()
                }
                is AppResult.Error -> {
                    onError(result.error.message)
                }
            }
        }
    }

    fun getLockoutSecondsRemaining(): Long {
        return securePinManager.getLockoutSecondsRemaining()
    }

    fun disableAppLock(onSuccess: () -> Unit) {
        viewModelScope.launch {
            disableAppLockUseCase()
            onSuccess()
        }
    }

    class Factory(
        private val getUserPreferencesUseCase: GetUserPreferencesUseCase,
        private val setThemeModeUseCase: SetThemeModeUseCase,
        private val setCurrencyUseCase: SetCurrencyUseCase,
        private val setOpeningBalanceUseCase: SetOpeningBalanceUseCase,
        private val setDailyReminderUseCase: SetDailyReminderUseCase,
        private val dailyReminderScheduler: DailyReminderScheduler,
        private val setAppLockEnabledUseCase: SetAppLockEnabledUseCase,
        private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
        private val setAutoLockDurationUseCase: SetAutoLockDurationUseCase,
        private val setHideContentInRecentsUseCase: SetHideContentInRecentsUseCase,
        private val disableAppLockUseCase: DisableAppLockUseCase,
        private val securePinManager: SecurePinManager,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val rescheduleAllRemindersUseCase: RescheduleAllRemindersUseCase,
        private val getGoogleBackupStateUseCase: GetGoogleBackupStateUseCase,
        private val disconnectGoogleAccountUseCase: DisconnectGoogleAccountUseCase,
        private val googleAccountManager: GoogleAccountManager,
        private val saveConnectedGoogleAccountUseCase: SaveConnectedGoogleAccountUseCase,
        private val setAutomaticBackupUseCase: SetAutomaticBackupUseCase,
        private val checkRestoreEligibilityUseCase: CheckRestoreEligibilityUseCase,
        private val dismissRestorePromptUseCase: DismissRestorePromptUseCase,
        private val performGoogleDriveBackupUseCase: PerformGoogleDriveBackupUseCase,
        private val prepareGoogleDriveRestoreUseCase: PrepareGoogleDriveRestoreUseCase,
        private val restoreBackupUseCase: RestoreBackupUseCase,
        private val validateBackupUseCase: ValidateBackupUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                getUserPreferencesUseCase,
                setThemeModeUseCase,
                setCurrencyUseCase,
                setOpeningBalanceUseCase,
                setDailyReminderUseCase,
                dailyReminderScheduler,
                setAppLockEnabledUseCase,
                setBiometricEnabledUseCase,
                setAutoLockDurationUseCase,
                setHideContentInRecentsUseCase,
                disableAppLockUseCase,
                securePinManager,
                userPreferencesRepository,
                rescheduleAllRemindersUseCase,
                getGoogleBackupStateUseCase,
                disconnectGoogleAccountUseCase,
                googleAccountManager,
                saveConnectedGoogleAccountUseCase,
                setAutomaticBackupUseCase,
                checkRestoreEligibilityUseCase,
                dismissRestorePromptUseCase,
                performGoogleDriveBackupUseCase,
                prepareGoogleDriveRestoreUseCase,
                restoreBackupUseCase,
                validateBackupUseCase
            ) as T
        }
    }
}
