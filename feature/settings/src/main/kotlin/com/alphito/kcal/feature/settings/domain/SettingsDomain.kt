package com.alphito.kcal.feature.settings.domain

/** Result of a password change attempt. */
sealed interface PasswordResult {
    data object Success : PasswordResult
    data object Unavailable : PasswordResult
}

/**
 * Account credentials. kCal is offline-first and has no account backend yet, so the bundled
 * implementation reports [PasswordResult.Unavailable] instead of pretending to succeed.
 */
interface PasswordService {
    suspend fun change(newPassword: String): PasswordResult
}

/** Builds a portable export of everything kCal stores locally. */
interface DataExporter {
    suspend fun exportJson(): String
}

object PasswordRules {
    fun isStrong(password: String) = password.length >= 8 && password.any(Char::isDigit)
}
