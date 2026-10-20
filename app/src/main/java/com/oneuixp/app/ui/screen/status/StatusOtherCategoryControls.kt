package com.oneuixp.app.ui.screen.status

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import com.oneuixp.app.R
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.common.Preference
import com.oneuixp.mod.ui.category.SystemUIEvent
import com.oneuixp.mod.ui.component.OneuiXPSlider
import com.oneuixp.mod.ui.component.SwitchItem
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Status bar > Other keeps the status-bar burn-in protection controls.
 * Power-menu actions and screenshot-sound controls live under Tools > Other functions.
 * The existing preference model, SystemUI events, and hook behavior remain shared.
 */
@Composable
fun StatusOtherCategoryControls(
    statusBarState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    StatusBurnInProtectionControls(
        uiState = statusBarState,
        onEvent = onEvent,
    )
}

@Composable
private fun StatusBurnInProtectionControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    val enabled = uiState.statusBarBurnInProtectionEnabled
    var expanded by rememberSaveable(enabled) { mutableStateOf(enabled) }
    var amplitude by remember(uiState.statusBarBurnInProtectionAmplitudeDp) {
        mutableFloatStateOf(uiState.statusBarBurnInProtectionAmplitudeDp.coerceIn(0f, 2f))
    }
    var period by remember(uiState.statusBarBurnInProtectionPeriodSeconds) {
        mutableFloatStateOf(uiState.statusBarBurnInProtectionPeriodSeconds.coerceIn(10, 300).toFloat())
    }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.mobile_screensaver),
                    title = stringResource(id = R.string.oneuixp_statusBarBurnInProtection_title),
                    summary = stringResource(id = R.string.oneuixp_statusBarBurnInProtection_summary),
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = enabled,
                    onCheckedChange = { value ->
                        expanded = value
                        onEvent(SystemUIEvent.StatusBar.StatusBarBurnInProtectionEnabled(value))
                    },
                )
            }

            if (enabled && expanded) {
                add {
                    BurnInSliderSetting(
                        title = stringResource(id = R.string.oneuixp_statusBarBurnInProtectionAmplitude_title),
                        summary = stringResource(id = R.string.oneuixp_statusBarBurnInProtectionAmplitude_summary) +
                            "\n" + stringResource(
                                id = R.string.oneuixp_statusBarBurnInProtectionAmplitude_value,
                                amplitude,
                            ),
                        value = amplitude,
                        valueRange = 0f..2f,
                        stepSize = 0.25f,
                        onValueChange = { raw ->
                            amplitude = (raw.coerceIn(0f, 2f) * 4f).roundToInt() / 4f
                            onEvent(
                                SystemUIEvent.StatusBarPreview(
                                    SystemUIEvent.StatusBar.StatusBarBurnInProtectionAmplitudeDp(amplitude)
                                )
                            )
                        },
                        onValueChangeFinished = {
                            onEvent(SystemUIEvent.StatusBar.StatusBarBurnInProtectionAmplitudeDp(amplitude))
                        },
                    )
                }
                add {
                    BurnInSliderSetting(
                        title = stringResource(id = R.string.oneuixp_statusBarBurnInProtectionPeriod_title),
                        summary = stringResource(id = R.string.oneuixp_statusBarBurnInProtectionPeriod_summary) +
                            "\n" + stringResource(
                                id = R.string.oneuixp_statusBarBurnInProtectionPeriod_value,
                                period.roundToInt(),
                            ),
                        value = period,
                        valueRange = 10f..300f,
                        stepSize = 5f,
                        onValueChange = { raw ->
                            period = raw.roundToInt().coerceIn(10, 300).toFloat()
                            onEvent(
                                SystemUIEvent.StatusBarPreview(
                                    SystemUIEvent.StatusBar.StatusBarBurnInProtectionPeriodSeconds(
                                        period.roundToInt()
                                    )
                                )
                            )
                        },
                        onValueChangeFinished = {
                            onEvent(
                                SystemUIEvent.StatusBar.StatusBarBurnInProtectionPeriodSeconds(
                                    period.roundToInt()
                                )
                            )
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun BurnInSliderSetting(
    title: String,
    summary: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    stepSize: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    // Keep the title, explanation and slider inside one native grouped row.
    // Previously OneuiXPInfoItem and StatusThemeChildItem each drew their own
    // Material segmented surface, visually splitting the same setting.
    StatusThemeChildItem {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (LocalUiMode.current == UiMode.Material) 8.dp else 16.dp,
                    vertical = 8.dp,
                ),
        ) {
            when (LocalUiMode.current) {
                UiMode.Material -> {
                    MaterialText(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    MaterialText(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                UiMode.Miuix -> {
                    MiuixText(
                        text = title,
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    MiuixText(
                        text = summary,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OneuiXPSlider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                valueRange = valueRange,
                stepSize = stepSize,
                onValueChangeFinished = onValueChangeFinished,
            )
        }
    }
}
