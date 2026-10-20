package com.oneuialoha.app.ui.component.bottombar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.oneuialoha.app.R
import com.oneuialoha.app.data.repository.SettingsRepositoryImpl
import com.oneuialoha.app.ui.LocalMainPagerState
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Miuix's own expandable rail and native selected state; no Material selection styling is applied. */
@Composable
fun SideNavigationRailMiuix(modifier: Modifier = Modifier) {
    val mainState = LocalMainPagerState.current
    val settingsRepo = remember { SettingsRepositoryImpl() }
    val state = rememberNavigationRailState(
        initialValue = if (settingsRepo.navigationRailExpanded) {
            NavigationRailValue.Expanded
        } else {
            NavigationRailValue.Collapsed
        }
    )
    LaunchedEffect(state.currentValue) {
        settingsRepo.navigationRailExpanded = state.isExpanded
    }

    val featuresIcon = ImageVector.vectorResource(id = R.drawable.apps)
    val items = BottomBarDestination.entries.map { destination ->
        Pair(
            stringResource(destination.label),
            if (destination == BottomBarDestination.Tools) featuresIcon else destination.icon
        )
    }

    NavigationRail(
        state = state,
        modifier = modifier,
        color = MiuixTheme.colorScheme.surface,
        expandContentDescription = stringResource(R.string.nav_rail_expand),
        collapseContentDescription = stringResource(R.string.nav_rail_collapse),
    ) {
        items.forEachIndexed { index, (label, icon) ->
            NavigationRailItem(
                selected = mainState.selectedPage == index,
                onClick = { if (mainState.selectedPage != index) mainState.animateToPage(index) },
                icon = icon,
                label = label,
            )
        }
    }
}
