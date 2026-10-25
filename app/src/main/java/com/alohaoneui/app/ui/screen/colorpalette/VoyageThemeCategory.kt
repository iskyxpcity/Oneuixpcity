package com.alohaoneui.app.ui.screen.colorpalette

import androidx.annotation.StringRes
import com.alohaoneui.app.R

/**
 * Navigation order recovered from Voyage's ThemeSettingsRoute destination.
 * Keep the category order and labels source-aligned; feature wiring is shared with Aloha.
 */
enum class VoyageThemeCategory(@StringRes val titleRes: Int) {
    MODE(R.string.voyage_theme_group_mode),
    DESIGN(R.string.voyage_theme_group_design),
    COMPONENTS(R.string.voyage_theme_group_components),
    LAUNCH(R.string.voyage_theme_group_launch),
    NAVIGATION_BAR(R.string.voyage_theme_group_navigation_bar),
    ASSIST(R.string.voyage_theme_group_assist),
    SHAPE(R.string.voyage_theme_group_shape),
    SHADOWS(R.string.voyage_theme_group_shadows),
}
