package com.oneuixp.app.ui.navigation3

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/** Only OneuiXP's four main pages, theme and About are navigable. */
sealed interface Route : NavKey, Parcelable {
    @Parcelize @Serializable data object Main : Route
    @Parcelize @Serializable data object Home : Route
    @Parcelize @Serializable data object Tools : Route
    @Parcelize @Serializable data object Status : Route
    @Parcelize @Serializable data object Settings : Route
    @Parcelize @Serializable data object About : Route
    @Parcelize @Serializable data object ColorPalette : Route
}
