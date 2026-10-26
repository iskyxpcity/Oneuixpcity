package com.alohaoneui.app.ui.screen.colorpalette

import androidx.annotation.StringRes
import com.alohaoneui.app.R

/** Category order recovered from the Voyage theme screen's resource call sequence. */
enum class VoyageThemeCategory(@StringRes val titleRes: Int) {
    SHADOWS(R.string.voyage_theme_group_shadows),
    COMPONENTS(R.string.voyage_theme_group_components),
    SHAPE(R.string.voyage_theme_group_shape),
    ASSIST(R.string.voyage_theme_group_assist),
    LAUNCH(R.string.voyage_theme_group_launch),
    MODE(R.string.voyage_theme_group_mode),
    NAVIGATION_BAR(R.string.voyage_theme_group_navigation_bar),
    DESIGN(R.string.voyage_theme_group_design),
}
