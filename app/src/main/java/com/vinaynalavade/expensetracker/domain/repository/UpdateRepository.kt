package com.vinaynalavade.expensetracker.domain.repository

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.DownloadProgress
import com.vinaynalavade.expensetracker.domain.model.RemoteReleaseInfo
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for managing In-App APK update lifecycle.
 */
interface UpdateRepository {

    /**
     * Fetches machine-readable release metadata from GitHub Releases.
     */
    suspend fun fetchLatestRelease(): AppResult<RemoteReleaseInfo>

    /**
     * Downloads the APK file in a streaming flow, emitting progress updates.
     */
    fun downloadApk(releaseInfo: RemoteReleaseInfo, destinationFile: File): Flow<AppResult<DownloadProgress>>

    /**
     * Retrieves the expected SHA-256 hash (from release metadata, asset digest, or .sha256 file).
     */
    suspend fun fetchExpectedSha256(releaseInfo: RemoteReleaseInfo): AppResult<String?>

    /**
     * Verifies the SHA-256 hash (if available) and validates APK archive identity.
     */
    suspend fun verifyDownloadedApk(apkFile: File, expectedSha256: String?, expectedVersionCode: Long): AppResult<Unit>

    /**
     * Resolves the app-private destination file path for a given release APK.
     */
    fun getUpdateTargetFile(releaseInfo: RemoteReleaseInfo): File

    /**
     * Cleans up stale update files and partial downloads.
     */
    fun cleanStaleUpdateFiles(): AppResult<Unit>
}
