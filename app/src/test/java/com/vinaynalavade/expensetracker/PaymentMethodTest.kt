package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying financial source standardization to Cash and Account and backward compatibility.
 */
class PaymentMethodTest {

    @Test
    fun testOnlyTwoStandardSourcesExist() {
        assertEquals(2, PaymentMethod.entries.size)
        assertEquals(PaymentMethod.CASH, PaymentMethod.entries[0])
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.entries[1])
        assertEquals("Cash", PaymentMethod.CASH.displayName)
        assertEquals("Account", PaymentMethod.ACCOUNT.displayName)
    }

    @Test
    fun testBackwardCompatibleParsing() {
        // Standard current values
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("CASH"))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("cash"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("ACCOUNT"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("account"))

        // Legacy values normalized to ACCOUNT
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("BANK_ACCOUNT"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("bank_account"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("Bank Account"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("UPI"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("upi"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("BANK"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("CARD"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("NET_BANKING"))
        assertEquals(PaymentMethod.ACCOUNT, PaymentMethod.fromString("Net Banking"))

        // Null and unknown fallbacks
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString(null))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString(""))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("UNKNOWN_VALUE"))
    }
}
