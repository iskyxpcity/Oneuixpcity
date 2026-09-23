package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.LocalMainPagerState
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun NavigationRailMiuix(
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    val mainState = LocalMainPagerState.current
    val items = BottomBarDestination.entries.map { destination ->
        Pair(stringResource(destination.label), destination.icon)
    }

    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        color = MiuixTheme.colorScheme.surface,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        items.forEachIndexed { index, (label, icon) ->
            NavigationRailItem(
                icon = icon,
                label = label,
                selected = mainState.selectedPage == index,
                onClick = { mainState.animateToPage(index) },
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
