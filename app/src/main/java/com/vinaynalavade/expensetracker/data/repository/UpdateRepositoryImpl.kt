package com.vinaynalavade.expensetracker.data.repository

import android.content.Context
import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.data.update.ApkDownloader
import com.vinaynalavade.expensetracker.data.update.ApkVerifier
import com.vinaynalavade.expensetracker.data.update.GitHubReleaseService
import com.vinaynalavade.expensetracker.domain.model.DownloadProgress
import com.vinaynalavade.expensetracker.domain.model.RemoteReleaseInfo
import com.vinaynalavade.expensetracker.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Default implementation of UpdateRepository.
 */
class UpdateRepositoryImpl(
    private val context: Context,
    private val gitHubReleaseService: GitHubReleaseService = GitHubReleaseService(),
    private val apkDownloader: ApkDownloader = ApkDownloader(),
    private val apkVerifier: ApkVerifier = ApkVerifier(context)
) : UpdateRepository {

    private val updatesDirectory: File by lazy {
        File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
        }
    }

    override suspend fun fetchLatestRelease(): AppResult<RemoteReleaseInfo> {
        return gitHubReleaseService.fetchLatestRelease()
    }

    override fun downloadApk(
        releaseInfo: RemoteReleaseInfo,
        destinationFile: File
    ): Flow<AppResult<DownloadProgress>> {
        return apkDownloader.download(releaseInfo.apkDownloadUrl, destinationFile)
    }

    override suspend fun fetchExpectedSha256(releaseInfo: RemoteReleaseInfo): AppResult<String?> {
        // 1. Check if already present in releaseInfo (from digest or release.json)
        if (!releaseInfo.expectedSha256.isNullOrBlank()) {
            return AppResult.Success(releaseInfo.expectedSha256)
        }

        // 2. If .sha256 download URL is available, download and parse
        val shaUrl = releaseInfo.sha256DownloadUrl
        if (!shaUrl.isNullOrBlank()) {
            return when (val rawResult = gitHubReleaseService.fetchPlainText(shaUrl)) {
                is AppResult.Success -> {
                    val cleanChecksum = apkVerifier.extractCleanChecksum(rawResult.data)
                    AppResult.Success(cleanChecksum)
                }
                is AppResult.Error -> AppResult.Success(null) // Non-fatal if .sha256 asset cannot be fetched
            }
        }

        return AppResult.Success(null)
    }

    override suspend fun verifyDownloadedApk(
        apkFile: File,
        expectedSha256: String?,
        expectedVersionCode: Long
    ): AppResult<Unit> {
        return apkVerifier.verifyApk(apkFile, expectedSha256, expectedVersionCode)
    }

    override fun getUpdateTargetFile(releaseInfo: RemoteReleaseInfo): File {
        return File(updatesDirectory, releaseInfo.apkFileName)
    }

    override fun cleanStaleUpdateFiles(): AppResult<Unit> {
        return try {
            if (updatesDirectory.exists()) {
                updatesDirectory.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UpdateError(message = "Failed to clean up update directory: ${e.localizedMessage ?: e.message}", cause = e))
        }
    }
}
