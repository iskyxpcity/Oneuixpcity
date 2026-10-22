package com.alohaoneui.app.ui.screen.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.R
import com.alohaoneui.app.alohaoneui.AlohaOneUICategory
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.component.configtransfer.ConfigTransferPopup
import com.alohaoneui.app.ui.component.rebootlistpopup.RebootListPopup
import com.alohaoneui.app.ui.component.material.SegmentedColumn
import com.alohaoneui.app.ui.component.material.SegmentedListItem
import com.alohaoneui.app.ui.navigation3.Navigator
import com.alohaoneui.app.ui.navigation3.Route
import com.alohaoneui.app.ui.screen.home.LocalLsposedHomeStatus
import com.alohaoneui.app.ui.theme.LocalEnableBlur
import com.alohaoneui.app.ui.util.BlurredBar
import com.alohaoneui.app.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent as MiuixBasicComponent
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
private fun NavigationFeatureIcon(
    icon: ShortcutEntryIcon,
    size: Dp,
) {
    val isOtherFunctions = icon == ShortcutEntryIcon.OtherFunctions
    val backgroundColors = if (isOtherFunctions) {
        listOf(Color(0xFF11B8A5), Color(0xFF1386D9), Color(0xFF435CE5))
    } else {
        listOf(Color(0xFF25B9EA), Color(0xFF3C70EC), Color(0xFF6A4EDB))
    }

    // Samsung One UI app-icon language: a vivid rounded-square tile, bold white
    // symbol, analogous-color gradient, and restrained highlights for soft depth.
    Canvas(modifier = Modifier.size(size)) {
        val unit = this.size.minDimension
        val radius = CornerRadius(unit * 0.255f, unit * 0.255f)
        val canvasSize = Size(this.size.width, this.size.height)

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.16f),
            topLeft = Offset(0f, unit * 0.025f),
            size = canvasSize,
            cornerRadius = radius,
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = backgroundColors,
                start = Offset(0f, 0f),
                end = Offset(this.size.width, this.size.height),
            ),
            size = canvasSize,
            cornerRadius = radius,
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.015f), Color.Transparent),
                startY = 0f,
                endY = this.size.height * 0.72f,
            ),
            size = canvasSize,
            cornerRadius = radius,
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.24f),
            topLeft = Offset(unit * 0.018f, unit * 0.018f),
            size = Size(this.size.width - unit * 0.036f, this.size.height - unit * 0.036f),
            cornerRadius = CornerRadius(unit * 0.238f, unit * 0.238f),
            style = Stroke(width = unit * 0.018f),
        )

        fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float) {
            val start = Offset(x1 * unit, y1 * unit)
            val end = Offset(x2 * unit, y2 * unit)
            drawLine(
                color = Color.Black.copy(alpha = 0.18f),
                start = Offset(start.x, start.y + unit * 0.015f),
                end = Offset(end.x, end.y + unit * 0.015f),
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.98f),
                start = start,
                end = end,
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
        }

        fun handle(cx: Float, cy: Float) {
            val center = Offset(cx * unit, cy * unit)
            drawCircle(
                color = Color.Black.copy(alpha = 0.18f),
                radius = unit * 0.076f,
                center = Offset(center.x, center.y + unit * 0.018f),
            )
            drawCircle(color = Color.White, radius = unit * 0.076f, center = center)
            drawCircle(
                color = backgroundColors[1],
                radius = unit * 0.035f,
                center = Offset(center.x, center.y - unit * 0.008f),
            )
        }

        if (isOtherFunctions) {
            val stroke = unit * 0.075f
            line(0.25f, 0.35f, 0.75f, 0.35f, stroke)
            line(0.25f, 0.50f, 0.75f, 0.50f, stroke)
            line(0.25f, 0.65f, 0.75f, 0.65f, stroke)
            handle(0.42f, 0.35f)
            handle(0.62f, 0.50f)
            handle(0.49f, 0.65f)
        } else {
            // Rounded finger strokes and a swipe arrow read as a gesture instead of a generic line glyph.
            val fingerWidth = unit * 0.092f
            line(0.34f, 0.34f, 0.34f, 0.55f, fingerWidth)
            line(0.50f, 0.27f, 0.50f, 0.56f, fingerWidth)
            line(0.66f, 0.34f, 0.66f, 0.55f, fingerWidth)
            line(0.28f, 0.72f, 0.68f, 0.72f, unit * 0.070f)
            line(0.57f, 0.61f, 0.68f, 0.72f, unit * 0.070f)
            line(0.57f, 0.83f, 0.68f, 0.72f, unit * 0.070f)
        }
    }
}

@Composable
private fun CategoryIcon(
    category: AlohaOneUICategory,
    size: Dp,
) {
    Image(
        bitmap = rememberAlohaOneUICategoryIcon(category),
        contentDescription = alohaOneUICategoryTitle(category),
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
    val categories = AlohaOneUICategory.entries
        .filterNot { it == AlohaOneUICategory.SystemUI }

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.alohaoneui_tools),
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
                                    navigator.push(Route.AlohaOneUICategory(category.name))
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
                                    text = alohaOneUICategoryTitle(category),
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
                            NavigationFeatureIcon(icon = ShortcutEntryIcon.OtherFunctions, size = 48.dp)
                            MiuixText(
                                text = stringResource(R.string.alohaoneui_other_functions_title),
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

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .graphicsLayer { alpha = if (lsposedActive) 1f else 0.38f },
                        onClick = {
                            if (lsposedActive) navigator.push(Route.GlobalShortcutActions)
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
                            NavigationFeatureIcon(
                                icon = ShortcutEntryIcon.Gesture,
                                size = 48.dp,
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp),
                            ) {
                                MiuixText(
                                    text = stringResource(R.string.alohaoneui_global_shortcut_actions_title),
                                    fontWeight = FontWeight(550),
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                MiuixText(
                                    text = stringResource(R.string.alohaoneui_global_shortcut_actions_summary),
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = colorScheme.onSurfaceVariantSummary,
                                    maxLines = 2,
                                )
                            }
                            val layoutDirection = LocalLayoutDirection.current
                            MiuixIcon(
                                modifier = Modifier
                                    .graphicsLayer { if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f }
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
    val coreCategories = AlohaOneUICategory.entries
        .take(5)
        .filterNot { it == AlohaOneUICategory.SystemUI }
    val appCategories = AlohaOneUICategory.entries.drop(5)

    androidx.compose.material3.Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.alohaoneui_tools)) },
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
                title = stringResource(R.string.alohaoneui_core_features),
                content = coreCategories.map { category ->
                    {
                        SegmentedListItem(
                            enabled = lsposedActive,
                            onClick = {
                                if (lsposedActive) {
                                    navigator.push(Route.AlohaOneUICategory(category.name))
                                }
                            },
                            headlineContent = {
                                Text(alohaOneUICategoryTitle(category))
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
                title = stringResource(R.string.alohaoneui_app_features),
                content = appCategories.map { category ->
                    {
                        SegmentedListItem(
                            enabled = lsposedActive,
                            onClick = {
                                if (lsposedActive) {
                                    navigator.push(Route.AlohaOneUICategory(category.name))
                                }
                            },
                            headlineContent = {
                                Text(alohaOneUICategoryTitle(category))
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
                            Text(stringResource(R.string.alohaoneui_other_functions_title))
                        },
                        leadingContent = {
                            NavigationFeatureIcon(icon = ShortcutEntryIcon.OtherFunctions, size = 48.dp)
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

            SegmentedColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .graphicsLayer { alpha = if (lsposedActive) 1f else 0.38f },
                content = listOf({
                    SegmentedListItem(
                        enabled = lsposedActive,
                        onClick = {
                            if (lsposedActive) navigator.push(Route.GlobalShortcutActions)
                        },
                        colors = com.alohaoneui.app.ui.component.material.alohaOneUISegmentedColors(),
                        headlineContent = { Text(stringResource(R.string.alohaoneui_global_shortcut_actions_title)) },
                        supportingContent = { Text(stringResource(R.string.alohaoneui_global_shortcut_actions_summary)) },
                        leadingContent = {
                            NavigationFeatureIcon(
                                icon = ShortcutEntryIcon.Gesture,
                                size = 48.dp,
                            )
                        },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        },
                    )
                }),
            )

            Spacer(Modifier.height(bottomInnerPadding))
        }
    }
}
