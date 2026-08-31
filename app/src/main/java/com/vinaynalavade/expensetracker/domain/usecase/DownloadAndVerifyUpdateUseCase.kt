package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.DownloadProgress
import com.vinaynalavade.expensetracker.domain.model.RemoteReleaseInfo
import com.vinaynalavade.expensetracker.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Use case orchestrating streaming APK download, checksum retrieval, SHA-256 verification,
 * and package identity validation.
 */
class DownloadAndVerifyUpdateUseCase(
    private val updateRepository: UpdateRepository
) {

    fun downloadApk(
        releaseInfo: RemoteReleaseInfo,
        destinationFile: File
    ): Flow<AppResult<DownloadProgress>> {
        return updateRepository.downloadApk(releaseInfo, destinationFile)
    }

    suspend fun fetchExpectedSha256(
        releaseInfo: RemoteReleaseInfo
    ): AppResult<String?> {
        return updateRepository.fetchExpectedSha256(releaseInfo)
    }

    suspend fun verifyApk(
        apkFile: File,
        expectedSha256: String?,
        expectedVersionCode: Long
    ): AppResult<Unit> {
        return updateRepository.verifyDownloadedApk(apkFile, expectedSha256, expectedVersionCode)
    }

    fun getUpdateTargetFile(releaseInfo: RemoteReleaseInfo): File {
        return updateRepository.getUpdateTargetFile(releaseInfo)
    }

    fun cleanStaleUpdateFiles(): AppResult<Unit> {
        return updateRepository.cleanStaleUpdateFiles()
    }
}
