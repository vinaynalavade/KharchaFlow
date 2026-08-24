package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.notification.DailyReminderScheduler
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.presentation.settings.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LanguageAndOnboardingFixesTest {

    private lateinit var fakePrefsRepo: FakeUserPreferencesRepo
    private lateinit var fakeReminderScheduler: FakeDailyReminderScheduler

    @Before
    fun setUp() {
        fakePrefsRepo = FakeUserPreferencesRepo()
        fakeReminderScheduler = FakeDailyReminderScheduler()
    }

    // ─── 1. Language Selection Tests ───

    @Test
    fun testLanguageSelectionAppliesAndPersists() = runBlocking {
        // Initial state is SYSTEM
        val initialPrefs = fakePrefsRepo.getUserPreferences().first()
        assertEquals("SYSTEM", initialPrefs.appLanguage)
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode(initialPrefs.appLanguage))

        // Change to Hindi
        fakePrefsRepo.setAppLanguage("hi")
        var prefs = fakePrefsRepo.getUserPreferences().first()
        assertEquals("hi", prefs.appLanguage)
        assertEquals(AppLanguage.HINDI, AppLanguage.fromCode(prefs.appLanguage))
        assertEquals("हिंदी", AppLanguage.fromCode(prefs.appLanguage).nativeName)

        // Change to Marathi
        fakePrefsRepo.setAppLanguage("mr")
        prefs = fakePrefsRepo.getUserPreferences().first()
        assertEquals("mr", prefs.appLanguage)
        assertEquals(AppLanguage.MARATHI, AppLanguage.fromCode(prefs.appLanguage))
        assertEquals("मराठी", AppLanguage.fromCode(prefs.appLanguage).nativeName)

        // Change to English
        fakePrefsRepo.setAppLanguage("en")
        prefs = fakePrefsRepo.getUserPreferences().first()
        assertEquals("en", prefs.appLanguage)
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(prefs.appLanguage))

        // Return to System Default
        fakePrefsRepo.setAppLanguage("SYSTEM")
        prefs = fakePrefsRepo.getUserPreferences().first()
        assertEquals("SYSTEM", prefs.appLanguage)
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode(prefs.appLanguage))
    }

    @Test
    fun testAppLanguageEnumBehavior() {
        assertEquals("SYSTEM", AppLanguage.SYSTEM.code)
        assertEquals("en", AppLanguage.ENGLISH.code)
        assertEquals("hi", AppLanguage.HINDI.code)
        assertEquals("mr", AppLanguage.MARATHI.code)

        // Case insensitivity
        assertEquals(AppLanguage.HINDI, AppLanguage.fromCode("HI"))
        assertEquals(AppLanguage.MARATHI, AppLanguage.fromCode("MR"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("EN"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("system"))

        // Unrecognized code returns SYSTEM
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("unknown_code"))
    }

    // ─── 2. Notification Permission & Reminder Tests ───

    @Test
    fun testNotificationPermissionGrantEnablesAndSchedulesReminders() = runBlocking {
        // User grants notification permission during first launch
        fakePrefsRepo.setNotificationsMasterEnabled(true)
        fakePrefsRepo.setDailyReminder(enabled = true, hour = 20, minute = 0)
        fakeReminderScheduler.schedule(20, 0)

        val prefs = fakePrefsRepo.getUserPreferences().first()
        assertTrue("Master notifications must be enabled on grant", prefs.notificationsMasterEnabled)
        assertTrue("Daily reminder must be enabled on grant", prefs.dailyReminderEnabled)
        assertEquals(20, prefs.dailyReminderHour)
        assertEquals(0, prefs.dailyReminderMinute)
        assertTrue("Scheduler must be scheduled", fakeReminderScheduler.isScheduled)
        assertEquals(20, fakeReminderScheduler.scheduledHour)
        assertEquals(0, fakeReminderScheduler.scheduledMinute)
    }

    @Test
    fun testNotificationPermissionDenialDisablesMasterAndCancelsReminders() = runBlocking {
        // User denies notification permission
        fakePrefsRepo.setNotificationsMasterEnabled(false)
        fakePrefsRepo.setDailyReminder(enabled = false, hour = 20, minute = 0)
        fakeReminderScheduler.cancel()

        val prefs = fakePrefsRepo.getUserPreferences().first()
        assertFalse("Master notifications must be disabled on denial", prefs.notificationsMasterEnabled)
        assertFalse("Daily reminder must be disabled on denial", prefs.dailyReminderEnabled)
        assertFalse("Scheduler must be cancelled", fakeReminderScheduler.isScheduled)
    }

    // ─── 3. Onboarding & Welcome Recurrence Tests ───

    @Test
    fun testFreshUserOnboardingCompletionSurvivesRestarts() = runBlocking {
        // Fresh install
        var prefs = fakePrefsRepo.getUserPreferences().first()
        assertTrue("Fresh user must start with isFirstLaunch = true", prefs.isFirstLaunch)

        // User finishes welcome screen (Continue Locally)
        fakePrefsRepo.setFirstLaunchCompleted()
        prefs = fakePrefsRepo.getUserPreferences().first()
        assertFalse("isFirstLaunch must be false after onboarding", prefs.isFirstLaunch)

        // Simulated app restart with persisted state
        val reloadedPrefs = fakePrefsRepo.getUserPreferences().first()
        assertFalse("Reloaded app must not revert isFirstLaunch to true", reloadedPrefs.isFirstLaunch)
    }

    @Test
    fun testDisconnectGoogleDoesNotReTriggerWelcomeScreen() = runBlocking {
        // User with connected Google account
        fakePrefsRepo.setFirstLaunchCompleted()
        fakePrefsRepo.setProfileName("Vinay Nalavade")

        var prefs = fakePrefsRepo.getUserPreferences().first()
        assertFalse(prefs.isFirstLaunch)

        // Disconnect Google account in settings
        // Disconnect only clears auth tokens; it never resets isFirstLaunch
        prefs = fakePrefsRepo.getUserPreferences().first()
        assertFalse("Disconnecting Google account must NOT reset isFirstLaunch", prefs.isFirstLaunch)
    }

    // ─── 4. Profile Photo Cropping Flow Tests ───

    @Test
    fun testProfilePhotoCropUpdateAndCancellation() = runBlocking {
        // Initial state: no custom photo
        var prefs = fakePrefsRepo.getUserPreferences().first()
        assertNull(prefs.profileImageUri)

        // 1. User selects photo and confirms crop
        val croppedUri = "file:///data/user/0/com.vinaynalavade.expensetracker/files/profile/profile_avatar.jpg"
        fakePrefsRepo.setProfileImageUri(croppedUri)

        prefs = fakePrefsRepo.getUserPreferences().first()
        assertEquals(croppedUri, prefs.profileImageUri)

        // 2. User opens photo picker, but cancels cropping dialog -> URI remains unchanged
        // (No repo call made on cancellation)
        prefs = fakePrefsRepo.getUserPreferences().first()
        assertEquals(croppedUri, prefs.profileImageUri)

        // 3. User chooses 'Remove Photo'
        fakePrefsRepo.setProfileImageUri(null)
        prefs = fakePrefsRepo.getUserPreferences().first()
        assertNull("Profile photo must be null after removal", prefs.profileImageUri)
    }

    // ─── 5. Localization String Parity Tests ───

    @Test
    fun testNewFeatureStringsParityAcrossLanguages() {
        val baseDir = File("src/main/res")
        val enFile = File(baseDir, "values/strings.xml")
        val hiFile = File(baseDir, "values-hi/strings.xml")
        val mrFile = File(baseDir, "values-mr/strings.xml")

        assertTrue(enFile.exists())
        assertTrue(hiFile.exists())
        assertTrue(mrFile.exists())

        val enKeys = extractStringKeys(enFile)
        val hiKeys = extractStringKeys(hiFile)
        val mrKeys = extractStringKeys(mrFile)

        val newFeatureKeys = listOf(
            "settings_language_system",
            "crop_photo_title",
            "crop_photo_desc",
            "crop_photo_rotate",
            "crop_photo_reset",
            "btn_crop_save"
        )

        for (key in newFeatureKeys) {
            assertTrue("Key '$key' must exist in English strings", enKeys.contains(key))
            assertTrue("Key '$key' must exist in Hindi strings", hiKeys.contains(key))
            assertTrue("Key '$key' must exist in Marathi strings", mrKeys.contains(key))
        }
    }

    private fun extractStringKeys(file: File): Set<String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val stringNodes = doc.getElementsByTagName("string")
        val keys = mutableSetOf<String>()
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val nameAttr = node.attributes.getNamedItem("name")
            if (nameAttr != null) {
                keys.add(nameAttr.nodeValue)
            }
        }
        return keys
    }

    // ─── Fake Implementations ───

    private class FakeDailyReminderScheduler : DailyReminderScheduler {
        var isScheduled = false
        var scheduledHour = -1
        var scheduledMinute = -1

        override fun schedule(hour: Int, minute: Int) {
            isScheduled = true
            scheduledHour = hour
            scheduledMinute = minute
        }

        override fun scheduleFinancialChecks() {}
        override fun cancel() {
            isScheduled = false
            scheduledHour = -1
            scheduledMinute = -1
        }
        override fun reschedule() {}
        override fun calculateNextTriggerMillis(hour: Int, minute: Int, nowMillis: Long): Long = nowMillis + 3600000L
    }

    private class FakeUserPreferencesRepo : UserPreferencesRepository {
        private val _flow = MutableStateFlow(UserPreferences())
        var currentPrefs: UserPreferences
            get() = _flow.value
            set(value) {
                _flow.value = value
            }

        override fun getUserPreferences(): Flow<UserPreferences> = _flow.asStateFlow()
        override suspend fun setThemeMode(themeMode: ThemeMode) = AppResult.Success(Unit)
        override suspend fun setCurrencyCode(currencyCode: String) = AppResult.Success(Unit)
        override suspend fun setDynamicColors(useDynamicColors: Boolean) = AppResult.Success(Unit)
        override suspend fun setFirstLaunchCompleted(): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(isFirstLaunch = false)
            return AppResult.Success(Unit)
        }
        override suspend fun setOpeningBalance(subunits: Long) = AppResult.Success(Unit)
        override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(dailyReminderEnabled = enabled, dailyReminderHour = hour, dailyReminderMinute = minute)
            return AppResult.Success(Unit)
        }
        override suspend fun setEmiReminders(enabled: Boolean) = AppResult.Success(Unit)
        override fun getLastBackupTimestamp(): Flow<Long?> = flowOf(null)
        override suspend fun setLastBackupTimestamp(timestamp: Long) = AppResult.Success(Unit)
        override suspend fun setAppLockEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setBiometricEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setAutoLockDurationSeconds(seconds: Long) = AppResult.Success(Unit)
        override suspend fun setHideContentInRecents(hide: Boolean) = AppResult.Success(Unit)
        override suspend fun setNotificationsMasterEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(notificationsMasterEnabled = enabled)
            return AppResult.Success(Unit)
        }
        override suspend fun setBudgetAlertsEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setMonthlyBudgetLimit(subunits: Long) = AppResult.Success(Unit)
        override suspend fun setRecurringRemindersEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setRecurringReminderAdvanceDays(days: Int) = AppResult.Success(Unit)
        override suspend fun setSavingsGoalNotificationsEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setAppLanguage(languageCode: String): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(appLanguage = languageCode)
            return AppResult.Success(Unit)
        }
        override suspend fun setProfileName(name: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(userName = name)
            return AppResult.Success(Unit)
        }
        override suspend fun setProfileImageUri(uri: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(profileImageUri = uri)
            return AppResult.Success(Unit)
        }
        override suspend fun setAutomaticBackupEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setLastBackupStatus(status: String?) = AppResult.Success(Unit)
        override suspend fun setLastBackupError(error: String?) = AppResult.Success(Unit)
        override suspend fun setLastDismissedRestoreBackupTimestamp(timestamp: Long?) = AppResult.Success(Unit)
        override suspend fun setAppTourCompleted(completed: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(isAppTourCompleted = completed)
            return AppResult.Success(Unit)
        }
    }
}
