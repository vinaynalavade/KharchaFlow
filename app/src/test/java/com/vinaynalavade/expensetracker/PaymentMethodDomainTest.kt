package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.RecurrenceFrequency
import com.vinaynalavade.expensetracker.domain.model.RecurringTransaction
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PaymentMethodDomainTest {

    @Test
    fun testPaymentMethodEnumEntries() {
        assertEquals("Cash", PaymentMethod.CASH.displayName)
        assertEquals("Bank Account", PaymentMethod.BANK_ACCOUNT.displayName)
        assertEquals("UPI", PaymentMethod.UPI.displayName)
    }

    @Test
    fun testPaymentMethodFromStringParsing() {
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("CASH"))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("cash"))
        assertEquals(PaymentMethod.BANK_ACCOUNT, PaymentMethod.fromString("BANK_ACCOUNT"))
        assertEquals(PaymentMethod.BANK_ACCOUNT, PaymentMethod.fromString("bank_account"))
        assertEquals(PaymentMethod.UPI, PaymentMethod.fromString("UPI"))
        assertEquals(PaymentMethod.UPI, PaymentMethod.fromString("upi"))

        // Unknown or blank values fallback to CASH
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("UNKNOWN_METHOD"))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString(""))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("  "))
    }

    @Test
    fun testTransactionDomainModelPreservesPaymentMethod() {
        val expenseCash = Transaction(
            id = 1L,
            amount = Amount(50000L),
            type = TransactionType.EXPENSE,
            category = Category.UNCATEGORIZED,
            paymentMethod = PaymentMethod.CASH,
            timestamp = 1000L
        )
        assertEquals(PaymentMethod.CASH, expenseCash.paymentMethod)

        val incomeBank = Transaction(
            id = 2L,
            amount = Amount(150000L),
            type = TransactionType.INCOME,
            category = Category.UNCATEGORIZED,
            paymentMethod = PaymentMethod.BANK_ACCOUNT,
            timestamp = 2000L
        )
        assertEquals(PaymentMethod.BANK_ACCOUNT, incomeBank.paymentMethod)

        val expenseUpi = Transaction(
            id = 3L,
            amount = Amount(25000L),
            type = TransactionType.EXPENSE,
            category = Category.UNCATEGORIZED,
            paymentMethod = PaymentMethod.UPI,
            timestamp = 3000L
        )
        assertEquals(PaymentMethod.UPI, expenseUpi.paymentMethod)
    }

    @Test
    fun testTransactionDefaultPaymentMethodIsCash() {
        val defaultTx = Transaction(
            id = 4L,
            amount = Amount(10000L),
            type = TransactionType.EXPENSE,
            category = Category.UNCATEGORIZED,
            timestamp = 4000L
        )
        assertEquals(PaymentMethod.CASH, defaultTx.paymentMethod)
    }

    @Test
    fun testRecurringTransactionDomainModelPreservesPaymentMethod() {
        val recurring = RecurringTransaction(
            id = 1L,
            title = "Gym Membership",
            amount = Amount(200000L),
            type = TransactionType.EXPENSE,
            category = Category.UNCATEGORIZED,
            paymentMethod = PaymentMethod.UPI,
            frequency = RecurrenceFrequency.MONTHLY
        )
        assertEquals(PaymentMethod.UPI, recurring.paymentMethod)

        val defaultRecurring = RecurringTransaction(
            id = 2L,
            title = "House Help",
            amount = Amount(500000L),
            type = TransactionType.EXPENSE,
            category = Category.UNCATEGORIZED
        )
        assertEquals(PaymentMethod.CASH, defaultRecurring.paymentMethod)
    }
}
