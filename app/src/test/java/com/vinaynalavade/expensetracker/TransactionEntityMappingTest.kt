package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.data.local.entity.CategoryEntity
import com.vinaynalavade.expensetracker.data.local.entity.RecurringTransactionEntity
import com.vinaynalavade.expensetracker.data.local.entity.RecurringWithCategory
import com.vinaynalavade.expensetracker.data.local.entity.TransactionEntity
import com.vinaynalavade.expensetracker.data.local.entity.TransactionWithCategory
import com.vinaynalavade.expensetracker.domain.model.Category
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.RecurrenceFrequency
import com.vinaynalavade.expensetracker.domain.model.RecurringTransaction
import com.vinaynalavade.expensetracker.domain.model.Transaction
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionEntityMappingTest {

    private val sampleCategory = Category(
        id = 10L,
        name = "Groceries",
        iconName = "shopping_cart",
        colorHex = "#22C55E",
        type = TransactionType.EXPENSE,
        isDefault = false
    )

    private val sampleCategoryEntity = CategoryEntity.fromDomainModel(sampleCategory)

    @Test
    fun testTransactionEntityMappingCash() {
        val domain = Transaction(
            id = 101L,
            amount = Amount(50000L),
            type = TransactionType.EXPENSE,
            category = sampleCategory,
            paymentMethod = PaymentMethod.CASH,
            note = "Weekly groceries",
            timestamp = 1700000000000L,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )

        val entity = TransactionEntity.fromDomainModel(domain)
        assertEquals("CASH", entity.paymentMethod)
        assertEquals(50000L, entity.amountSubunits)
        assertEquals(10L, entity.categoryId)

        val withCategory = TransactionWithCategory(transaction = entity, category = sampleCategoryEntity)
        val mappedDomain = withCategory.toDomainModel()

        assertEquals(domain.id, mappedDomain.id)
        assertEquals(domain.amount, mappedDomain.amount)
        assertEquals(domain.type, mappedDomain.type)
        assertEquals(domain.paymentMethod, mappedDomain.paymentMethod)
        assertEquals(PaymentMethod.CASH, mappedDomain.paymentMethod)
        assertEquals(domain.note, mappedDomain.note)
    }

    @Test
    fun testTransactionEntityMappingBankAccount() {
        val domain = Transaction(
            id = 102L,
            amount = Amount(7500000L),
            type = TransactionType.INCOME,
            category = sampleCategory,
            paymentMethod = PaymentMethod.BANK_ACCOUNT,
            note = "Salary direct deposit",
            timestamp = 1700000000000L
        )

        val entity = TransactionEntity.fromDomainModel(domain)
        assertEquals("BANK_ACCOUNT", entity.paymentMethod)

        val withCategory = TransactionWithCategory(transaction = entity, category = sampleCategoryEntity)
        val mappedDomain = withCategory.toDomainModel()

        assertEquals(PaymentMethod.BANK_ACCOUNT, mappedDomain.paymentMethod)
    }

    @Test
    fun testTransactionEntityMappingUpi() {
        val domain = Transaction(
            id = 103L,
            amount = Amount(12000L),
            type = TransactionType.EXPENSE,
            category = sampleCategory,
            paymentMethod = PaymentMethod.UPI,
            note = "Street food scan",
            timestamp = 1700000000000L
        )

        val entity = TransactionEntity.fromDomainModel(domain)
        assertEquals("UPI", entity.paymentMethod)

        val withCategory = TransactionWithCategory(transaction = entity, category = sampleCategoryEntity)
        val mappedDomain = withCategory.toDomainModel()

        assertEquals(PaymentMethod.UPI, mappedDomain.paymentMethod)
    }

    @Test
    fun testLegacyEntityMappingDefaultsToCash() {
        // Entity constructed with unknown or legacy value
        val entity = TransactionEntity(
            id = 104L,
            amountSubunits = 30000L,
            type = "EXPENSE",
            categoryId = 10L,
            paymentMethod = "UNKNOWN_SOURCE",
            timestamp = 1700000000000L
        )

        val withCategory = TransactionWithCategory(transaction = entity, category = sampleCategoryEntity)
        val domain = withCategory.toDomainModel()

        assertEquals(PaymentMethod.CASH, domain.paymentMethod)
    }

    @Test
    fun testRecurringTransactionEntityMappingAllPaymentMethods() {
        for (method in PaymentMethod.entries) {
            val recurringDomain = RecurringTransaction(
                id = 201L,
                title = "Subscription ${method.name}",
                amount = Amount(99900L),
                type = TransactionType.EXPENSE,
                category = sampleCategory,
                paymentMethod = method,
                frequency = RecurrenceFrequency.MONTHLY,
                dayOfMonth = 5
            )

            val entity = RecurringTransactionEntity.fromDomainModel(recurringDomain)
            assertEquals(method.name, entity.paymentMethod)

            val withCategory = RecurringWithCategory(recurring = entity, category = sampleCategoryEntity)
            val mappedDomain = withCategory.toDomainModel()

            assertEquals(method, mappedDomain.paymentMethod)
        }
    }
}
