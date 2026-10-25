package com.alohaoneui.app.ui

import androidx.compose.runtime.Composable
import com.alohaoneui.app.ui.theme.AppSettings
import com.alohaoneui.app.ui.theme.MaterialAlohaOneUITheme
import com.alohaoneui.app.ui.theme.MiuixAlohaOneUITheme

/**
 * Identifies a visual theme implementation, not an application feature.
 * Persisted values are intentionally stable for backward compatibility.
 */
enum class ThemeId(val persistedValue: String) {
    MIUIX("miuix"),
    VOYAGE_MATERIAL3("material");

    companion object {
        fun fromPersistedValue(value: String): ThemeId =
            entries.firstOrNull { it.persistedValue == value } ?: MIUIX
    }
}

/** Only capabilities already backed by a working implementation may be marked true. */
data class ThemeCapabilities(
    val dynamicColor: Boolean,
    val customSeedColor: Boolean,
    val paletteStyleSelection: Boolean,
    val colorSpecSelection: Boolean,
    val amoledBlack: Boolean,
    val expressiveMotion: Boolean,
    val customColorSchemes: Boolean,
)

/** A separate renderer keeps theme-specific UI implementation out of feature screens. */
interface ThemeAdapter {
    @Composable
    fun Apply(appSettings: AppSettings, content: @Composable () -> Unit)
}

private object MiuixThemeAdapter : ThemeAdapter {
    @Composable
    override fun Apply(appSettings: AppSettings, content: @Composable () -> Unit) {
        MiuixAlohaOneUITheme(appSettings = appSettings, content = content)
    }
}

private object Material3ThemeAdapter : ThemeAdapter {
    @Composable
    override fun Apply(appSettings: AppSettings, content: @Composable () -> Unit) {
        MaterialAlohaOneUITheme(appSettings = appSettings, content = content)
    }
}

data class ThemeDefinition(
    val id: ThemeId,
    val displayName: String,
    val capabilities: ThemeCapabilities,
    val adapter: ThemeAdapter,
)

/**
 * Register a theme here with its own renderer. New themes can be added without changing
 * app navigation, business logic, or the settings persistence keys.
 */
object ThemeRegistry {
    val definitions: List<ThemeDefinition> = listOf(
        ThemeDefinition(
            id = ThemeId.MIUIX,
            displayName = "Miuix",
            adapter = MiuixThemeAdapter,
            capabilities = ThemeCapabilities(
                dynamicColor = true,
                customSeedColor = true,
                paletteStyleSelection = true,
                colorSpecSelection = true,
                amoledBlack = true,
                expressiveMotion = false,
                customColorSchemes = false,
            ),
        ),
        ThemeDefinition(
            id = ThemeId.VOYAGE_MATERIAL3,
            displayName = "Material 3",
            adapter = Material3ThemeAdapter,
            capabilities = ThemeCapabilities(
                dynamicColor = true,
                customSeedColor = true,
                paletteStyleSelection = true,
                colorSpecSelection = true,
                amoledBlack = true,
                expressiveMotion = true,
                // Voyage has custom color-scheme editing, but that editor is not yet
                // ported into Aloha; keep this false until the feature is implemented.
                customColorSchemes = false,
            ),
        ),
    )

    fun definition(id: ThemeId): ThemeDefinition =
        definitions.first { it.id == id }
}
