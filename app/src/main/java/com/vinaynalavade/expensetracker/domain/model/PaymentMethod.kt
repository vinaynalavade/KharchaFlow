package com.vinaynalavade.expensetracker.domain.model

/**
 * Strongly typed representation of financial payment methods / sources.
 * In Phase 2, standardized strictly to Cash and Account.
 */
enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    ACCOUNT("Account");

    companion object {
        val DEFAULT = CASH

        fun fromString(value: String?): PaymentMethod {
            if (value == null) return CASH
            val normalized = value.trim().uppercase().replace(" ", "_")
            return when (normalized) {
                "CASH" -> CASH
                "ACCOUNT", "BANK_ACCOUNT", "UPI", "BANK", "CARD", "NET_BANKING" -> ACCOUNT
                else -> CASH
            }
        }
    }
}

