package com.vinaynalavade.expensetracker.presentation.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinaynalavade.expensetracker.BuildConfig
import com.vinaynalavade.expensetracker.core.installer.PackageInstallerHelper
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.RemoteReleaseInfo
import com.vinaynalavade.expensetracker.domain.model.UpdateCheckResult
import com.vinaynalavade.expensetracker.domain.model.UpdateUiState
import com.vinaynalavade.expensetracker.domain.usecase.CheckForUpdateUseCase
import com.vinaynalavade.expensetracker.domain.usecase.DownloadAndVerifyUpdateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel managing the In-App APK update lifecycle, state transitions, and user interactions.
 */
class UpdateViewModel(
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val downloadAndVerifyUpdateUseCase: DownloadAndVerifyUpdateUseCase,
    private val packageInstallerHelper: PackageInstallerHelper,
    private val currentVersionCode: Long = BuildConfig.VERSION_CODE.toLong(),
    private val currentVersionName: String = BuildConfig.VERSION_NAME,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    /**
     * Checks whether an update is available from GitHub Releases.
     */
    fun checkForUpdates() {
        val currentState = _uiState.value
        if (currentState is UpdateUiState.Checking ||
            currentState is UpdateUiState.Downloading ||
            currentState is UpdateUiState.Verifying
        ) {
            // Prevent duplicate concurrent requests
            return
        }

        _uiState.value = UpdateUiState.Checking

        scope.launch {
            when (val result = checkForUpdateUseCase(currentVersionCode, currentVersionName)) {
                is AppResult.Success -> {
                    when (val checkResult = result.data) {
                        is UpdateCheckResult.UpdateAvailable -> {
                            _uiState.value = UpdateUiState.UpdateAvailable(
                                releaseInfo = checkResult.releaseInfo,
                                currentVersionName = checkResult.currentVersionName,
                                currentVersionCode = checkResult.currentVersionCode
                            )
                        }
                        is UpdateCheckResult.UpToDate -> {
                            _uiState.value = UpdateUiState.UpToDate(
                                currentVersionName = checkResult.currentVersionName,
                                currentVersionCode = checkResult.currentVersionCode,
                                lastCheckedMillis = checkResult.lastCheckedMillis
                            )
                        }
                    }
                }
                is AppResult.Error -> {
                    _uiState.value = UpdateUiState.DownloadFailed(
                        message = result.error.userMessage,
                        canRetry = true,
                        releaseInfo = null
                    )
                }
            }
        }
    }

    /**
     * Starts downloading the update APK and initiates verification upon completion.
     */
    fun startDownload(releaseInfo: RemoteReleaseInfo) {
        val currentState = _uiState.value
        if (currentState is UpdateUiState.Downloading || currentState is UpdateUiState.Verifying) {
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch {
            _uiState.value = UpdateUiState.Downloading(
                releaseInfo = releaseInfo,
                progressPercentage = 0,
                bytesDownloaded = 0L,
                totalBytes = releaseInfo.apkSizeBytes
            )

            val destinationFile = downloadAndVerifyUpdateUseCase.getUpdateTargetFile(releaseInfo)

            var downloadSuccess = false
            downloadAndVerifyUpdateUseCase.downloadApk(releaseInfo, destinationFile).collect { downloadResult ->
                when (downloadResult) {
                    is AppResult.Success -> {
                        val progress = downloadResult.data
                        _uiState.value = UpdateUiState.Downloading(
                            releaseInfo = releaseInfo,
                            progressPercentage = progress.progressPercentage,
                            bytesDownloaded = progress.bytesDownloaded,
                            totalBytes = progress.totalBytes
                        )
                        if (progress.progressPercentage == 100) {
                            downloadSuccess = true
                        }
                    }
                    is AppResult.Error -> {
                        _uiState.value = UpdateUiState.DownloadFailed(
                            message = downloadResult.error.userMessage,
                            canRetry = true,
                            releaseInfo = releaseInfo
                        )
                    }
                }
            }

            if (downloadSuccess) {
                verifyAndPrepareInstall(destinationFile, releaseInfo)
            }
        }
    }

    private suspend fun verifyAndPrepareInstall(apkFile: File, releaseInfo: RemoteReleaseInfo) {
        _uiState.value = UpdateUiState.Verifying(releaseInfo)

        // 1. Fetch expected checksum if available (from metadata, digest, or .sha256 file)
        val shaResult = downloadAndVerifyUpdateUseCase.fetchExpectedSha256(releaseInfo)
        val expectedSha256 = (shaResult as? AppResult.Success)?.data ?: releaseInfo.expectedSha256

        // 2. Verify SHA-256 (if present) and Package Archive identity
        when (val verifyResult = downloadAndVerifyUpdateUseCase.verifyApk(
            apkFile = apkFile,
            expectedSha256 = expectedSha256,
            expectedVersionCode = releaseInfo.latestVersionCode
        )) {
            is AppResult.Success -> {
                if (packageInstallerHelper.canRequestPackageInstalls()) {
                    _uiState.value = UpdateUiState.ReadyToInstall(apkFile, releaseInfo)
                } else {
                    _uiState.value = UpdateUiState.InstallPermissionRequired(apkFile, releaseInfo)
                }
            }
            is AppResult.Error -> {
                _uiState.value = UpdateUiState.VerificationFailed(
                    reason = verifyResult.error.userMessage,
                    releaseInfo = releaseInfo
                )
            }
        }
    }

    /**
     * Checks if the install permission was granted after returning from Android Settings.
     */
    fun checkPermissionAndProceed() {
        val currentState = _uiState.value
        if (currentState is UpdateUiState.InstallPermissionRequired) {
            if (packageInstallerHelper.canRequestPackageInstalls()) {
                _uiState.value = UpdateUiState.ReadyToInstall(currentState.apkFile, currentState.releaseInfo)
            }
        }
    }

    /**
     * Launches Android's official Package Installer.
     */
    fun installApk(apkFile: File) {
        packageInstallerHelper.launchInstaller(apkFile)
    }

    /**
     * Cancels an ongoing download.
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.value = UpdateUiState.Idle
    }

    /**
     * Dismisses any active update dialog or error state back to Idle.
     */
    fun dismissDialog() {
        _uiState.value = UpdateUiState.Idle
    }

    class Factory(
        private val checkForUpdateUseCase: CheckForUpdateUseCase,
        private val downloadAndVerifyUpdateUseCase: DownloadAndVerifyUpdateUseCase,
        private val packageInstallerHelper: PackageInstallerHelper,
        private val currentVersionCode: Long = BuildConfig.VERSION_CODE.toLong(),
        private val currentVersionName: String = BuildConfig.VERSION_NAME
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UpdateViewModel(
                checkForUpdateUseCase = checkForUpdateUseCase,
                downloadAndVerifyUpdateUseCase = downloadAndVerifyUpdateUseCase,
                packageInstallerHelper = packageInstallerHelper,
                currentVersionCode = currentVersionCode,
                currentVersionName = currentVersionName
            ) as T
        }
    }
}
