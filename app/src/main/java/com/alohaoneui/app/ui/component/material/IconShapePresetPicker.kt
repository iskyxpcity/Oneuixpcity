package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import com.alohaoneui.app.R

internal const val ICON_SHAPE_NONE = 0
internal const val ICON_SHAPE_RANDOM = 1
internal const val ICON_SHAPE_PRESET_COUNT = 50
internal const val ICON_SHAPE_OPTION_COUNT = ICON_SHAPE_PRESET_COUNT + 2

/**
 * Shape-preview strip for the icon mask setting. Index 0 preserves the unmasked
 * tile, index 1 selects a stable per-icon random mask, and indices 2..51 expose
 * fifty individually selectable geometric masks.
 */
@Composable
internal fun IconShapePresetPicker(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.theme_icon_shape_title),
            style = MaterialTheme.typography.titleSmall,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            items(ICON_SHAPE_OPTION_COUNT) { option ->
                val shape = remember(option) {
                    iconShapeForStyle(if (option < 2) option else option, seed = option * 17 + 3)
                        ?: RoundedCornerShape(12.dp)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onSelected(option) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(shape)
                            .background(
                                if (selectedIndex == option) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .then(
                                if (selectedIndex == option) Modifier.border(
                                    2.dp, MaterialTheme.colorScheme.primary, shape
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when (option) {
                                ICON_SHAPE_NONE -> "—"
                                ICON_SHAPE_RANDOM -> "R"
                                else -> "%02d".format(option - 1)
                            },
                            color = if (selectedIndex == option) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(
                        text = when (option) {
                            ICON_SHAPE_NONE -> stringResource(R.string.theme_icon_shape_none)
                            ICON_SHAPE_RANDOM -> stringResource(R.string.theme_icon_shape_random)
                            else -> "%02d".format(option - 1)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedIndex == option) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Returns the 50 selectable masks, with deterministic resolution for Random. */
internal fun iconShapeForStyle(style: Int, seed: Int = 0): Shape? {
    if (style == ICON_SHAPE_NONE) return null
    val presetIndex = if (style == ICON_SHAPE_RANDOM) {
        Math.floorMod(seed * 31 + 7, ICON_SHAPE_PRESET_COUNT)
    } else {
        (style - 2).coerceIn(0, ICON_SHAPE_PRESET_COUNT - 1)
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

