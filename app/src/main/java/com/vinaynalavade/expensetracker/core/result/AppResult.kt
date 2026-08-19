package com.vinaynalavade.expensetracker.core.result

/**
 * Type-safe result wrapper for domain and repository operations.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Error(val error: AppError) : AppResult<Nothing>
}

/**
 * Base taxonomy for application domain errors.
 */
sealed interface AppError {
    val message: String

    data class DatabaseError(override val message: String, val cause: Throwable? = null) : AppError
    data class PreferencesError(override val message: String, val cause: Throwable? = null) : AppError
    data class ValidationError(override val message: String, val field: String? = null) : AppError
    data class NotFound(override val message: String) : AppError
    data class UnknownError(override val message: String, val cause: Throwable? = null) : AppError
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(error)
    return this
}

fun <T> AppResult<T>.getOrDefault(defaultValue: T): T = when (this) {
    is AppResult.Success -> data
    is AppResult.Error -> defaultValue
}
