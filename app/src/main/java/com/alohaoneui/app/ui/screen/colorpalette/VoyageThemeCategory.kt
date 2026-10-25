package com.alohaoneui.app.ui.screen.colorpalette

import androidx.annotation.StringRes
import com.alohaoneui.app.R

/** Category order extracted from Voyage's ThemeSettingsRoute destination. */
enum class VoyageThemeCategory(@StringRes val titleRes: Int) {
    DESIGN(R.string.voyage_theme_group_design),
    NAVIGATION_BAR(R.string.voyage_theme_group_navigation_bar),
    MODE(R.string.voyage_theme_group_mode),
    LAUNCH(R.string.voyage_theme_group_launch),
    ASSIST(R.string.voyage_theme_group_assist),
    SHAPE(R.string.voyage_theme_group_shape),
    COMPONENTS(R.string.voyage_theme_group_components),
    SHADOWS(R.string.voyage_theme_group_shadows),
}
