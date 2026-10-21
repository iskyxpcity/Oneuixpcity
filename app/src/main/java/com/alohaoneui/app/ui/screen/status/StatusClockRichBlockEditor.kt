package com.alohaoneui.app.ui.screen.status

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.R
import com.alohaoneui.common.Preference
import com.alohaoneui.mod.ui.component.AlohaOneUIInfoItem
import com.alohaoneui.mod.ui.component.AlohaOneUIInlineTextField
import com.alohaoneui.mod.ui.component.AlohaOneUISlider
import com.alohaoneui.mod.ui.component.SelectItem
import com.alohaoneui.mod.ui.component.SwitchItem
import kotlin.math.roundToInt

private fun richArgbText(color: Int): String =
    "#%08X".format(color.toLong() and 0xFFFFFFFFL)

private fun parseRichArgb(value: String): Int? {
    val raw = value.trim().removePrefix("#")
    if (raw.length != 6 && raw.length != 8) return null
    val parsed = raw.toLongOrNull(16) ?: return null
    return if (raw.length == 6) {
        (0xFF000000L or parsed).toInt()
    } else {
        parsed.toInt()
    }
}

@Composable
fun StatusClockRichBlockEditor(
    style: Preference.SystemUI.StatusBarClockBlockStyle,
    onStyleChange: (Preference.SystemUI.StatusBarClockBlockStyle) -> Unit,
    onStylePreview: (Preference.SystemUI.StatusBarClockBlockStyle) -> Unit = onStyleChange,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SwitchItem(
            title = stringResource(id = R.string.alohaoneui_clockRichTextSize_title),
            summary = if (style.overrideTextSize) {
                stringResource(
                    id = R.string.alohaoneui_clockRichTextSize_summary,
                    style.textSizeSp,
                )
            } else {
                null
            },
            checked = style.overrideTextSize,
            onCheckedChange = { enabled ->
                onStyleChange(style.copy(overrideTextSize = enabled))
            },
        )

        if (style.overrideTextSize) {
            var textSize by remember(style.textSizeSp) {
                mutableFloatStateOf(style.textSizeSp)
            }
            AlohaOneUIInfoItem(
                title = stringResource(id = R.string.alohaoneui_clockRichTextSize_title),
                summary = stringResource(
                    id = R.string.alohaoneui_clockRichTextSize_summary,
                    textSize,
                ),
            )
            AlohaOneUISlider( 
                stepSize = 1f,
                value = textSize,
                onValueChange = { raw ->
                    val value = (raw * 2f).roundToInt() / 2f
                    textSize = value
                    onStylePreview(style.copy(textSizeSp = value))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                valueRange = 6f..24f,
                onValueChangeFinished = {
                    onStyleChange(style.copy(textSizeSp = textSize))
                },
            )
        }

        var horizontalOffset by remember(style.horizontalOffsetDp) {
            mutableFloatStateOf(style.horizontalOffsetDp.coerceIn(-20f, 20f))
        }
        AlohaOneUIInfoItem(
            title = stringResource(id = R.string.alohaoneui_clockRichHorizontalOffset_title),
            summary = stringResource(
                id = R.string.alohaoneui_clockRichOffset_summary,
                horizontalOffset,
            ),
        )
        AlohaOneUISlider(
            value = horizontalOffset,
            stepSize = 1f,
            onValueChange = { raw ->
                val value = raw.roundToInt().coerceIn(-20, 20).toFloat()
                horizontalOffset = value
                onStylePreview(style.copy(horizontalOffsetDp = value))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            valueRange = -20f..20f,
            onValueChangeFinished = {
                onStyleChange(style.copy(horizontalOffsetDp = horizontalOffset))
            },
        )

        var verticalOffset by remember(style.verticalOffsetDp) {
            mutableFloatStateOf(style.verticalOffsetDp.coerceIn(-20f, 20f))
        }
        AlohaOneUIInfoItem(
            title = stringResource(id = R.string.alohaoneui_clockRichVerticalOffset_title),
            summary = stringResource(
                id = R.string.alohaoneui_clockRichOffset_summary,
                verticalOffset,
            ),
        )
        AlohaOneUISlider(
            value = verticalOffset,
            stepSize = 1f,
            onValueChange = { raw ->
                val value = raw.roundToInt().coerceIn(-20, 20).toFloat()
                verticalOffset = value
                onStylePreview(style.copy(verticalOffsetDp = value))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            valueRange = -20f..20f,
            onValueChangeFinished = {
                onStyleChange(style.copy(verticalOffsetDp = verticalOffset))
            },
        )

        val weights = listOf(0, 300, 400, 500, 600, 700, 800, 900)
        val weightLabels = listOf(
            "跟随系统",
            "300",
            "400",
            "500",
            "600",
            "700",
            "800",
            "900",
        )
        val weightIndex = if (style.overrideFontWeight) {
            weights.indexOf(style.fontWeight).coerceAtLeast(2)
        } else {
            0
        }
        SelectItem(
            title = stringResource(id = R.string.alohaoneui_clockRichFontWeight_title),
            entries = weightLabels,
            selectedIndex = weightIndex,
            onSelectedIndexChange = { selected ->
                if (selected == 0) {
                    onStyleChange(style.copy(overrideFontWeight = false))
                } else {
                    onStyleChange(
                        style.copy(
                            overrideFontWeight = true,
                            fontWeight = weights[selected],
                        )
                    )
                }
            },
        )

        SwitchItem(
            title = stringResource(id = R.string.alohaoneui_clockRichTextColor_title),
            summary = if (style.overrideTextColor) {
                richArgbText(style.textColorArgb)
            } else {
                null
            },
            checked = style.overrideTextColor,
            onCheckedChange = { enabled ->
                onStyleChange(style.copy(overrideTextColor = enabled))
            },
        )

        if (style.overrideTextColor) {
            StatusClockColorPicker(
                colorArgb = style.textColorArgb,
                onColorPreview = { selected ->
                    onStylePreview(style.copy(textColorArgb = selected))
                },
                onColorChange = { selected ->
                    onStyleChange(style.copy(textColorArgb = selected))
                },
            )
        }

        SwitchItem(
            title = stringResource(id = R.string.alohaoneui_clockRichBackground_title),
            summary = if (style.backgroundEnabled) {
                richArgbText(style.backgroundColorArgb)
            } else {
                null
            },
            checked = style.backgroundEnabled,
            onCheckedChange = { enabled ->
                onStyleChange(style.copy(backgroundEnabled = enabled))
            },
        )

        if (style.backgroundEnabled) {
            StatusClockColorPicker(
                colorArgb = style.backgroundColorArgb,
                onColorPreview = { selected ->
                    onStylePreview(style.copy(backgroundColorArgb = selected))
                },
                onColorChange = { selected ->
                    onStyleChange(style.copy(backgroundColorArgb = selected))
                },
            )

            var alpha by remember(style.backgroundAlpha) {
                mutableFloatStateOf(style.backgroundAlpha)
            }
            AlohaOneUIInfoItem(
                title = stringResource(id = R.string.alohaoneui_clockRichBackgroundAlpha_title),
                summary = stringResource(
                    id = R.string.alohaoneui_clockRichBackgroundAlpha_summary,
                    (alpha * 100f).roundToInt(),
                ),
            )
            AlohaOneUISlider(
                value = alpha * 100f,
                stepSize = 1f,
                onValueChange = { raw ->
                    val value = raw.roundToInt().coerceIn(0, 100) / 100f
                    alpha = value
                    onStylePreview(style.copy(backgroundAlpha = value))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                valueRange = 0f..100f,
                onValueChangeFinished = {
                    onStyleChange(style.copy(backgroundAlpha = alpha))
                },
            )

            SwitchItem(
                title = stringResource(id = R.string.alohaoneui_clockRichBorder_title),
                summary = if (style.backgroundBorderEnabled) {
                    richArgbText(style.backgroundBorderColorArgb)
                } else {
                    null
                },
                checked = style.backgroundBorderEnabled,
                onCheckedChange = { enabled ->
                    onStyleChange(style.copy(backgroundBorderEnabled = enabled))
                },
            )

            if (style.backgroundBorderEnabled) {
                StatusClockColorPicker(
                    colorArgb = style.backgroundBorderColorArgb,
                    onColorPreview = { selected ->
                        onStylePreview(style.copy(backgroundBorderColorArgb = selected))
                    },
                    onColorChange = { selected ->
                        onStyleChange(style.copy(backgroundBorderColorArgb = selected))
                    },
                )

                var borderWidth by remember(style.backgroundBorderWidthDp) {
                    mutableFloatStateOf(style.backgroundBorderWidthDp)
                }
                AlohaOneUIInfoItem(
                    title = stringResource(id = R.string.alohaoneui_clockRichBorderWidth_title),
                    summary = stringResource(
                        id = R.string.alohaoneui_clockRichBorderWidth_summary,
                        borderWidth,
                    ),
                )
                AlohaOneUISlider(
                    value = borderWidth,
                    onValueChange = { raw ->
                        val value = (raw * 2f).roundToInt() / 2f
                        borderWidth = value
                        onStylePreview(style.copy(backgroundBorderWidthDp = value))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    valueRange = 0.5f..4f,
                    onValueChangeFinished = {
                        onStyleChange(style.copy(backgroundBorderWidthDp = borderWidth))
                    },
                )
            }

            var radius by remember(style.cornerRadiusDp) {
                mutableFloatStateOf(style.cornerRadiusDp)
            }
            AlohaOneUIInfoItem(
                title = stringResource(id = R.string.alohaoneui_clockRichCornerRadius_title),
                summary = stringResource(
                    id = R.string.alohaoneui_clockRichCornerRadius_summary,
                    radius,
                ),
            )
            AlohaOneUISlider( 
                stepSize = 1f,
                value = radius,
                onValueChange = { raw ->
                    val value = (raw * 2f).roundToInt() / 2f
                    radius = value
                    onStylePreview(style.copy(cornerRadiusDp = value))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                valueRange = 0f..12f,
                onValueChangeFinished = {
                    onStyleChange(style.copy(cornerRadiusDp = radius))
                },
            )

            var horizontalPadding by remember(style.horizontalPaddingDp) {
                mutableFloatStateOf(style.horizontalPaddingDp)
            }
            AlohaOneUIInfoItem(
                title = stringResource(id = R.string.alohaoneui_clockRichHorizontalPadding_title),
                summary = stringResource(
                    id = R.string.alohaoneui_clockRichPadding_summary,
                    horizontalPadding,
                ),
            )
            AlohaOneUISlider( 
                stepSize = 1f,
                value = horizontalPadding,
                onValueChange = { raw ->
                    val value = (raw * 2f).roundToInt() / 2f
                    horizontalPadding = value
                    onStylePreview(style.copy(horizontalPaddingDp = value))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                valueRange = 0f..8f,
                onValueChangeFinished = {
                    onStyleChange(style.copy(horizontalPaddingDp = horizontalPadding))
                },
            )

            var verticalPadding by remember(style.verticalPaddingDp) {
                mutableFloatStateOf(style.verticalPaddingDp)
            }
            AlohaOneUIInfoItem(
                title = stringResource(id = R.string.alohaoneui_clockRichVerticalPadding_title),
                summary = stringResource(
                    id = R.string.alohaoneui_clockRichPadding_summary,
                    verticalPadding,
                ),
            )
            AlohaOneUISlider( 
                stepSize = 1f,
                value = verticalPadding,
                onValueChange = { raw ->
                    val value = (raw * 2f).roundToInt() / 2f
                    verticalPadding = value
                    onStylePreview(style.copy(verticalPaddingDp = value))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                valueRange = 0f..4f,
                onValueChangeFinished = {
                    onStyleChange(style.copy(verticalPaddingDp = verticalPadding))
                },
            )
        }
    }
}
