package com.alohaoneui.app.ui

import androidx.compose.runtime.Composable
import com.alohaoneui.app.ui.theme.AppSettings
import com.alohaoneui.app.ui.theme.MaterialAlohaOneUITheme
import com.alohaoneui.app.ui.theme.MiuixAlohaOneUITheme

/** Identifies a visual theme implementation; persisted values remain backward-compatible. */
enum class ThemeId(val persistedValue: String) {
    MIUIX("miuix"),
    VOYAGE_MATERIAL3("material");

    companion object {
        fun fromPersistedValue(value: String): ThemeId =
            entries.firstOrNull { it.persistedValue == value } ?: MIUIX
    }
}

/**
 * Capability inventory for implementation and future settings routing.
 * A flag is true only when the selected renderer actually implements that capability.
 */
data class ThemeCapabilities(
    val dynamicColor: Boolean,
    val customSeedColor: Boolean,
    val contrastPreference: Boolean,
    val paletteStyleSelection: Boolean,
    val colorSpecSelection: Boolean,
    val amoledBlack: Boolean,
    val expressiveMotion: Boolean,
    val customColorSchemes: Boolean,
    val fontFamilySelection: Boolean,
    val fontScale: Boolean,
    val cornerScale: Boolean,
    val borderWidth: Boolean,
    val colorVisionMode: Boolean,
    val contentColorExtraction: Boolean,
    val navigationMotionSelection: Boolean,
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

/** Register new theme renderers here without changing navigation or business logic. */
object ThemeRegistry {
    val definitions: List<ThemeDefinition> = listOf(
        ThemeDefinition(
            id = ThemeId.MIUIX,
            displayName = "Miuix",
            adapter = MiuixThemeAdapter,
            capabilities = ThemeCapabilities(
                dynamicColor = true,
                customSeedColor = true,
                contrastPreference = false,
                paletteStyleSelection = true,
                colorSpecSelection = true,
                // Current Miuix mapping uses MonetDark rather than a pure-black scheme.
                amoledBlack = false,
                expressiveMotion = false,
                customColorSchemes = false,
                fontFamilySelection = false,
                fontScale = false,
                cornerScale = false,
                borderWidth = false,
                colorVisionMode = false,
                contentColorExtraction = false,
                navigationMotionSelection = false,
            ),
        ),
        ThemeDefinition(
            id = ThemeId.VOYAGE_MATERIAL3,
            displayName = "Material 3",
            adapter = Material3ThemeAdapter,
            capabilities = ThemeCapabilities(
                dynamicColor = true,
                customSeedColor = true,
                contrastPreference = true,
                paletteStyleSelection = true,
                colorSpecSelection = true,
                amoledBlack = true,
                expressiveMotion = true,
                // Voyage supports this, but the editor has not yet been ported to Aloha.
                customColorSchemes = false,
                fontFamilySelection = true,
                fontScale = true,
                cornerScale = true,
                borderWidth = true,
                colorVisionMode = true,
                contentColorExtraction = false,
                navigationMotionSelection = false,
            ),
        ),
    )

    fun definition(id: ThemeId): ThemeDefinition =
        definitions.first { it.id == id }
}
