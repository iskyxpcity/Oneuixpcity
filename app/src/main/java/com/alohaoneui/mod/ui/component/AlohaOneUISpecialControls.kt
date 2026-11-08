package com.alohaoneui.mod.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.component.material.ExpressiveSwitch
import com.alohaoneui.app.ui.component.material.ExpressiveOutlinedTextField
import com.alohaoneui.app.ui.component.material.ExpressiveButton
import com.alohaoneui.app.ui.component.material.LocalListItemShapes
import com.alohaoneui.app.ui.component.material.SegmentedListItem
import com.alohaoneui.app.ui.component.material.TonalCard
import com.alohaoneui.app.ui.component.material.alohaOneUISegmentedColors
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

val LocalAlohaOneUIGroupedPreference = compositionLocalOf { false }

@Composable
fun AlohaOneUIPPreferenceGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> {
            if (LocalAlohaOneUIGroupedPreference.current) {
                // Nested functional groups remain distinct child surfaces.
                // This is important on detail pages that contain multiple
                // independently titled/expandable option groups.
                TonalCard(
                    modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    CompositionLocalProvider(
                        LocalAlohaOneUIGroupedPreference provides true,
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            content = content,
                        )
                    }
                }
            } else {
                val baseShapes = ListItemDefaults.segmentedShapes(0, 1)
                val flatShapes = baseShapes.copy(
                    shape = RoundedCornerShape(0.dp),
                    selectedShape = RoundedCornerShape(0.dp),
                    pressedShape = RoundedCornerShape(0.dp),
                    focusedShape = RoundedCornerShape(0.dp),
                    hoveredShape = RoundedCornerShape(0.dp),
                    draggedShape = RoundedCornerShape(0.dp),
                )
                TonalCard(
                    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    CompositionLocalProvider(
                        LocalAlohaOneUIGroupedPreference provides true,
                        LocalListItemShapes provides flatShapes,
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            content = content,
                        )
                    }
                }
            }
        }

        UiMode.Miuix -> Card(
            modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            CompositionLocalProvider(LocalAlohaOneUIGroupedPreference provides true) {
                Column(content = content)
            }
        }
    }
}

/**
 * Group ordinary detail-page rows with the same segmented-card language used
 * by the four main option pages. Miuix keeps its existing layout unchanged.
 */
@Composable
fun AlohaOneUIDetailGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> AlohaOneUIPPreferenceGroup(modifier = modifier, content = content)
        UiMode.Miuix -> Column(modifier = modifier, content = content)
    }
}


@Composable
fun AlohaOneUIChildOptionSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> TonalCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                content()
            }
        }

        UiMode.Miuix -> Box(modifier = modifier) {
            content()
        }
    }
}


@Composable
fun AlohaOneUIChildVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(visible = visible) {
        AlohaOneUIChildOptionSurface {
            content()
        }
    }
}

@Composable
fun AlohaOneUIActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    fillWidth: Boolean = false,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> ExpressiveButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        ) {
            MaterialText(text)
        }

        UiMode.Miuix -> MiuixButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = MiuixButtonDefaults.buttonColorsPrimary(),
        ) {
            Box(
                modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
                contentAlignment = Alignment.Center,
            ) {
                MiuixText(text)
            }
        }
    }
}

@Composable
fun AlohaOneUISwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> ExpressiveSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )

        UiMode.Miuix -> MiuixSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
    }
}

@Composable
fun AlohaOneUIInlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    labelText: String? = null,
    singleLine: Boolean = false,
) {
    val grouped = LocalAlohaOneUIGroupedPreference.current
    when (LocalUiMode.current) {
        UiMode.Material -> {
            @Composable
            fun Content(contentModifier: Modifier) {
                ExpressiveOutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = contentModifier,
                    label = labelText?.let { label -> { MaterialText(label) } },
                    singleLine = singleLine,
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
            }
            if (grouped) {
                AlohaOneUIChildOptionSurface(modifier = modifier) { Content(Modifier) }
            } else {
                Content(modifier)
            }
        }

        UiMode.Miuix -> {
            @Composable
            fun Content(contentModifier: Modifier = Modifier) {
                Column(
                    modifier = contentModifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (!labelText.isNullOrBlank()) {
                        MiuixText(
                            text = labelText,
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    MiuixTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        insideMargin = DpSize(12.dp, 10.dp),
                        cornerRadius = 12.dp,
                        enabled = true,
                        singleLine = singleLine,
                        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                    )
                }
            }

            if (grouped) {
                Content(modifier)
            } else {
                Card(modifier = modifier) {
                    Content()
                }
            }
        }
    }
}

@Composable
fun AlohaOneUIInlineTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    labelText: String? = null,
    singleLine: Boolean = false,
) {
    val grouped = LocalAlohaOneUIGroupedPreference.current
    when (LocalUiMode.current) {
        UiMode.Material -> {
            @Composable
            fun Content(contentModifier: Modifier) {
                ExpressiveOutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = contentModifier,
                    label = labelText?.let { label -> { MaterialText(label) } },
                    singleLine = singleLine,
                )
            }
            if (grouped) {
                AlohaOneUIChildOptionSurface(modifier = modifier) { Content(Modifier) }
            } else {
                Content(modifier)
            }
        }

        UiMode.Miuix -> {
            @Composable
            fun Content(contentModifier: Modifier = Modifier) {
                Column(
                    modifier = contentModifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (!labelText.isNullOrBlank()) {
                        MiuixText(
                            text = labelText,
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    MiuixTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        insideMargin = DpSize(12.dp, 10.dp),
                        cornerRadius = 12.dp,
                        enabled = true,
                        singleLine = singleLine,
                        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                    )
                }
            }

            if (grouped) {
                Content(modifier)
            } else {
                Card(modifier = modifier) {
                    Content()
                }
            }
        }
    }
}


@Composable
fun AlohaOneUIInfoItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val grouped = LocalAlohaOneUIGroupedPreference.current
    when (LocalUiMode.current) {
        UiMode.Material -> {
            @Composable
            fun Content(contentModifier: Modifier) {
                SegmentedListItem(
                    modifier = contentModifier,
                    colors = alohaOneUISegmentedColors(),
                    headlineContent = { MaterialText(title) },
                    supportingContent = summary?.let { { MaterialText(it) } },
                    leadingContent = icon?.let { imageVector ->
                        { Icon(imageVector, contentDescription = title) }
                    },
                    trailingContent = trailingContent,
                )
            }

            if (grouped) {
                AlohaOneUIChildOptionSurface(modifier = modifier) {
                    Content(Modifier)
                }
            } else {
                Content(modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }

        UiMode.Miuix -> {
            @Composable
            fun Content() {
                BasicComponent(
                    title = title,
                    summary = summary,
                    startAction = icon?.let { imageVector ->
                        {
                            MiuixIcon(
                                imageVector = imageVector,
                                contentDescription = title,
                                tint = colorScheme.onSurface,
                            )
                        }
                    },
                    endActions = trailingContent?.let { content ->
                        { content() }
                    },
                )
            }

            if (grouped) {
                Content()
            } else {
                Card(
                    modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Content()
                }
            }
        }
    }
}

@Composable
fun AlohaOneUISectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialText(
            text = text,
            modifier = modifier,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
        )

        UiMode.Miuix -> MiuixText(
            text = text,
            modifier = modifier,
            color = colorScheme.primary,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
        )
    }
}

@Composable
fun AlohaOneUISupportingText(
    text: String,
    modifier: Modifier = Modifier,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialText(
            text = text,
            modifier = modifier,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )

        UiMode.Miuix -> MiuixText(
            text = text,
            modifier = modifier,
            color = colorScheme.onSurfaceVariantSummary,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
        )
    }
}
