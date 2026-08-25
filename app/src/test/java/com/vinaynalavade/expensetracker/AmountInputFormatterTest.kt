package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.core.util.AmountInputFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests covering Amount live comma formatting, sanitization, and parsing logic.
 */
class AmountInputFormatterTest {

    @Test
    fun testIntegerAmountFormatting() {
        assertEquals("", AmountInputFormatter.formatWithCommas(""))
        assertEquals("1", AmountInputFormatter.formatWithCommas("1"))
        assertEquals("12", AmountInputFormatter.formatWithCommas("12"))
        assertEquals("123", AmountInputFormatter.formatWithCommas("123"))
        assertEquals("1,234", AmountInputFormatter.formatWithCommas("1234"))
        assertEquals("12,345", AmountInputFormatter.formatWithCommas("12345"))
        assertEquals("123,456", AmountInputFormatter.formatWithCommas("123456"))
        assertEquals("1,234,567", AmountInputFormatter.formatWithCommas("1234567"))
        assertEquals("12,345,678", AmountInputFormatter.formatWithCommas("12345678"))
        assertEquals("123,456,789", AmountInputFormatter.formatWithCommas("123456789"))
    }

    @Test
    fun testDecimalAmountFormatting() {
        assertEquals("1,234.", AmountInputFormatter.formatWithCommas("1234."))
        assertEquals("1,234.5", AmountInputFormatter.formatWithCommas("1234.5"))
        assertEquals("1,234.56", AmountInputFormatter.formatWithCommas("1234.56"))
        assertEquals("1,234,567.89", AmountInputFormatter.formatWithCommas("1234567.89"))
        assertEquals("0.5", AmountInputFormatter.formatWithCommas("0.5"))
        assertEquals("0.50", AmountInputFormatter.formatWithCommas("0.50"))
    }

    @Test
    fun testInputSanitization() {
        // Disallow multiple decimal dots
        assertEquals("123.45", AmountInputFormatter.sanitizeAmountInput("123.45.67", maxDecimalDigits = 2))
        // Truncate decimal digits past max
        assertEquals("123.45", AmountInputFormatter.sanitizeAmountInput("123.456", maxDecimalDigits = 2))
        // Clean commas from pasted strings
        assertEquals("1234567.89", AmountInputFormatter.sanitizeAmountInput("1,234,567.89", maxDecimalDigits = 2))
        // Normalize redundant leading zeros
        assertEquals("5", AmountInputFormatter.sanitizeAmountInput("05", maxDecimalDigits = 2))
        assertEquals("0", AmountInputFormatter.sanitizeAmountInput("00", maxDecimalDigits = 2))
        assertEquals("0.", AmountInputFormatter.sanitizeAmountInput("0.", maxDecimalDigits = 2))
        assertEquals("0.5", AmountInputFormatter.sanitizeAmountInput("0.5", maxDecimalDigits = 2))
    }

    @Test
    fun testAmountParsingWithoutCommas() {
        val amount1 = Amount.fromStringOrNull("1,234.56", Currency.INR)
        assertNotNull(amount1)
        assertEquals(123456L, amount1?.subunits)

        val amount2 = Amount.fromStringOrNull("1234.56", Currency.INR)
        assertNotNull(amount2)
        assertEquals(123456L, amount2?.subunits)

        val amount3 = Amount.fromStringOrNull("1,234,567", Currency.INR)
        assertNotNull(amount3)
        assertEquals(123456700L, amount3?.subunits)

        val amount4 = Amount.fromStringOrNull("0.00", Currency.INR)
        assertNotNull(amount4)
        assertEquals(0L, amount4?.subunits)

        val invalid = Amount.fromStringOrNull("abc", Currency.INR)
        assertNull(invalid)
    }
}
