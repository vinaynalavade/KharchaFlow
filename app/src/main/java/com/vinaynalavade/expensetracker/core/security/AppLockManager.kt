package com.vinaynalavade.expensetracker.core.security

import com.vinaynalavade.expensetracker.domain.model.AppLockState
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized application security session and auto-lock lifecycle manager.
 */
class AppLockManager {

    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L

    fun unlock() {
        _isSessionUnlocked.value = true
    }

    fun lock() {
        _isSessionUnlocked.value = false
    }

    fun onAppBackgrounded() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppForegrounded(appLockEnabled: Boolean, autoLockDurationSeconds: Long) {
        if (!appLockEnabled) {
            _isSessionUnlocked.value = true
            return
        }

        if (!_isSessionUnlocked.value) {
            // Already locked
            return
        }

        val elapsedMillis = System.currentTimeMillis() - lastBackgroundTimestamp
        val timeoutMillis = autoLockDurationSeconds * 1000L

        if (autoLockDurationSeconds == 0L || (lastBackgroundTimestamp > 0L && elapsedMillis >= timeoutMillis)) {
            _isSessionUnlocked.value = false
        }
    }

    fun isLocked(userPreferences: UserPreferences): Boolean {
        return userPreferences.appLockEnabled && !_isSessionUnlocked.value
    }

    fun getState(userPreferences: UserPreferences): AppLockState {
        return when {
            !userPreferences.appLockEnabled -> AppLockState.Disabled
            _isSessionUnlocked.value -> AppLockState.Unlocked
            else -> AppLockState.Locked
        }
    }
}
