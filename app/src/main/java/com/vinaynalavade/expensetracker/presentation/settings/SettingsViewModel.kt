package com.vinaynalavade.expensetracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.notification.DailyReminderScheduler
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.usecase.GetUserPreferencesUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetCurrencyUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetDailyReminderUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetOpeningBalanceUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    getUserPreferencesUseCase: GetUserPreferencesUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setCurrencyUseCase: SetCurrencyUseCase,
    private val setOpeningBalanceUseCase: SetOpeningBalanceUseCase,
    private val setDailyReminderUseCase: SetDailyReminderUseCase,
    private val dailyReminderScheduler: DailyReminderScheduler
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = getUserPreferencesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

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

    fun onDailyReminderToggled(enabled: Boolean) {
        val currentHour = userPreferences.value.dailyReminderHour
        val currentMinute = userPreferences.value.dailyReminderMinute
        viewModelScope.launch {
            setDailyReminderUseCase(enabled, currentHour, currentMinute)
            if (enabled) {
                dailyReminderScheduler.schedule(currentHour, currentMinute)
            } else {
                dailyReminderScheduler.cancel()
            }
        }
    }

    fun onReminderTimeSelected(hour: Int, minute: Int) {
        val isEnabled = userPreferences.value.dailyReminderEnabled
        viewModelScope.launch {
            setDailyReminderUseCase(isEnabled, hour, minute)
            if (isEnabled) {
                dailyReminderScheduler.schedule(hour, minute)
            }
        }
    }

    class Factory(
        private val getUserPreferencesUseCase: GetUserPreferencesUseCase,
        private val setThemeModeUseCase: SetThemeModeUseCase,
        private val setCurrencyUseCase: SetCurrencyUseCase,
        private val setOpeningBalanceUseCase: SetOpeningBalanceUseCase,
        private val setDailyReminderUseCase: SetDailyReminderUseCase,
        private val dailyReminderScheduler: DailyReminderScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                getUserPreferencesUseCase,
                setThemeModeUseCase,
                setCurrencyUseCase,
                setOpeningBalanceUseCase,
                setDailyReminderUseCase,
                dailyReminderScheduler
            ) as T
        }
    }
}
