package com.oneuixp.app.ui.screen.status

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarClockBlockParser
import io.github.soclear.oneuix.hook.util.StatusBarClockFormatter
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import com.oneuixp.mod.ui.component.OneuiXPActionButton
import com.oneuixp.mod.ui.component.OneuiXPInfoItem
import com.oneuixp.mod.ui.component.OneuiXPInlineTextField
import com.oneuixp.mod.ui.component.OneuiXPSlider
import com.oneuixp.mod.ui.component.OneuiXPSectionLabel
import com.oneuixp.mod.ui.component.OneuiXPSupportingText
import io.github.soclear.oneuix.ui.component.SelectItem
import io.github.soclear.oneuix.ui.component.SwitchItem
import java.time.LocalDateTime
import kotlin.math.roundToInt

private val STATUS_CATEGORY_TIME_DOUBLE_LINE_CLOCK_SIZE_VALUES = listOf(
    "small",
    "compact",
    "standard",
    "large",
    "extra_large",
)

private fun statusCategoryTimeLegacyDoubleLineClockSize(scale: Float): String = when {
    scale < 0.925f -> "small"
    scale < 0.975f -> "compact"
    scale < 1.025f -> "standard"
    scale < 1.075f -> "large"
    else -> "extra_large"
}

private fun clockRichStyleFor(
    uiState: Preference.SystemUI.StatusBar,
    block: StatusBarClockBlockParser.Block,
): Preference.SystemUI.StatusBarClockBlockStyle {
    val index = block.styleIndex ?: 0
    return uiState.statusBarClockBlockStyles.firstOrNull {
        it.blockIndex == index && it.source == block.source
    } ?: Preference.SystemUI.StatusBarClockBlockStyle(
        blockIndex = index,
        source = block.source,
    )
}

private fun clockRichPreview(
    block: StatusBarClockBlockParser.Block,
    now: LocalDateTime,
): String = block.literalText ?: runCatching {
    StatusBarClockFormatter.format(block.source, now)
}.getOrElse { block.source }

private fun clockRichArgbText(color: Int): String =
    "#%08X".format(color.toLong() and 0xFFFFFFFFL)

private fun parseClockRichArgb(value: String): Int? {
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
fun StatusTimeCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDoubleLine =
        uiState.setStatusBarClockFormat && uiState.statusBarClockFormat.contains('\n')
    val isFold7 = Build.MODEL.startsWith("SM-F966", ignoreCase = true)

    var formatExpanded by rememberSaveable { mutableStateOf(false) }
    var tempDataTimeFormat by remember(uiState.statusBarClockFormat) {
        mutableStateOf(
            TextFieldValue(
                text = uiState.statusBarClockFormat,
                selection = TextRange(uiState.statusBarClockFormat.length),
            )
        )
    }
    var formatLabel by remember { mutableStateOf("") }
    var showFormatHelp by rememberSaveable { mutableStateOf(false) }

    var timeScale by remember(uiState.fold7DoubleLineClockTimeScale) {
        mutableFloatStateOf(uiState.fold7DoubleLineClockTimeScale)
    }
    var dateScale by remember(uiState.fold7DoubleLineClockDateScale) {
        mutableFloatStateOf(uiState.fold7DoubleLineClockDateScale)
    }

    var lineGapDp by remember(uiState.doubleLineClockGapDp) {
        mutableFloatStateOf(uiState.doubleLineClockGapDp.coerceIn(-3f, 3f))
    }
    var lineGapEnabled by remember(uiState.modifyDoubleLineClockGap) {
        mutableStateOf(uiState.modifyDoubleLineClockGap)
    }

    var singleLineScale by remember(uiState.statusBarClockTextScale) {
        mutableFloatStateOf(uiState.statusBarClockTextScale)
    }
    var singleLineScaleExpanded by rememberSaveable { mutableStateOf(false) }
    var clockOffsetX by remember(uiState.statusBarClockOffsetXDp) {
        mutableFloatStateOf(uiState.statusBarClockOffsetXDp.coerceIn(-20f, 20f))
    }
    var clockOffsetY by remember(uiState.statusBarClockOffsetYDp) {
        mutableFloatStateOf(uiState.statusBarClockOffsetYDp.coerceIn(-20f, 20f))
    }
    var clockLeftMargin by remember(uiState.statusBarClockLeftMarginDp) {
        mutableFloatStateOf(uiState.statusBarClockLeftMarginDp.coerceIn(0f, 40f))
    }
    var clockRightMargin by remember(uiState.statusBarClockRightMarginDp) {
        mutableFloatStateOf(uiState.statusBarClockRightMarginDp.coerceIn(0f, 40f))
    }
    var clockFixedContentWidth by remember(uiState.statusBarClockFixedContentWidthDp) {
        mutableFloatStateOf(uiState.statusBarClockFixedContentWidthDp.coerceIn(0f, 200f))
    }
    var clockAlignment by remember(uiState.statusBarClockAlignment) {
        mutableStateOf(uiState.statusBarClockAlignment)
    }
    var selectedRichBlockPosition by rememberSaveable { mutableStateOf(0) }
    var expandedRichBlock by rememberSaveable { mutableStateOf(-1) }

    StatusThemeGroup(
        modifier = modifier,
        items = buildList {
            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuixp_setStatusBarClockFormat_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.setStatusBarClockFormat) {
                        uiState.statusBarClockFormat
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.nest_clock_farsight_digital),
                    clickable = true,
                    onClick = { formatExpanded = !formatExpanded },
                    checked = uiState.setStatusBarClockFormat,
                    onCheckedChange = {
                        if (it && uiState.statusBarClockFormat == "HH:mm") {
                            formatExpanded = true
                        } else if (!it) {
                            formatExpanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.SetStatusBarClockFormat(it))
                    },
                )
            }
            if (formatExpanded && uiState.setStatusBarClockFormat) {
                add {
                    StatusThemeChildItem {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                OneuiXPInlineTextField(
                                    value = tempDataTimeFormat,
                                    onValueChange = {
                                        tempDataTimeFormat = it
                                        formatLabel = runCatching {
                                            StatusBarClockFormatter.format(
                                                it.text,
                                                LocalDateTime.now(),
                                            )
                                        }.getOrElse { "error" }
                                    },
                                    modifier = Modifier.weight(1f),
                                    labelText = formatLabel.takeIf { it.isNotBlank() },
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OneuiXPActionButton(
                                    text = stringResource(id = R.string.oneuixp_confirm),
                                    primary = true,
                                    onClick = {
                                        var right = true
                                        formatLabel = try {
                                            StatusBarClockFormatter.format(
                                                tempDataTimeFormat.text,
                                                LocalDateTime.now(),
                                            )
                                        } catch (_: Throwable) {
                                            right = false
                                            "error"
                                        }
                                        if (right) {
                                            onEvent(
                                                SystemUIEvent.StatusBar.StatusBarClockFormat(
                                                    tempDataTimeFormat.text
                                                )
                                            )
                                        }
                                    },
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OneuiXPActionButton(
                                    text = stringResource(
                                        id = R.string.oneuixp_clockFormatHelp_show
                                    ),
                                    onClick = { showFormatHelp = true },
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 6.dp, bottom = 4.dp),
                                horizontalArrangement =
                                    androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                            ) {
                                STATUS_CLOCK_FORMAT_PRESETS.forEach { preset ->
                                    OneuiXPActionButton(
                                        text = stringResource(id = preset.labelRes),
                                        onClick = {
                                            val selection = tempDataTimeFormat.selection
                                            val start = selection.min.coerceIn(
                                                0,
                                                tempDataTimeFormat.text.length,
                                            )
                                            val end = selection.max.coerceIn(
                                                start,
                                                tempDataTimeFormat.text.length,
                                            )
                                            val updatedText =
                                                tempDataTimeFormat.text.replaceRange(
                                                    start,
                                                    end,
                                                    preset.token,
                                                )
                                            val cursor = start + preset.token.length
                                            tempDataTimeFormat = TextFieldValue(
                                                text = updatedText,
                                                selection = TextRange(cursor),
                                            )
                                            formatLabel = runCatching {
                                                StatusBarClockFormatter.format(
                                                    updatedText,
                                                    LocalDateTime.now(),
                                                )
                                            }.getOrElse { "error" }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.setStatusBarClockFormat) {
                add {
                    SwitchItem(
                        title = stringResource(id = R.string.oneuixp_clockRichStyle_title),
                        summary = stringResource(id = R.string.oneuixp_clockRichStyle_summary),
                        icon = ImageVector.vectorResource(id = R.drawable.format_size),
                        checked = uiState.enableStatusBarClockRichStyle,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.StatusBar.SetStatusBarClockRichStyle(it))
                        },
                    )
                }

                if (uiState.enableStatusBarClockRichStyle) {
                    val now = LocalDateTime.now()
                    val blocks = StatusBarClockBlockParser.parse(uiState.statusBarClockFormat)
                        .filter { it.isStyleable }

                    if (blocks.isNotEmpty()) {
                        val safePosition =
                            selectedRichBlockPosition.coerceIn(0, blocks.lastIndex)
                        val selectedBlock = blocks[safePosition]
                        val selectedBlockIndex = selectedBlock.styleIndex ?: 0
                        val selectedStyle = clockRichStyleFor(uiState, selectedBlock)
                        val selectedPreview =
                            clockRichPreview(selectedBlock, now).ifEmpty { " " }

                        add {
                            SelectItem(
                                title = stringResource(
                                    id = R.string.oneuixp_clockRichSelectBlock_title
                                ),
                                entries = blocks.mapIndexed { index, block ->
                                    val preview =
                                        clockRichPreview(block, now).ifEmpty { " " }
                                    stringResource(
                                        id = R.string.oneuixp_clockRichBlock_title,
                                        (block.styleIndex ?: index) + 1,
                                        block.source.replace("\n", "\\n"),
                                    ) + " · " + preview
                                },
                                selectedIndex = safePosition,
                                onSelectedIndexChange = { position ->
                                    selectedRichBlockPosition = position
                                    expandedRichBlock = -1
                                },
                            )
                        }

                        add {
                            SwitchItem(
                                title = stringResource(
                                    id = R.string.oneuixp_clockRichBlock_title,
                                    selectedBlockIndex + 1,
                                    selectedBlock.source.replace("\n", "\\n"),
                                ),
                                summary = stringResource(
                                    id = R.string.oneuixp_clockRichBlock_summary,
                                    selectedPreview,
                                ),
                                clickable = true,
                                showChildIndicator = true,
                                onClick = {
                                    expandedRichBlock =
                                        if (expandedRichBlock == selectedBlockIndex) {
                                            -1
                                        } else {
                                            selectedBlockIndex
                                        }
                                },
                                checked = selectedStyle.enabled,
                                onCheckedChange = { enabled ->
                                    onEvent(
                                        SystemUIEvent.StatusBar.UpdateStatusBarClockBlockStyle(
                                            selectedStyle.copy(enabled = enabled)
                                        )
                                    )
                                },
                            )
                        }

                        if (
                            expandedRichBlock == selectedBlockIndex &&
                            selectedStyle.enabled
                        ) {
                            add {
                                StatusThemeChildItem {
                                    StatusClockRichBlockEditor(
                                        style = selectedStyle,
                                        onStylePreview = { updated ->
                                            onEvent(
                                                SystemUIEvent.StatusBarPreview(
                                                    SystemUIEvent.StatusBar.UpdateStatusBarClockBlockStyle(
                                                        updated
                                                    )
                                                )
                                            )
                                        },
                                        onStyleChange = { updated ->
                                            onEvent(
                                                SystemUIEvent.StatusBar.UpdateStatusBarClockBlockStyle(
                                                    updated
                                                )
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.padding),
                    title = stringResource(
                        id = R.string.oneuixp_statusBarClockPosition_title
                    ),
                    summary = if (uiState.modifyStatusBarClockPosition) {
                        stringResource(
                            id = R.string.oneuixp_statusBarClockPosition_summary,
                            clockOffsetX,
                            clockOffsetY,
                        )
                    } else {
                        null
                    },
                    showChildIndicator = true,
                    checked = uiState.modifyStatusBarClockPosition,
                    onCheckedChange = {
                        onEvent(
                            SystemUIEvent.StatusBar.ModifyStatusBarClockPosition(it)
                        )
                    },
                )
            }
            if (uiState.modifyStatusBarClockPosition) {
                add {
                    StatusThemeChildItem {
                        // StatusThemeChildItem uses a Box; stack multiple controls explicitly
                        // so the X and Y labels/sliders cannot overlap each other.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            OneuiXPSectionLabel(
                                text = stringResource(
                                    id = R.string.oneuixp_statusBarClockOffsetX_title
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            OneuiXPSupportingText(
                                text = stringResource(
                                    id = R.string.oneuixp_statusBarClockOffset_summary,
                                    clockOffsetX,
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            OneuiXPSlider(
                                value = clockOffsetX,
                                stepSize = 1f,
                                onValueChange = { raw ->
                                    val value = raw.roundToInt().coerceIn(-20, 20).toFloat()
                                    clockOffsetX = value
                                    onEvent(
                                        SystemUIEvent.StatusBarPreview(
                                            SystemUIEvent.StatusBar.StatusBarClockOffsetXDp(value)
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                valueRange = -20f..20f,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.StatusBarClockOffsetXDp(clockOffsetX)
                                    )
                                },
                            )
                            OneuiXPSectionLabel(
                                text = stringResource(
                                    id = R.string.oneuixp_statusBarClockOffsetY_title
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            OneuiXPSupportingText(
                                text = stringResource(
                                    id = R.string.oneuixp_statusBarClockOffset_summary,
                                    clockOffsetY,
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            OneuiXPSlider(
                                value = clockOffsetY,
                                stepSize = 1f,
                                onValueChange = { raw ->
                                    val value = raw.roundToInt().coerceIn(-20, 20).toFloat()
                                    clockOffsetY = value
                                    onEvent(
                                        SystemUIEvent.StatusBarPreview(
                                            SystemUIEvent.StatusBar.StatusBarClockOffsetYDp(value)
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                valueRange = -20f..20f,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.StatusBarClockOffsetYDp(clockOffsetY)
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (uiState.modifyStatusBarClockPosition) {
                add {
                    StatusThemeChildItem {
                        SelectItem(
                            icon = ImageVector.vectorResource(id = R.drawable.format_size),
                            title = stringResource(
                                id = R.string.oneuixp_statusBarClockAlignment_title
                            ),
                            entries = listOf(
                                stringResource(id = R.string.oneuixp_statusBarClockAlignment_native),
                                stringResource(id = R.string.oneuixp_statusBarClockAlignment_start),
                                stringResource(id = R.string.oneuixp_statusBarClockAlignment_center),
                                stringResource(id = R.string.oneuixp_statusBarClockAlignment_end),
                            ),
                            selectedIndex = listOf("native", "start", "center", "end")
                                .indexOf(clockAlignment)
                                .coerceAtLeast(0),
                            onSelectedIndexChange = { selected ->
                                val value = listOf("native", "start", "center", "end")
                                    .getOrElse(selected) { "native" }
                                // Update the selection and SystemUI preview immediately;
                                // persistence is kept as the normal follow-up event.
                                clockAlignment = value
                                onEvent(
                                    SystemUIEvent.StatusBarPreview(
                                        SystemUIEvent.StatusBar.StatusBarClockAlignment(value)
                                    )
                                )
                                onEvent(
                                    SystemUIEvent.StatusBar.StatusBarClockAlignment(value)
                                )
                            },
                        )
                    }
                }
                add {
                    StatusThemeChildItem {
                        // Keep the title/value immediately above its own slider.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            OneuiXPSectionLabel(
                                text = stringResource(
                                    id = R.string.oneuixp_statusBarClockFixedContentWidth_title
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            OneuiXPSupportingText(
                                text = stringResource(
                                    id = R.string.oneuixp_statusBarClockFixedContentWidth_summary,
                                    clockFixedContentWidth,
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            OneuiXPSlider(
                                value = clockFixedContentWidth,
                                stepSize = 1f,
                                onValueChange = { raw ->
                                    val value = raw.roundToInt().coerceIn(0, 200).toFloat()
                                    clockFixedContentWidth = value
                                    onEvent(
                                        SystemUIEvent.StatusBarPreview(
                                            SystemUIEvent.StatusBar.StatusBarClockFixedContentWidthDp(value)
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                valueRange = 0f..200f,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.StatusBarClockFixedContentWidthDp(
                                            clockFixedContentWidth
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
                add {
                    StatusThemeChildItem {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            OneuiXPSectionLabel(
                                text = stringResource(
                                    id = R.string.oneuixp_statusBarClockLeftMargin_title
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            OneuiXPSupportingText(
                                text = stringResource(
                                    id = R.string.oneuixp_statusBarClockMargin_summary,
                                    clockLeftMargin,
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            OneuiXPSlider(
                                value = clockLeftMargin,
                                stepSize = 1f,
                                onValueChange = { raw ->
                                    val value = raw.roundToInt().coerceIn(0, 40).toFloat()
                                    clockLeftMargin = value
                                    onEvent(
                                        SystemUIEvent.StatusBarPreview(
                                            SystemUIEvent.StatusBar.StatusBarClockLeftMarginDp(value)
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                valueRange = 0f..40f,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.StatusBarClockLeftMarginDp(
                                            clockLeftMargin
                                        )
                                    )
                                },
                            )
                            OneuiXPSectionLabel(
                                text = stringResource(
                                    id = R.string.oneuixp_statusBarClockRightMargin_title
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            OneuiXPSupportingText(
                                text = stringResource(
                                    id = R.string.oneuixp_statusBarClockMargin_summary,
                                    clockRightMargin,
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            OneuiXPSlider(
                                value = clockRightMargin,
                                stepSize = 1f,
                                onValueChange = { raw ->
                                    val value = raw.roundToInt().coerceIn(0, 40).toFloat()
                                    clockRightMargin = value
                                    onEvent(
                                        SystemUIEvent.StatusBarPreview(
                                            SystemUIEvent.StatusBar.StatusBarClockRightMarginDp(value)
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                valueRange = 0f..40f,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.StatusBarClockRightMarginDp(
                                            clockRightMargin
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (isDoubleLine) {
                if (isFold7) {
                    add {
                        SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.format_size),
                            title = stringResource(
                                id = R.string.oneuixp_fold7CustomDoubleLineClockScale_title
                            ),
                            showChildIndicator = true,
                            checked = uiState.useFold7CustomDoubleLineClockScale,
                            onCheckedChange = {
                                onEvent(
                                    SystemUIEvent.StatusBar.SetFold7CustomDoubleLineClockScale(it)
                                )
                            },
                        )
                    }
                    if (uiState.useFold7CustomDoubleLineClockScale) {
                        add {
                            StatusThemeChildItem {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    OneuiXPInfoItem(
                                        title = stringResource(
                                            id = R.string.oneuixp_fold7DoubleLineClockTimeScale_title
                                        ),
                                        summary = stringResource(
                                            id = R.string.oneuixp_fold7DoubleLineClockTimeScale_summary,
                                            timeScale,
                                        ),
                                    )
                                    OneuiXPSlider(
                                        value = timeScale * 100f,
                                        stepSize = 1f,
                                        onValueChange = { raw ->
                                            val value = raw.roundToInt().coerceIn(50, 110) / 100f
                                            timeScale = value
                                            onEvent(
                                                SystemUIEvent.StatusBarPreview(
                                                    SystemUIEvent.StatusBar.Fold7DoubleLineClockTimeScale(
                                                        value
                                                    )
                                                )
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        valueRange = 50f..110f,
                                        onValueChangeFinished = {
                                            onEvent(
                                                SystemUIEvent.StatusBar.Fold7DoubleLineClockTimeScale(
                                                    timeScale
                                                )
                                            )
                                        },
                                    )
                                    OneuiXPInfoItem(
                                        title = stringResource(
                                            id = R.string.oneuixp_fold7DoubleLineClockDateScale_title
                                        ),
                                        summary = stringResource(
                                            id = R.string.oneuixp_fold7DoubleLineClockDateScale_summary,
                                            dateScale,
                                        ),
                                    )
                                    OneuiXPSlider(
                                        value = dateScale * 100f,
                                        stepSize = 1f,
                                        onValueChange = { raw ->
                                            val value = raw.roundToInt().coerceIn(50, 110) / 100f
                                            dateScale = value
                                            onEvent(
                                                SystemUIEvent.StatusBarPreview(
                                                    SystemUIEvent.StatusBar.Fold7DoubleLineClockDateScale(
                                                        value
                                                    )
                                                )
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        valueRange = 50f..110f,
                                        onValueChangeFinished = {
                                            onEvent(
                                                SystemUIEvent.StatusBar.Fold7DoubleLineClockDateScale(
                                                    dateScale
                                                )
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    add {
                        val sizeLabels = listOf(
                            stringResource(id = R.string.oneuixp_statusBarDoubleLineClockSize_small),
                            stringResource(id = R.string.oneuixp_statusBarDoubleLineClockSize_compact),
                            stringResource(id = R.string.oneuixp_statusBarDoubleLineClockSize_standard),
                            stringResource(id = R.string.oneuixp_statusBarDoubleLineClockSize_large),
                            stringResource(id = R.string.oneuixp_statusBarDoubleLineClockSize_extraLarge),
                        )
                        val selectedSize = uiState.statusBarDoubleLineClockSize.ifBlank {
                            statusCategoryTimeLegacyDoubleLineClockSize(uiState.statusBarClockTextScale)
                        }
                        val selectedIndex =
                            STATUS_CATEGORY_TIME_DOUBLE_LINE_CLOCK_SIZE_VALUES.indexOf(
                                selectedSize
                            ).coerceAtLeast(0)
                        SelectItem(
                            icon = ImageVector.vectorResource(id = R.drawable.format_size),
                            title = stringResource(
                                id = R.string.oneuixp_statusBarDoubleLineClockSize_title
                            ),
                            entries = sizeLabels,
                            selectedIndex = selectedIndex,
                            onSelectedIndexChange = {
                                onEvent(
                                    SystemUIEvent.StatusBar.StatusBarDoubleLineClockSize(
                                        STATUS_CATEGORY_TIME_DOUBLE_LINE_CLOCK_SIZE_VALUES[it]
                                    )
                                )
                            },
                        )
                    }
                }

                add {
                    SwitchItem(
                        title = stringResource(id = R.string.oneuixp_statusBarDoubleLineClockGap_title),
                        summary = if (lineGapEnabled) {
                            lineGapDp.roundToInt().toString() + " dp"
                        } else null,
                        icon = ImageVector.vectorResource(id = R.drawable.format_letter_spacing),
                        checked = lineGapEnabled,
                        onCheckedChange = { enabled ->
                            lineGapEnabled = enabled
                            onEvent(SystemUIEvent.StatusBar.ModifyDoubleLineClockGap(enabled))
                        },
                    )
                }
                if (lineGapEnabled) {
                    add {
                        StatusThemeChildItem {
                            OneuiXPSlider(
                                value = lineGapDp,
                                stepSize = 1f,
                                onValueChange = { raw ->
                                    val value = raw.roundToInt().coerceIn(-3, 3).toFloat()
                                    lineGapDp = value
                                    onEvent(
                                        SystemUIEvent.StatusBarPreview(
                                            SystemUIEvent.StatusBar.DoubleLineClockGapDp(
                                                value
                                            )
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                valueRange = -3f..3f,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.DoubleLineClockGapDp(
                                            lineGapDp
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            } else {
                add {
                    SwitchItem(
                        title = stringResource(id = R.string.oneuixp_setStatusBarClockTextScale_title),
                        modifier = Modifier.animateContentSize(),
                        summary = if (uiState.setStatusBarClockTextScale) {
                            "${(singleLineScale * 100f).roundToInt()}%"
                        } else null,
                        icon = ImageVector.vectorResource(id = R.drawable.format_size),
                        clickable = true,
                        onClick = { singleLineScaleExpanded = !singleLineScaleExpanded },
                        checked = uiState.setStatusBarClockTextScale,
                        onCheckedChange = {
                            if (it && singleLineScale == 1f) {
                                singleLineScaleExpanded = true
                            } else if (!it) {
                                singleLineScaleExpanded = false
                            }
                            onEvent(SystemUIEvent.StatusBar.SetStatusBarClockTextScale(it))
                        },
                    )
                }
                if (singleLineScaleExpanded && uiState.setStatusBarClockTextScale) {
                    add {
                        StatusThemeChildItem {
                            OneuiXPSlider(
                                value = singleLineScale * 100f,
                                stepSize = 1f,
                                onValueChange = { raw ->
                                    val value = raw.roundToInt().coerceIn(50, 250) / 100f
                                    singleLineScale = value
                                    onEvent(
                                        SystemUIEvent.StatusBarPreview(
                                            SystemUIEvent.StatusBar.StatusBarClockTextScale(
                                                value
                                            )
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                valueRange = 50f..250f,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.StatusBarClockTextScale(
                                            singleLineScale
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            }

            add {
                SwitchItem(
                    title = stringResource(
                        id = R.string.oneuixp_updateStatusBarClockEverySecond_title
                    ),
                    summary = stringResource(
                        id = R.string.oneuixp_updateStatusBarClockEverySecond_summary
                    ),
                    icon = ImageVector.vectorResource(
                        id = R.drawable.nest_clock_farsight_digital
                    ),
                    checked = uiState.updateStatusBarClockEverySecond,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.UpdateStatusBarClockEverySecond(it))
                    },
                )
            }
        },
    )

    if (showFormatHelp) {
        StatusClockFormatHelpDialog(
            onDismiss = { showFormatHelp = false },
        )
    }

    if (isDoubleLine && !isFold7) {
        OneuiXPSupportingText(
            text = stringResource(id = R.string.oneuixp_statusBarDoubleLineClockSize_summary),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
