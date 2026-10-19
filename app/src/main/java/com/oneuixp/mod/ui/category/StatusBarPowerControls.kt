package com.oneuixp.mod.ui.category

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.oneuixp.common.ONE_UI_VERSION
import com.oneuixp.common.Preference
import com.oneuixp.mod.ui.component.OneuiXPChildVisibility
import com.oneuixp.mod.ui.component.OneuiXPPPreferenceGroup
import com.oneuixp.mod.ui.component.OneuiXPSlider
import com.oneuixp.mod.ui.component.SwitchItem

/**
 * Canonical status-bar battery/power controls shared by the legacy SystemUI
 * page and Status > Power. Platform guards, preference state and events are
 * shared; Miuix and Material only provide different rendering.
 */
@Composable
fun StatusBarPowerControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    OneuiXPPPreferenceGroup {
        var widthScale by remember(uiState.batteryIconWidthScale) {
            mutableFloatStateOf(uiState.batteryIconWidthScale)
        }
        var expanded by rememberSaveable { mutableStateOf(false) }

        SwitchItem(
            title = stringResource(id = R.string.oneuixp_setBatteryIconWidthScale_title),
            modifier = Modifier.animateContentSize(),
            summary = if (uiState.setBatteryIconWidthScale) {
                "%.2f".format(widthScale)
            } else {
                null
            },
            icon = ImageVector.vectorResource(id = R.drawable.battery),
            clickable = true,
            onClick = { expanded = !expanded },
            checked = uiState.setBatteryIconWidthScale,
            onCheckedChange = {
                if (it && widthScale == 0f) {
                    expanded = true
                } else if (!it) {
                    expanded = false
                }
                onEvent(SystemUIEvent.StatusBar.SetBatteryIconWidthScale(it))
            },
        )
        OneuiXPChildVisibility(expanded && uiState.setBatteryIconWidthScale) {
            OneuiXPSlider(
                value = widthScale,
                onValueChange = { widthScale = it },
                modifier = Modifier.padding(horizontal = 16.dp),
                valueRange = 0.5f..2f,
                onValueChangeFinished = {
                    onEvent(SystemUIEvent.StatusBar.BatteryIconWidthScale(widthScale))
                },
            )
        }
    }

    OneuiXPPPreferenceGroup {
        var heightScale by remember(uiState.batteryIconHeightScale) {
            mutableFloatStateOf(uiState.batteryIconHeightScale)
        }
        var expanded by rememberSaveable { mutableStateOf(false) }

        SwitchItem(
            title = stringResource(id = R.string.oneuixp_setBatteryIconHeightScale_title),
            modifier = Modifier.animateContentSize(),
            summary = if (uiState.setBatteryIconHeightScale) {
                "%.2f".format(heightScale)
            } else {
                null
            },
            icon = ImageVector.vectorResource(id = R.drawable.battery),
            clickable = true,
            onClick = { expanded = !expanded },
            checked = uiState.setBatteryIconHeightScale,
            onCheckedChange = {
                if (it && heightScale == 0f) {
                    expanded = true
                } else if (!it) {
                    expanded = false
                }
                onEvent(SystemUIEvent.StatusBar.SetBatteryIconHeightScale(it))
            },
        )
        OneuiXPChildVisibility(expanded && uiState.setBatteryIconHeightScale) {
            OneuiXPSlider(
                value = heightScale,
                onValueChange = { heightScale = it },
                modifier = Modifier.padding(horizontal = 16.dp),
                valueRange = 0.5f..2f,
                onValueChangeFinished = {
                    onEvent(SystemUIEvent.StatusBar.BatteryIconHeightScale(heightScale))
                },
            )
        }
    }

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.battery),
            title = stringResource(id = R.string.oneuixp_hideBatteryPercentageSign_title),
            summary = stringResource(id = R.string.oneuixp_hideBatteryPercentageSign_summary),
            checked = uiState.hideBatteryPercentageSign,
            onCheckedChange = {
                onEvent(SystemUIEvent.StatusBar.HideBatteryPercentageSign(it))
            },
        )
    }

    SwitchItem(
        icon = ImageVector.vectorResource(id = R.drawable.battery),
        title = stringResource(id = R.string.oneuixp_hideBatteryIcon_title),
        summary = stringResource(id = R.string.oneuixp_hideBatteryIcon_summary),
        checked = uiState.hideBatteryIcon,
        onCheckedChange = {
            onEvent(SystemUIEvent.StatusBar.HideBatteryIcon(it))
        },
    )

    if (ONE_UI_VERSION >= 70000) {
        OneuiXPPPreferenceGroup {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.battery),
                title = stringResource(id = R.string.oneuixp_addBatteryLevelText_title),
                summary = stringResource(id = R.string.oneuixp_addBatteryLevelText_summary),
                showChildIndicator = true,
                checked = uiState.addBatteryLevelText,
                onCheckedChange = {
                    onEvent(SystemUIEvent.StatusBar.AddBatteryLevelText(it))
                },
            )
            OneuiXPChildVisibility(uiState.addBatteryLevelText) {
                Column {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.battery),
                        title = stringResource(
                            id = R.string.oneuixp_hideBatteryLevelTextPercentageSign_title
                        ),
                        checked = uiState.hideBatteryLevelTextPercentageSign,
                        onCheckedChange = {
                            onEvent(
                                SystemUIEvent.StatusBar.HideBatteryLevelTextPercentageSign(it)
                            )
                        },
                    )
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.battery),
                        title = stringResource(
                            id = R.string.oneuixp_hideBatteryLevelTextChargingIcon_title
                        ),
                        checked = uiState.hideBatteryLevelTextChargingIcon,
                        onCheckedChange = {
                            onEvent(
                                SystemUIEvent.StatusBar.HideBatteryLevelTextChargingIcon(it)
                            )
                        },
                    )
                }
            }
        }
    }
}
