package com.vinaynalavade.expensetracker.domain.model

/**
 * Strongly typed representation of financial payment methods / sources.
 */
enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    BANK_ACCOUNT("Bank Account"),
    UPI("UPI");

    companion object {
        fun fromString(value: String): PaymentMethod {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: CASH
        }
    }
}
