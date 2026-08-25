package com.vinaynalavade.expensetracker

import android.content.SharedPreferences
import com.vinaynalavade.expensetracker.core.backup.BackupPreferences
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.security.AppLockManager
import com.vinaynalavade.expensetracker.core.security.PinVerificationResult
import com.vinaynalavade.expensetracker.core.security.SecurePinManager
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.ThemeMode
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository
import com.vinaynalavade.expensetracker.domain.usecase.ChangePinUseCase
import com.vinaynalavade.expensetracker.domain.usecase.DisableAppLockUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SavePinUseCase
import com.vinaynalavade.expensetracker.domain.usecase.SetAppLockEnabledUseCase
import com.vinaynalavade.expensetracker.domain.usecase.VerifyPinUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockSecurityTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var securePinManager: SecurePinManager
    private lateinit var appLockManager: AppLockManager
    private lateinit var fakeUserPrefsRepo: FakeUserPreferencesRepo

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        securePinManager = SecurePinManager(injectedPrefs = fakePrefs)
        appLockManager = AppLockManager()
        fakeUserPrefsRepo = FakeUserPreferencesRepo()
    }

    @Test
    fun testPinSaltAndHashSecurity() {
        val pin = "1234"
        val saveResult = securePinManager.savePin(pin)
        assertTrue(saveResult is AppResult.Success)

        // Raw PIN must never be stored anywhere in SharedPreferences
        val rawValues = fakePrefs.getAllValues()
        for (value in rawValues.values) {
            assertNotEquals("Plaintext PIN found in storage!", pin, value.toString())
            assertFalse("PIN contains raw digit sequence", value.toString().contains("1234"))
        }

        assertTrue(securePinManager.isPinSet())
    }

    @Test
    fun testPinVerificationSuccessAndFailure() {
        securePinManager.savePin("4567")

        // Correct PIN
        val successResult = securePinManager.verifyPin("4567")
        assertTrue(successResult is PinVerificationResult.Success)

        // Incorrect PIN
        val failureResult = securePinManager.verifyPin("0000")
        assertTrue(failureResult is PinVerificationResult.Incorrect)
        assertEquals(4, (failureResult as PinVerificationResult.Incorrect).remainingAttempts)
    }

    @Test
    fun testThrottlingLockoutAfterFiveFailedAttempts() {
        securePinManager.savePin("9999")

        // 1st to 4th incorrect attempts
        for (i in 1..4) {
            val result = securePinManager.verifyPin("1111")
            assertTrue(result is PinVerificationResult.Incorrect)
            assertEquals(5 - i, (result as PinVerificationResult.Incorrect).remainingAttempts)
        }

        // 5th incorrect attempt -> triggers 30-second lockout
        val lockoutResult = securePinManager.verifyPin("1111")
        assertTrue(lockoutResult is PinVerificationResult.LockedOut)
        val lockout = lockoutResult as PinVerificationResult.LockedOut
        assertTrue(lockout.secondsRemaining in 1..30)

        // Subsequent attempt while locked out stays locked out
        val retryResult = securePinManager.verifyPin("9999")
        assertTrue(retryResult is PinVerificationResult.LockedOut)
    }

    @Test
    fun testChangePinWorkflow() {
        val changePinUseCase = ChangePinUseCase(securePinManager)
        securePinManager.savePin("1234")

        // Failed change with wrong current PIN
        val badResult = changePinUseCase("0000", "5678")
        assertTrue(badResult is AppResult.Error)

        // Successful change with correct current PIN
        val goodResult = changePinUseCase("1234", "5678")
        assertTrue(goodResult is AppResult.Success)

        // Old PIN no longer works
        val oldVerify = securePinManager.verifyPin("1234")
        assertTrue(oldVerify is PinVerificationResult.Incorrect)

        // New PIN works
        val newVerify = securePinManager.verifyPin("5678")
        assertTrue(newVerify is PinVerificationResult.Success)
    }

    @Test
    fun testDisableAppLockWithValidPinSuccess() = runBlocking {
        val setAppLockUseCase = SetAppLockEnabledUseCase(fakeUserPrefsRepo, appLockManager)
        val disableAppLockUseCase = DisableAppLockUseCase(securePinManager, fakeUserPrefsRepo, appLockManager)

        securePinManager.savePin("1234")
        setAppLockUseCase(true)
        assertTrue(fakeUserPrefsRepo.currentPrefs.appLockEnabled)
        assertTrue(securePinManager.isPinSet())

        // Disable with valid PIN
        val result = disableAppLockUseCase("1234")
        assertTrue(result is AppResult.Success)
        assertFalse(fakeUserPrefsRepo.currentPrefs.appLockEnabled)
        assertFalse(securePinManager.isPinSet())
    }

    @Test
    fun testDisableAppLockWithInvalidPinFailsAndPreservesAppLock() = runBlocking {
        val setAppLockUseCase = SetAppLockEnabledUseCase(fakeUserPrefsRepo, appLockManager)
        val disableAppLockUseCase = DisableAppLockUseCase(securePinManager, fakeUserPrefsRepo, appLockManager)

        securePinManager.savePin("1234")
        setAppLockUseCase(true)
        assertTrue(fakeUserPrefsRepo.currentPrefs.appLockEnabled)
        assertTrue(securePinManager.isPinSet())

        // Attempt disable with incorrect PIN
        val result = disableAppLockUseCase("9999")
        assertTrue(result is AppResult.Error)
        assertEquals("Incorrect PIN. 4 attempts remaining.", (result as AppResult.Error).error.message)

        // App Lock state and PIN vault must remain completely untouched
        assertTrue(fakeUserPrefsRepo.currentPrefs.appLockEnabled)
        assertTrue(securePinManager.isPinSet())

        // Verify existing PIN "1234" still unlocks
        val verifyOld = securePinManager.verifyPin("1234")
        assertTrue(verifyOld is PinVerificationResult.Success)
    }

    @Test
    fun testDisableAppLockLockoutAfterMaxFailedAttempts() = runBlocking {
        val setAppLockUseCase = SetAppLockEnabledUseCase(fakeUserPrefsRepo, appLockManager)
        val disableAppLockUseCase = DisableAppLockUseCase(securePinManager, fakeUserPrefsRepo, appLockManager)

        securePinManager.savePin("1234")
        setAppLockUseCase(true)

        // 1 to 4 failed attempts
        for (i in 1..4) {
            val result = disableAppLockUseCase("0000")
            assertTrue(result is AppResult.Error)
            assertTrue((result as AppResult.Error).error.message.contains("${5 - i} attempts remaining"))
            assertTrue(fakeUserPrefsRepo.currentPrefs.appLockEnabled)
        }

        // 5th failed attempt triggers lockout
        val lockoutResult = disableAppLockUseCase("0000")
        assertTrue(lockoutResult is AppResult.Error)
        assertTrue((lockoutResult as AppResult.Error).error.message.contains("Too many incorrect attempts"))
        assertTrue(fakeUserPrefsRepo.currentPrefs.appLockEnabled)
        assertTrue(securePinManager.getLockoutSecondsRemaining() > 0)

        // Even correct PIN is rejected during active lockout
        val blockedAttempt = disableAppLockUseCase("1234")
        assertTrue(blockedAttempt is AppResult.Error)
        assertTrue((blockedAttempt as AppResult.Error).error.message.contains("Too many incorrect attempts"))
        assertTrue(fakeUserPrefsRepo.currentPrefs.appLockEnabled)
    }

    @Test
    fun testDirectAdministrativeDisableAppLockWorkflow() = runBlocking {
        val setAppLockUseCase = SetAppLockEnabledUseCase(fakeUserPrefsRepo, appLockManager)
        val disableAppLockUseCase = DisableAppLockUseCase(securePinManager, fakeUserPrefsRepo, appLockManager)

        securePinManager.savePin("1234")
        setAppLockUseCase(true)
        assertTrue(fakeUserPrefsRepo.currentPrefs.appLockEnabled)

        // Direct reset without PIN
        val result = disableAppLockUseCase()
        assertTrue(result is AppResult.Success)
        assertFalse(fakeUserPrefsRepo.currentPrefs.appLockEnabled)
        assertFalse(securePinManager.isPinSet())
    }

    @Test
    fun testBackupDataNeverIncludesAppLockOrPinCredentials() {
        val backupPrefs = BackupPreferences(
            openingBalanceSubunits = 50000L,
            currencyCode = "INR",
            themeMode = "DARK",
            dailyReminderEnabled = true,
            dailyReminderHour = 21,
            dailyReminderMinute = 0,
            emiRemindersEnabled = true
        )

        // Verify reflection fields on BackupPreferences
        val fieldNames = BackupPreferences::class.java.declaredFields.map { it.name.lowercase() }
        assertFalse("BackupPreferences must never contain pin", fieldNames.any { it.contains("pin") })
        assertFalse("BackupPreferences must never contain salt", fieldNames.any { it.contains("salt") })
        assertFalse("BackupPreferences must never contain lock", fieldNames.any { it.contains("lock") })
        assertFalse("BackupPreferences must never contain hash", fieldNames.any { it.contains("hash") })
    }

    private class FakeUserPreferencesRepo : UserPreferencesRepository {
        var currentPrefs = UserPreferences()
        private val _flow = MutableStateFlow(currentPrefs)

        override fun getUserPreferences(): Flow<UserPreferences> = _flow.asStateFlow()
        override suspend fun setThemeMode(themeMode: ThemeMode) = AppResult.Success(Unit)
        override suspend fun setCurrencyCode(currencyCode: String) = AppResult.Success(Unit)
        override suspend fun setDynamicColors(useDynamicColors: Boolean) = AppResult.Success(Unit)
        override suspend fun setFirstLaunchCompleted() = AppResult.Success(Unit)
        override suspend fun setOpeningBalance(subunits: Long) = AppResult.Success(Unit)
        override suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) = AppResult.Success(Unit)
        override suspend fun setEmiReminders(enabled: Boolean) = AppResult.Success(Unit)
        override fun getLastBackupTimestamp(): Flow<Long?> = flowOf(null)
        override suspend fun setLastBackupTimestamp(timestamp: Long) = AppResult.Success(Unit)

        override suspend fun setAppLockEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(appLockEnabled = enabled)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setBiometricEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(biometricEnabled = enabled)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setAutoLockDurationSeconds(seconds: Long): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(autoLockDurationSeconds = seconds)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setHideContentInRecents(hide: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(hideContentInRecents = hide)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setNotificationsMasterEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(notificationsMasterEnabled = enabled)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setBudgetAlertsEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(budgetAlertsEnabled = enabled)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setMonthlyBudgetLimit(subunits: Long): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(monthlyBudgetLimitSubunits = subunits)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setRecurringRemindersEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(recurringRemindersEnabled = enabled)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setRecurringReminderAdvanceDays(days: Int): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(recurringReminderAdvanceDays = days)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setSavingsGoalNotificationsEnabled(enabled: Boolean): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(savingsGoalNotificationsEnabled = enabled)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

        override suspend fun setAppLanguage(languageCode: String): AppResult<Unit> {
            currentPrefs = currentPrefs.copy(appLanguage = languageCode)
            _flow.value = currentPrefs
            return AppResult.Success(Unit)
        }

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

    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any>()

        fun getAllValues(): Map<String, Any> = map.toMap()

        override fun getAll(): MutableMap<String, *> = map
        override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = null
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val storage: MutableMap<String, Any>) : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any>()
            private var clear = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) if (value != null) temp[key] = value else temp.remove(key)
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) temp.remove(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clear = true
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clear) storage.clear()
                storage.putAll(temp)
            }
        }
    }
}
