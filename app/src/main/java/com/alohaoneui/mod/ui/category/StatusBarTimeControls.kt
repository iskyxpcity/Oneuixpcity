package com.alohaoneui.mod.ui.category

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.alohaoneui.app.R
import com.alohaoneui.common.Preference
import com.alohaoneui.hook.util.StatusBarClockFormatter
import com.alohaoneui.mod.ui.component.AlohaOneUIActionButton
import com.alohaoneui.mod.ui.component.AlohaOneUIChildVisibility
import com.alohaoneui.mod.ui.component.AlohaOneUIInfoItem
import com.alohaoneui.mod.ui.component.AlohaOneUIInlineTextField
import com.alohaoneui.mod.ui.component.AlohaOneUIPPreferenceGroup
import com.alohaoneui.mod.ui.component.AlohaOneUISlider
import com.alohaoneui.mod.ui.component.AlohaOneUISupportingText
import com.alohaoneui.mod.ui.component.SelectItem
import com.alohaoneui.mod.ui.component.SwitchItem
import java.time.LocalDateTime
import kotlin.math.roundToInt

private val STATUS_TIME_DOUBLE_LINE_CLOCK_SIZE_VALUES = listOf(
    "small",
    "compact",
    "standard",
    "large",
    "extra_large",
)

private fun statusTimeLegacyDoubleLineClockSize(scale: Float): String = when {
    scale < 0.925f -> "small"
    scale < 0.975f -> "compact"
    scale < 1.025f -> "standard"
    scale < 1.075f -> "large"
    else -> "extra_large"
}

/**
 * Canonical status-bar time controls shared by the legacy SystemUI detail page
 * and the new third-tab "状态栏时间" category.
 *
 * Keep Preference, events, validation and parent/child behavior here so Miuix
 * and Material remain two visual skins over one functional tree.
 */
@Composable
fun StatusBarTimeControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AlohaOneUIPPreferenceGroup {
        Column {
            var expanded by rememberSaveable { mutableStateOf(false) }

            SwitchItem(
                title = stringResource(id = R.string.alohaoneui_setStatusBarClockFormat_title),
                modifier = Modifier.animateContentSize(),
                summary = if (uiState.setStatusBarClockFormat) {
                    uiState.statusBarClockFormat
                } else null,
                icon = ImageVector.vectorResource(id = R.drawable.nest_clock_farsight_digital),
                clickable = true,
                onClick = { expanded = !expanded },
                checked = uiState.setStatusBarClockFormat,
                onCheckedChange = {
                    if (it && uiState.statusBarClockFormat == "HH:mm") {
                        expanded = true
                    } else if (!it) {
                        expanded = false
                    }
                    onEvent(SystemUIEvent.StatusBar.SetStatusBarClockFormat(it))
                }
            )

            AlohaOneUIChildVisibility(expanded && uiState.setStatusBarClockFormat) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    var tempDataTimeFormat by remember {
                        mutableStateOf(uiState.statusBarClockFormat)
                    }
                    var label by remember { mutableStateOf("") }
                    AlohaOneUIInlineTextField(
                        value = tempDataTimeFormat,
                        onValueChange = {
                            tempDataTimeFormat = it
                            label = runCatching {
                                StatusBarClockFormatter.format(it, LocalDateTime.now())
                            }.getOrElse { "error" }
                        },
                        modifier = Modifier.weight(1f),
                        labelText = label.takeIf { it.isNotBlank() },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AlohaOneUIActionButton(
                        text = stringResource(id = R.string.alohaoneui_confirm),
                        primary = true,
                        onClick = {
                            var right = true
                            label = try {
                                StatusBarClockFormatter.format(
                                    tempDataTimeFormat,
                                    LocalDateTime.now()
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
            }
        }
        if (
            uiState.setStatusBarClockFormat &&
                uiState.statusBarClockFormat.contains('\n')
        ) {
            Column {
                val isFold7 = Build.MODEL.startsWith("SM-F966", ignoreCase = true)
                if (isFold7) {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.format_size),
                        title = stringResource(
                            id = R.string.alohaoneui_fold7CustomDoubleLineClockScale_title
                        ),
                        showChildIndicator = true,
                checked = uiState.useFold7CustomDoubleLineClockScale,
                        onCheckedChange = {
                            onEvent(
                                SystemUIEvent.StatusBar.SetFold7CustomDoubleLineClockScale(it)
                            )
                        }
                    )
                    AlohaOneUIChildVisibility(uiState.useFold7CustomDoubleLineClockScale) {
                        Column {
                            var timeScale by remember(
                                uiState.fold7DoubleLineClockTimeScale
                            ) {
                                mutableFloatStateOf(
                                    uiState.fold7DoubleLineClockTimeScale
                                )
                            }
                            AlohaOneUIInfoItem(
                                title = stringResource(
                                    id = R.string.alohaoneui_fold7DoubleLineClockTimeScale_title
                                ),
                                summary = stringResource(
                                    id = R.string.alohaoneui_fold7DoubleLineClockTimeScale_summary,
                                    timeScale,
                                ),
                            )
                            AlohaOneUISlider(
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
                                modifier = Modifier.padding(horizontal = 16.dp),
                                valueRange = 0.50f..1.10f,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.Fold7DoubleLineClockTimeScale(
                                            timeScale
                                        )
                                    )
                                }
                            )
                            var dateScale by remember(
                                uiState.fold7DoubleLineClockDateScale
                            ) {
                                mutableFloatStateOf(
                                    uiState.fold7DoubleLineClockDateScale
                                )
                            }
                            AlohaOneUIInfoItem(
                                title = stringResource(
                                    id = R.string.alohaoneui_fold7DoubleLineClockDateScale_title
                                ),
                                summary = stringResource(
                                    id = R.string.alohaoneui_fold7DoubleLineClockDateScale_summary,
                                    dateScale,
                                ),
                            )
                            AlohaOneUISlider(
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
                                modifier = Modifier.padding(horizontal = 16.dp),
                                valueRange = 0.50f..1.10f,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.Fold7DoubleLineClockDateScale(
                                            dateScale
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
                if (!isFold7) {
                    val sizeLabels = listOf(
                        stringResource(id = R.string.alohaoneui_statusBarDoubleLineClockSize_small),
                        stringResource(id = R.string.alohaoneui_statusBarDoubleLineClockSize_compact),
                        stringResource(id = R.string.alohaoneui_statusBarDoubleLineClockSize_standard),
                        stringResource(id = R.string.alohaoneui_statusBarDoubleLineClockSize_large),
                        stringResource(id = R.string.alohaoneui_statusBarDoubleLineClockSize_extraLarge),
                    )
                    val selectedSize = uiState.statusBarDoubleLineClockSize.ifBlank {
                        statusTimeLegacyDoubleLineClockSize(uiState.statusBarClockTextScale)
                    }
                    val selectedIndex = STATUS_TIME_DOUBLE_LINE_CLOCK_SIZE_VALUES.indexOf(
                        selectedSize
                    ).coerceAtLeast(0)
                    SelectItem(
                        icon = ImageVector.vectorResource(id = R.drawable.format_size),
                        title = stringResource(id = R.string.alohaoneui_statusBarDoubleLineClockSize_title),
                        entries = sizeLabels,
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = {
                            onEvent(
                                SystemUIEvent.StatusBar.StatusBarDoubleLineClockSize(
                                    STATUS_TIME_DOUBLE_LINE_CLOCK_SIZE_VALUES[it]
                                )
                            )
                        }
                    )
                    val positionLabels = listOf(
                        stringResource(id = R.string.alohaoneui_statusBarDoubleLineClockPosition_top),
                        stringResource(id = R.string.alohaoneui_statusBarDoubleLineClockPosition_middle),
                        stringResource(id = R.string.alohaoneui_statusBarDoubleLineClockPosition_bottom),
                    )
                    val positionValues = listOf("top", "middle", "bottom")
                    val positionIndex = positionValues.indexOf(
                        uiState.statusBarDoubleLineClockVerticalPosition
                    ).let { if (it >= 0) it else 1 }
                    SelectItem(
                        icon = ImageVector.vectorResource(id = R.drawable.format_size),
                        title = stringResource(id = R.string.alohaoneui_statusBarDoubleLineClockPosition_title),
                        entries = positionLabels,
                        selectedIndex = positionIndex,
                        onSelectedIndexChange = {
                            onEvent(
                                SystemUIEvent.StatusBar.StatusBarDoubleLineClockVerticalPosition(
                                    positionValues[it]
                                )
                            )
                        }
                    )
                }
                var lineGapDp by remember(uiState.doubleLineClockGapDp) {
                    mutableFloatStateOf(uiState.doubleLineClockGapDp)
                }
                var gapExpanded by rememberSaveable { mutableStateOf(false) }
                AlohaOneUIPPreferenceGroup {
                    SwitchItem(
                        title = stringResource(id = R.string.alohaoneui_statusBarDoubleLineClockGap_title),
                        summary = if (uiState.modifyDoubleLineClockGap) {
                            stringResource(
                                id = R.string.alohaoneui_statusBarDoubleLineClockGap_summary,
                                lineGapDp
                            )
                        } else null,
                        icon = ImageVector.vectorResource(id = R.drawable.format_letter_spacing),
                        clickable = true,
                        onClick = { gapExpanded = !gapExpanded },
                        checked = uiState.modifyDoubleLineClockGap,
                        onCheckedChange = {
                            gapExpanded = it
                            onEvent(SystemUIEvent.StatusBar.ModifyDoubleLineClockGap(it))
                        }
                    )
                    AlohaOneUIChildVisibility(
                        gapExpanded && uiState.modifyDoubleLineClockGap
                    ) {
                        AlohaOneUISlider(
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
                            modifier = Modifier.padding(horizontal = 16.dp),
                            valueRange = 0f..2f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.DoubleLineClockGapDp(
                                        lineGapDp
                                    )
                                )
                            }
                        )
                    }
                }
                if (!isFold7) {
                    AlohaOneUISupportingText(
                        text = stringResource(id = R.string.alohaoneui_statusBarDoubleLineClockSize_summary),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            Column {
                var scale by remember {
                    mutableFloatStateOf(uiState.statusBarClockTextScale)
                }
                var expanded by rememberSaveable { mutableStateOf(false) }

                SwitchItem(
                    title = stringResource(id = R.string.alohaoneui_setStatusBarClockTextScale_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.setStatusBarClockTextScale) {
                        "%.2fx".format(scale)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.format_size),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.setStatusBarClockTextScale,
                    onCheckedChange = {
                        if (it && scale == 1f) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.SetStatusBarClockTextScale(it))
                    }
                )
                AlohaOneUIChildVisibility(expanded && uiState.setStatusBarClockTextScale) {
                    AlohaOneUISlider(
                        value = scale,
                        onValueChange = { raw ->
                            val value = (raw * 100f).roundToInt() / 100f
                            scale = value
                            onEvent(
                                SystemUIEvent.StatusBarPreview(
                                    SystemUIEvent.StatusBar.StatusBarClockTextScale(value)
                                )
                            )
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        valueRange = 0.5f..2.5f,
                        onValueChangeFinished = {
                            onEvent(SystemUIEvent.StatusBar.StatusBarClockTextScale(scale))
                        }
                    )
                }
            }
        }
        }
        SwitchItem(
            title = stringResource(id = R.string.alohaoneui_updateStatusBarClockEverySecond_title),
            summary = stringResource(id = R.string.alohaoneui_updateStatusBarClockEverySecond_summary),
            icon = ImageVector.vectorResource(id = R.drawable.nest_clock_farsight_digital),
            checked = uiState.updateStatusBarClockEverySecond,
            onCheckedChange = {
                onEvent(SystemUIEvent.StatusBar.UpdateStatusBarClockEverySecond(it))
            }
        )
    }
}