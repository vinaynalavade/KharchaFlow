package com.vinaynalavade.expensetracker.data.update

import android.content.Context
import android.os.Build
import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Validates the downloaded APK file against SHA-256 checksums and Android package archive properties.
 */
class ApkVerifier(
    private val context: Context? = null
) {

    /**
     * Computes the SHA-256 hash of a local file.
     */
    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        FileInputStream(file).use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Extracts and normalizes the 64-character SHA-256 hex string from raw .sha256 file text.
     * Supports standalone hash or standard 'sha256sum' format (e.g. '<hash>  <filename>').
     */
    fun extractCleanChecksum(rawText: String): String? {
        val trimmed = rawText.trim()
        val match = Regex("([0-9a-fA-F]{64})").find(trimmed)
        return match?.groupValues?.get(1)?.lowercase()
    }

    /**
     * Verifies the APK file against the expected checksum (if available) and validates package archive properties.
     */
    suspend fun verifyApk(
        apkFile: File,
        expectedSha256Raw: String?,
        expectedVersionCode: Long
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            return@withContext AppResult.Error(
                AppError.UpdateError(
                    message = "APK file does not exist or is empty.",
                    userMessage = "Update download could not be verified. Please try downloading again."
                )
            )
        }

        // 1. If expected checksum is provided, verify SHA-256
        if (!expectedSha256Raw.isNullOrBlank()) {
            val cleanExpectedSha256 = extractCleanChecksum(expectedSha256Raw)
            if (cleanExpectedSha256 != null) {
                val actualSha256 = try {
                    computeSha256(apkFile)
                } catch (e: Exception) {
                    apkFile.delete()
                    return@withContext AppResult.Error(
                        AppError.UpdateError(
                            message = "Failed to calculate SHA-256 checksum: ${e.localizedMessage ?: e.message}",
                            cause = e,
                            userMessage = "Failed to verify update security. Please try again."
                        )
                    )
                }

                if (!actualSha256.equals(cleanExpectedSha256, ignoreCase = true)) {
                    apkFile.delete()
                    return@withContext AppResult.Error(
                        AppError.UpdateError(
                            message = "SHA-256 checksum verification failed. Expected: $cleanExpectedSha256, Actual: $actualSha256.",
                            userMessage = "The downloaded update failed security verification. The file may be corrupted."
                        )
                    )
                }
            }
        }

        // 2. Validate Android PackageArchive metadata (when running in Android runtime)
        if (context != null) {
            val pm = context.packageManager
            val packageInfo = try {
                pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            } catch (_: Exception) {
                null
            }

            if (packageInfo == null) {
                apkFile.delete()
                return@withContext AppResult.Error(
                    AppError.UpdateError(
                        message = "The downloaded file is not a valid Android APK package.",
                        userMessage = "The downloaded file is not a valid Android package."
                    )
                )
            }

            val expectedPackageName = context.packageName
            if (packageInfo.packageName != expectedPackageName) {
                apkFile.delete()
                return@withContext AppResult.Error(
                    AppError.UpdateError(
                        message = "APK package mismatch: expected '$expectedPackageName', but found '${packageInfo.packageName}'.",
                        userMessage = "Security check failed: Package identifier mismatch."
                    )
                )
            }

            @Suppress("DEPRECATION")
            val archiveVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }

            if (archiveVersionCode != expectedVersionCode) {
                apkFile.delete()
                return@withContext AppResult.Error(
                    AppError.UpdateError(
                        message = "APK versionCode mismatch: release declares $expectedVersionCode, but archive has $archiveVersionCode.",
                        userMessage = "Security check failed: Package version mismatch."
                    )
                )
            }
        }

        AppResult.Success(Unit)
    }
}
