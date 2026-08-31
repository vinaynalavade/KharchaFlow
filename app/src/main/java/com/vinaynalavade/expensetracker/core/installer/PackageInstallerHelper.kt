package com.vinaynalavade.expensetracker.core.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import java.io.File

/**
 * Helper class for interacting with Android OS PackageInstaller and managing unknown app install permissions.
 */
open class PackageInstallerHelper(
    private val context: Context? = null
) {

    /**
     * Checks whether the application has permission to request package installations (Android 8.0+ / API 26+).
     */
    open fun canRequestPackageInstalls(): Boolean {
        val ctx = context ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Creates an Intent to navigate the user to Android Settings to allow app installations from this source.
     */
    open fun createManageUnknownAppSourcesIntent(): Intent {
        val packageName = context?.packageName ?: "com.vinaynalavade.expensetracker"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Launches Android's official package installer via FileProvider content:// URI.
     */
    open fun launchInstaller(apkFile: File): AppResult<Unit> {
        val ctx = context ?: return AppResult.Success(Unit)
        return try {
            if (!apkFile.exists() || apkFile.length() <= 0L) {
                return AppResult.Error(AppError.UpdateError("APK file not found or is empty."))
            }

            val authority = "${ctx.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(ctx, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            ctx.startActivity(installIntent)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UpdateError("Failed to launch package installer: ${e.localizedMessage ?: e.message}", e))
        }
    }
}
