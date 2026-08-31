package com.vinaynalavade.expensetracker.domain.usecase

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.RemoteReleaseInfo
import com.vinaynalavade.expensetracker.domain.model.UpdateCheckResult
import com.vinaynalavade.expensetracker.domain.repository.UpdateRepository

/**
 * Use case to check for available updates comparing local versionCode against remote release metadata.
 * Authoritative comparison is strictly versionCode (never string versionName).
 * Rejects equal, older, and downgrade versions.
 */
class CheckForUpdateUseCase(
    private val updateRepository: UpdateRepository
) {

    suspend operator fun invoke(
        localVersionCode: Long,
        localVersionName: String
    ): AppResult<UpdateCheckResult> {
        return when (val releaseResult = updateRepository.fetchLatestRelease()) {
            is AppResult.Success -> {
                val releaseInfo = releaseResult.data
                if (releaseInfo.latestVersionCode > localVersionCode) {
                    AppResult.Success(
                        UpdateCheckResult.UpdateAvailable(
                            releaseInfo = releaseInfo,
                            currentVersionName = localVersionName,
                            currentVersionCode = localVersionCode
                        )
                    )
                } else {
                    AppResult.Success(
                        UpdateCheckResult.UpToDate(
                            currentVersionName = localVersionName,
                            currentVersionCode = localVersionCode
                        )
                    )
                }
            }
            is AppResult.Error -> {
                AppResult.Error(releaseResult.error)
            }
        }
    }
}
