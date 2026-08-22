package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.security.AppLockManager
import com.vinaynalavade.expensetracker.domain.model.AppLockState
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockLifecycleTest {

    private lateinit var appLockManager: AppLockManager

    @Before
    fun setUp() {
        appLockManager = AppLockManager()
    }

    @Test
    fun testDefaultNewInstallationStateIsDisabledAndNotLocked() {
        val defaultPrefs = UserPreferences() // appLockEnabled = false
        assertFalse(defaultPrefs.appLockEnabled)
        assertFalse(appLockManager.isLocked(defaultPrefs))
        assertEquals(AppLockState.Disabled, appLockManager.getState(defaultPrefs))
    }

    @Test
    fun testUnlockAndLockManualTransitions() {
        val lockedPrefs = UserPreferences(appLockEnabled = true)

        // Initial state before unlock
        assertTrue(appLockManager.isLocked(lockedPrefs))
        assertEquals(AppLockState.Locked, appLockManager.getState(lockedPrefs))

        // Unlock
        appLockManager.unlock()
        assertFalse(appLockManager.isLocked(lockedPrefs))
        assertEquals(AppLockState.Unlocked, appLockManager.getState(lockedPrefs))

        // Lock
        appLockManager.lock()
        assertTrue(appLockManager.isLocked(lockedPrefs))
        assertEquals(AppLockState.Locked, appLockManager.getState(lockedPrefs))
    }

    @Test
    fun testAutoLockImmediatelyTimeout() {
        val prefs = UserPreferences(appLockEnabled = true, autoLockDurationSeconds = 0L)

        appLockManager.unlock()
        assertFalse(appLockManager.isLocked(prefs))

        // Background app
        appLockManager.onAppBackgrounded()

        // Return immediately
        appLockManager.onAppForegrounded(appLockEnabled = true, autoLockDurationSeconds = 0L)

        // Must be locked immediately
        assertTrue(appLockManager.isLocked(prefs))
        assertEquals(AppLockState.Locked, appLockManager.getState(prefs))
    }

    @Test
    fun testAutoLockWithGracePeriod() {
        val prefs30s = UserPreferences(appLockEnabled = true, autoLockDurationSeconds = 30L)

        appLockManager.unlock()
        assertFalse(appLockManager.isLocked(prefs30s))

        // Background app
        appLockManager.onAppBackgrounded()

        // Foreground within grace period (0ms elapsed in test)
        appLockManager.onAppForegrounded(appLockEnabled = true, autoLockDurationSeconds = 30L)

        // Should still be unlocked
        assertFalse(appLockManager.isLocked(prefs30s))
        assertEquals(AppLockState.Unlocked, appLockManager.getState(prefs30s))
    }

    @Test
    fun testAppLockDisabledIgnoresLifecycleEvents() {
        val disabledPrefs = UserPreferences(appLockEnabled = false)

        appLockManager.onAppBackgrounded()
        appLockManager.onAppForegrounded(appLockEnabled = false, autoLockDurationSeconds = 0L)

        assertFalse(appLockManager.isLocked(disabledPrefs))
        assertEquals(AppLockState.Disabled, appLockManager.getState(disabledPrefs))
    }
}
