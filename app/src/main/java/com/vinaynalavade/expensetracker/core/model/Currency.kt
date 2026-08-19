package com.vinaynalavade.expensetracker.core.model

/**
 * Currency configuration abstraction for KharchaFlow.
 * Designed to cleanly support any ISO currency with exact subunit scaling.
 *
 * @param code ISO 4217 currency code (e.g. "INR", "USD", "EUR")
 * @param symbol The display symbol (e.g. "₹", "$", "€")
 * @param name The human-readable currency name (e.g. "Indian Rupee")
 * @param subunitName The minor currency unit name (e.g. "Paise", "Cent")
 * @param decimalDigits Number of fractional digits (e.g. 2 for INR/USD, 0 for JPY)
 * @param subunitFactor Multiplier factor from main unit to smallest unit (e.g. 100 for INR, 1 for JPY)
 * @param symbolBeforeAmount True if symbol precedes the number (e.g. ₹100, $100)
 */
data class Currency(
    val code: String,
    val symbol: String,
    val name: String,
    val subunitName: String,
    val decimalDigits: Int = 2,
    val subunitFactor: Long = 100L,
    val symbolBeforeAmount: Boolean = true
) {
    companion object {
        val INR = Currency(
            code = "INR",
            symbol = "₹",
            name = "Indian Rupee",
            subunitName = "Paise",
            decimalDigits = 2,
            subunitFactor = 100L,
            symbolBeforeAmount = true
        )

        val USD = Currency(
            code = "USD",
            symbol = "$",
            name = "US Dollar",
            subunitName = "Cent",
            decimalDigits = 2,
            subunitFactor = 100L,
            symbolBeforeAmount = true
        )

        val EUR = Currency(
            code = "EUR",
            symbol = "€",
            name = "Euro",
            subunitName = "Cent",
            decimalDigits = 2,
            subunitFactor = 100L,
            symbolBeforeAmount = true
        )

        val GBP = Currency(
            code = "GBP",
            symbol = "£",
            name = "British Pound",
            subunitName = "Penny",
            decimalDigits = 2,
            subunitFactor = 100L,
            symbolBeforeAmount = true
        )

        val AED = Currency(
            code = "AED",
            symbol = "د.إ",
            name = "UAE Dirham",
            subunitName = "Fils",
            decimalDigits = 2,
            subunitFactor = 100L,
            symbolBeforeAmount = true
        )

        val SGD = Currency(
            code = "SGD",
            symbol = "S$",
            name = "Singapore Dollar",
            subunitName = "Cent",
            decimalDigits = 2,
            subunitFactor = 100L,
            symbolBeforeAmount = true
        )

        val CAD = Currency(
            code = "CAD",
            symbol = "C$",
            name = "Canadian Dollar",
            subunitName = "Cent",
            decimalDigits = 2,
            subunitFactor = 100L,
            symbolBeforeAmount = true
        )

        val AUD = Currency(
            code = "AUD",
            symbol = "A$",
            name = "Australian Dollar",
            subunitName = "Cent",
            decimalDigits = 2,
            subunitFactor = 100L,
            symbolBeforeAmount = true
        )

        val JPY = Currency(
            code = "JPY",
            symbol = "¥",
            name = "Japanese Yen",
            subunitName = "Yen",
            decimalDigits = 0,
            subunitFactor = 1L,
            symbolBeforeAmount = true
        )

        val DEFAULT: Currency = INR

        val SUPPORTED_CURRENCIES: List<Currency> = listOf(
            INR, USD, EUR, GBP, AED, SGD, CAD, AUD, JPY
        )

        fun fromCode(code: String): Currency {
            return SUPPORTED_CURRENCIES.find { it.code.equals(code, ignoreCase = true) } ?: DEFAULT
        }
    }
}
