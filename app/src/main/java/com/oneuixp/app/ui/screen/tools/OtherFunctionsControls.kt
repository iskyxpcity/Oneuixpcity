package com.oneuixp.app.ui.screen.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.oneuixp.app.R
import com.oneuixp.app.ui.screen.status.StatusThemeGroup
import io.github.soclear.oneuix.common.PowerMenuAction
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import io.github.soclear.oneuix.ui.component.OneuiXActionButton
import io.github.soclear.oneuix.ui.component.OneuiXInfoItem
import io.github.soclear.oneuix.ui.component.OneuiXSwitch
import io.github.soclear.oneuix.ui.component.SwitchItem
import io.github.soclear.oneuix.common.R as CommonR

/**
 * Each option uses its own card group. The migrated system features are stored under
 * Preference.Other and handled by OneuiXP's libxposed module.
 */
@Composable
fun OtherFunctionsControls(
    uiState: Preference.SystemUI.Other,
    otherState: Preference.Other,
    onEvent: (SystemUIEvent) -> Unit,
    onOtherChange: ((Preference.Other) -> Preference.Other) -> Unit,
) {
    var powerMenuExpanded by rememberSaveable { mutableStateOf(false) }

    // Power menu stays separate from screenshot sound.
    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.power_settings_new),
                    title = stringResource(id = R.string.oneuix_customPowerMenu_title),
                    clickable = true,
                    onClick = { powerMenuExpanded = !powerMenuExpanded },
                    checked = uiState.customPowerMenu,
                    onCheckedChange = {
                        powerMenuExpanded = it
                        onEvent(SystemUIEvent.Other.CustomPowerMenu(it))
                    },
                )
            }

            if (powerMenuExpanded && uiState.customPowerMenu) {
                val actions = PowerMenuAction.normalize(uiState.powerMenuActions)
                actions.forEachIndexed { index, action ->
                    add {
                        val visiblePosition = actions.take(index + 1).count { it.visible }
                        OneuiXInfoItem(
                            title = stringResource(id = powerMenuActionTitle(action.name)),
                            summary = if (action.visible) {
                                stringResource(
                                    id = R.string.oneuix_powerMenuActionVisible_summary,
                                    visiblePosition,
                                )
                            } else {
                                stringResource(id = R.string.oneuix_powerMenuActionHidden_summary)
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OneuiXActionButton(
                                        text = stringResource(id = R.string.oneuix_moveUp),
                                        enabled = index > 0,
                                        onClick = {
                                            onEvent(
                                                SystemUIEvent.Other.PowerMenuActions(
                                                    actions.move(index, index - 1)
                                                )
                                            )
                                        },
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    OneuiXActionButton(
                                        text = stringResource(id = R.string.oneuix_moveDown),
                                        enabled = index < actions.lastIndex,
                                        onClick = {
                                            onEvent(
                                                SystemUIEvent.Other.PowerMenuActions(
                                                    actions.move(index, index + 1)
                                                )
                                            )
                                        },
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    OneuiXSwitch(
                                        checked = action.visible,
                                        onCheckedChange = { visible ->
                                            onEvent(
                                                SystemUIEvent.Other.PowerMenuActions(
                                                    actions.mapIndexed { actionIndex, item ->
                                                        if (actionIndex == index) item.copy(visible = visible) else item
                                                    }
                                                )
                                            )
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.screenshot),
                    title = stringResource(id = R.string.oneuix_disableScreenshotCaptureSound_title),
                    checked = uiState.disableScreenshotCaptureSound,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.Other.DisableScreenshotCaptureSound(it))
                    },
                )
            }
        },
    )

    // Call return combines the enable switch and delay selector in one feature card.
    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.phone_forwarded),
                    title = stringResource(id = R.string.oneuix_callReturnDelay_title),
                    checked = otherState.callReturnDelayEnabled,
                    onCheckedChange = { checked ->
                        onOtherChange { current -> current.copy(callReturnDelayEnabled = checked) }
                    },
                )
            }
            if (otherState.callReturnDelayEnabled) {
                add {
                    OneuiXInfoItem(
                        title = stringResource(id = R.string.oneuix_callReturnDelaySeconds_title),
                        summary = stringResource(
                            id = R.string.oneuix_callReturnDelaySeconds_summary,
                            otherState.callReturnDelaySeconds.coerceIn(0, 5),
                        ),
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OneuiXActionButton(
                                    text = "−",
                                    enabled = otherState.callReturnDelaySeconds > 0,
                                    onClick = {
                                        onOtherChange { current ->
                                            current.copy(
                                                callReturnDelaySeconds =
                                                    (current.callReturnDelaySeconds - 1).coerceAtLeast(0)
                                            )
                                        }
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                OneuiXActionButton(
                                    text = "+",
                                    enabled = otherState.callReturnDelaySeconds < 5,
                                    onClick = {
                                        onOtherChange { current ->
                                            current.copy(
                                                callReturnDelaySeconds =
                                                    (current.callReturnDelaySeconds + 1).coerceAtMost(5)
                                            )
                                        }
                                    },
                                )
                            }
                        },
                    )
                }
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.expand),
                    title = stringResource(id = R.string.oneuix_settingsTwoPane_title),
                    checked = otherState.settingsTwoPaneEnabled,
                    onCheckedChange = { checked ->
                        onOtherChange { current -> current.copy(settingsTwoPaneEnabled = checked) }
                    },
                )
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.apk_document),
                    title = stringResource(id = R.string.oneuix_allowAppDowngrade_title),
                    checked = otherState.allowAppDowngradeEnabled,
                    onCheckedChange = { checked ->
                        onOtherChange { current -> current.copy(allowAppDowngradeEnabled = checked) }
                    },
                )
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.screenshot),
                    title = stringResource(id = R.string.oneuix_disableSecureScreenshotRestriction_title),
                    checked = otherState.disableSecureScreenshotRestrictionEnabled,
                    onCheckedChange = { checked ->
                        onOtherChange { current ->
                            current.copy(disableSecureScreenshotRestrictionEnabled = checked)
                        }
                    },
                )
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ad_off),
                    title = stringResource(id = R.string.oneuix_hidePackageInstallerRecommendation_title),
                    checked = otherState.hidePackageInstallerRecommendationEnabled,
                    onCheckedChange = { checked ->
                        onOtherChange { current ->
                            current.copy(hidePackageInstallerRecommendationEnabled = checked)
                        }
                    },
                )
            }
        },
    )
}

private fun List<PowerMenuAction>.move(fromIndex: Int, toIndex: Int): List<PowerMenuAction> =
    toMutableList().apply { add(toIndex, removeAt(fromIndex)) }

@StringRes
private fun powerMenuActionTitle(actionName: String): Int = when (actionName) {
    PowerMenuAction.POWER -> R.string.oneuix_powerMenuAction_power
    PowerMenuAction.DATA_MODE -> R.string.oneuix_powerMenuAction_dataMode
    PowerMenuAction.RESTART -> R.string.oneuix_powerMenuAction_restart
    PowerMenuAction.SAFE_MODE -> R.string.oneuix_powerMenuAction_safeMode
    PowerMenuAction.LOCK_DOWN_MODE -> R.string.oneuix_powerMenuAction_lockDownMode
    PowerMenuAction.EMERGENCY_CALL -> R.string.oneuix_powerMenuAction_emergencyCall
    PowerMenuAction.MEDICAL_INFO -> R.string.oneuix_powerMenuAction_medicalInfo
    PowerMenuAction.FORCE_RESTART_MESSAGE -> R.string.oneuix_powerMenuAction_forceRestartMessage
    PowerMenuAction.RESTART_SYSTEMUI -> CommonR.string.restartSystemUI
    PowerMenuAction.RESTART_RECOVERY -> CommonR.string.restartRecovery
    PowerMenuAction.RESTART_DOWNLOAD -> CommonR.string.restartDownload
    else -> R.string.oneuix_other
}
