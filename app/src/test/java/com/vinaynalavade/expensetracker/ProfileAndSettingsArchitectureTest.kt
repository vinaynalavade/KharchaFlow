package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.GoogleAccountInfo
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupState
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileAndSettingsArchitectureTest {

    private lateinit var fakePrefsRepo: FakeUserPrefsRepo

    @Before
    fun setUp() {
        fakePrefsRepo = FakeUserPrefsRepo()
    }

    @Test
    fun testDefaultProfileStateForLocalUser() = runBlocking {
        val prefs = fakePrefsRepo.getUserPreferences().first()

        assertNull("Default profile name should be null", prefs.userName)
        assertNull("Default profile image URI should be null", prefs.profileImageUri)

        // Effective display name fallback logic
        val effectiveName = prefs.userName?.takeIf { it.isNotBlank() } ?: "Personal Finance Profile"
        assertEquals("Personal Finance Profile", effectiveName)
    }

    @Test
    fun testSetAndPersistCustomProfileName() = runBlocking {
        val result = fakePrefsRepo.setProfileName("Vinay Nalavade")
        assertTrue(result is AppResult.Success)

        val updatedPrefs = fakePrefsRepo.getUserPreferences().first()
        assertEquals("Vinay Nalavade", updatedPrefs.userName)
    }

    @Test
    fun testSetAndPersistProfileImageUri() = runBlocking {
        val testUri = "content://media/external/images/media/42"
        val result = fakePrefsRepo.setProfileImageUri(testUri)
        assertTrue(result is AppResult.Success)

        val updatedPrefs = fakePrefsRepo.getUserPreferences().first()
        assertEquals(testUri, updatedPrefs.profileImageUri)
    }

    @Test
    fun testRemoveProfileImageRestoresDefaultAvatar() = runBlocking {
        // 1. Set image
        fakePrefsRepo.setProfileImageUri("content://media/external/images/media/42")
        assertEquals("content://media/external/images/media/42", fakePrefsRepo.getUserPreferences().first().profileImageUri)

        // 2. Remove image
        val removeResult = fakePrefsRepo.setProfileImageUri(null)
        assertTrue(removeResult is AppResult.Success)

        val clearedPrefs = fakePrefsRepo.getUserPreferences().first()
        assertNull("Profile image URI must be null after removal", clearedPrefs.profileImageUri)
    }

    @Test
    fun testCustomNamePrecedenceOverGoogleAccountName() {
        val customName = "Vinay Custom"
        val googleAccount = GoogleAccountInfo(
            email = "vinay@example.com",
            displayName = "Vinay Google Account",
            photoUrl = null
        )
        val googleState = GoogleBackupState.Connected(googleAccount, null)

        val userPrefsWithCustom = UserPreferences(userName = customName)
        val effectiveDisplayName = when {
            !userPrefsWithCustom.userName.isNullOrBlank() -> userPrefsWithCustom.userName!!
            googleState is GoogleBackupState.Connected && !googleState.account.displayName.isNullOrBlank() -> googleState.account.displayName!!
            else -> "Personal Finance Profile"
        }

        assertEquals("Vinay Custom", effectiveDisplayName)
    }

    @Test
    fun testFallbackToGoogleNameWhenNoCustomName() {
        val googleAccount = GoogleAccountInfo(
            email = "vinay@example.com",
            displayName = "Vinay Google Account",
            photoUrl = null
        )
        val googleState = GoogleBackupState.Connected(googleAccount, null)

        val userPrefsEmpty = UserPreferences(userName = null)
        val effectiveDisplayName = when {
            !userPrefsEmpty.userName.isNullOrBlank() -> userPrefsEmpty.userName!!
            googleState is GoogleBackupState.Connected && !googleState.account.displayName.isNullOrBlank() -> googleState.account.displayName!!
            else -> "Personal Finance Profile"
        }

        assertEquals("Vinay Google Account", effectiveDisplayName)
    }

    @Test
    fun testLocalAccountStateRepresentation() {
        val googleState: GoogleBackupState = GoogleBackupState.Disconnected
        val isConnected = googleState is GoogleBackupState.Connected

        assertFalse("Local user should not be connected to Google", isConnected)
    }

    @Test
    fun testConnectedAccountStateRepresentation() {
        val googleAccount = GoogleAccountInfo(
            email = "user@gmail.com",
            displayName = "Android User",
            photoUrl = "https://example.com/photo.jpg"
        )
        val googleState: GoogleBackupState = GoogleBackupState.Connected(googleAccount, 1724500000000L)
        val isConnected = googleState is GoogleBackupState.Connected

        assertTrue("Connected state should be recognized", isConnected)
        assertEquals("user@gmail.com", (googleState as GoogleBackupState.Connected).account.email)
        assertEquals("Android User", googleState.account.displayName)
    }

    // ─── Test Fake ───

    private class FakeUserPrefsRepo : UserPreferencesRepository {
        private var currentPrefs = UserPreferences()
        private val _flow = MutableStateFlow(currentPrefs)

        override fun getUserPreferences(): Flow<UserPreferences> = _flow.asStateFlow()
        override suspend fun setThemeMode(themeMode: ThemeMode) = AppResult.Success(Unit)
        override suspend fun setCurrencyCode(currencyCode: String) = AppResult.Success(Unit)
        override suspend fun setDynamicColors(useDynamicColors: Boolean) = AppResult.Success(Unit)
        override suspend fun setFirstLaunchCompleted() = AppResult.Success(Unit)
        override suspend fun setOpeningBalance(subunits: Long) = AppResult.Success(Unit)
        override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) = AppResult.Success(Unit)
        override suspend fun setEmiReminders(enabled: Boolean) = AppResult.Success(Unit)
        override fun getLastBackupTimestamp(): Flow<Long?> = MutableStateFlow(null)
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

        override suspend fun setProfileName(name: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(userName = name)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setProfileImageUri(uri: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(profileImageUri = uri)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setAutomaticBackupEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(automaticBackupEnabled = enabled)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setLastBackupStatus(status: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(lastBackupStatus = status)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setLastBackupError(error: String?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(lastBackupError = error)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setLastDismissedRestoreBackupTimestamp(timestamp: Long?): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(lastDismissedRestoreBackupTimestamp = timestamp)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

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
