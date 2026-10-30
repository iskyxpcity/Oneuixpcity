package com.alohaoneui.app.ui.component.bottombar

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.LocalMainPagerState
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.component.FloatingBottomBar
import com.alohaoneui.app.ui.component.FloatingBottomBarItem
import com.alohaoneui.app.ui.theme.LocalEnableFloatingBottomBar
import com.alohaoneui.app.ui.theme.LocalEnableFloatingBottomBarBlur
import com.alohaoneui.app.ui.util.BlurredBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BottomBarMiuix(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
    modifier: Modifier,
) {
    val mainState = LocalMainPagerState.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val uiMode = LocalUiMode.current
    val floatingBarAccentColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.primary
        UiMode.Miuix -> MiuixTheme.colorScheme.primary
    }
    val floatingBarSurfaceColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surfaceContainer
        UiMode.Miuix -> MiuixTheme.colorScheme.surfaceContainer
    }
    val floatingBarContentColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.onSurface
        UiMode.Miuix -> MiuixTheme.colorScheme.onSurface
    }

    val featuresIcon = ImageVector.vectorResource(id = R.drawable.apps)
    val items = BottomBarDestination.entries.map { destination ->
        NavigationItem(
            label = stringResource(destination.label),
            icon = if (destination == BottomBarDestination.Tools) featuresIcon else destination.icon,
        )
    }
    if (!enableFloatingBottomBar) {
        BlurredBar(blurBackdrop) {
            NavigationBar(
                modifier = modifier,
                color = if (blurBackdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                content = {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            modifier = Modifier.weight(1f),
                            icon = item.icon,
                            label = item.label,
                            selected = mainState.selectedPage == index,
                            onClick = {
                                mainState.animateToPage(index)
                            }
                        )
                    }
                }
            )
        }
    } else {
        FloatingBottomBar(
            modifier = modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            selectedIndex = { mainState.selectedPage },
            onSelected = { mainState.animateToPage(it) },
            backdrop = backdrop,
            tabsCount = items.size,
            accentColor = floatingBarAccentColor,
            surfaceContainerColor = floatingBarSurfaceColor,
            isBlurEnabled = enableFloatingBottomBarBlur,
        ) {
            items.forEachIndexed { index, item ->
                FloatingBottomBarItem(
                    onClick = {
                        mainState.animateToPage(index)
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = floatingBarContentColor
                    )
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = floatingBarContentColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }
}

enum class BottomBarDestination(
    @get:StringRes val label: Int,
    val icon: ImageVector,
) {
    Home(R.string.home, Icons.Rounded.Cottage),
    Tools(R.string.alohaoneui_tools, Icons.Rounded.Extension),
    Status(R.string.alohaoneui_status, Icons.Rounded.Extension),
    Setting(R.string.settings, Icons.Rounded.Settings)
}
