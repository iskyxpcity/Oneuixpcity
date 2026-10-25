package com.alohaoneui.app.ui

/**
 * Identifies a visual theme implementation, not an application feature.
 *
 * Keep stable persisted values so future theme implementations can be added without
 * changing navigation, business state, or the existing Miuix preference.
 */
enum class ThemeId(val persistedValue: String) {
    MIUIX("miuix"),
    VOYAGE_MATERIAL3("material");

    companion object {
        fun fromPersistedValue(value: String): ThemeId =
            entries.firstOrNull { it.persistedValue == value } ?: MIUIX
    }
}

/**
 * Capability flags let settings UI expose only controls implemented by a theme.
 * They intentionally describe presentation capabilities rather than app features.
 */
data class ThemeCapabilities(
    val dynamicColor: Boolean,
    val customSeedColor: Boolean,
    val paletteStyleSelection: Boolean,
    val colorSpecSelection: Boolean,
    val amoledBlack: Boolean,
    val expressiveMotion: Boolean,
    val customColorSchemes: Boolean,
)

/** Metadata for a theme adapter registered with Aloha OneUI. */
data class ThemeDefinition(
    val id: ThemeId,
    val displayName: String,
    val capabilities: ThemeCapabilities,
)

/**
 * Central registry for theme implementations.
 *
 * Add future themes here and implement their adapter independently. Do not place
 * theme-specific rendering decisions in feature screens or settings business logic.
 */
object ThemeRegistry {
    val definitions: List<ThemeDefinition> = listOf(
        ThemeDefinition(
            id = ThemeId.MIUIX,
            displayName = "Miuix",
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
            capabilities = ThemeCapabilities(
                dynamicColor = true,
                customSeedColor = true,
                paletteStyleSelection = true,
                colorSpecSelection = true,
                amoledBlack = true,
                expressiveMotion = true,
                customColorSchemes = true,
            ),
        ),
    )

    fun definition(id: ThemeId): ThemeDefinition =
        definitions.first { it.id == id }
}
