package com.vinaynalavade.expensetracker.domain.model

/**
 * Encapsulates verified machine-readable release metadata retrieved from GitHub Releases.
 */
data class RemoteReleaseInfo(
    val latestVersionName: String,
    val latestVersionCode: Long,
    val apkFileName: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long,
    val sha256FileName: String? = null,
    val sha256DownloadUrl: String? = null,
    val expectedSha256: String? = null,
    val releaseNotes: String = "",
    val releaseUrl: String = "",
    val publishedAtEpochMillis: Long = System.currentTimeMillis()
)
