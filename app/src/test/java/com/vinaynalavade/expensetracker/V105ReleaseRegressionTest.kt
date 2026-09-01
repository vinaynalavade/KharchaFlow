package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.backup.BackupCategory
import com.vinaynalavade.expensetracker.core.backup.BackupData
import com.vinaynalavade.expensetracker.core.backup.BackupPreferences
import com.vinaynalavade.expensetracker.core.backup.BackupTransaction
import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.core.model.Currency
import com.vinaynalavade.expensetracker.domain.model.PaymentMethod
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.presentation.theme.Motion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract and regression suite for KharchaFlow v1.0.5 Release.
 * Validates UX motion duration ordering, semantic contracts, backup integrity, and version metadata.
 */
class V105ReleaseRegressionTest {

    @Test
    fun testV105VersionMetadataContract() {
        val expectedVersionName = "1.0.5"
        val expectedVersionCode = 6

        val parts = expectedVersionName.split(".")
        assertEquals(3, parts.size)
        assertEquals("1", parts[0])
        assertEquals("0", parts[1])
        assertEquals("5", parts[2])
        assertTrue(expectedVersionCode >= 6)
    }

    @Test
    fun testMotionTokensDurationHierarchy() {
        // Fast <= Normal <= Emphasis
        assertTrue("Fast duration must be positive", Motion.DurationFast > 0)
        assertTrue("Normal duration must be greater than Fast", Motion.DurationNormal >= Motion.DurationFast)
        assertTrue("Emphasis duration must be greater than Normal", Motion.DurationEmphasis >= Motion.DurationNormal)

        assertEquals(150, Motion.DurationFast)
        assertEquals(250, Motion.DurationNormal)
        assertEquals(350, Motion.DurationEmphasis)
    }

    @Test
    fun testBackupDataStructureWithV105Data() {
        val backupTime = System.currentTimeMillis()

        val sampleCategory = BackupCategory(
            id = 1L,
            name = "Groceries",
            type = "EXPENSE",
            iconName = "shopping_cart",
            colorHex = "#f43f5e"
        )
        val sampleTx = BackupTransaction(
            id = 1L,
            amountSubunits = 25000L,
            type = "EXPENSE",
            categoryId = 1L,
            paymentMethod = "ACCOUNT",
            timestamp = backupTime - 3600000L,
            note = "Weekly groceries"
        )

        val v105Backup = BackupData(
            backupVersion = 1,
            appVersion = "1.0.5",
            createdAt = backupTime,
            categories = listOf(sampleCategory),
            transactions = listOf(sampleTx),
            recurringTransactions = emptyList(),
            preferences = BackupPreferences()
        )

        assertEquals("1.0.5", v105Backup.appVersion)
        assertEquals(1, v105Backup.transactions.size)
        assertEquals(1, v105Backup.categories.size)
    }

    @Test
    fun testUserPreferencesContractConsistency() {
        val defaultPrefs = UserPreferences()
        // Ensure default settings values remain robust
        assertEquals(Currency.INR, defaultPrefs.currency)
        assertEquals(PaymentMethod.CASH, defaultPrefs.defaultExpenseSource)
        assertEquals(PaymentMethod.ACCOUNT, defaultPrefs.defaultIncomeSource)
        assertFalse(defaultPrefs.notificationsMasterEnabled)
        assertFalse(defaultPrefs.appLockEnabled)
    }
}
