package com.alohaoneui.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.theme.AppSettings

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enablePredictiveBack: Boolean,
    val enableSwipeBack: Boolean,
    val pagerGestureMode: Int,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val uiMode: UiMode,
)
