package com.alohaoneui.app.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Backwards-compatible UI mode facade used by existing screens and saved settings.
 * Theme implementation metadata lives in [ThemeRegistry].
 */
enum class UiMode(val value: String) {
    Miuix(ThemeId.MIUIX.persistedValue),
    Material(ThemeId.VOYAGE_MATERIAL3.persistedValue);

    val themeDefinition: ThemeDefinition
        get() = ThemeRegistry.definition(
            when (this) {
                Miuix -> ThemeId.MIUIX
                Material -> ThemeId.VOYAGE_MATERIAL3
            }
        )

    companion object {
        fun fromValue(value: String): UiMode =
            when (ThemeId.fromPersistedValue(value)) {
                ThemeId.VOYAGE_MATERIAL3 -> Material
                ThemeId.MIUIX -> Miuix
            }

        val DEFAULT_VALUE = Miuix.value
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Miuix }
