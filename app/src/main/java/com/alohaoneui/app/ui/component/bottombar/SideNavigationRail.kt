package com.alohaoneui.app.ui.component.bottombar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode

@Composable
fun SideNavigationRail(modifier: Modifier = Modifier) {
    when (LocalUiMode.current) {
        UiMode.Material -> SideNavigationRailMaterial(modifier)
        UiMode.Miuix -> SideNavigationRailMiuix(modifier)
    }
}
