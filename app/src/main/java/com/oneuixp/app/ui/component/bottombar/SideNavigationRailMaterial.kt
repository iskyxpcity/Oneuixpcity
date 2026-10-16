package com.oneuixp.app.ui.component.bottombar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import com.oneuixp.app.R
import com.oneuixp.app.ui.LocalMainPagerState

/**
 * Material 3 navigation rail for expanded windows.
 *
 * Destination order and icon resources intentionally match BottomBarMaterial.
 */
@Composable
fun SideNavigationRailMaterial(modifier: Modifier = Modifier) {
    val mainPagerState = LocalMainPagerState.current
    val featuresFilledIcon = ImageVector.vectorResource(id = R.drawable.apps)
    val featuresOutlinedIcon = ImageVector.vectorResource(id = R.drawable.apps_outlined)
    val items = listOf(
        Triple(R.string.home, Icons.Filled.Home, Icons.Outlined.Home),
        Triple(R.string.oneuixp_tools, featuresFilledIcon, featuresOutlinedIcon),
        Triple(R.string.oneuixp_status, Icons.Filled.Extension, Icons.Outlined.Extension),
        Triple(R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    NavigationRail(
        modifier = modifier,
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            WindowInsetsSides.Start + WindowInsetsSides.Vertical
        )
    ) {
        items.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
            val selected = mainPagerState.selectedPage == index
            NavigationRailItem(
                selected = selected,
                onClick = {
                    if (!selected) mainPagerState.animateToPage(index)
                },
                icon = {
                    Icon(
                        imageVector = if (selected) selectedIcon else unselectedIcon,
                        contentDescription = stringResource(label)
                    )
                },
                label = {
                    Text(
                        text = stringResource(label),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}
