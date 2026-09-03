package com.vinaynalavade.expensetracker.domain.model

/**
 * Domain representation of a connected Google Account.
 */
data class GoogleAccountInfo(
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null
)

/**
 * Metadata for a Leaf backup file stored in Google Drive appDataFolder.
 */
data class GoogleBackupMetadata(
    val fileId: String,
    val modifiedTime: Long,
    val sizeBytes: Long
)

/**
 * State representing Google Drive backup connectivity.
 */
sealed interface GoogleBackupState {
    data object Disconnected : GoogleBackupState

    data class Connected(
        val account: GoogleAccountInfo,
        val lastBackupTimestamp: Long? = null,
        val cloudBackupExists: Boolean = false
    ) : GoogleBackupState
}
