package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialCalculationTest {

    @Test
    fun testAmountArithmetic() {
        val zero = Amount.ZERO
        assertEquals(0L, zero.subunits)
        assertTrue(zero.isZero)

        val amount1 = Amount(10050L) // ₹100.50
        val amount2 = Amount(5025L)  // ₹50.25

        val sum = amount1 + amount2
        assertEquals(15075L, sum.subunits)

        val diff = amount1 - amount2
        assertEquals(5025L, diff.subunits)
    }

    @Test
    fun testContinuousBalanceModel() {
        // One-time Starting Balance: ₹50,000 (5,000,000 subunits)
        val startingBalance = Amount.fromMainUnit(50000L, Currency.INR)

        // July: Income = ₹20,000, Expense = ₹15,000
        val julyIncome = Amount.fromMainUnit(20000L, Currency.INR)
        val julyExpense = Amount.fromMainUnit(15000L, Currency.INR)
        val julyNetChange = julyIncome - julyExpense // +₹5,000
        val julyClosing = startingBalance + julyNetChange // ₹55,000

        assertEquals(5500000L, julyClosing.subunits)

        // August: Opening Balance derived as July Closing Balance = ₹55,000
        val augustOpening = julyClosing
        assertEquals(startingBalance + julyNetChange, augustOpening)

        // August: Income = ₹10,000, Expense = ₹18,000
        val augustIncome = Amount.fromMainUnit(10000L, Currency.INR)
        val augustExpense = Amount.fromMainUnit(18000L, Currency.INR)
        val augustNetChange = augustIncome - augustExpense // -₹8,000
        val augustClosing = augustOpening + augustNetChange // ₹47,000

        assertEquals(4700000L, augustClosing.subunits)

        // September: Opening Balance derived as August Closing Balance = ₹47,000
        val septemberOpening = augustClosing
        assertEquals(Amount.fromMainUnit(47000L, Currency.INR), septemberOpening)

        // Current Account Balance = Starting Balance + All Income - All Expenses
        val totalIncome = julyIncome + augustIncome // ₹30,000
        val totalExpense = julyExpense + augustExpense // ₹33,000
        val currentAccountBalance = startingBalance + totalIncome - totalExpense
        assertEquals(septemberOpening, currentAccountBalance)
    }

    @Test
    fun testCurrencyFormatting() {
        val inr = Currency.INR
        val amount = Amount.fromStringOrNull("1234.50", inr)
        assertTrue(amount != null)
        val formatted = amount!!.format(inr)
        assertTrue(formatted.contains("1,234.50") || formatted.contains("1234.50"))
        assertTrue(formatted.startsWith("₹"))
    }
}
