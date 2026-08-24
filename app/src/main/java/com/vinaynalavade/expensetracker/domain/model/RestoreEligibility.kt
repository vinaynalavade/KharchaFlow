package com.vinaynalavade.expensetracker.domain.model

sealed interface RestoreEligibility {
    data class Eligible(
        val metadata: GoogleBackupMetadata,
        val formattedDate: String,
        val hasExistingLocalData: Boolean
    ) : RestoreEligibility

    data object NotEligible : RestoreEligibility
}
