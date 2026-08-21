package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.domain.model.DailyReminderSettings
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DailyReminderSettingsTest {

    @Test
    fun testDefaultReminderValues() {
        val defaultSettings = DailyReminderSettings()
        assertFalse(defaultSettings.enabled)
        assertEquals(21, defaultSettings.hour)
        assertEquals(0, defaultSettings.minute)

        val defaultPrefs = UserPreferences()
        assertFalse(defaultPrefs.dailyReminderEnabled)
        assertEquals(21, defaultPrefs.dailyReminderHour)
        assertEquals(0, defaultPrefs.dailyReminderMinute)
    }

    @Test
    fun testCustomReminderSettings() {
        val custom = DailyReminderSettings(
            enabled = true,
            hour = 20,
            minute = 45
        )
        assertEquals(true, custom.enabled)
        assertEquals(20, custom.hour)
        assertEquals(45, custom.minute)

        val customPrefs = UserPreferences(
            dailyReminderEnabled = true,
            dailyReminderHour = 8,
            dailyReminderMinute = 30
        )
        assertEquals(true, customPrefs.dailyReminderEnabled)
        assertEquals(8, customPrefs.dailyReminderHour)
        assertEquals(30, customPrefs.dailyReminderMinute)
    }
}
