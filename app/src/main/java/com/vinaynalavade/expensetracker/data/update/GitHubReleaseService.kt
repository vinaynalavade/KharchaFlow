package com.vinaynalavade.expensetracker.data.update

import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.util.SimpleJsonParser
import com.vinaynalavade.expensetracker.domain.model.RemoteReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Service responsible for communicating with GitHub Releases API and parsing release metadata.
 * Works seamlessly with direct GitHub Releases, release notes tables, asset digests, and optional release.json files.
 */
class GitHubReleaseService(
    private val owner: String = "vinaynalavade",
    private val repo: String = "Leaf"
) {

    companion object {
        private const val API_BASE_URL = "https://api.github.com"
        private const val CONNECT_TIMEOUT_MS = 15000
        private const val READ_TIMEOUT_MS = 20000
        private const val USER_AGENT = "Leaf-Android-App"
        const val RELEASE_METADATA_FILENAME = "release.json"

        // Matches "| **Version Code** | `5` |", "Version Code: 5", "versionCode: 5", "<!-- versionCode: 5 -->"
        private val VERSION_CODE_REGEX = Regex("""(?i)(?:version_?code|version\s*code)[\s:=|*`]+(\d+)""")
    }

    /**
     * Fetches the latest published release and resolves its release metadata.
     */
    suspend fun fetchLatestRelease(): AppResult<RemoteReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "$API_BASE_URL/repos/$owner/$repo/releases/latest"
            val responseResult = httpGetText(endpoint)

            if (responseResult is AppResult.Error) {
                return@withContext responseResult
            }

            val releaseJsonText = (responseResult as AppResult.Success).data
            val releaseObj = try {
                SimpleJsonParser.parse(releaseJsonText) as? SimpleJsonParser.JsonObject
            } catch (e: Exception) {
                null
            } ?: return@withContext AppResult.Error(
                AppError.UpdateError(
                    message = "Invalid release JSON format from GitHub API.",
                    userMessage = "Unable to check for updates right now. Please try again."
                )
            )

            parseGitHubRelease(releaseObj)
        } catch (e: Exception) {
            AppResult.Error(
                AppError.NetworkError(
                    message = "Failed to check for updates: ${e.localizedMessage ?: e.message}",
                    cause = e,
                    userMessage = "Unable to connect to the update service. Please check your network connection and try again."
                )
            )
        }
    }

    /**
     * Parses the GitHub Release object, supporting both release.json and standard GitHub Release metadata.
     */
    suspend fun parseGitHubRelease(releaseObj: SimpleJsonParser.JsonObject): AppResult<RemoteReleaseInfo> {
        val assetsArray = releaseObj.getArray("assets")
            ?: return AppResult.Error(
                AppError.UpdateError(
                    message = "Latest release does not contain any assets.",
                    userMessage = "Unable to check for updates because the latest release contains no downloadable assets."
                )
            )

        // 1. Check if release.json asset is attached
        val releaseJsonAsset = findAssetByName(assetsArray, RELEASE_METADATA_FILENAME)
        if (releaseJsonAsset != null) {
            val releaseJsonDownloadUrl = releaseJsonAsset.getString("browser_download_url") ?: ""
            if (releaseJsonDownloadUrl.isNotBlank()) {
                val metadataResult = httpGetText(releaseJsonDownloadUrl)
                if (metadataResult is AppResult.Success) {
                    val metadataParsed = parseAndValidateReleaseMetadata(metadataResult.data, releaseObj, assetsArray)
                    if (metadataParsed is AppResult.Success) {
                        return metadataParsed
                    }
                }
            }
        }

        // 2. Direct GitHub Release Metadata Resolution
        return parseDirectReleaseMetadata(releaseObj, assetsArray)
    }

    /**
     * Resolves release info directly from standard GitHub release fields, release notes, and assets.
     */
    fun parseDirectReleaseMetadata(
        releaseObj: SimpleJsonParser.JsonObject,
        assetsArray: List<SimpleJsonParser.JsonElement>
    ): AppResult<RemoteReleaseInfo> {
        val rawTag = releaseObj.getString("tag_name") ?: ""
        val rawName = releaseObj.getString("name") ?: ""
        val body = releaseObj.getString("body") ?: ""
        val htmlUrl = releaseObj.getString("html_url") ?: "https://github.com/$owner/$repo/releases"

        val versionName = rawTag.removePrefix("v").removePrefix("V").trim().ifBlank {
            rawName.replace(Regex("(?i)(?:Leaf|KharchaFlow)\\s*v?"), "").trim().ifBlank { "1.0.0" }
        }

        val versionCode = extractVersionCode(body, releaseObj)
        if (versionCode <= 0L) {
            return AppResult.Error(
                AppError.UpdateError(
                    message = "Could not resolve remote versionCode from release metadata.",
                    userMessage = "Unable to check for updates right now. The latest release is missing version metadata."
                )
            )
        }

        val apkAsset = findApkAsset(assetsArray)
            ?: return AppResult.Error(
                AppError.UpdateError(
                    message = "No APK asset found in release.",
                    userMessage = "Unable to check for updates because no application package was found in the latest release."
                )
            )

        val apkFileName = apkAsset.getString("name") ?: "Leaf.apk"
        val apkDownloadUrl = apkAsset.getString("browser_download_url") ?: ""
        val apkSizeBytes = apkAsset.getLong("size") ?: 0L

        if (apkDownloadUrl.isBlank()) {
            return AppResult.Error(
                AppError.UpdateError(
                    message = "APK asset '$apkFileName' has an invalid download URL.",
                    userMessage = "Unable to check for updates because the application package download link is invalid."
                )
            )
        }

        // Check for .sha256 asset or GitHub asset digest
        val sha256Asset = findSha256Asset(assetsArray, apkFileName)
        val sha256DownloadUrl = sha256Asset?.getString("browser_download_url")
        val digest = apkAsset.getString("digest")
        val expectedSha256FromDigest = if (digest?.startsWith("sha256:", ignoreCase = true) == true) {
            digest.substringAfter("sha256:").trim().lowercase()
        } else {
            null
        }

        val publishedAtStr = releaseObj.getString("published_at") ?: ""
        val publishedAtEpoch = try {
            if (publishedAtStr.isNotBlank()) Instant.parse(publishedAtStr).toEpochMilli() else System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        val releaseNotes = body.ifBlank { "No release notes provided." }

        return AppResult.Success(
            RemoteReleaseInfo(
                latestVersionName = versionName,
                latestVersionCode = versionCode,
                apkFileName = apkFileName,
                apkDownloadUrl = apkDownloadUrl,
                apkSizeBytes = apkSizeBytes,
                sha256FileName = sha256Asset?.getString("name"),
                sha256DownloadUrl = sha256DownloadUrl,
                expectedSha256 = expectedSha256FromDigest,
                releaseNotes = releaseNotes,
                releaseUrl = htmlUrl,
                publishedAtEpochMillis = publishedAtEpoch
            )
        )
    }

    /**
     * Extracts versionCode from release body or release object.
     */
    fun extractVersionCode(body: String, releaseObj: SimpleJsonParser.JsonObject): Long {
        // 1. Check explicit field in release object
        val directCode = releaseObj.getLong("versionCode")
        if (directCode != null && directCode > 0L) {
            return directCode
        }

        // 2. Extract from release body table or notes
        val match = VERSION_CODE_REGEX.find(body)
        if (match != null) {
            val codeStr = match.groupValues[1]
            val parsed = codeStr.toLongOrNull()
            if (parsed != null && parsed > 0L) {
                return parsed
            }
        }

        return -1L
    }

    /**
     * Parses metadata JSON and ensures declared APK and .sha256 files exist in the release.
     */
    fun parseAndValidateReleaseMetadata(
        metadataJsonText: String,
        releaseObj: SimpleJsonParser.JsonObject,
        assetsArray: List<SimpleJsonParser.JsonElement>
    ): AppResult<RemoteReleaseInfo> {
        return try {
            val metaObj = try {
                SimpleJsonParser.parse(metadataJsonText) as? SimpleJsonParser.JsonObject
            } catch (e: Exception) {
                null
            } ?: return AppResult.Error(
                AppError.UpdateError(
                    message = "Malformed release metadata JSON.",
                    userMessage = "Unable to check for updates because the release metadata is invalid."
                )
            )

            val versionName = (metaObj.getString("versionName") ?: "").trim()
            if (versionName.isBlank()) {
                return AppResult.Error(
                    AppError.UpdateError(
                        message = "Metadata error: 'versionName' is missing or empty.",
                        userMessage = "Unable to check for updates because the release version information is missing."
                    )
                )
            }

            val versionCode = metaObj.getLong("versionCode") ?: -1L
            if (versionCode <= 0L) {
                return AppResult.Error(
                    AppError.UpdateError(
                        message = "Metadata error: 'versionCode' is missing or invalid ($versionCode).",
                        userMessage = "Unable to check for updates because the release version code is invalid."
                    )
                )
            }

            val apkFileName = (metaObj.getString("apkFileName") ?: "").trim()
            if (apkFileName.isBlank() || !apkFileName.endsWith(".apk", ignoreCase = true)) {
                return AppResult.Error(
                    AppError.UpdateError(
                        message = "Metadata error: 'apkFileName' is missing or not a valid .apk file.",
                        userMessage = "Unable to check for updates because the release package specification is invalid."
                    )
                )
            }

            // Cross-check declared APK asset
            val apkAsset = findAssetByName(assetsArray, apkFileName)
                ?: return AppResult.Error(
                    AppError.UpdateError(
                        message = "Declared APK asset '$apkFileName' was not found in release assets.",
                        userMessage = "Unable to check for updates because the application package is missing from the release."
                    )
                )

            val apkDownloadUrl = (apkAsset.getString("browser_download_url") ?: "").trim()
            val apkSizeBytes = apkAsset.getLong("size") ?: 0L
            if (apkDownloadUrl.isBlank()) {
                return AppResult.Error(
                    AppError.UpdateError(
                        message = "APK asset '$apkFileName' has an invalid download URL.",
                        userMessage = "Unable to check for updates because the application package download link is invalid."
                    )
                )
            }

            // Optional SHA-256 asset
            val sha256FileName = (metaObj.getString("sha256FileName") ?: "").trim()
            val sha256Asset = if (sha256FileName.isNotBlank()) findAssetByName(assetsArray, sha256FileName) else null
            val sha256DownloadUrl = sha256Asset?.getString("browser_download_url")?.trim()

            val releaseNotes = metaObj.getString("releaseNotes")?.takeIf { it.isNotBlank() }
                ?: releaseObj.getString("body")
                ?: "No release notes provided."

            val releaseUrl = metaObj.getString("releaseUrl")?.takeIf { it.isNotBlank() }
                ?: releaseObj.getString("html_url")
                ?: "https://github.com/$owner/$repo/releases"

            val publishedAtStr = releaseObj.getString("published_at") ?: ""
            val publishedAtEpoch = try {
                if (publishedAtStr.isNotBlank()) Instant.parse(publishedAtStr).toEpochMilli() else System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }

            AppResult.Success(
                RemoteReleaseInfo(
                    latestVersionName = versionName,
                    latestVersionCode = versionCode,
                    apkFileName = apkFileName,
                    apkDownloadUrl = apkDownloadUrl,
                    apkSizeBytes = apkSizeBytes,
                    sha256FileName = sha256FileName.ifBlank { null },
                    sha256DownloadUrl = sha256DownloadUrl,
                    expectedSha256 = metaObj.getString("sha256"),
                    releaseNotes = releaseNotes,
                    releaseUrl = releaseUrl,
                    publishedAtEpochMillis = publishedAtEpoch
                )
            )
        } catch (e: Exception) {
            AppResult.Error(
                AppError.UpdateError(
                    message = "Failed to parse release metadata: ${e.localizedMessage ?: e.message}",
                    cause = e,
                    userMessage = "Unable to check for updates because the release metadata could not be processed."
                )
            )
        }
    }

    /**
     * Finds the best release APK asset from the release assets list.
     */
    fun findApkAsset(assets: List<SimpleJsonParser.JsonElement>): SimpleJsonParser.JsonObject? {
        val apkAssets = assets.mapNotNull { it as? SimpleJsonParser.JsonObject }
            .filter { (it.getString("name") ?: "").endsWith(".apk", ignoreCase = true) }
            .filterNot { (it.getString("name") ?: "").contains("debug", ignoreCase = true) }

        // Prefer Leaf or legacy KharchaFlow named APK
        return apkAssets.firstOrNull { (it.getString("name") ?: "").contains("Leaf", ignoreCase = true) }
            ?: apkAssets.firstOrNull { (it.getString("name") ?: "").contains("KharchaFlow", ignoreCase = true) }
            ?: apkAssets.firstOrNull()
    }

    private fun findSha256Asset(assets: List<SimpleJsonParser.JsonElement>, apkFileName: String): SimpleJsonParser.JsonObject? {
        return findAssetByName(assets, "$apkFileName.sha256")
            ?: assets.mapNotNull { it as? SimpleJsonParser.JsonObject }
                .firstOrNull { (it.getString("name") ?: "").endsWith(".sha256", ignoreCase = true) }
    }

    private fun findAssetByName(assets: List<SimpleJsonParser.JsonElement>, targetName: String): SimpleJsonParser.JsonObject? {
        for (item in assets) {
            val asset = item as? SimpleJsonParser.JsonObject ?: continue
            val name = asset.getString("name")
            if (name.equals(targetName, ignoreCase = true)) {
                return asset
            }
        }
        return null
    }

    /**
     * Fetches raw text from a given HTTPS URL.
     */
    suspend fun fetchPlainText(url: String): AppResult<String> = withContext(Dispatchers.IO) {
        httpGetText(url)
    }

    private fun httpGetText(urlString: String): AppResult<String> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json, text/plain, */*")
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                    val content = reader.use { it.readText() }
                    AppResult.Success(content)
                }
                HttpURLConnection.HTTP_FORBIDDEN -> {
                    AppResult.Error(
                        AppError.NetworkError(
                            message = "GitHub API rate limit reached.",
                            userMessage = "Update service is temporarily busy. Please try again in a few minutes."
                        )
                    )
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    AppResult.Error(
                        AppError.NotFound(
                            message = "No release found on GitHub.",
                            userMessage = "No published releases found."
                        )
                    )
                }
                else -> {
                    AppResult.Error(
                        AppError.NetworkError(
                            message = "GitHub returned HTTP error $responseCode.",
                            userMessage = "Unable to check for updates right now. Please try again later."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppResult.Error(
                AppError.NetworkError(
                    message = "Network request failed: ${e.localizedMessage ?: e.message}",
                    cause = e,
                    userMessage = "Unable to connect to the update service. Please check your network connection."
                )
            )
        } finally {
            connection?.disconnect()
        }
    }
}
