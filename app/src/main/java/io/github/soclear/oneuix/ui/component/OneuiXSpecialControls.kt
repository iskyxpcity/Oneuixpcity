package io.github.soclear.oneuix.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.component.material.ExpressiveSwitch
import com.oneuixp.app.ui.component.material.SegmentedListItem
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

val LocalOneuiXGroupedPreference = compositionLocalOf { false }

@Composable
fun OneuiXPreferenceGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialCard(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            ),
        ) {
            CompositionLocalProvider(LocalOneuiXGroupedPreference provides true) {
                Column(content = content)
            }
        }

        UiMode.Miuix -> Card(
            modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            CompositionLocalProvider(LocalOneuiXGroupedPreference provides true) {
                Column(content = content)
            }
        }
    }
}

@Composable
fun OneuiXActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialButton(
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
        ) {
            MiuixText(text)
        }
    }
}

@Composable
fun OneuiXSwitch(
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
fun OneuiXInlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    labelText: String? = null,
    singleLine: Boolean = false,
) {
    val grouped = LocalOneuiXGroupedPreference.current
    when (LocalUiMode.current) {
        UiMode.Material -> OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = labelText?.let { label -> { MaterialText(label) } },
            singleLine = singleLine,
        )

        UiMode.Miuix -> {
            @Composable
            fun Content() {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (!labelText.isNullOrEmpty()) {
                        MiuixText(
                            text = labelText,
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MiuixTheme.textStyles.main.copy(
                            color = colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(colorScheme.primary),
                    )
                }
            }

            if (grouped) {
                Content()
            } else {
                Card(modifier = modifier) {
                    Content()
                }
            }
        }
    }
}

@Composable
fun OneuiXInfoItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val grouped = LocalOneuiXGroupedPreference.current
    val materialColors = ListItemDefaults.colors().copy(
        containerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
    )
    when (LocalUiMode.current) {
        UiMode.Material -> SegmentedListItem(
            modifier = if (grouped) modifier else modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            colors = if (grouped) materialColors else ListItemDefaults.colors(),
            headlineContent = { MaterialText(title) },
            supportingContent = summary?.let { { MaterialText(it) } },
            leadingContent = icon?.let { imageVector ->
                { Icon(imageVector, contentDescription = title) }
            },
            trailingContent = trailingContent,
        )

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
fun OneuiXSectionLabel(
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
fun OneuiXSupportingText(
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
