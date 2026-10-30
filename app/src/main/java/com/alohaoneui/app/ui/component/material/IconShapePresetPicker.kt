package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowTopBars
import kotlin.random.Random

internal const val ICON_SHAPE_NONE = -1
internal const val ICON_SHAPE_PRESET_COUNT = VoyageIconShapePresets.COUNT
internal const val ICON_SHAPE_RANDOM = ICON_SHAPE_PRESET_COUNT
internal const val ICON_SHAPE_OPTION_COUNT = ICON_SHAPE_PRESET_COUNT + 2

/**
 * The source theme page presents Icon shape as a settings row. Selecting it opens
 * the source-ordered list of 51 presets, followed by Random and None.
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
                navigationIcon = { TopBarBackButton(onClick = onBack) },
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
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ICON_SHAPE_OPTION_COUNT) { option ->
                val style = when {
                    option < ICON_SHAPE_PRESET_COUNT -> option
                    option == ICON_SHAPE_RANDOM -> ICON_SHAPE_RANDOM
                    else -> ICON_SHAPE_NONE
                }
                val label = when (style) {
                    ICON_SHAPE_NONE -> stringResource(R.string.theme_icon_shape_none)
                    ICON_SHAPE_RANDOM -> stringResource(R.string.theme_icon_shape_random)
                    else -> "${stringResource(R.string.theme_icon_shape_title)} ${style + 1}"
                }
                IconShapePresetRow(
                    style = style,
                    label = label,
                    selected = selectedIndex == style,
                    onClick = {
                        onSelected(style)
                    },
                )
            }
        }
    }
}

@Composable
private fun IconShapePresetRow(
    style: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember(style) { MutableInteractionSource() }
    val previewShape = remember(style) {
        if (style in 0 until ICON_SHAPE_PRESET_COUNT) {
            VoyageIconShapePresets.shape(style)
        } else {
            RectangleShape
        }
    }
    val colors = MaterialTheme.colorScheme
    val tileBackground = if (selected) colors.primaryContainer else colors.surfaceContainerLow
    val tileContent = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
    val tileBorder = if (selected) colors.primary else colors.outlineVariant

    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(tileBackground)
                .border(if (selected) 0.7.dp else 0.1.dp, tileBorder, androidx.compose.foundation.shape.CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onClick()
                    },
                )
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            when (style) {
                ICON_SHAPE_NONE -> Icon(
                    imageVector = Icons.Rounded.Block,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = tileContent,
                )
                ICON_SHAPE_RANDOM -> Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = tileContent,
                )
                else -> Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(previewShape)
                        .background(tileContent)
                        .border(2.dp, tileContent, previewShape),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
        )
    }
}

/** Applies the exact Voyage preset, or a stable random member of the same source list. */
internal fun iconShapeForStyle(style: Int, seed: Int = 0): Shape? {
    if (style == ICON_SHAPE_NONE) return null
    val presetIndex = if (style == ICON_SHAPE_RANDOM) {
        Random(seed).nextInt(ICON_SHAPE_PRESET_COUNT)
    } else {
        style.coerceIn(0, ICON_SHAPE_PRESET_COUNT - 1)
    }
    return VoyageIconShapePresets.shape(presetIndex)
}