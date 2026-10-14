package io.github.soclear.oneuix.ui.category

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.ui.component.OneuiXChildVisibility
import io.github.soclear.oneuix.ui.component.OneuiXInfoItem
import io.github.soclear.oneuix.ui.component.OneuiXPreferenceGroup
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.SwitchItem
import kotlin.math.roundToInt

@Composable
fun StatusBarBurnInProtectionControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    OneuiXPreferenceGroup {
        var expanded by rememberSaveable(uiState.statusBarBurnInProtectionEnabled) {
            mutableStateOf(uiState.statusBarBurnInProtectionEnabled)
        }
        SwitchItem(
            title = stringResource(id = R.string.oneuix_statusBarBurnInProtection_title),
            summary = stringResource(id = R.string.oneuix_statusBarBurnInProtection_summary),
            icon = ImageVector.vectorResource(id = R.drawable.mobile_screensaver),
            modifier = Modifier.animateContentSize(),
            clickable = true,
            onClick = { expanded = !expanded },
            checked = uiState.statusBarBurnInProtectionEnabled,
            onCheckedChange = { enabled ->
                expanded = enabled
                onEvent(SystemUIEvent.StatusBar.StatusBarBurnInProtectionEnabled(enabled))
            },
        )
        OneuiXChildVisibility(expanded && uiState.statusBarBurnInProtectionEnabled) {
            Column {
                var amplitude by remember(uiState.statusBarBurnInProtectionAmplitudeDp) {
                    mutableFloatStateOf(uiState.statusBarBurnInProtectionAmplitudeDp.coerceIn(0f, 2f))
                }
                OneuiXInfoItem(
                    title = stringResource(id = R.string.oneuix_statusBarBurnInProtectionAmplitude_title),
                    summary = stringResource(id = R.string.oneuix_statusBarBurnInProtectionAmplitude_summary) +
                        "\n" + stringResource(
                            id = R.string.oneuix_statusBarBurnInProtectionAmplitude_value,
                            amplitude,
                        ),
                )
                OneuiXSlider(
                    value = amplitude,
                    onValueChange = { raw ->
                        amplitude = (raw.coerceIn(0f, 2f) * 4f).roundToInt() / 4f
                        onEvent(SystemUIEvent.StatusBarPreview(
                            SystemUIEvent.StatusBar.StatusBarBurnInProtectionAmplitudeDp(amplitude)
                        ))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    valueRange = 0f..2f,
                    stepSize = 0.25f,
                    onValueChangeFinished = {
                        onEvent(SystemUIEvent.StatusBar.StatusBarBurnInProtectionAmplitudeDp(amplitude))
                    },
                )
                var period by remember(uiState.statusBarBurnInProtectionPeriodSeconds) {
                    mutableIntStateOf(uiState.statusBarBurnInProtectionPeriodSeconds.coerceIn(10, 300))
                }
                OneuiXInfoItem(
                    title = stringResource(id = R.string.oneuix_statusBarBurnInProtectionPeriod_title),
                    summary = stringResource(id = R.string.oneuix_statusBarBurnInProtectionPeriod_summary) +
                        "\n" + stringResource(
                            id = R.string.oneuix_statusBarBurnInProtectionPeriod_value,
                            period,
                        ),
                )
                OneuiXSlider(
                    value = period.toFloat(),
                    onValueChange = { raw ->
                        period = raw.roundToInt().coerceIn(10, 300)
                        onEvent(SystemUIEvent.StatusBarPreview(
                            SystemUIEvent.StatusBar.StatusBarBurnInProtectionPeriodSeconds(period)
                        ))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    valueRange = 10f..300f,
                    stepSize = 1f,
                    onValueChangeFinished = {
                        onEvent(SystemUIEvent.StatusBar.StatusBarBurnInProtectionPeriodSeconds(period))
                    },
                )
            }
        }
    }
}
