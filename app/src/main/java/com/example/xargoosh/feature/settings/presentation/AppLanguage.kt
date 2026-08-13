package com.example.xargoosh.feature.settings.presentation

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

data class AppLanguage(val tag: String, val nativeName: String)

val supportedAppLanguages = listOf(
    AppLanguage("", "System default"),
    AppLanguage("en", "English"),
    AppLanguage("zh-CN", "简体中文"),
    AppLanguage("zh-TW", "繁體中文"),
    AppLanguage("ku", "Kurdî"),
    AppLanguage("ckb", "کوردی"),
    AppLanguage("ar", "العربية"),
    AppLanguage("fa", "فارسی"),
    AppLanguage("tr", "Türkçe"),
    AppLanguage("ur", "اردو"),
    AppLanguage("ru", "Русский"),
    AppLanguage("es", "Español"),
    AppLanguage("de", "Deutsch"),
    AppLanguage("pt", "Português"),
    AppLanguage("pl", "Polski"),
    AppLanguage("fr", "Français"),
    AppLanguage("it", "Italiano"),
    AppLanguage("hi", "हिन्दी"),
    AppLanguage("ja", "日本語"),
    AppLanguage("ko", "한국어"),
    AppLanguage("id", "Bahasa Indonesia"),
    AppLanguage("iw", "עברית")
)

fun currentAppLanguageTag(): String =
    AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag().orEmpty()

fun selectAppLanguage(tag: String) {
    AppCompatDelegate.setApplicationLocales(
        if (tag.isBlank()) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(tag)
    )
}
