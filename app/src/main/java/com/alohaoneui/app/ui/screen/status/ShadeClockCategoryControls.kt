package com.alohaoneui.app.ui.screen.status

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.R
import com.alohaoneui.common.Preference
import com.alohaoneui.hook.util.ShadeClockFormatter
import com.alohaoneui.mod.ui.component.AlohaOneUIActionButton
import com.alohaoneui.mod.ui.component.AlohaOneUIInfoItem
import com.alohaoneui.mod.ui.component.SelectItem
import com.alohaoneui.mod.ui.component.SwitchItem

@Composable
fun ShadeClockCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onChange: (Preference.SystemUI.StatusBar) -> Unit,
    onApply: () -> Unit,
) {
    val enabled = uiState.shadeClockCustomEnabled

    // Keep the source screen's three-part structure: enable card, option card,
    // and live preview card. The rows themselves use the active AlohaOneUI theme.
    StatusThemeGroup(
        title = stringResource(R.string.status_shade_clock_group_basic),
        items = listOf(
            {
                SwitchItem(
                    title = stringResource(R.string.status_shade_clock_enable),
                    summary = stringResource(
                        if (enabled) R.string.status_shade_clock_state_enabled
                        else R.string.status_shade_clock_state_disabled,
                    ),
                    checked = enabled,
                    onCheckedChange = { onChange(uiState.copy(shadeClockCustomEnabled = it)) },
                )
            },
        ),
    )

    StatusThemeGroup(
        title = stringResource(R.string.status_shade_clock_group_content),
        items = buildList {
            add {
                SwitchItem(
                    title = stringResource(R.string.status_shade_clock_time),
                    enabled = enabled,
                    checked = uiState.shadeClockTimeEnabled,
                    onCheckedChange = { onChange(uiState.copy(shadeClockTimeEnabled = it)) },
                )
            }
            add {
                SwitchItem(
                    title = stringResource(R.string.status_shade_clock_date),
                    enabled = enabled,
                    checked = uiState.shadeClockDateEnabled,
                    onCheckedChange = { onChange(uiState.copy(shadeClockDateEnabled = it)) },
                )
            }
            add {
                SelectItem(
                    title = stringResource(R.string.status_shade_clock_date_format),
                    enabled = enabled && uiState.shadeClockDateEnabled,
                    entries = listOf(
                        stringResource(R.string.status_shade_clock_option_month_day),
                        stringResource(R.string.status_shade_clock_option_full_date),
                        stringResource(R.string.status_shade_clock_option_short_date),
                    ),
                    selectedIndex = uiState.shadeClockDateFormat.coerceIn(0, 2),
                    onSelectedIndexChange = { onChange(uiState.copy(shadeClockDateFormat = it)) },
                )
            }
            add {
                SwitchItem(
                    title = stringResource(R.string.status_shade_clock_week),
                    enabled = enabled,
                    checked = uiState.shadeClockWeekEnabled,
                    onCheckedChange = { onChange(uiState.copy(shadeClockWeekEnabled = it)) },
                )
            }
            add {
                SelectItem(
                    title = stringResource(R.string.status_shade_clock_week_format),
                    enabled = enabled && uiState.shadeClockWeekEnabled,
                    entries = listOf(
                        stringResource(R.string.status_shade_clock_option_week_short),
                        stringResource(R.string.status_shade_clock_option_week_full),
                        stringResource(R.string.status_shade_clock_option_week_day),
                    ),
                    selectedIndex = uiState.shadeClockWeekFormat.coerceIn(0, 2),
                    onSelectedIndexChange = { onChange(uiState.copy(shadeClockWeekFormat = it)) },
                )
            }
            add {
                SwitchItem(
                    title = stringResource(R.string.status_shade_clock_lunar),
                    enabled = enabled,
                    checked = uiState.shadeClockLunarEnabled,
                    onCheckedChange = { onChange(uiState.copy(shadeClockLunarEnabled = it)) },
                )
            }
            add {
                SelectItem(
                    title = stringResource(R.string.status_shade_clock_lunar_format),
                    enabled = enabled && uiState.shadeClockLunarEnabled,
                    entries = listOf(
                        stringResource(R.string.status_shade_clock_option_lunar_special),
                        stringResource(R.string.status_shade_clock_option_animal_lunar_special),
                        stringResource(R.string.status_shade_clock_option_lunar_animal),
                        stringResource(R.string.status_shade_clock_option_detail_year_lunar_special),
                        stringResource(R.string.status_shade_clock_option_lunar_only),
                        stringResource(R.string.status_shade_clock_option_animal_lunar),
                        stringResource(R.string.status_shade_clock_option_detail_year_lunar),
                    ),
                    selectedIndex = uiState.shadeClockLunarFormat.coerceIn(0, 6),
                    onSelectedIndexChange = { onChange(uiState.copy(shadeClockLunarFormat = it)) },
                )
            }
            add {
                SelectItem(
                    title = stringResource(R.string.status_shade_clock_layout),
                    enabled = enabled,
                    entries = listOf(
                        stringResource(R.string.status_shade_clock_option_single_line),
                        stringResource(R.string.status_shade_clock_option_double_line),
                    ),
                    selectedIndex = uiState.shadeClockLayoutStyle.coerceIn(0, 1),
                    onSelectedIndexChange = { onChange(uiState.copy(shadeClockLayoutStyle = it)) },
                )
            }
            if (enabled && uiState.shadeClockLayoutStyle == 1) {
                val lines = listOf(
                    stringResource(R.string.status_shade_clock_option_top),
                    stringResource(R.string.status_shade_clock_option_bottom),
                )
                add {
                    SelectItem(
                        title = stringResource(R.string.status_shade_clock_line_time),
                        entries = lines,
                        selectedIndex = uiState.shadeClockTimeLine.coerceIn(0, 1),
                        onSelectedIndexChange = { onChange(uiState.copy(shadeClockTimeLine = it)) },
                    )
                }
                add {
                    SelectItem(
                        title = stringResource(R.string.status_shade_clock_line_date),
                        enabled = uiState.shadeClockDateEnabled,
                        entries = lines,
                        selectedIndex = uiState.shadeClockDateLine.coerceIn(0, 1),
                        onSelectedIndexChange = { onChange(uiState.copy(shadeClockDateLine = it)) },
                    )
                }
                add {
                    SelectItem(
                        title = stringResource(R.string.status_shade_clock_line_week),
                        enabled = uiState.shadeClockWeekEnabled,
                        entries = lines,
                        selectedIndex = uiState.shadeClockWeekLine.coerceIn(0, 1),
                        onSelectedIndexChange = { onChange(uiState.copy(shadeClockWeekLine = it)) },
                    )
                }
                add {
                    SelectItem(
                        title = stringResource(R.string.status_shade_clock_line_lunar),
                        enabled = uiState.shadeClockLunarEnabled,
                        entries = lines,
                        selectedIndex = uiState.shadeClockLunarLine.coerceIn(0, 1),
                        onSelectedIndexChange = { onChange(uiState.copy(shadeClockLunarLine = it)) },
                    )
                }
                if (uiState.shadeClockDetailStyle != 0) {
                    add {
                        SelectItem(
                            title = stringResource(R.string.status_shade_clock_line_detail),
                            entries = lines,
                            selectedIndex = uiState.shadeClockDetailLine.coerceIn(0, 1),
                            onSelectedIndexChange = { onChange(uiState.copy(shadeClockDetailLine = it)) },
                        )
                    }
                }
            }
            add {
                SelectItem(
                    title = stringResource(R.string.status_shade_clock_detail_style),
                    enabled = enabled,
                    entries = listOf(
                        stringResource(R.string.status_shade_clock_option_disabled),
                        stringResource(R.string.status_shade_clock_option_period),
                        stringResource(R.string.status_shade_clock_option_chinese_hour),
                        stringResource(R.string.status_shade_clock_option_season),
                    ),
                    selectedIndex = uiState.shadeClockDetailStyle.coerceIn(0, 3),
                    onSelectedIndexChange = { onChange(uiState.copy(shadeClockDetailStyle = it)) },
                )
            }
        },
    )

    StatusThemeGroup(
        title = stringResource(R.string.status_shade_clock_group_preview),
        items = listOf(
            {
                AlohaOneUIInfoItem(
                    title = ShadeClockFormatter.format(uiState),
                )
            },
        ),
    )

    AlohaOneUIActionButton(
        text = stringResource(R.string.status_shade_clock_apply),
        onClick = onApply,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        fillWidth = true,
    )
}
