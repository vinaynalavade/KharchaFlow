package com.vinaynalavade.expensetracker.core.google

import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

/**
 * Lightweight, dependency-free Google Drive v3 REST API client communicating with appDataFolder.
 */
class GoogleDriveRestService {

    companion object {
        const val BACKUP_FILENAME = "kharchaflow_backup.json"
        private const val BASE_API_URL = "https://www.googleapis.com/drive/v3"
        private const val BASE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3"
    }

    /**
     * Searches for the primary KharchaFlow backup file inside the user's appDataFolder.
     */
    suspend fun findBackupFile(accessToken: String): AppResult<GoogleBackupMetadata?> = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode("name = '$BACKUP_FILENAME' and trashed = false", "UTF-8")
            val urlString = "$BASE_API_URL/files?spaces=appDataFolder&q=$query&fields=files(id,name,modifiedTime,size)"
            val url = URL(urlString)

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val files = json.optJSONArray("files")

                if (files != null && files.length() > 0) {
                    val firstFile = files.getJSONObject(0)
                    val fileId = firstFile.getString("id")
                    val modifiedTimeStr = firstFile.optString("modifiedTime")
                    val sizeBytes = firstFile.optLong("size", 0L)
                    val epochMillis = try {
                        Instant.parse(modifiedTimeStr).toEpochMilli()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }

                    AppResult.Success(
                        GoogleBackupMetadata(
                            fileId = fileId,
                            modifiedTime = epochMillis,
                            sizeBytes = sizeBytes
                        )
                    )
                } else {
                    AppResult.Success(null)
                }
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                AppResult.Error(AppError.UnknownError("Google authorization expired (401). Please reconnect your account."))
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                AppResult.Error(AppError.UnknownError("Failed to query Google Drive (Code: $responseCode): $errorStream"))
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError("Network error querying Google Drive: ${e.message}", e))
        }
    }

    /**
     * Uploads a new backup file to appDataFolder using a multipart upload request.
     */
    suspend fun createBackupFile(accessToken: String, jsonContent: String): AppResult<GoogleBackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val boundary = "===============${System.currentTimeMillis()}=="
            val url = URL("$BASE_UPLOAD_URL/files?uploadType=multipart")

            val metadataJson = JSONObject().apply {
                put("name", BACKUP_FILENAME)
                put("parents", org.json.JSONArray().apply { put("appDataFolder") })
                put("description", "KharchaFlow automatic application backup")
            }.toString()

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                connectTimeout = 20000
                readTimeout = 20000
            }

            connection.outputStream.use { os ->
                val writer = OutputStreamWriter(os, Charsets.UTF_8)
                // Part 1: Metadata
                writer.write("--$boundary\r\n")
                writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                writer.write(metadataJson)
                writer.write("\r\n")

                // Part 2: Media Payload
                writer.write("--$boundary\r\n")
                writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                writer.write(jsonContent)
                writer.write("\r\n")

                // End of multipart
                writer.write("--$boundary--\r\n")
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val fileId = json.getString("id")
                val modifiedTimeStr = json.optString("modifiedTime")
                val sizeBytes = json.optLong("size", jsonContent.toByteArray().size.toLong())
                val epochMillis = try {
                    Instant.parse(modifiedTimeStr).toEpochMilli()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }

                AppResult.Success(
                    GoogleBackupMetadata(
                        fileId = fileId,
                        modifiedTime = epochMillis,
                        sizeBytes = sizeBytes
                    )
                )
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() }
                AppResult.Error(AppError.UnknownError("Failed to upload backup to Google Drive (Code: $responseCode): $err"))
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError("Network error uploading backup to Google Drive: ${e.message}", e))
        }
    }

    /**
     * Updates the media content of an existing backup file in Google Drive.
     */
    suspend fun updateBackupFile(accessToken: String, fileId: String, jsonContent: String): AppResult<GoogleBackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_UPLOAD_URL/files/$fileId?uploadType=media")

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 20000
                readTimeout = 20000
            }

            connection.outputStream.use { os ->
                OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                    writer.write(jsonContent)
                }
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val id = json.getString("id")
                val modifiedTimeStr = json.optString("modifiedTime")
                val sizeBytes = json.optLong("size", jsonContent.toByteArray().size.toLong())
                val epochMillis = try {
                    Instant.parse(modifiedTimeStr).toEpochMilli()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }

                AppResult.Success(
                    GoogleBackupMetadata(
                        fileId = id,
                        modifiedTime = epochMillis,
                        sizeBytes = sizeBytes
                    )
                )
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() }
                AppResult.Error(AppError.UnknownError("Failed to update Google Drive backup (Code: $responseCode): $err"))
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError("Network error updating backup on Google Drive: ${e.message}", e))
        }
    }

    /**
     * Downloads the raw JSON backup string from Google Drive.
     */
    suspend fun downloadBackupFile(accessToken: String, fileId: String): AppResult<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_API_URL/files/$fileId?alt=media")

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = 20000
                readTimeout = 20000
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonContent = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                AppResult.Success(jsonContent)
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                AppResult.Error(AppError.NotFound("No KharchaFlow backup was found in this Google account."))
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() }
                AppResult.Error(AppError.UnknownError("Failed to download backup from Google Drive (Code: $responseCode): $err"))
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError("Network error downloading backup from Google Drive: ${e.message}", e))
        }
    }
}
