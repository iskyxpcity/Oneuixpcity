package com.oneuixp.app.ui.screen.status

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        ) {
                            OneuiXInlineTextField(
                                value = tempDataTimeFormat,
                                onValueChange = {
                                    tempDataTimeFormat = it
                                    formatLabel = runCatching {
                                        StatusBarClockFormatter.format(it, LocalDateTime.now())
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
                                        onValueChange = { timeScale = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        valueRange = 0.50f..1.10f,
                                        steps = 39,
                                        onValueChangeFinished = {
                                            onEvent(
                                                SystemUIEvent.StatusBar.Fold7DoubleLineClockTimeScale(
                                                    (timeScale * 100f).roundToInt() / 100f
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
                                        onValueChange = { dateScale = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        valueRange = 0.50f..1.10f,
                                        steps = 39,
                                        onValueChangeFinished = {
                                            onEvent(
                                                SystemUIEvent.StatusBar.Fold7DoubleLineClockDateScale(
                                                    (dateScale * 100f).roundToInt() / 100f
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
                                onValueChange = { lineGapDp = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                valueRange = 0f..2f,
                                steps = 19,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.DoubleLineClockGapDp(
                                            (lineGapDp * 10f).roundToInt() / 10f
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
                                onValueChange = { singleLineScale = it },
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

    if (isDoubleLine && !isFold7) {
        OneuiXSupportingText(
            text = stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_summary),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
