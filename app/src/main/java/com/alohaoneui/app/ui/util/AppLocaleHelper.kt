package com.alohaoneui.app.ui.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object AppLocaleHelper {
    fun getLanguage(context: Context): String =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("app_language", "system") ?: "system"

    fun wrap(base: Context): Context {
        val language = getLanguage(base)
        if (language == "system") return base
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }

    fun apply(context: Context, language: String) {
        val safeLanguage = language.takeIf { it in setOf("system", "en", "zh-CN") } ?: "system"
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("app_language", safeLanguage).apply()
    }
}
