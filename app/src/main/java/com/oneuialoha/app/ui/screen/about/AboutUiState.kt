package com.oneuialoha.app.ui.screen.about

import androidx.compose.runtime.Immutable

@Immutable
data class AboutUiState(
    val title: String,
    val appName: String,
    val versionName: String,
    val featureIntroductionTitle: String,
    val disclaimerTitle: String,
)

@Immutable
data class AboutScreenActions(
    val onBack: () -> Unit,
    val onOpenFeatureIntroduction: () -> Unit,
    val onOpenDisclaimer: () -> Unit,
)
