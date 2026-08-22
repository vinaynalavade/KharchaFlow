package com.vinaynalavade.expensetracker.core.security

import android.content.Context
import android.content.SharedPreferences
import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

sealed interface PinVerificationResult {
    data object Success : PinVerificationResult
    data class Incorrect(val remainingAttempts: Int) : PinVerificationResult
    data class LockedOut(val secondsRemaining: Long) : PinVerificationResult
}

/**
 * Manages secure, salted PIN derivation, verification, and temporary lockout throttling.
 * Credentials are stored in a private security vault isolated from general preferences and backups.
 */
class SecurePinManager(
    private val context: Context? = null,
    private val prefsName: String = PREFS_NAME,
    injectedPrefs: SharedPreferences? = null
) {
    companion object {
        const val PREFS_NAME = "kharchaflow_security_vault"
        private const val KEY_PIN_SALT = "sec_pin_salt"
        private const val KEY_PIN_HASH = "sec_pin_hash"
        private const val KEY_FAILED_ATTEMPTS = "sec_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "sec_lockout_until"

        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MILLIS = 30_000L // 30 seconds
    }

    private val prefs: SharedPreferences by lazy {
        injectedPrefs ?: requireNotNull(context) { "Context or SharedPreferences required" }
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    fun isPinSet(): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null)
        val hash = prefs.getString(KEY_PIN_HASH, null)
        return !salt.isNullOrBlank() && !hash.isNullOrBlank()
    }

    fun savePin(pin: String): AppResult<Unit> {
        return try {
            require(pin.length >= 4) { "PIN must be at least 4 digits." }

            val random = SecureRandom()
            val saltBytes = ByteArray(16)
            random.nextBytes(saltBytes)

            val hashBytes = computeSaltedHash(pin, saltBytes)

            val saltB64 = Base64.getEncoder().encodeToString(saltBytes)
            val hashB64 = Base64.getEncoder().encodeToString(hashBytes)

            prefs.edit()
                .putString(KEY_PIN_SALT, saltB64)
                .putString(KEY_PIN_HASH, hashB64)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.SecurityError(e.message ?: "Failed to save secure PIN.", e))
        }
    }

    fun verifyPin(pin: String): PinVerificationResult {
        val lockoutSeconds = getLockoutSecondsRemaining()
        if (lockoutSeconds > 0) {
            return PinVerificationResult.LockedOut(lockoutSeconds)
        }

        val saltB64 = prefs.getString(KEY_PIN_SALT, null)
        val hashB64 = prefs.getString(KEY_PIN_HASH, null)

        if (saltB64.isNullOrBlank() || hashB64.isNullOrBlank()) {
            return PinVerificationResult.Incorrect(remainingAttempts = 0)
        }

        return try {
            val saltBytes = Base64.getDecoder().decode(saltB64)
            val storedHashBytes = Base64.getDecoder().decode(hashB64)

            val computedHashBytes = computeSaltedHash(pin, saltBytes)

            if (MessageDigest.isEqual(storedHashBytes, computedHashBytes)) {
                // Reset failed attempts on success
                prefs.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, 0)
                    .putLong(KEY_LOCKOUT_UNTIL, 0L)
                    .apply()
                PinVerificationResult.Success
            } else {
                val currentAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
                if (currentAttempts >= MAX_FAILED_ATTEMPTS) {
                    val lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MILLIS
                    prefs.edit()
                        .putInt(KEY_FAILED_ATTEMPTS, currentAttempts)
                        .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
                        .apply()
                    PinVerificationResult.LockedOut(LOCKOUT_DURATION_MILLIS / 1000)
                } else {
                    prefs.edit()
                        .putInt(KEY_FAILED_ATTEMPTS, currentAttempts)
                        .apply()
                    val remaining = MAX_FAILED_ATTEMPTS - currentAttempts
                    PinVerificationResult.Incorrect(remaining)
                }
            }
        } catch (e: Exception) {
            PinVerificationResult.Incorrect(remainingAttempts = 0)
        }
    }

    fun getLockoutSecondsRemaining(): Long {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        return if (lockoutUntil > now) {
            (lockoutUntil - now + 999L) / 1000L
        } else {
            0L
        }
    }

    fun clearPin(): AppResult<Unit> {
        return try {
            prefs.edit().clear().apply()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.SecurityError(e.message ?: "Failed to clear PIN.", e))
        }
    }

    private fun computeSaltedHash(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
    }
}
