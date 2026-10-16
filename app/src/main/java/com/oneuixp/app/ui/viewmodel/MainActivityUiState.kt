package com.oneuixp.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.theme.AppSettings

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enableSwipeBack: Boolean,
    val pagerGestureMode: Int,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val uiMode: UiMode,
)
