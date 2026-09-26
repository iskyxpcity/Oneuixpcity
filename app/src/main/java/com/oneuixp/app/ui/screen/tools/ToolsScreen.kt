package com.oneuixp.app.ui.screen.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oneuixp.app.R
import com.oneuixp.app.oneuix.OneuiXCategory
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.component.material.SegmentedColumn
import com.oneuixp.app.ui.component.material.SegmentedListItem
import com.oneuixp.app.ui.navigation3.Navigator
import com.oneuixp.app.ui.navigation3.Route
import com.oneuixp.app.ui.theme.LocalEnableBlur
import com.oneuixp.app.ui.util.BlurredBar
import com.oneuixp.app.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ToolsPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ToolsPagerMiuix(navigator, bottomInnerPadding)
        UiMode.Material -> ToolsPagerMaterial(navigator, bottomInnerPadding)
    }
}

@Composable
private fun categoryTitle(category: OneuiXCategory): String {
    val context = LocalContext.current
    return remember(category, context.resources.configuration.locales) {
        if (category.packageName == "android") {
            category.fallbackTitle
        } else {
            runCatching {
                val info = context.packageManager.getApplicationInfo(category.packageName, 0)
                info.loadLabel(context.packageManager).toString()
            }.getOrDefault(category.fallbackTitle)
        }
    }
}

@Composable
private fun ToolsPagerMiuix(
    navigator: Navigator,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    val categories = OneuiXCategory.entries

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.oneuixp_tools),
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = {},
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        categories.take(5).forEach { category ->
                            ArrowPreference(
                                title = categoryTitle(category),
                                summary = category.packageName,
                                onClick = { navigator.push(Route.OneuiXCategory(category.name)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        categories.drop(5).forEach { category ->
                            ArrowPreference(
                                title = categoryTitle(category),
                                summary = category.packageName,
                                onClick = { navigator.push(Route.OneuiXCategory(category.name)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

@Composable
private fun ToolsPagerMaterial(
    navigator: Navigator,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val categories = OneuiXCategory.entries

    androidx.compose.material3.Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.oneuixp_tools)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {
            SegmentedColumn(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                title = stringResource(R.string.oneuix_core_features),
                content = categories.take(5).map { category ->
                    {
                        SegmentedListItem(
                            onClick = {
                                navigator.push(Route.OneuiXCategory(category.name))
                            },
                            headlineContent = { Text(categoryTitle(category)) },
                            supportingContent = { Text(category.packageName) },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                },
            )
            SegmentedColumn(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                title = stringResource(R.string.oneuix_app_features),
                content = categories.drop(5).map { category ->
                    {
                        SegmentedListItem(
                            onClick = {
                                navigator.push(Route.OneuiXCategory(category.name))
                            },
                            headlineContent = { Text(categoryTitle(category)) },
                            supportingContent = { Text(category.packageName) },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                },
            )
            Spacer(Modifier.height(bottomInnerPadding))
        }
    }
}
