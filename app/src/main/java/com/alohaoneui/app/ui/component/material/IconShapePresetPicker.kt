package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowContainers
import com.alohaoneui.app.ui.theme.LocalMaterialShadowTopBars
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import kotlin.math.pow

internal const val ICON_SHAPE_NONE = -1
internal const val ICON_SHAPE_PRESET_COUNT = 50
internal const val ICON_SHAPE_RANDOM = ICON_SHAPE_PRESET_COUNT
internal const val ICON_SHAPE_OPTION_COUNT = ICON_SHAPE_PRESET_COUNT + 2

/**
 * The source theme page presents Icon shape as a settings row. Selecting it opens
 * the detail list, whose individual choices use a 52dp row and source color states.
 */
@Composable
internal fun IconShapePresetPicker(
    selectedIndex: Int,
    onOpen: () -> Unit,
) {
    val selectedLabel = when (selectedIndex) {
        ICON_SHAPE_NONE -> stringResource(R.string.theme_icon_shape_none)
        ICON_SHAPE_RANDOM -> stringResource(R.string.theme_icon_shape_random)
        in 0 until ICON_SHAPE_PRESET_COUNT ->
            "${stringResource(R.string.theme_icon_shape_title)} ${selectedIndex + 1}"
        else -> stringResource(R.string.theme_icon_shape_none)
    }
    SegmentedListItem(
        onClick = onOpen,
        headlineContent = {
            Text(
                text = stringResource(R.string.theme_icon_shape_title),
                style = MaterialTheme.typography.titleSmall,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedLabel,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

@Composable
internal fun IconShapePresetDetailScreen(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier
                    .then(if (LocalMaterialShadowTopBars.current) Modifier.shadow(2.dp) else Modifier)
                    .then(
                        LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                            Modifier.border(it.dp, MaterialTheme.colorScheme.outlineVariant)
                        } ?: Modifier
                    ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text(stringResource(R.string.theme_icon_shape_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ICON_SHAPE_OPTION_COUNT) { option ->
                val style = when {
                    option < ICON_SHAPE_PRESET_COUNT -> option
                    option == ICON_SHAPE_RANDOM -> ICON_SHAPE_RANDOM
                    else -> ICON_SHAPE_NONE
                }
                val selected = selectedIndex == style
                val shape = remember(style) {
                    iconShapeForStyle(style, seed = style * 17 + 3) ?: RectangleShape
                }
                val label = when (style) {
                    ICON_SHAPE_NONE -> stringResource(R.string.theme_icon_shape_none)
                    ICON_SHAPE_RANDOM -> stringResource(R.string.theme_icon_shape_random)
                    else -> "${stringResource(R.string.theme_icon_shape_title)} ${style + 1}"
                }
                val interactionSource = remember(style) { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(voyageCornerShape(8.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                onSelected(style)
                            },
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(shape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            ),
                    )
                    Text(
                        text = label,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/** Placeholder shape geometry until all fifty original Voyage paths are ported exactly. */
internal fun iconShapeForStyle(style: Int, seed: Int = 0): Shape? {
    if (style == ICON_SHAPE_NONE) return null
    val presetIndex = if (style == ICON_SHAPE_RANDOM) {
        Math.floorMod(seed * 31 + 7, ICON_SHAPE_PRESET_COUNT)
    } else {
        style.coerceIn(0, ICON_SHAPE_PRESET_COUNT - 1)
    }
    return geometricIconShape(presetIndex)
}

private fun geometricIconShape(index: Int): Shape {
    val family = index / 10
    val variant = index % 10
    return androidx.compose.foundation.shape.GenericShape { size, _ ->
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f
        val points = when (family) {
            0 -> 3 + variant
            1 -> 4 + variant % 5
            2 -> (4 + variant % 5) * 2
            3 -> 8 + variant * 2
            else -> 12 + variant * 2
        }
        val pathPoints = ArrayList<androidx.compose.ui.geometry.Offset>(points)
        val exponent = 0.55f + variant * 0.16f
        for (i in 0 until points) {
            val angle = -Math.PI / 2.0 + 2.0 * Math.PI * i / points
            val base = when (family) {
                2 -> if (i % 2 == 0) 1.0 else (0.36 + variant * 0.035)
                3 -> 0.78 + 0.22 * kotlin.math.cos(angle * (3 + variant % 5))
                4 -> 0.84 + 0.16 * kotlin.math.sin(angle * (2 + variant % 6))
                else -> 1.0
            }
            val x = kotlin.math.cos(angle) * base
            val y = kotlin.math.sin(angle) * base
            val shapedX = (kotlin.math.sign(x) * kotlin.math.abs(x).pow(exponent.toDouble())).toFloat()
            val shapedY = (kotlin.math.sign(y) * kotlin.math.abs(y).pow(exponent.toDouble())).toFloat()
            pathPoints += androidx.compose.ui.geometry.Offset(
                cx + shapedX * cx * (0.94f - variant * 0.006f),
                cy + shapedY * cy * (0.94f - variant * 0.006f),
            )
        }
        if (pathPoints.isEmpty()) return@GenericShape
        val smoothing = 0.12f + (variant % 5) * 0.10f
        fun midpoint(a: androidx.compose.ui.geometry.Offset, b: androidx.compose.ui.geometry.Offset) =
            androidx.compose.ui.geometry.Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        val first = midpoint(pathPoints.last(), pathPoints.first())
        moveTo(first.x, first.y)
        pathPoints.forEachIndexed { i, point ->
            val next = pathPoints[(i + 1) % pathPoints.size]
            val mid = midpoint(point, next)
            quadraticTo(
                point.x + (mid.x - point.x) * smoothing,
                point.y + (mid.y - point.y) * smoothing,
                mid.x,
                mid.y,
            )
        }
        close()
    }
}
