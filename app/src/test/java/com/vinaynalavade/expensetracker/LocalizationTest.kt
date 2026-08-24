package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.presentation.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationTest {

    @Test
    fun testAppLanguageEnumMappings() {
        assertEquals("en", AppLanguage.ENGLISH.code)
        assertEquals("hi", AppLanguage.HINDI.code)
        assertEquals("mr", AppLanguage.MARATHI.code)

        assertEquals("English", AppLanguage.ENGLISH.displayName)
        assertEquals("Hindi", AppLanguage.HINDI.displayName)
        assertEquals("Marathi", AppLanguage.MARATHI.displayName)

        assertEquals("English", AppLanguage.ENGLISH.nativeName)
        assertEquals("हिंदी", AppLanguage.HINDI.nativeName)
        assertEquals("मराठी", AppLanguage.MARATHI.nativeName)
    }

    @Test
    fun testFromCodeFallbackToEnglish() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("EN"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromCode("hi"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromCode("HI"))
        assertEquals(AppLanguage.MARATHI, AppLanguage.fromCode("mr"))
        assertEquals(AppLanguage.MARATHI, AppLanguage.fromCode("MR"))

        // Unknown / System / blank fall back to English
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("SYSTEM"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("fr"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(""))
    }

    @Test
    fun testStringResourceKeysParityAcrossLanguages() {
        val baseDir = File("src/main/res")
        val enFile = File(baseDir, "values/strings.xml")
        val hiFile = File(baseDir, "values-hi/strings.xml")
        val mrFile = File(baseDir, "values-mr/strings.xml")

        assertTrue("English strings.xml should exist", enFile.exists())
        assertTrue("Hindi strings.xml should exist", hiFile.exists())
        assertTrue("Marathi strings.xml should exist", mrFile.exists())

        val enKeys = extractStringKeys(enFile)
        val hiKeys = extractStringKeys(hiFile)
        val mrKeys = extractStringKeys(mrFile)

        assertTrue("English keys should not be empty", enKeys.isNotEmpty())
        assertTrue("Hindi keys should not be empty", hiKeys.isNotEmpty())
        assertTrue("Marathi keys should not be empty", mrKeys.isNotEmpty())

        // Essential core keys that MUST exist in all 3 language bundles
        val mandatoryKeys = listOf(
            "app_name",
            "nav_dashboard",
            "nav_transactions",
            "nav_analytics",
            "nav_settings",
            "total_balance",
            "total_income",
            "total_expense",
            "settings_language",
            "settings_section_profile",
            "profile_default_local_name",
            "profile_local_account_desc",
            "profile_connected_google",
            "profile_status_google_connected",
            "profile_status_local_account",
            "profile_edit_name_title",
            "profile_photo_options_title",
            "profile_choose_photo",
            "profile_remove_photo",
            "btn_save",
            "btn_cancel",
            "btn_done"
        )

        for (key in mandatoryKeys) {
            assertTrue("Key '$key' must exist in English bundle", enKeys.contains(key))
            assertTrue("Key '$key' must exist in Hindi bundle", hiKeys.contains(key))
            assertTrue("Key '$key' must exist in Marathi bundle", mrKeys.contains(key))
        }
    }

    private fun extractStringKeys(file: File): Set<String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val stringNodes = doc.getElementsByTagName("string")
        val keys = mutableSetOf<String>()
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val nameAttr = node.attributes.getNamedItem("name")
            if (nameAttr != null) {
                keys.add(nameAttr.nodeValue)
            }
        }
        return keys
    }
}
