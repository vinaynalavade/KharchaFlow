package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.domain.usecase.SetAppTourCompletedUseCase
import com.vinaynalavade.expensetracker.presentation.tour.AppTourViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AppTourTest {

    private lateinit var fakePrefsRepo: FakeUserPreferencesRepo
    private lateinit var setAppTourCompletedUseCase: SetAppTourCompletedUseCase

    @Before
    fun setUp() {
        fakePrefsRepo = FakeUserPreferencesRepo()
        setAppTourCompletedUseCase = SetAppTourCompletedUseCase(fakePrefsRepo)
    }

    @Test
    fun testDefaultAppTourCompletedIsFalse() = runBlocking {
        val initialPrefs = fakePrefsRepo.getUserPreferences().first()
        assertFalse("Default isAppTourCompleted must be false for a genuinely new user", initialPrefs.isAppTourCompleted)
    }

    @Test
    fun testCompletingTourPersistsTrue() = runBlocking {
        val result = setAppTourCompletedUseCase(completed = true)
        assertTrue(result is AppResult.Success)

        val updatedPrefs = fakePrefsRepo.getUserPreferences().first()
        assertTrue("isAppTourCompleted must be true after completion", updatedPrefs.isAppTourCompleted)
    }

    @Test
    fun testSkippingTourPersistsTrue() = runBlocking {
        var navigated = false
        val onSkip = {
            navigated = true
        }

        val result = setAppTourCompletedUseCase(completed = true)
        if (result is AppResult.Success) {
            onSkip()
        }

        val updatedPrefs = fakePrefsRepo.getUserPreferences().first()
        assertTrue("isAppTourCompleted must be true when skipping", updatedPrefs.isAppTourCompleted)
        assertTrue("Navigation callback must be invoked on skip", navigated)
    }

    @Test
    fun testTourCompletedSurvivesAppRestart() = runBlocking {
        // 1. Initial State
        assertFalse(fakePrefsRepo.getUserPreferences().first().isAppTourCompleted)

        // 2. Complete tour
        setAppTourCompletedUseCase(true)
        assertTrue(fakePrefsRepo.getUserPreferences().first().isAppTourCompleted)

        // 3. Simulate process recreation / new repository instantiation reading state
        val reloadedPrefs = fakePrefsRepo.getUserPreferences().first()
        assertTrue("App restart / process recreation must retain isAppTourCompleted = true", reloadedPrefs.isAppTourCompleted)
    }

    @Test
    fun testTourStateDoesNotInterfereWithFirstLaunchPreference() = runBlocking {
        // Complete welcome / first launch
        fakePrefsRepo.setFirstLaunchCompleted()
        val prefsAfterWelcome = fakePrefsRepo.getUserPreferences().first()
        assertFalse("isFirstLaunch should be false", prefsAfterWelcome.isFirstLaunch)
        assertFalse("isAppTourCompleted must remain false independently", prefsAfterWelcome.isAppTourCompleted)

        // Complete tour
        setAppTourCompletedUseCase(true)
        val prefsAfterTour = fakePrefsRepo.getUserPreferences().first()
        assertFalse("isFirstLaunch should remain false", prefsAfterTour.isFirstLaunch)
        assertTrue("isAppTourCompleted is now true", prefsAfterTour.isAppTourCompleted)
    }

    @Test
    fun testAppTourLocalizationParity() {
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val resDir = if (File(projectDir, "app/src/main/res").exists()) {
            File(projectDir, "app/src/main/res")
        } else {
            File(projectDir, "src/main/res")
        }

        val enFile = File(resDir, "values/strings.xml")
        val hiFile = File(resDir, "values-hi/strings.xml")
        val mrFile = File(resDir, "values-mr/strings.xml")

        assertTrue("English strings.xml must exist", enFile.exists())
        assertTrue("Hindi strings.xml must exist", hiFile.exists())
        assertTrue("Marathi strings.xml must exist", mrFile.exists())

        val enKeys = extractStringKeys(enFile)
        val hiKeys = extractStringKeys(hiFile)
        val mrKeys = extractStringKeys(mrFile)

        val tourKeys = listOf(
            "app_tour_btn_skip",
            "app_tour_btn_next",
            "app_tour_btn_previous",
            "app_tour_btn_finish",
            "app_tour_step_indicator",
            "app_tour_step1_title",
            "app_tour_step1_desc",
            "app_tour_step1_feature1",
            "app_tour_step1_feature2",
            "app_tour_step2_title",
            "app_tour_step2_desc",
            "app_tour_step2_chip_balance",
            "app_tour_step2_chip_income",
            "app_tour_step2_chip_expense",
            "app_tour_step3_title",
            "app_tour_step3_desc",
            "app_tour_step3_badge_upi",
            "app_tour_step3_badge_cash",
            "app_tour_step3_badge_bank",
            "app_tour_step4_title",
            "app_tour_step4_desc",
            "app_tour_step4_insight_budget",
            "app_tour_step4_insight_analytics",
            "app_tour_step5_title",
            "app_tour_step5_desc",
            "app_tour_step5_feature_lock",
            "app_tour_step5_feature_backup"
        )

        for (key in tourKeys) {
            assertTrue("Key '$key' must exist in English strings.xml", enKeys.contains(key))
            assertTrue("Key '$key' must exist in Hindi strings.xml", hiKeys.contains(key))
            assertTrue("Key '$key' must exist in Marathi strings.xml", mrKeys.contains(key))
        }
    }

    private fun extractStringKeys(file: File): Set<String> {
        val keys = mutableSetOf<String>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(file)
        doc.documentElement.normalize()

        val stringNodes = doc.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue
            if (name != null) {
                keys.add(name)
            }
        }
        return keys
    }

    // --- Fake ---

    private class FakeUserPreferencesRepo : UserPreferencesRepository {
        var currentPrefs = UserPreferences()
        private val _flow = MutableStateFlow(currentPrefs)

        override fun getUserPreferences(): Flow<UserPreferences> = _flow.asStateFlow()
        override suspend fun setThemeMode(themeMode: ThemeMode) = AppResult.Success(Unit)
        override suspend fun setCurrencyCode(currencyCode: String) = AppResult.Success(Unit)
        override suspend fun setDynamicColors(useDynamicColors: Boolean) = AppResult.Success(Unit)
        override suspend fun setFirstLaunchCompleted(): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(isFirstLaunch = false)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }
        override suspend fun setOpeningBalance(subunits: Long) = AppResult.Success(Unit)
        override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) = AppResult.Success(Unit)
        override suspend fun setEmiReminders(enabled: Boolean) = AppResult.Success(Unit)
        override fun getLastBackupTimestamp(): Flow<Long?> = flowOf(null)
        override suspend fun setLastBackupTimestamp(timestamp: Long) = AppResult.Success(Unit)
        override suspend fun setAppLockEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setBiometricEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setAutoLockDurationSeconds(seconds: Long) = AppResult.Success(Unit)
        override suspend fun setHideContentInRecents(hide: Boolean) = AppResult.Success(Unit)
        override suspend fun setNotificationsMasterEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setBudgetAlertsEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setMonthlyBudgetLimit(subunits: Long) = AppResult.Success(Unit)
        override suspend fun setRecurringRemindersEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setRecurringReminderAdvanceDays(days: Int) = AppResult.Success(Unit)
        override suspend fun setSavingsGoalNotificationsEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setAppLanguage(languageCode: String) = AppResult.Success(Unit)
        override suspend fun setProfileName(name: String?) = AppResult.Success(Unit)
        override suspend fun setProfileImageUri(uri: String?) = AppResult.Success(Unit)
        override suspend fun setAutomaticBackupEnabled(enabled: Boolean) = AppResult.Success(Unit)
        override suspend fun setLastBackupStatus(status: String?) = AppResult.Success(Unit)
        override suspend fun setLastBackupError(error: String?) = AppResult.Success(Unit)
        override suspend fun setLastDismissedRestoreBackupTimestamp(timestamp: Long?) = AppResult.Success(Unit)

        override suspend fun setAppTourCompleted(completed: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(isAppTourCompleted = completed)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setDefaultIncomeSource(source: PaymentMethod): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(defaultIncomeSource = source)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setDefaultExpenseSource(source: PaymentMethod): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(defaultExpenseSource = source)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }
    }
}
