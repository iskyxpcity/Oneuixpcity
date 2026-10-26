package com.alohaoneui.app.ui.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Mirrors Voyage's locale application path:
 * Android 13+ uses LocaleManager application locales; older Android versions
 * apply the saved language to the Activity context before resources inflate.
 */
object AppLocaleContext {
    private fun canonicalTag(language: String?): String? = when (language) {
        "zh-CN", "zh-Hans" -> "zh-Hans"
        "zh-TW", "zh-Hant" -> "zh-Hant"
        "en" -> "en"
        "ja" -> "ja"
        "ko" -> "ko"
        else -> null
    }

    fun apply(context: Context, language: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val tag = canonicalTag(language)
            localeManager.applicationLocales = if (tag == null) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(tag)
            }
        }
    }

    fun wrap(base: Context): Context {
        // On Android 13+, the platform owns application locale configuration.
        if (Build.VERSION.SDK_INT >= 33) return base

        val language = runCatching {
            base.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getString("app_language", "system")
        }.getOrNull() ?: "system"
        val tag = canonicalTag(language) ?: return base
        val locale = Locale.forLanguageTag(tag)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }
}
