package com.oneuialoha.app.ui.screen.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.oneuialoha.app.R
import com.oneuialoha.app.oneuixp.OneUIAlohaCategory
import com.oneuialoha.app.ui.LocalUiMode
import com.oneuialoha.app.ui.UiMode
import com.oneuialoha.app.ui.component.configtransfer.ConfigTransferPopup
import com.oneuialoha.app.ui.component.rebootlistpopup.RebootListPopup
import com.oneuialoha.app.ui.component.material.SegmentedColumn
import com.oneuialoha.app.ui.component.material.SegmentedListItem
import com.oneuialoha.app.ui.navigation3.Navigator
import com.oneuialoha.app.ui.navigation3.Route
import com.oneuialoha.app.ui.screen.home.LocalLsposedHomeStatus
import com.oneuialoha.app.ui.theme.LocalEnableBlur
import com.oneuialoha.app.ui.util.BlurredBar
import com.oneuialoha.app.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ToolsPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val lsposedActive = LocalLsposedHomeStatus.current.active
    when (LocalUiMode.current) {
        UiMode.Miuix -> ToolsPagerMiuix(navigator, bottomInnerPadding, lsposedActive)
        UiMode.Material -> ToolsPagerMaterial(navigator, bottomInnerPadding, lsposedActive)
    }
}


@Composable
private fun OtherFunctionsCategoryIcon(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF607D8B)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.other_functions),
            contentDescription = stringResource(R.string.oneuialoha_other_functions_title),
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun CategoryIcon(
    category: OneUIAlohaCategory,
    size: Dp,
) {
    Image(
        bitmap = rememberOneUIAlohaCategoryIcon(category),
        contentDescription = oneUIAlohaCategoryTitle(category),
        modifier = Modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun ToolsPagerMiuix(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    lsposedActive: Boolean,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    val categories = OneUIAlohaCategory.entries
        .filterNot { it == OneUIAlohaCategory.SystemUI }

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.oneuialoha_tools),
                    navigationIcon = { ConfigTransferPopup() },
                    actions = { RebootListPopup() },
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
                item { Spacer(Modifier.height(12.dp)) }

                categories.forEach { category ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .graphicsLayer {
                                    alpha = if (lsposedActive) 1f else 0.38f
                                },
                            onClick = {
                                if (lsposedActive) {
                                    navigator.push(Route.OneUIAlohaCategory(category.name))
                                }
                            },
                            showIndication = lsposedActive,
                            insideMargin = PaddingValues(
                                start = 10.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 8.dp,
                            ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CategoryIcon(category = category, size = 48.dp)
                                MiuixText(
                                    text = oneUIAlohaCategoryTitle(category),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp),
                                    fontWeight = FontWeight(550),
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                val layoutDirection = LocalLayoutDirection.current
                                MiuixIcon(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                        }
                                        .padding(start = 8.dp)
                                        .size(width = 10.dp, height = 16.dp),
                                    imageVector = MiuixIcons.Basic.ArrowRight,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariantActions,
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .graphicsLayer {
                                alpha = if (lsposedActive) 1f else 0.38f
                            },
                        onClick = {
                            if (lsposedActive) navigator.push(Route.OtherFunctions)
                        },
                        showIndication = lsposedActive,
                        insideMargin = PaddingValues(
                            start = 10.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 8.dp,
                        ),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OtherFunctionsCategoryIcon(size = 48.dp)
                            MiuixText(
                                text = stringResource(R.string.oneuialoha_other_functions_title),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp),
                                fontWeight = FontWeight(550),
                                color = colorScheme.onSurface,
                                maxLines = 1,
                            )
                            val layoutDirection = LocalLayoutDirection.current
                            MiuixIcon(
                                modifier = Modifier
                                    .graphicsLayer {
                                        if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                    }
                                    .padding(start = 8.dp)
                                    .size(width = 10.dp, height = 16.dp),
                                imageVector = MiuixIcons.Basic.ArrowRight,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariantActions,
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(bottomInnerPadding)) }
            }
        }
    }
}

@Composable
private fun ToolsPagerMaterial(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    lsposedActive: Boolean,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val coreCategories = OneUIAlohaCategory.entries
        .take(5)
        .filterNot { it == OneUIAlohaCategory.SystemUI }
    val appCategories = OneUIAlohaCategory.entries.drop(5)

    androidx.compose.material3.Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.oneuialoha_tools)) },
                navigationIcon = { ConfigTransferPopup() },
                actions = { RebootListPopup() },
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
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .graphicsLayer {
                        alpha = if (lsposedActive) 1f else 0.38f
                    },
                title = stringResource(R.string.oneuialoha_core_features),
                content = coreCategories.map { category ->
                    {
                        SegmentedListItem(
                            enabled = lsposedActive,
                            onClick = {
                                if (lsposedActive) {
                                    navigator.push(Route.OneUIAlohaCategory(category.name))
                                }
                            },
                            headlineContent = {
                                Text(oneUIAlohaCategoryTitle(category))
                            },
                            leadingContent = {
                                CategoryIcon(category = category, size = 48.dp)
                            },
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
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .graphicsLayer {
                        alpha = if (lsposedActive) 1f else 0.38f
                    },
                title = stringResource(R.string.oneuialoha_app_features),
                content = appCategories.map { category ->
                    {
                        SegmentedListItem(
                            enabled = lsposedActive,
                            onClick = {
                                if (lsposedActive) {
                                    navigator.push(Route.OneUIAlohaCategory(category.name))
                                }
                            },
                            headlineContent = {
                                Text(oneUIAlohaCategoryTitle(category))
                            },
                            leadingContent = {
                                CategoryIcon(category = category, size = 48.dp)
                            },
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
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .graphicsLayer {
                        alpha = if (lsposedActive) 1f else 0.38f
                    },
                content = listOf({
                    SegmentedListItem(
                        enabled = lsposedActive,
                        onClick = {
                            if (lsposedActive) navigator.push(Route.OtherFunctions)
                        },
                        headlineContent = {
                            Text(stringResource(R.string.oneuialoha_other_functions_title))
                        },
                        leadingContent = {
                            OtherFunctionsCategoryIcon(size = 48.dp)
                        },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                            )
                        },
                    )
                }),
            )

            Spacer(Modifier.height(bottomInnerPadding))
        }
    }
}
