package com.oneuixp.app.ui.screen.status

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import io.github.soclear.oneuix.common.ONE_UI_VERSION
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.SwitchItem

@Composable
fun StatusPowerCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    var widthScale by remember(uiState.batteryIconWidthScale) {
        mutableFloatStateOf(uiState.batteryIconWidthScale)
    }
    var widthExpanded by rememberSaveable { mutableStateOf(false) }

    var heightScale by remember(uiState.batteryIconHeightScale) {
        mutableFloatStateOf(uiState.batteryIconHeightScale)
    }
    var heightExpanded by rememberSaveable { mutableStateOf(false) }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_setBatteryIconWidthScale_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.setBatteryIconWidthScale) {
                        "%.2f".format(widthScale)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.battery),
                    clickable = true,
                    onClick = { widthExpanded = !widthExpanded },
                    checked = uiState.setBatteryIconWidthScale,
                    onCheckedChange = {
                        if (it && widthScale == 0f) {
                            widthExpanded = true
                        } else if (!it) {
                            widthExpanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.SetBatteryIconWidthScale(it))
                    },
                )
            }
            if (widthExpanded && uiState.setBatteryIconWidthScale) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = widthScale,
                            onValueChange = { widthScale = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            valueRange = 0.5f..2f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.BatteryIconWidthScale(widthScale)
                                )
                            },
                        )
                    }
                }
            }

            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_setBatteryIconHeightScale_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.setBatteryIconHeightScale) {
                        "%.2f".format(heightScale)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.battery),
                    clickable = true,
                    onClick = { heightExpanded = !heightExpanded },
                    checked = uiState.setBatteryIconHeightScale,
                    onCheckedChange = {
                        if (it && heightScale == 0f) {
                            heightExpanded = true
                        } else if (!it) {
                            heightExpanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.SetBatteryIconHeightScale(it))
                    },
                )
            }
            if (heightExpanded && uiState.setBatteryIconHeightScale) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = heightScale,
                            onValueChange = { heightScale = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            valueRange = 0.5f..2f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.BatteryIconHeightScale(heightScale)
                                )
                            },
                        )
                    }
                }
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.battery),
                        title = stringResource(
                            id = R.string.oneuix_hideBatteryPercentageSign_title
                        ),
                        summary = stringResource(
                            id = R.string.oneuix_hideBatteryPercentageSign_summary
                        ),
                        checked = uiState.hideBatteryPercentageSign,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.StatusBar.HideBatteryPercentageSign(it))
                        },
                    )
                }
            }

            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.battery),
                    title = stringResource(id = R.string.oneuix_hideBatteryIcon_title),
                    summary = stringResource(id = R.string.oneuix_hideBatteryIcon_summary),
                    checked = uiState.hideBatteryIcon,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.HideBatteryIcon(it))
                    },
                )
            }

            if (ONE_UI_VERSION >= 70000) {
                add {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.battery),
                        title = stringResource(id = R.string.oneuix_addBatteryLevelText_title),
                        summary = stringResource(id = R.string.oneuix_addBatteryLevelText_summary),
                        showChildIndicator = true,
                        checked = uiState.addBatteryLevelText,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.StatusBar.AddBatteryLevelText(it))
                        },
                    )
                }
                if (uiState.addBatteryLevelText) {
                    add {
                        SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.battery),
                            title = stringResource(
                                id = R.string.oneuix_hideBatteryLevelTextPercentageSign_title
                            ),
                            checked = uiState.hideBatteryLevelTextPercentageSign,
                            onCheckedChange = {
                                onEvent(
                                    SystemUIEvent.StatusBar.HideBatteryLevelTextPercentageSign(it)
                                )
                            },
                        )
                    }
                    add {
                        SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.battery),
                            title = stringResource(
                                id = R.string.oneuix_hideBatteryLevelTextChargingIcon_title
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
        },
    )
}
