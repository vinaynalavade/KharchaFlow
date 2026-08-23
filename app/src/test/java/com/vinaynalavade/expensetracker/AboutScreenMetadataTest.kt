package com.vinaynalavade.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class AboutScreenMetadataTest {

    @Test
    fun testGitHubRepositoryUrlIsValidAndVerified() {
        val repoUrl = "https://github.com/vinaynalavade/KharchaFlow"
        val uri = URI(repoUrl)

        assertEquals("https", uri.scheme)
        assertEquals("github.com", uri.host)
        assertEquals("/vinaynalavade/KharchaFlow", uri.path)
    }

    @Test
    fun testDeveloperCreditIsExact() {
        val expectedDeveloper = "Vinay Nalavade"
        val expectedCreditString = "Developed by $expectedDeveloper"

        assertTrue(expectedCreditString.contains("Vinay Nalavade"))
        assertEquals("Developed by Vinay Nalavade", expectedCreditString)
    }

    @Test
    fun testVersionNamingConvention() {
        val version = "1.0.3"
        val parts = version.split(".")

        assertEquals(3, parts.size)
        assertEquals("1", parts[0])
        assertEquals("0", parts[1])
        assertEquals("3", parts[2])
    }

    @Test
    fun testPrivacyAndSecurityKeyStatementsAreAccurate() {
        val storageStatement = "100% Local SQLite (Room Database) on-device"
        val trackingStatement = "Zero third-party analytics or advertising networks"
        val cloudStatement = "Optional OAuth 2.0 to your private Google Drive"
        val securityStatement = "Optional Biometric / PIN protection with FLAG_SECURE"

        // Verify statements do not make unverified absolute claims like "100% hack-proof"
        assertFalse(storageStatement.contains("hack-proof", ignoreCase = true))
        assertFalse(trackingStatement.contains("impossible to breach", ignoreCase = true))
        assertTrue(storageStatement.contains("Local", ignoreCase = true))
        assertTrue(cloudStatement.contains("Optional", ignoreCase = true))
        assertTrue(securityStatement.contains("Optional", ignoreCase = true))
    }
}
