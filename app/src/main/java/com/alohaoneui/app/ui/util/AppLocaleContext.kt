package com.alohaoneui.app.ui.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies the user's in-app language before an Activity inflates resources.
 * "system" deliberately leaves the system configuration untouched.
 */
object AppLocaleContext {
    fun wrap(base: Context): Context {
        val language = runCatching {
            base.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getString("app_language", "system")
        }.getOrNull() ?: "system"

        val tag = when (language) {
            "zh-CN" -> "zh-CN"
            "zh-TW" -> "zh-TW"
            "en" -> "en"
            "ja" -> "ja"
            "ko" -> "ko"
            else -> return base
        }
        val locale = Locale.forLanguageTag(tag)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }
}
