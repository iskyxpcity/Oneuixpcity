package com.oneuixp.app.ui.component.bottombar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode

@Composable
fun SideNavigationRail(modifier: Modifier = Modifier) {
    when (LocalUiMode.current) {
        UiMode.Material -> SideNavigationRailMaterial(modifier)
        UiMode.Miuix -> SideNavigationRailMiuix(modifier)
    }
}
