package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.core.util.SimpleJsonParser
import com.vinaynalavade.expensetracker.data.update.ApkVerifier
import com.vinaynalavade.expensetracker.data.update.GitHubReleaseService
import com.vinaynalavade.expensetracker.domain.model.RemoteReleaseInfo
import com.vinaynalavade.expensetracker.domain.model.UpdateCheckResult
import com.vinaynalavade.expensetracker.domain.usecase.CheckForUpdateUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

/**
 * Comprehensive test suite for the Leaf In-App APK Update System.
 */
class AppUpdateSystemTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // =========================================================================
    // 1. GitHub Release Parsing Tests (Leaf & Legacy KharchaFlow)
    // =========================================================================

    @Test
    fun testParseRealWorldLeafRelease() {
        val service = GitHubReleaseService()

        val rawJson = """
        {
          "tag_name": "v1.0.5",
          "name": "Leaf v1.0.5",
          "html_url": "https://github.com/vinaynalavade/Leaf/releases/tag/v1.0.5",
          "published_at": "2026-09-03T10:00:00Z",
          "body": "## What's New\r\n\r\n- Brand migration to Leaf\r\n- Edit transaction time feature\r\n\r\n## 📦 Version Information\r\n| | |\r\n|---|---|\r\n| **Version** | `1.0.5` |\r\n| **Version Code** | `6` |\r\n",
          "assets": [
            {
              "name": "Leaf_v1.0.5.apk",
              "content_type": "application/vnd.android.package-archive",
              "size": 3953040,
              "digest": "sha256:8f96ca10295c6368d5981b47942a7b2dbef22e4905c92b93bf6cb34f4225fbc7",
              "browser_download_url": "https://github.com/vinaynalavade/Leaf/releases/download/v1.0.5/Leaf_v1.0.5.apk"
            }
          ]
        }
        """.trimIndent()

        val parsed = SimpleJsonParser.parse(rawJson) as SimpleJsonParser.JsonObject
        val result = runBlocking { service.parseGitHubRelease(parsed) }

        assertTrue("Expected parsing to succeed", result is AppResult.Success)
        val info = (result as AppResult.Success).data

        assertEquals("1.0.5", info.latestVersionName)
        assertEquals(6L, info.latestVersionCode)
        assertEquals("Leaf_v1.0.5.apk", info.apkFileName)
        assertEquals("https://github.com/vinaynalavade/Leaf/releases/download/v1.0.5/Leaf_v1.0.5.apk", info.apkDownloadUrl)
        assertEquals(3953040L, info.apkSizeBytes)
        assertEquals("8f96ca10295c6368d5981b47942a7b2dbef22e4905c92b93bf6cb34f4225fbc7", info.expectedSha256)
    }

    @Test
    fun testParseLegacyKharchaFlowV104Release() {
        val service = GitHubReleaseService(owner = "vinaynalavade", repo = "KharchaFlow")

        val rawJson = """
        {
          "tag_name": "v1.0.4",
          "name": "KharchaFlow v1.0.4",
          "html_url": "https://github.com/vinaynalavade/KharchaFlow/releases/tag/v1.0.4",
          "published_at": "2026-08-24T10:22:41Z",
          "body": "## What's New\r\n\r\n- Bug fixes and stability improvements.\r\n\r\n## 📦 Version Information\r\n| | |\r\n|---|---|\r\n| **Version** | `1.0.4` |\r\n| **Version Code** | `5` |\r\n",
          "assets": [
            {
              "name": "KharchaFlow_v1.0.4.apk",
              "content_type": "application/vnd.android.package-archive",
              "size": 3953040,
              "digest": "sha256:9ef32daef27bde5afb2a29963e21219507ffa3984f1f64f52d7dae2424e0d909",
              "browser_download_url": "https://github.com/vinaynalavade/KharchaFlow/releases/download/v1.0.4/KharchaFlow_v1.0.4.apk"
            }
          ]
        }
        """.trimIndent()

        val parsed = SimpleJsonParser.parse(rawJson) as SimpleJsonParser.JsonObject
        val result = runBlocking { service.parseGitHubRelease(parsed) }

        assertTrue("Expected parsing to succeed", result is AppResult.Success)
        val info = (result as AppResult.Success).data

        assertEquals("1.0.4", info.latestVersionName)
        assertEquals(5L, info.latestVersionCode)
        assertEquals("KharchaFlow_v1.0.4.apk", info.apkFileName)
        assertEquals("https://github.com/vinaynalavade/KharchaFlow/releases/download/v1.0.4/KharchaFlow_v1.0.4.apk", info.apkDownloadUrl)
        assertEquals(3953040L, info.apkSizeBytes)
        assertEquals("9ef32daef27bde5afb2a29963e21219507ffa3984f1f64f52d7dae2424e0d909", info.expectedSha256)
    }

    @Test
    fun testParseReleaseWithDedicatedReleaseJsonAsset() {
        val service = GitHubReleaseService()

        val metaJson = """
        {
          "versionName": "1.0.5",
          "versionCode": 6,
          "apkFileName": "Leaf-v1.0.5.apk",
          "sha256FileName": "Leaf-v1.0.5.apk.sha256",
          "sha256": "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
          "releaseNotes": "Release with manifest"
        }
        """.trimIndent()

        val rawRelease = """
        {
          "tag_name": "v1.0.5",
          "body": "Fallback body",
          "html_url": "https://github.com/vinaynalavade/Leaf/releases/tag/v1.0.5",
          "assets": [
            {
              "name": "Leaf-v1.0.5.apk",
              "size": 4000000,
              "browser_download_url": "https://example.com/Leaf-v1.0.5.apk"
            },
            {
              "name": "Leaf-v1.0.5.apk.sha256",
              "size": 64,
              "browser_download_url": "https://example.com/Leaf-v1.0.5.apk.sha256"
            }
          ]
        }
        """.trimIndent()

        val releaseObj = SimpleJsonParser.parse(rawRelease) as SimpleJsonParser.JsonObject
        val assets = releaseObj.getArray("assets")!!

        val result = service.parseAndValidateReleaseMetadata(metaJson, releaseObj, assets)

        assertTrue(result is AppResult.Success)
        val info = (result as AppResult.Success).data
        assertEquals("1.0.5", info.latestVersionName)
        assertEquals(6L, info.latestVersionCode)
        assertEquals("Leaf-v1.0.5.apk", info.apkFileName)
        assertEquals("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890", info.expectedSha256)
    }

    // =========================================================================
    // 2. Version Code Comparison Logic Tests
    // =========================================================================

    @Test
    fun testUpdateCheck_CurrentVersionCode_ReportsUpToDate() {
        val fakeRepo = createFakeRepository(
            RemoteReleaseInfo(
                latestVersionName = "1.0.5",
                latestVersionCode = 6L,
                apkFileName = "Leaf_v1.0.5.apk",
                apkDownloadUrl = "https://example.com/apk",
                apkSizeBytes = 3000000L
            )
        )
        val useCase = CheckForUpdateUseCase(fakeRepo)

        val result = runBlocking { useCase(localVersionCode = 6L, localVersionName = "1.0.5") }
        assertTrue(result is AppResult.Success)

        val checkResult = (result as AppResult.Success).data
        assertTrue("Installed version 6 == remote 6 must report UpToDate", checkResult is UpdateCheckResult.UpToDate)
        assertEquals("1.0.5", (checkResult as UpdateCheckResult.UpToDate).currentVersionName)
        assertEquals(6L, checkResult.currentVersionCode)
    }

    @Test
    fun testUpdateCheck_NewerVersionCode_ReportsUpdateAvailable() {
        val fakeRepo = createFakeRepository(
            RemoteReleaseInfo(
                latestVersionName = "1.0.6",
                latestVersionCode = 7L,
                apkFileName = "Leaf-v1.0.6.apk",
                apkDownloadUrl = "https://example.com/apk",
                apkSizeBytes = 4000000L,
                releaseNotes = "New features in 1.0.6"
            )
        )
        val useCase = CheckForUpdateUseCase(fakeRepo)

        val result = runBlocking { useCase(localVersionCode = 6L, localVersionName = "1.0.5") }
        assertTrue(result is AppResult.Success)

        val checkResult = (result as AppResult.Success).data
        assertTrue("Remote 7 > installed 6 must report UpdateAvailable", checkResult is UpdateCheckResult.UpdateAvailable)
        val available = checkResult as UpdateCheckResult.UpdateAvailable
        assertEquals("1.0.6", available.releaseInfo.latestVersionName)
        assertEquals(7L, available.releaseInfo.latestVersionCode)
    }

    @Test
    fun testUpdateCheck_OlderRemoteVersionCode_RejectsDowngrade() {
        val fakeRepo = createFakeRepository(
            RemoteReleaseInfo(
                latestVersionName = "1.0.4",
                latestVersionCode = 5L,
                apkFileName = "Leaf_v1.0.4.apk",
                apkDownloadUrl = "https://example.com/apk",
                apkSizeBytes = 2500000L
            )
        )
        val useCase = CheckForUpdateUseCase(fakeRepo)

        val result = runBlocking { useCase(localVersionCode = 6L, localVersionName = "1.0.5") }
        assertTrue(result is AppResult.Success)

        val checkResult = (result as AppResult.Success).data
        assertTrue("Remote 5 < installed 6 must be treated as UpToDate (downgrade prevention)", checkResult is UpdateCheckResult.UpToDate)
    }

    // =========================================================================
    // 3. Version Code Extraction Patterns Tests
    // =========================================================================

    @Test
    fun testExtractVersionCodePatterns() {
        val service = GitHubReleaseService()
        val emptyObj = SimpleJsonParser.JsonObject(emptyMap())

        // Table format
        val body1 = "| **Version Code** | `6` |"
        assertEquals(6L, service.extractVersionCode(body1, emptyObj))

        // Unquoted table format
        val body2 = "| **Version Code** | 6 |"
        assertEquals(6L, service.extractVersionCode(body2, emptyObj))

        // Colon format
        val body3 = "Version Code: 6"
        assertEquals(6L, service.extractVersionCode(body3, emptyObj))

        // CamelCase format
        val body4 = "versionCode: 7"
        assertEquals(7L, service.extractVersionCode(body4, emptyObj))

        // HTML comment format
        val body5 = "<!-- versionCode: 8 -->"
        assertEquals(8L, service.extractVersionCode(body5, emptyObj))

        // No version code in body
        val bodyNone = "Just a general release without code."
        assertEquals(-1L, service.extractVersionCode(bodyNone, emptyObj))
    }

    // =========================================================================
    // 4. SHA-256 Checksum Verification and Deletion on Mismatch Tests
    // =========================================================================

    @Test
    fun testSha256Verification_Matches_Passes() {
        val verifier = ApkVerifier(context = null)
        val testFile = tempFolder.newFile("test.apk")
        FileOutputStream(testFile).use { it.write("test apk binary payload".toByteArray(Charsets.UTF_8)) }

        val actualSha256 = verifier.computeSha256(testFile)

        val result = runBlocking {
            verifier.verifyApk(
                apkFile = testFile,
                expectedSha256Raw = actualSha256,
                expectedVersionCode = 6L
            )
        }

        assertTrue(result is AppResult.Success)
        assertTrue(testFile.exists())
    }

    @Test
    fun testSha256Verification_Mismatch_FailsAndDeletesFile() {
        val verifier = ApkVerifier(context = null)
        val testFile = tempFolder.newFile("tampered.apk")
        FileOutputStream(testFile).use { it.write("corrupted content".toByteArray(Charsets.UTF_8)) }

        val badExpectedSha256 = "0000000000000000000000000000000000000000000000000000000000000000"

        val result = runBlocking {
            verifier.verifyApk(
                apkFile = testFile,
                expectedSha256Raw = badExpectedSha256,
                expectedVersionCode = 6L
            )
        }

        assertTrue("Checksum mismatch must return error", result is AppResult.Error)
        assertFalse("Tampered APK must be deleted immediately", testFile.exists())
    }

    @Test
    fun testSha256ExtractionFormats() {
        val verifier = ApkVerifier(context = null)
        val clean64 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

        assertEquals(clean64, verifier.extractCleanChecksum(clean64))
        assertEquals(clean64, verifier.extractCleanChecksum("E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855"))
        assertEquals(clean64, verifier.extractCleanChecksum("\n  $clean64 \r\n"))
        assertEquals(clean64, verifier.extractCleanChecksum("$clean64  Leaf-v1.0.5.apk\n"))
    }

    // =========================================================================
    // 5. APK Asset Selection Tests
    // =========================================================================

    @Test
    fun testFindApkAsset_IgnoresDebug_PrefersLeaf() {
        val service = GitHubReleaseService()

        val assetsJson = """
        [
          {"name": "app-debug.apk", "browser_download_url": "https://example.com/debug.apk", "size": 100},
          {"name": "source.zip", "browser_download_url": "https://example.com/source.zip", "size": 200},
          {"name": "Leaf_v1.0.5.apk", "browser_download_url": "https://example.com/leaf.apk", "size": 300},
          {"name": "Leaf_v1.0.5.apk.sha256", "browser_download_url": "https://example.com/sha", "size": 64}
        ]
        """.trimIndent()

        val assets = (SimpleJsonParser.parse(assetsJson) as SimpleJsonParser.JsonArray).list
        val chosen = service.findApkAsset(assets)

        assertNotNull(chosen)
        assertEquals("Leaf_v1.0.5.apk", chosen!!.getString("name"))
    }

    // =========================================================================
    // 6. Error Handling Tests
    // =========================================================================

    @Test
    fun testReleaseWithoutApk_ReturnsUserFriendlyError() {
        val service = GitHubReleaseService()

        val rawJson = """
        {
          "tag_name": "v1.0.5",
          "body": "| **Version Code** | `6` |",
          "assets": [
            {"name": "notes.txt", "browser_download_url": "https://example.com/notes.txt", "size": 10}
          ]
        }
        """.trimIndent()

        val releaseObj = SimpleJsonParser.parse(rawJson) as SimpleJsonParser.JsonObject
        val result = runBlocking { service.parseGitHubRelease(releaseObj) }

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertEquals("Unable to check for updates because no application package was found in the latest release.", error.userMessage)
    }

    @Test
    fun testReleaseWithoutVersionCode_ReturnsUserFriendlyError() {
        val service = GitHubReleaseService()

        val rawJson = """
        {
          "tag_name": "v1.0.5",
          "body": "No version information in this body",
          "assets": [
            {"name": "Leaf_v1.0.5.apk", "browser_download_url": "https://example.com/apk", "size": 1000}
          ]
        }
        """.trimIndent()

        val releaseObj = SimpleJsonParser.parse(rawJson) as SimpleJsonParser.JsonObject
        val result = runBlocking { service.parseGitHubRelease(releaseObj) }

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertEquals("Unable to check for updates right now. The latest release is missing version metadata.", error.userMessage)
    }

    private fun createFakeRepository(remoteInfo: RemoteReleaseInfo): com.vinaynalavade.expensetracker.domain.repository.UpdateRepository {
        return object : com.vinaynalavade.expensetracker.domain.repository.UpdateRepository {
            override suspend fun fetchLatestRelease(): AppResult<RemoteReleaseInfo> {
                return AppResult.Success(remoteInfo)
            }

            override fun downloadApk(
                releaseInfo: RemoteReleaseInfo,
                destinationFile: File
            ): kotlinx.coroutines.flow.Flow<AppResult<com.vinaynalavade.expensetracker.domain.model.DownloadProgress>> {
                return kotlinx.coroutines.flow.flowOf(
                    AppResult.Success(
                        com.vinaynalavade.expensetracker.domain.model.DownloadProgress(
                            bytesDownloaded = releaseInfo.apkSizeBytes,
                            totalBytes = releaseInfo.apkSizeBytes,
                            progressPercentage = 100
                        )
                    )
                )
            }

            override suspend fun fetchExpectedSha256(releaseInfo: RemoteReleaseInfo): AppResult<String?> {
                return AppResult.Success(releaseInfo.expectedSha256)
            }

            override suspend fun verifyDownloadedApk(
                apkFile: File,
                expectedSha256: String?,
                expectedVersionCode: Long
            ): AppResult<Unit> {
                return AppResult.Success(Unit)
            }

            override fun getUpdateTargetFile(releaseInfo: RemoteReleaseInfo): File {
                return File(tempFolder.root, releaseInfo.apkFileName)
            }

            override fun cleanStaleUpdateFiles(): AppResult<Unit> {
                return AppResult.Success(Unit)
            }
        }
    }
}
