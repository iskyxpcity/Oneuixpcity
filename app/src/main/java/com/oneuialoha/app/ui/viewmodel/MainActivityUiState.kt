package com.oneuialoha.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.oneuialoha.app.ui.UiMode
import com.oneuialoha.app.ui.theme.AppSettings

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
