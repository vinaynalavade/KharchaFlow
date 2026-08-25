package com.vinaynalavade.expensetracker.core.util

/**
 * High-performance, robust utility for financial amount input sanitization and live comma formatting.
 */
object AmountInputFormatter {

    /**
     * Formats a raw numeric string (e.g. "1234567.89", "1234.", or "0.50") with standard 3-digit comma grouping.
     */
    fun formatWithCommas(raw: String): String {
        if (raw.isEmpty()) return ""
        val clean = raw.filter { it.isDigit() || it == '.' || it == '-' }
        if (clean.isEmpty()) return ""

        val isNegative = clean.startsWith("-")
        val withoutMinus = if (isNegative) clean.substring(1) else clean
        val prefix = if (isNegative) "-" else ""

        val parts = withoutMinus.split('.')
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else null
        val hasDot = withoutMinus.contains('.')

        val formattedInteger = if (integerPart.isEmpty()) {
            ""
        } else {
            val reversed = integerPart.reversed()
            val chunked = reversed.chunked(3)
            chunked.joinToString(",").reversed()
        }

        return when {
            hasDot && decimalPart != null -> "$prefix$formattedInteger.$decimalPart"
            hasDot -> "$prefix$formattedInteger."
            else -> "$prefix$formattedInteger"
        }
    }

    /**
     * Sanitizes raw user input, preventing invalid characters, multiple decimal dots,
     * truncating excess decimal digits, and safely handling leading zeros.
     */
    fun sanitizeAmountInput(input: String, maxDecimalDigits: Int = 2): String {
        // Strip commas and whitespace
        val clean = input.replace(",", "").trim().filter { it.isDigit() || it == '.' }
        if (clean.isEmpty()) return ""

        val parts = clean.split('.')
        if (parts.size > 2) {
            // Disallow multiple decimal dots
            val intPart = parts[0]
            val decPart = parts[1].take(maxDecimalDigits)
            val cleanInt = normalizeLeadingZeros(intPart)
            return if (decPart.isNotEmpty()) "$cleanInt.$decPart" else "$cleanInt."
        }

        if (parts.size == 2) {
            val intPart = parts[0]
            val decPart = parts[1].take(maxDecimalDigits)
            val cleanInt = normalizeLeadingZeros(intPart)
            return if (cleanInt.isEmpty()) "0.$decPart" else "$cleanInt.$decPart"
        }

        // Single integer part: normalize leading zeros like "05" -> "5", "00" -> "0"
        return normalizeLeadingZeros(clean)
    }

    /**
     * Strips commas and extraneous whitespace from an amount string.
     */
    fun cleanToRaw(input: String): String {
        return input.replace(",", "").trim()
    }

    private fun normalizeLeadingZeros(intPart: String): String {
        if (intPart.length > 1 && intPart.startsWith("0")) {
            val stripped = intPart.dropWhile { it == '0' }
            return stripped.ifEmpty { "0" }
        }
        return intPart
    }
}
