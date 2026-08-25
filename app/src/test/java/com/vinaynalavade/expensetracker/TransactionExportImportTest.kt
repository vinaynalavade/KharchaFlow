package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.backup.CsvTransactionHelper
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionExportImportTest {

    private val foodCategory = Category(
        id = 1L,
        name = "Food & Dining",
        iconName = "restaurant",
        colorHex = "#EF4444",
        type = TransactionType.EXPENSE,
        isDefault = true
    )

    private val salaryCategory = Category(
        id = 2L,
        name = "Salary",
        iconName = "payments",
        colorHex = "#10B981",
        type = TransactionType.INCOME,
        isDefault = true
    )

    @Test
    fun testCsvExportWithEscaping() {
        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = Amount(150000L),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                paymentMethod = PaymentMethod.CASH,
                note = "Dinner, \"special\" with team\nSecond line",
                timestamp = 1787400000000L
            ),
            Transaction(
                id = 2L,
                amount = Amount(5000000L),
                type = TransactionType.INCOME,
                category = salaryCategory,
                paymentMethod = PaymentMethod.ACCOUNT,
                note = null,
                timestamp = 1787300000000L
            )
        )

        val csv = CsvTransactionHelper.exportToCsv(transactions, Currency.INR)

        // Check header
        assertTrue(csv.startsWith("Date,Type,Category,Amount,Currency,Payment Method,Note,Created At"))

        // Check CSV escaping of comma, quote and newline
        assertTrue(csv.contains("\"Dinner, \"\"special\"\" with team\nSecond line\""))
        assertTrue(csv.contains("Cash"))
        assertTrue(csv.contains("Account"))
    }

    @Test
    fun testCsvImportParsingWithValidAndInvalidRows() {
        val csvContent = """
            Date,Type,Category,Amount,Currency,Payment Method,Note,Created At
            2026-08-22 10:30:00,Expense,Food & Dining,150.00,INR,UPI,"Groceries and snacks",1787400000000
            2026-08-22 11:00:00,Income,Salary,5000.00,INR,Bank Account,August Salary,1787410000000
            INVALID_DATE,Expense,Shopping,200.00,INR,Cash,Invalid row 1,
            2026-08-22 12:00:00,UNKNOWN_TYPE,Bills,100.00,INR,Cash,Invalid row 2,
            2026-08-22 13:00:00,Expense,Bills,-50.00,INR,Cash,Invalid row 3,
        """.trimIndent()

        val parsed = CsvTransactionHelper.parseCsv(csvContent, Currency.INR)

        assertEquals(5, parsed.size)

        // Row 1 (valid with legacy UPI source mapping to ACCOUNT)
        assertTrue(parsed[0].isValid)
        assertEquals(TransactionType.EXPENSE, parsed[0].type)
        assertEquals("Food & Dining", parsed[0].categoryName)
        assertEquals(15000L, parsed[0].amount?.subunits)
        assertEquals(PaymentMethod.ACCOUNT, parsed[0].paymentMethod)
        assertEquals("Groceries and snacks", parsed[0].note)

        // Row 2 (valid with legacy Bank Account source mapping to ACCOUNT)
        assertTrue(parsed[1].isValid)
        assertEquals(TransactionType.INCOME, parsed[1].type)
        assertEquals(500000L, parsed[1].amount?.subunits)
        assertEquals(PaymentMethod.ACCOUNT, parsed[1].paymentMethod)

        // Row 3 (invalid date)
        assertFalse(parsed[2].isValid)
        assertTrue(parsed[2].errorMessage!!.contains("Invalid date"))

        // Row 4 (invalid type)
        assertFalse(parsed[3].isValid)
        assertTrue(parsed[3].errorMessage!!.contains("Invalid transaction type"))

        // Row 5 (invalid amount)
        assertFalse(parsed[4].isValid)
        assertTrue(parsed[4].errorMessage!!.contains("Invalid amount"))
    }
}
