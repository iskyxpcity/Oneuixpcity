package com.alohaoneui.app.ui.navigation3

import android.os.Parcelable
import top.yukonga.miuix.kmp.nav.core.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/** AlohaOneUI main pages plus theme/About and AlohaOneUI feature-category details. */
@Serializable
sealed interface Route : NavKey, Parcelable {
    @Parcelize @Serializable data object Main : Route
    @Parcelize @Serializable data object Home : Route
    @Parcelize @Serializable data object Tools : Route
    @Parcelize @Serializable data object Status : Route
    @Parcelize @Serializable data object Settings : Route
    @Parcelize @Serializable data object About : Route
    @Parcelize @Serializable data object Disclaimer : Route
    @Parcelize @Serializable data object FeatureIntroduction : Route
    @Parcelize @Serializable data object VersionChangelog261025 : Route
    @Parcelize @Serializable data object VersionChangelog261023 : Route
    @Parcelize @Serializable data object VersionChangelog2610191 : Route
    @Parcelize @Serializable data object VersionChangelog : Route
    @Parcelize @Serializable data object VersionChangelog261018 : Route
    @Parcelize @Serializable data object VersionChangelog261016 : Route
    @Parcelize @Serializable data object VersionChangelog261013 : Route
    @Parcelize @Serializable data object ThemeSettings : Route
    @Parcelize @Serializable data object ColorPalette : Route
    @Parcelize @Serializable data class AlohaOneUICategory(val category: String) : Route
    @Parcelize @Serializable data class StatusCategory(val category: String) : Route
    @Parcelize @Serializable data object OtherFunctions : Route
    @Parcelize @Serializable data object CallReturnDelay : Route
    @Parcelize @Serializable data object GlobalShortcutActions : Route
    @Parcelize @Serializable data object GlobalThreeFingerActions : Route
    @Parcelize @Serializable data object GlobalThreeFingerSwipe : Route
    @Parcelize @Serializable data object GlobalThreeFingerPinch : Route
    @Parcelize @Serializable data object GlobalThreeFingerSpread : Route
    @Parcelize @Serializable data object GlobalFloatingBallShortcut : Route
    @Parcelize @Serializable data object GlobalStatusBarShortcut : Route
    @Parcelize @Serializable data object GlobalNavigationKeyShortcut : Route
    @Parcelize @Serializable data object GlobalScreenOffVolumeKeyShortcut : Route
}
