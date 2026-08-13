package com.example.xargoosh.feature.settings.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {
    @Test
    fun languageRegistryHasUniqueTagsAndNativeNames() {
        assertEquals(supportedAppLanguages.size, supportedAppLanguages.map { it.tag }.distinct().size)
        assertTrue(supportedAppLanguages.all { it.nativeName.isNotBlank() })
    }

    @Test
    fun requestedLanguagesAreAvailable() {
        val tags = supportedAppLanguages.map { it.tag }.toSet()
        assertTrue(tags.containsAll(setOf("en", "zh-CN", "zh-TW", "ku", "ckb", "ar", "fa", "tr", "ur", "ru", "es", "de", "pt", "pl")))
    }
}
