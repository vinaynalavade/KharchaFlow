package com.vinaynalavade.expensetracker.domain.model

import java.io.File

/**
 * Result of checking for updates against the latest remote release metadata.
 */
sealed interface UpdateCheckResult {
    data class UpToDate(
        val currentVersionName: String,
        val currentVersionCode: Long,
        val lastCheckedMillis: Long = System.currentTimeMillis()
    ) : UpdateCheckResult

    data class UpdateAvailable(
        val releaseInfo: RemoteReleaseInfo,
        val currentVersionName: String,
        val currentVersionCode: Long
    ) : UpdateCheckResult
}

/**
 * Download progress information emitted during streaming APK download.
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progressPercentage: Int
)

/**
 * UI State for the In-App Update flow.
 */
sealed interface UpdateUiState {
    data object Idle : UpdateUiState

    data object Checking : UpdateUiState

    data class UpToDate(
        val currentVersionName: String,
        val currentVersionCode: Long,
        val lastCheckedMillis: Long = System.currentTimeMillis()
    ) : UpdateUiState

    data class UpdateAvailable(
        val releaseInfo: RemoteReleaseInfo,
        val currentVersionName: String,
        val currentVersionCode: Long
    ) : UpdateUiState

    data class Downloading(
        val releaseInfo: RemoteReleaseInfo,
        val progressPercentage: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateUiState

    data class Verifying(
        val releaseInfo: RemoteReleaseInfo
    ) : UpdateUiState

    data class VerificationFailed(
        val reason: String,
        val releaseInfo: RemoteReleaseInfo
    ) : UpdateUiState

    data class DownloadFailed(
        val message: String,
        val canRetry: Boolean = true,
        val releaseInfo: RemoteReleaseInfo? = null
    ) : UpdateUiState

    data class InstallPermissionRequired(
        val apkFile: File,
        val releaseInfo: RemoteReleaseInfo
    ) : UpdateUiState

    data class ReadyToInstall(
        val apkFile: File,
        val releaseInfo: RemoteReleaseInfo
    ) : UpdateUiState
}
