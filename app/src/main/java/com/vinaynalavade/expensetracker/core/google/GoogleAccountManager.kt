package com.vinaynalavade.expensetracker.core.google

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.vinaynalavade.expensetracker.core.result.AppError
import com.vinaynalavade.expensetracker.core.result.AppResult
import com.vinaynalavade.expensetracker.domain.model.GoogleAccountInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Manages Google Sign-In authentication, scopes, and OAuth2 access token acquisition
 * for Google Drive appDataFolder access.
 */
class GoogleAccountManager(private val context: Context) {

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val OAUTH_SCOPE_PREFIX = "oauth2:$DRIVE_APPDATA_SCOPE"
    }

    private val gso: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(Scopes.DRIVE_APPFOLDER))
            .build()
    }

    private val client: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return client.signInIntent
    }

    fun parseSignInResult(data: Intent?): AppResult<GoogleAccountInfo> {
        if (data == null) {
            return AppResult.Error(AppError.UnknownError("Google sign-in was cancelled."))
        }

        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val email = account.email
            if (email.isNullOrBlank()) {
                AppResult.Error(AppError.UnknownError("Unable to retrieve email from Google Account."))
            } else {
                AppResult.Success(
                    GoogleAccountInfo(
                        email = email,
                        displayName = account.displayName,
                        photoUrl = account.photoUrl?.toString()
                    )
                )
            }
        } catch (e: ApiException) {
            val msg = when (e.statusCode) {
                12501 -> "Google sign-in was cancelled."
                12500 -> "Google Sign-In failed. Please check Google Play Services."
                7 -> "Network error occurred during Google Sign-In."
                else -> "Google sign-in failed (Code: ${e.statusCode}). Please try again."
            }
            AppResult.Error(AppError.UnknownError(msg, e))
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError(e.message ?: "Google sign-in failed.", e))
        }
    }

    fun getLastSignedInAccount(): GoogleAccountInfo? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val email = account.email ?: return null
        return GoogleAccountInfo(
            email = email,
            displayName = account.displayName,
            photoUrl = account.photoUrl?.toString()
        )
    }

    suspend fun getOAuthAccessToken(email: String): AppResult<String> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext AppResult.Error(AppError.UnknownError("No internet connection available."))
        }

        try {
            val account = Account(email, "com.google")
            val token = GoogleAuthUtil.getToken(context, account, OAUTH_SCOPE_PREFIX)
            if (token.isNullOrBlank()) {
                AppResult.Error(AppError.UnknownError("Failed to acquire Google Drive authorization token."))
            } else {
                AppResult.Success(token)
            }
        } catch (e: GoogleAuthException) {
            AppResult.Error(AppError.UnknownError("Google authorization failed: ${e.message}", e))
        } catch (e: IOException) {
            AppResult.Error(AppError.UnknownError("Network error contacting Google Drive.", e))
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError("Authentication error: ${e.message}", e))
        }
    }

    suspend fun invalidateToken(token: String) = withContext(Dispatchers.IO) {
        try {
            GoogleAuthUtil.clearToken(context, token)
        } catch (_: Exception) {}
    }

    suspend fun signOut(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            Tasks.await(client.signOut())
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.UnknownError("Error signing out: ${e.message}", e))
        }
    }

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
