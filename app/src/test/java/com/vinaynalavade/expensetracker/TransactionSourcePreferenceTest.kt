package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying default Income and Expense financial source preferences.
 */
class TransactionSourcePreferenceTest {

    @Test
    fun testDefaultSourcePreferences() {
        val defaultPrefs = UserPreferences()
        assertEquals(PaymentMethod.ACCOUNT, defaultPrefs.defaultIncomeSource)
        assertEquals(PaymentMethod.CASH, defaultPrefs.defaultExpenseSource)

        val updatedPrefs = defaultPrefs.copy(
            defaultIncomeSource = PaymentMethod.CASH,
            defaultExpenseSource = PaymentMethod.ACCOUNT
        )
        assertEquals(PaymentMethod.CASH, updatedPrefs.defaultIncomeSource)
        assertEquals(PaymentMethod.ACCOUNT, updatedPrefs.defaultExpenseSource)
    }
}
