package com.oneuixp.app.ui.component.bottombar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.oneuixp.app.R
import com.oneuixp.app.ui.LocalMainPagerState
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Miuix-native rail. NavigationRailItem owns the selected state styling here;
 * no Material 3 selection colors or button shapes are imposed on Miuix.
 * Icons mirror BottomBarMiuix exactly.
 */
@Composable
fun SideNavigationRailMiuix(modifier: Modifier = Modifier) {
    val mainState = LocalMainPagerState.current
    val featuresIcon = ImageVector.vectorResource(id = R.drawable.apps)
    val items = BottomBarDestination.entries.map { destination ->
        NavigationItem(
            label = stringResource(destination.label),
            icon = if (destination == BottomBarDestination.Tools) featuresIcon else destination.icon,
        )
    }

    NavigationRail(
        modifier = modifier,
        color = MiuixTheme.colorScheme.surface,
    ) {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                selected = mainState.selectedPage == index,
                onClick = {
                    if (mainState.selectedPage != index) {
                        mainState.animateToPage(index)
                    }
                },
                icon = item.icon,
                label = item.label,
            )
        }
    }
}
