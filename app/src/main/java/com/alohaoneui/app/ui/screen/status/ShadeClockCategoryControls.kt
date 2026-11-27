package com.alohaoneui.app.ui.screen.status

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.common.Preference
import com.alohaoneui.hook.util.ShadeClockFormatter
import com.alohaoneui.mod.ui.component.AlohaOneUIActionButton
import com.alohaoneui.mod.ui.component.AlohaOneUISwitch
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ShadeClockCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onChange: (Preference.SystemUI.StatusBar) -> Unit,
    onApply: () -> Unit,
) {
    val enabled = uiState.shadeClockCustomEnabled
    val showSeconds = Settings.Secure.getString(
        androidx.compose.ui.platform.LocalContext.current.contentResolver,
        "icon_blacklist",
    ).orEmpty().split(",").any { it.trim() == "slimindicator_show_seconds" }

    // The source screen has four distinct surfaces: enable, the complete
    // options card, preview, and the apply action. Keep those boundaries.
    StatusThemeGroup(
        items = listOf(
            {
                StatusThemeChildItem {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ShadeClockSwitchRow(
                            title = stringResource(R.string.status_shade_clock_enable),
                            checked = enabled,
                            enabled = true,
                            onCheckedChange = {
                                onChange(uiState.copy(shadeClockCustomEnabled = it))
                            },
                        )
                        ShadeClockStatusText(enabled = enabled)
                    }
                }
            },
        ),
    )

    StatusThemeGroup(
        items = listOf(
            {
                StatusThemeChildItem {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        ShadeClockSwitchRow(
                            title = stringResource(R.string.status_shade_clock_time),
                            checked = uiState.shadeClockTimeEnabled,
                            enabled = enabled,
                            onCheckedChange = {
                                onChange(uiState.copy(shadeClockTimeEnabled = it))
                            },
                        )
                        ShadeClockSwitchRow(
                            title = stringResource(R.string.status_shade_clock_week),
                            checked = uiState.shadeClockWeekEnabled,
                            enabled = enabled,
                            onCheckedChange = {
                                onChange(uiState.copy(shadeClockWeekEnabled = it))
                            },
                        )
                        ShadeClockSectionTitle(stringResource(R.string.status_shade_clock_week_format), topPadding = 8.dp)
                        ShadeClockChoiceRow(
                            choices = listOf(
                                ShadeClockChoice(weekSample(0), 0),
                                ShadeClockChoice(weekSample(1), 1),
                                ShadeClockChoice(weekSample(2), 2),
                            ),
                            selectedValue = uiState.shadeClockWeekFormat,
                            enabled = enabled && uiState.shadeClockWeekEnabled,
                            onSelected = { onChange(uiState.copy(shadeClockWeekFormat = it)) },
                        )

                        ShadeClockSwitchRow(
                            title = stringResource(R.string.status_shade_clock_date),
                            checked = uiState.shadeClockDateEnabled,
                            enabled = enabled,
                            onCheckedChange = {
                                onChange(uiState.copy(shadeClockDateEnabled = it))
                            },
                        )
                        ShadeClockSectionTitle(stringResource(R.string.status_shade_clock_date_format), topPadding = 8.dp)
                        ShadeClockChoiceGrid(
                            choices = listOf(
                                ShadeClockChoice(dateSample(0), 0),
                                ShadeClockChoice(dateSample(1), 1),
                                ShadeClockChoice(dateSample(2), 2),
                            ),
                            selectedValue = uiState.shadeClockDateFormat,
                            enabled = enabled && uiState.shadeClockDateEnabled,
                            onSelected = { onChange(uiState.copy(shadeClockDateFormat = it)) },
                        )

                        ShadeClockSwitchRow(
                            title = stringResource(R.string.status_shade_clock_lunar),
                            checked = uiState.shadeClockLunarEnabled,
                            enabled = enabled,
                            onCheckedChange = {
                                onChange(uiState.copy(shadeClockLunarEnabled = it))
                            },
                        )
                        ShadeClockSectionTitle(stringResource(R.string.status_shade_clock_lunar_format), topPadding = 8.dp)
                        ShadeClockChoiceGrid(
                            choices = listOf(
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_lunar_special), 0),
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_animal_lunar_special), 1),
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_lunar_animal), 2),
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_detail_year_lunar_special), 3),
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_lunar_only), 4),
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_animal_lunar), 5),
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_detail_year_lunar), 6),
                            ),
                            selectedValue = uiState.shadeClockLunarFormat,
                            enabled = enabled && uiState.shadeClockLunarEnabled,
                            onSelected = { onChange(uiState.copy(shadeClockLunarFormat = it)) },
                        )

                        ShadeClockSectionTitle(stringResource(R.string.status_shade_clock_layout))
                        ShadeClockChoiceRow(
                            choices = listOf(
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_single_line), 0),
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_double_line), 1),
                            ),
                            selectedValue = uiState.shadeClockLayoutStyle,
                            enabled = enabled,
                            onSelected = { onChange(uiState.copy(shadeClockLayoutStyle = it)) },
                        )

                        // Keep all five source line selectors visible. They become
                        // enabled only for double-line mode and their corresponding data.
                        ShadeClockSectionTitle(stringResource(R.string.status_shade_clock_group_lines))
                        ShadeClockLinePicker(
                            title = stringResource(R.string.status_shade_clock_line_time),
                            selectedValue = uiState.shadeClockTimeLine,
                            enabled = enabled && uiState.shadeClockLayoutStyle == 1,
                            onSelected = { onChange(uiState.copy(shadeClockTimeLine = it)) },
                        )
                        ShadeClockLinePicker(
                            title = stringResource(R.string.status_shade_clock_line_date),
                            selectedValue = uiState.shadeClockDateLine,
                            enabled = enabled && uiState.shadeClockLayoutStyle == 1 && uiState.shadeClockDateEnabled,
                            onSelected = { onChange(uiState.copy(shadeClockDateLine = it)) },
                        )
                        ShadeClockLinePicker(
                            title = stringResource(R.string.status_shade_clock_line_week),
                            selectedValue = uiState.shadeClockWeekLine,
                            enabled = enabled && uiState.shadeClockLayoutStyle == 1 && uiState.shadeClockWeekEnabled,
                            onSelected = { onChange(uiState.copy(shadeClockWeekLine = it)) },
                        )
                        ShadeClockLinePicker(
                            title = stringResource(R.string.status_shade_clock_line_lunar),
                            selectedValue = uiState.shadeClockLunarLine,
                            enabled = enabled && uiState.shadeClockLayoutStyle == 1 && uiState.shadeClockLunarEnabled,
                            onSelected = { onChange(uiState.copy(shadeClockLunarLine = it)) },
                        )
                        ShadeClockLinePicker(
                            title = stringResource(R.string.status_shade_clock_line_detail),
                            selectedValue = uiState.shadeClockDetailLine,
                            enabled = enabled && uiState.shadeClockLayoutStyle == 1 && uiState.shadeClockDetailStyle != 0,
                            onSelected = { onChange(uiState.copy(shadeClockDetailLine = it)) },
                        )

                        ShadeClockSectionTitle(stringResource(R.string.status_shade_clock_detail_style))
                        ShadeClockChoiceGrid(
                            choices = listOf(
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_disabled), 0),
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_period), 1),
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_chinese_hour), 2),
                                ShadeClockChoice(stringResource(R.string.status_shade_clock_option_season), 3),
                            ),
                            selectedValue = uiState.shadeClockDetailStyle,
                            enabled = enabled,
                            onSelected = { onChange(uiState.copy(shadeClockDetailStyle = it)) },
                        )
                    }
                }
            },
        ),
    )

    StatusThemeGroup(
        items = listOf(
            {
                StatusThemeChildItem {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ShadeClockSectionTitle(stringResource(R.string.status_shade_clock_group_preview))
                        when (LocalUiMode.current) {
                            UiMode.Material -> MaterialText(
                                text = ShadeClockFormatter.format(uiState, showSeconds = showSeconds),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            UiMode.Miuix -> MiuixText(
                                text = ShadeClockFormatter.format(uiState, showSeconds = showSeconds),
                                color = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                }
            },
        ),
    )

    AlohaOneUIActionButton(
        text = stringResource(R.string.status_shade_clock_apply),
        onClick = onApply,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp),
        fillWidth = true,
    )
}

@Composable
private fun ShadeClockSwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val alpha = if (enabled) 1f else 0.46f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (LocalUiMode.current) {
            UiMode.Material -> MaterialText(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            UiMode.Miuix -> MiuixText(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
        }
        AlohaOneUISwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun ShadeClockSectionTitle(text: String, topPadding: androidx.compose.ui.unit.Dp = 12.dp) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialText(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = topPadding),
        )
        UiMode.Miuix -> MiuixText(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(top = topPadding),
        )
    }
}

private data class ShadeClockChoice(val label: String, val value: Int)

@Composable
private fun ShadeClockChoiceRow(
    choices: List<ShadeClockChoice>,
    selectedValue: Int,
    enabled: Boolean,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        choices.forEach { choice ->
            ShadeClockChoiceButton(
                choice = choice,
                selected = selectedValue == choice.value,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(choice.value) },
            )
        }
    }
}

@Composable
private fun ShadeClockChoiceGrid(
    choices: List<ShadeClockChoice>,
    selectedValue: Int,
    enabled: Boolean,
    onSelected: (Int) -> Unit,
) {
    choices.chunked(2).forEach { rowChoices ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rowChoices.forEach { choice ->
                ShadeClockChoiceButton(
                    choice = choice,
                    selected = selectedValue == choice.value,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelected(choice.value) },
                )
            }
            if (rowChoices.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ShadeClockLinePicker(
    title: String,
    selectedValue: Int,
    enabled: Boolean,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (LocalUiMode.current) {
            UiMode.Material -> MaterialText(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            UiMode.Miuix -> MiuixText(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }
        listOf(
            ShadeClockChoice(stringResource(R.string.status_shade_clock_option_top), 0),
            ShadeClockChoice(stringResource(R.string.status_shade_clock_option_bottom), 1),
        ).forEach { choice ->
            ShadeClockChoiceButton(
                choice = choice,
                selected = selectedValue == choice.value,
                enabled = enabled,
                modifier = Modifier.padding(start = 8.dp),
                fixedWidth = 72.dp,
                onClick = { onSelected(choice.value) },
            )
        }
    }
}

@Composable
private fun ShadeClockChoiceButton(
    choice: ShadeClockChoice,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    fixedWidth: androidx.compose.ui.unit.Dp? = null,
    onClick: () -> Unit,
) {
    val buttonModifier = if (fixedWidth != null) {
        modifier.width(fixedWidth).height(36.dp)
    } else {
        modifier.height(40.dp)
    }
    val containerColor = if (LocalUiMode.current == UiMode.Material) {
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    } else null
    val contentColor = if (LocalUiMode.current == UiMode.Material) {
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    } else null
    AlohaOneUIActionButton(
        text = choice.label,
        onClick = onClick,
        modifier = buttonModifier,
        enabled = enabled,
        primary = selected,
        fillWidth = fixedWidth == null,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

private fun dateSample(format: Int): String {
    val pattern = when (format) {
        1 -> "yyyy-MM-dd"
        2 -> "MM/dd"
        else -> "M月d日"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
}

private fun weekSample(format: Int): String {
    val calendar = Calendar.getInstance()
    val day = calendar.get(Calendar.DAY_OF_WEEK)
    return when (format) {
        1 -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        2 -> when (day) {
            Calendar.SUNDAY -> "日"
            Calendar.MONDAY -> "一"
            Calendar.TUESDAY -> "二"
            Calendar.WEDNESDAY -> "三"
            Calendar.THURSDAY -> "四"
            Calendar.FRIDAY -> "五"
            else -> "六"
        }
        else -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date())
    }
}

@Composable
private fun ShadeClockStatusText(enabled: Boolean) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialText(
            text = stringResource(
                if (enabled) R.string.status_shade_clock_state_enabled
                else R.string.status_shade_clock_state_disabled,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        UiMode.Miuix -> MiuixText(
            text = stringResource(
                if (enabled) R.string.status_shade_clock_state_enabled
                else R.string.status_shade_clock_state_disabled,
            ),
            color = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
