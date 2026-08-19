package com.vinaynalavade.expensetracker.core.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Value-based financial amount model stored strictly in the smallest currency unit (subunits/paise/cents)
 * to avoid any floating-point arithmetic rounding errors.
 *
 * For example:
 * ₹125.50 is stored as 12550 paise (subunits = 12550L).
 *
 * @param subunits The monetary value in smallest currency units as [Long].
 */
@JvmInline
value class Amount(val subunits: Long) : Comparable<Amount> {

    operator fun plus(other: Amount): Amount = Amount(this.subunits + other.subunits)

    operator fun minus(other: Amount): Amount = Amount(this.subunits - other.subunits)

    operator fun times(factor: Long): Amount = Amount(this.subunits * factor)

    operator fun unaryMinus(): Amount = Amount(-this.subunits)

    override fun compareTo(other: Amount): Int = this.subunits.compareTo(other.subunits)

    val isZero: Boolean get() = subunits == 0L
    val isPositive: Boolean get() = subunits > 0L
    val isNegative: Boolean get() = subunits < 0L

    fun absolute(): Amount = Amount(abs(subunits))

    /**
     * Formats the amount into a clean display string using the provided [Currency].
     * Example: ₹125.50 or ₹1,250.00
     */
    fun format(currency: Currency = Currency.DEFAULT, includeSymbol: Boolean = true): String {
        val absVal = abs(subunits)
        val mainUnit = absVal / currency.subunitFactor
        val fractional = absVal % currency.subunitFactor

        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val formatter = DecimalFormat("#,##0", symbols)
        val formattedMain = formatter.format(mainUnit)

        val numberString = if (currency.decimalDigits > 0) {
            val fractionPadded = fractional.toString().padStart(currency.decimalDigits, '0')
            "$formattedMain.$fractionPadded"
        } else {
            formattedMain
        }

        val prefix = if (subunits < 0) "-" else ""
        return if (includeSymbol) {
            if (currency.symbolBeforeAmount) {
                "$prefix${currency.symbol}$numberString"
            } else {
                "$prefix$numberString ${currency.symbol}"
            }
        } else {
            "$prefix$numberString"
        }
    }

    /**
     * Formats to editable text format (e.g. "125.50" or "0.00") without commas or symbols.
     */
    fun toInputString(currency: Currency = Currency.DEFAULT): String {
        if (subunits == 0L) return ""
        val absVal = abs(subunits)
        val mainUnit = absVal / currency.subunitFactor
        val fractional = absVal % currency.subunitFactor
        return if (currency.decimalDigits > 0) {
            val fractionPadded = fractional.toString().padStart(currency.decimalDigits, '0')
            val trimmed = fractionPadded.trimEnd('0')
            if (trimmed.isEmpty()) "$mainUnit" else "$mainUnit.$trimmed"
        } else {
            mainUnit.toString()
        }
    }

    companion object {
        val ZERO = Amount(0L)

        /**
         * Safely parses a string input representation (e.g. "125.50" or "100") into an [Amount]
         * without using floating-point parsing.
         */
        fun fromStringOrNull(input: String, currency: Currency = Currency.DEFAULT): Amount? {
            val clean = input.trim().replace(",", "")
            if (clean.isEmpty()) return null

            val isNegative = clean.startsWith("-")
            val sanitized = if (isNegative) clean.substring(1) else clean

            val parts = sanitized.split(".")
            if (parts.size > 2) return null

            val mainPart = parts[0]
            val mainVal = if (mainPart.isEmpty()) 0L else mainPart.toLongOrNull() ?: return null

            val fractionPart = if (parts.size == 2) parts[1] else ""
            if (fractionPart.length > currency.decimalDigits) {
                // Reject or truncate beyond decimal digits
                val truncatedFraction = fractionPart.substring(0, currency.decimalDigits)
                val paddedFraction = truncatedFraction.padEnd(currency.decimalDigits, '0')
                val fractionVal = paddedFraction.toLongOrNull() ?: return null
                val totalSubunits = (mainVal * currency.subunitFactor) + fractionVal
                return Amount(if (isNegative) -totalSubunits else totalSubunits)
            }

            val paddedFraction = fractionPart.padEnd(currency.decimalDigits, '0')
            val fractionVal = if (paddedFraction.isEmpty()) 0L else paddedFraction.toLongOrNull() ?: return null

            val totalSubunits = (mainVal * currency.subunitFactor) + fractionVal
            return Amount(if (isNegative) -totalSubunits else totalSubunits)
        }

        fun fromSubunits(subunits: Long): Amount = Amount(subunits)

        fun fromMainUnit(mainUnit: Long, currency: Currency = Currency.DEFAULT): Amount =
            Amount(mainUnit * currency.subunitFactor)
    }
}
