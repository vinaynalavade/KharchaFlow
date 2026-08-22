package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.security.AppLockManager
import com.vinaynalavade.expensetracker.core.security.PinVerificationResult
import com.vinaynalavade.expensetracker.core.security.SecurePinManager
import com.vinaynalavade.expensetracker.domain.repository.UserPreferencesRepository

class SetAppLockEnabledUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appLockManager: AppLockManager
) {
    suspend operator fun invoke(enabled: Boolean): AppResult<Unit> {
        val result = userPreferencesRepository.setAppLockEnabled(enabled)
        if (result is AppResult.Success) {
            appLockManager.unlock()
        }
        return result
    }
}

class SetBiometricEnabledUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean): AppResult<Unit> {
        return userPreferencesRepository.setBiometricEnabled(enabled)
    }
}

class SetAutoLockDurationUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(seconds: Long): AppResult<Unit> {
        return userPreferencesRepository.setAutoLockDurationSeconds(seconds)
    }
}

class SetHideContentInRecentsUseCase(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(hide: Boolean): AppResult<Unit> {
        return userPreferencesRepository.setHideContentInRecents(hide)
    }
}

class VerifyPinUseCase(
    private val securePinManager: SecurePinManager,
    private val appLockManager: AppLockManager
) {
    operator fun invoke(pin: String): PinVerificationResult {
        val result = securePinManager.verifyPin(pin)
        if (result is PinVerificationResult.Success) {
            appLockManager.unlock()
        }
        return result
    }
}

class SavePinUseCase(
    private val securePinManager: SecurePinManager
) {
    operator fun invoke(pin: String): AppResult<Unit> {
        return securePinManager.savePin(pin)
    }
}

class ChangePinUseCase(
    private val securePinManager: SecurePinManager
) {
    operator fun invoke(currentPin: String, newPin: String): AppResult<Unit> {
        val verification = securePinManager.verifyPin(currentPin)
        if (verification !is PinVerificationResult.Success) {
            return AppResult.Error(AppError.SecurityError("Current PIN is incorrect."))
        }
        return securePinManager.savePin(newPin)
    }
}

class DisableAppLockUseCase(
    private val securePinManager: SecurePinManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appLockManager: AppLockManager
) {
    suspend operator fun invoke(): AppResult<Unit> {
        securePinManager.clearPin()
        val result = userPreferencesRepository.setAppLockEnabled(false)
        userPreferencesRepository.setBiometricEnabled(false)
        appLockManager.unlock()
        return result
    }
}
