package com.vinaynalavade.expensetracker.data.update

import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.DownloadProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads APK files over HTTPS in streaming chunks, reporting continuous progress.
 */
class ApkDownloader {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 20000
        private const val READ_TIMEOUT_MS = 30000
        private const val BUFFER_SIZE = 8192
        private const val USER_AGENT = "KharchaFlow-Android-App"
        private const val TEMP_FILE_SUFFIX = ".tmp"
    }

    /**
     * Streams the APK download into a temporary file, then renames to destinationFile.
     */
    fun download(
        downloadUrl: String,
        destinationFile: File
    ): Flow<AppResult<DownloadProgress>> = flow {
        val parentDir = destinationFile.parentFile ?: destinationFile.canonicalFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }

        val tempFile = File(parentDir, "${destinationFile.name}$TEMP_FILE_SUFFIX")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            connection = openConnectionWithRedirects(downloadUrl)

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                emit(AppResult.Error(AppError.NetworkError("Failed to download APK: HTTP $responseCode")))
                return@flow
            }

            val totalBytes = connection.contentLengthLong.let { if (it > 0) it else -1L }
            inputStream = connection.inputStream
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesDownloaded = 0L
            var lastEmittedPercentage = -1

            emit(AppResult.Success(DownloadProgress(0L, totalBytes, 0)))

            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead

                val percentage = if (totalBytes > 0L) {
                    ((bytesDownloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                } else {
                    -1
                }

                if (percentage != lastEmittedPercentage) {
                    lastEmittedPercentage = percentage
                    emit(AppResult.Success(DownloadProgress(bytesDownloaded, totalBytes, percentage)))
                }
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null

            inputStream.close()
            inputStream = null

            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val renamed = tempFile.renameTo(destinationFile)
            if (!renamed) {
                // Fallback copy if rename fails across file systems
                tempFile.copyTo(destinationFile, overwrite = true)
                tempFile.delete()
            }

            emit(AppResult.Success(DownloadProgress(bytesDownloaded, totalBytes, 100)))
        } catch (ce: CancellationException) {
            tempFile.delete()
            throw ce
        } catch (e: Exception) {
            tempFile.delete()
            emit(AppResult.Error(AppError.NetworkError("Download failed: ${e.localizedMessage ?: e.message}", e)))
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            try {
                inputStream?.close()
            } catch (_: Exception) {}
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun openConnectionWithRedirects(initialUrl: String, maxRedirects: Int = 5): HttpURLConnection {
        var currentUrl = initialUrl
        var redirects = 0

        while (redirects < maxRedirects) {
            val url = URL(currentUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = false
            }

            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER ||
                status == 307 ||
                status == 308
            ) {
                val newUrl = connection.getHeaderField("Location")
                connection.disconnect()
                if (newUrl.isNullOrBlank()) {
                    throw IllegalStateException("Redirect requested without Location header")
                }
                currentUrl = newUrl
                redirects++
            } else {
                return connection
            }
        }
        throw IllegalStateException("Too many redirects ($redirects)")
    }
}
