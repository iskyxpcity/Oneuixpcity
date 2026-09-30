package com.oneuixp.app.ui.screen.status

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarClockBlockParser
import io.github.soclear.oneuix.hook.util.StatusBarClockFormatter
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import io.github.soclear.oneuix.ui.component.OneuiXActionButton
import io.github.soclear.oneuix.ui.component.OneuiXInfoItem
import io.github.soclear.oneuix.ui.component.OneuiXInlineTextField
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.OneuiXSupportingText
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
        mutableStateOf(uiState.statusBarClockFormat)
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
        mutableFloatStateOf(uiState.doubleLineClockGapDp)
    }
    var gapExpanded by rememberSaveable { mutableStateOf(false) }

    var singleLineScale by remember(uiState.statusBarClockTextScale) {
        mutableFloatStateOf(uiState.statusBarClockTextScale)
    }
    var singleLineScaleExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedRichBlockPosition by rememberSaveable { mutableStateOf(0) }
    var expandedRichBlock by rememberSaveable { mutableStateOf(-1) }

    StatusThemeGroup(
        modifier = modifier,
        items = buildList {
            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_setStatusBarClockFormat_title),
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
                                OneuiXInlineTextField(
                                    value = tempDataTimeFormat,
                                    onValueChange = {
                                        tempDataTimeFormat = it
                                        formatLabel = runCatching {
                                            StatusBarClockFormatter.format(
                                                it,
                                                LocalDateTime.now(),
                                            )
                                        }.getOrElse { "error" }
                                    },
                                    modifier = Modifier.weight(1f),
                                    labelText = formatLabel.takeIf { it.isNotBlank() },
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OneuiXActionButton(
                                    text = stringResource(id = R.string.oneuix_confirm),
                                    primary = true,
                                    onClick = {
                                        var right = true
                                        formatLabel = try {
                                            StatusBarClockFormatter.format(
                                                tempDataTimeFormat,
                                                LocalDateTime.now(),
                                            )
                                        } catch (_: Throwable) {
                                            right = false
                                            "error"
                                        }
                                        if (right) {
                                            onEvent(
                                                SystemUIEvent.StatusBar.StatusBarClockFormat(
                                                    tempDataTimeFormat
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
                                OneuiXActionButton(
                                    text = stringResource(
                                        id = R.string.oneuix_clockFormatHelp_show
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
                                    OneuiXActionButton(
                                        text = stringResource(id = preset.labelRes),
                                        onClick = {
                                            tempDataTimeFormat += preset.token
                                            formatLabel = runCatching {
                                                StatusBarClockFormatter.format(
                                                    tempDataTimeFormat,
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
                        title = stringResource(id = R.string.oneuix_clockRichStyle_title),
                        summary = stringResource(id = R.string.oneuix_clockRichStyle_summary),
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
                                    id = R.string.oneuix_clockRichSelectBlock_title
                                ),
                                entries = blocks.mapIndexed { index, block ->
                                    val preview =
                                        clockRichPreview(block, now).ifEmpty { " " }
                                    stringResource(
                                        id = R.string.oneuix_clockRichBlock_title,
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
                                    id = R.string.oneuix_clockRichBlock_title,
                                    selectedBlockIndex + 1,
                                    selectedBlock.source.replace("\n", "\\n"),
                                ),
                                summary = stringResource(
                                    id = R.string.oneuix_clockRichBlock_summary,
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

            if (isDoubleLine) {
                if (isFold7) {
                    add {
                        SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.format_size),
                            title = stringResource(
                                id = R.string.oneuix_fold7CustomDoubleLineClockScale_title
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
                                    OneuiXInfoItem(
                                        title = stringResource(
                                            id = R.string.oneuix_fold7DoubleLineClockTimeScale_title
                                        ),
                                        summary = stringResource(
                                            id = R.string.oneuix_fold7DoubleLineClockTimeScale_summary,
                                            timeScale,
                                        ),
                                    )
                                    OneuiXSlider(
                                        value = timeScale,
                                        onValueChange = { raw ->
                                            val value = (raw * 100f).roundToInt() / 100f
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
                                        valueRange = 0.50f..1.10f,
                                        onValueChangeFinished = {
                                            onEvent(
                                                SystemUIEvent.StatusBar.Fold7DoubleLineClockTimeScale(
                                                    timeScale
                                                )
                                            )
                                        },
                                    )
                                    OneuiXInfoItem(
                                        title = stringResource(
                                            id = R.string.oneuix_fold7DoubleLineClockDateScale_title
                                        ),
                                        summary = stringResource(
                                            id = R.string.oneuix_fold7DoubleLineClockDateScale_summary,
                                            dateScale,
                                        ),
                                    )
                                    OneuiXSlider(
                                        value = dateScale,
                                        onValueChange = { raw ->
                                            val value = (raw * 100f).roundToInt() / 100f
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
                                        valueRange = 0.50f..1.10f,
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
                            stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_small),
                            stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_compact),
                            stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_standard),
                            stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_large),
                            stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_extraLarge),
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
                                id = R.string.oneuix_statusBarDoubleLineClockSize_title
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
                        title = stringResource(id = R.string.oneuix_statusBarDoubleLineClockGap_title),
                        summary = if (uiState.modifyDoubleLineClockGap) {
                            stringResource(
                                id = R.string.oneuix_statusBarDoubleLineClockGap_summary,
                                lineGapDp,
                            )
                        } else null,
                        icon = ImageVector.vectorResource(id = R.drawable.format_letter_spacing),
                        clickable = true,
                        onClick = { gapExpanded = !gapExpanded },
                        checked = uiState.modifyDoubleLineClockGap,
                        onCheckedChange = {
                            gapExpanded = it
                            onEvent(SystemUIEvent.StatusBar.ModifyDoubleLineClockGap(it))
                        },
                    )
                }
                if (gapExpanded && uiState.modifyDoubleLineClockGap) {
                    add {
                        StatusThemeChildItem {
                            OneuiXSlider(
                                value = lineGapDp,
                                onValueChange = { raw ->
                                    val value = (raw * 10f).roundToInt() / 10f
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
                                valueRange = 0f..2f,
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
                        title = stringResource(id = R.string.oneuix_setStatusBarClockTextScale_title),
                        modifier = Modifier.animateContentSize(),
                        summary = if (uiState.setStatusBarClockTextScale) {
                            "%.2fx".format(singleLineScale)
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
                            OneuiXSlider(
                                value = singleLineScale,
                                onValueChange = { raw ->
                                    val value = (raw * 100f).roundToInt() / 100f
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
                                valueRange = 0.5f..2.5f,
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
                        id = R.string.oneuix_updateStatusBarClockEverySecond_title
                    ),
                    summary = stringResource(
                        id = R.string.oneuix_updateStatusBarClockEverySecond_summary
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
        OneuiXSupportingText(
            text = stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_summary),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
