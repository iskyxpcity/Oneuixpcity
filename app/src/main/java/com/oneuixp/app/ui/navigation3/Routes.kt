package com.oneuixp.app.ui.navigation3

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/** OneuiXP main pages plus theme/About and OneUIX feature-category details. */
sealed interface Route : NavKey, Parcelable {
    @Parcelize @Serializable data object Main : Route
    @Parcelize @Serializable data object Home : Route
    @Parcelize @Serializable data object Tools : Route
    @Parcelize @Serializable data object Status : Route
    @Parcelize @Serializable data object Settings : Route
    @Parcelize @Serializable data object About : Route
    @Parcelize @Serializable data object ColorPalette : Route
    @Parcelize @Serializable data class OneuiXCategory(val category: String) : Route
}
