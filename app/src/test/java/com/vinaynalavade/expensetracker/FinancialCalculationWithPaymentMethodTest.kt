package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalculationWithPaymentMethodTest {

    @Test
    fun testFinancialTotalsUnchangedByPaymentMethod() {
        val cat = Category.UNCATEGORIZED

        // Mix of transactions across CASH and ACCOUNT
        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(100000L), // ₹1,000 Cash Income
                type = TransactionType.INCOME,
                category = cat,
                paymentMethod = PaymentMethod.CASH,
                timestamp = 1000L
            ),
            Transaction(
                id = 2L,
                amount = Amount(500000L), // ₹5,000 Account Income
                type = TransactionType.INCOME,
                category = cat,
                paymentMethod = PaymentMethod.ACCOUNT,
                timestamp = 2000L
            ),
            Transaction(
                id = 3L,
                amount = Amount(150000L), // ₹1,500 Account Expense
                type = TransactionType.EXPENSE,
                category = cat,
                paymentMethod = PaymentMethod.ACCOUNT,
                timestamp = 3000L
            ),
            Transaction(
                id = 4L,
                amount = Amount(50000L), // ₹500 Cash Expense
                type = TransactionType.EXPENSE,
                category = cat,
                paymentMethod = PaymentMethod.CASH,
                timestamp = 4000L
            ),
            Transaction(
                id = 5L,
                amount = Amount(200000L), // ₹2,000 Account Expense
                type = TransactionType.EXPENSE,
                category = cat,
                paymentMethod = PaymentMethod.ACCOUNT,
                timestamp = 5000L
            )
        )

        var totalIncomeSubunits = 0L
        var totalExpenseSubunits = 0L

        for (tx in transactions) {
            if (tx.type == TransactionType.INCOME) {
                totalIncomeSubunits += tx.amount.subunits
            } else {
                totalExpenseSubunits += tx.amount.subunits
            }
        }

        // Total Income = 1,000 + 5,000 = ₹6,000 (600,000 subunits)
        assertEquals(600000L, totalIncomeSubunits)

        // Total Expense = 1,500 + 500 + 2,000 = ₹4,000 (400,000 subunits)
        assertEquals(400000L, totalExpenseSubunits)

        // Net Change = ₹2,000 (200,000 subunits)
        val netChangeSubunits = totalIncomeSubunits - totalExpenseSubunits
        assertEquals(200000L, netChangeSubunits)

        // Running balance with starting balance of ₹10,000 (1,000,000 subunits)
        val startingBalance = Amount(1000000L)
        val currentBalance = startingBalance + Amount(netChangeSubunits)
        assertEquals(1200000L, currentBalance.subunits) // ₹12,000
    }
}
