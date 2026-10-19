package com.oneuixp.app.ui.screen.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
 * Controls relocated from Status bar > Other into Tools > Other functions.
 * Preference and runtime event handling are unchanged.
 */
@Composable
fun OtherFunctionsControls(
    uiState: Preference.SystemUI.Other,
    otherState: Preference.Other,
    onEvent: (SystemUIEvent) -> Unit,
    onOtherChange: ((Preference.Other) -> Preference.Other) -> Unit,
) {
    var powerMenuExpanded by rememberSaveable { mutableStateOf(false) }

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
                        val visiblePosition = actions
                            .take(index + 1)
                            .count { it.visible }

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
                                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
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
                                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                                    OneuiXSwitch(
                                        checked = action.visible,
                                        onCheckedChange = { visible ->
                                            onEvent(
                                                SystemUIEvent.Other.PowerMenuActions(
                                                    actions.mapIndexed { actionIndex, item ->
                                                        if (actionIndex == index) {
                                                            item.copy(visible = visible)
                                                        } else {
                                                            item
                                                        }
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
        Spacer(Modifier.height(8.dp))
        StatusThemeGroup(items = listOf({
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.screenshot),
                title = stringResource(R.string.oneuix_disableScreenshotCaptureSound_title),
                checked = uiState.disableScreenshotCaptureSound,
                onCheckedChange = { onEvent(SystemUIEvent.Other.DisableScreenshotCaptureSound(it)) },
            )
        }))

        Spacer(Modifier.height(8.dp))
        StatusThemeGroup(items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.phone_forwarded),
                    title = stringResource(R.string.oneuix_callReturnDelay_title),
                    summary = stringResource(R.string.oneuix_callReturnDelay_summary),
                    checked = otherState.callReturnDelayEnabled,
                    onCheckedChange = { enabled -> onOtherChange { it.copy(callReturnDelayEnabled = enabled) } },
                )
            }
            if (otherState.callReturnDelayEnabled) {
                add {
                    OneuiXInfoItem(
                        title = stringResource(R.string.oneuix_callReturnDelaySeconds_title),
                        summary = stringResource(R.string.oneuix_callReturnDelaySeconds_summary, otherState.callReturnDelaySeconds.coerceIn(0, 5)),
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OneuiXActionButton(
                                    text = "−",
                                    enabled = otherState.callReturnDelaySeconds > 0,
                                    onClick = { onOtherChange { v -> v.copy(callReturnDelaySeconds = (v.callReturnDelaySeconds - 1).coerceAtLeast(0)) } },
                                )
                                Spacer(Modifier.width(8.dp))
                                OneuiXActionButton(
                                    text = "+",
                                    enabled = otherState.callReturnDelaySeconds < 5,
                                    onClick = { onOtherChange { v -> v.copy(callReturnDelaySeconds = (v.callReturnDelaySeconds + 1).coerceAtMost(5)) } },
                                )
                            }
                        },
                    )
                }
            }
        })

        Spacer(Modifier.height(8.dp))
        StatusThemeGroup(items = listOf({
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                title = stringResource(R.string.oneuix_settingsTwoPane_title),
                summary = stringResource(R.string.oneuix_settingsTwoPane_summary),
                checked = otherState.settingsTwoPaneEnabled,
                onCheckedChange = { enabled -> onOtherChange { it.copy(settingsTwoPaneEnabled = enabled) } },
            )
        }))
        Spacer(Modifier.height(8.dp))

        StatusThemeGroup(items = listOf({
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.apk_document),
                title = stringResource(R.string.oneuix_allowAppDowngrade_title),
                summary = stringResource(R.string.oneuix_allowAppDowngrade_summary),
                checked = otherState.allowAppDowngradeEnabled,
                onCheckedChange = { enabled -> onOtherChange { it.copy(allowAppDowngradeEnabled = enabled) } },
            )
        }))
        Spacer(Modifier.height(8.dp))

        StatusThemeGroup(items = listOf({
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.screenshot),
                title = stringResource(R.string.oneuix_disableSecureScreenshotRestriction_title),
                summary = stringResource(R.string.oneuix_disableSecureScreenshotRestriction_summary),
                checked = otherState.disableSecureScreenshotRestrictionEnabled,
                onCheckedChange = { enabled -> onOtherChange { it.copy(disableSecureScreenshotRestrictionEnabled = enabled) } },
            )
        }))
        Spacer(Modifier.height(8.dp))

        StatusThemeGroup(items = listOf({
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.apps_outlined),
                title = stringResource(R.string.oneuix_hidePackageInstallerRecommendation_title),
                summary = stringResource(R.string.oneuix_hidePackageInstallerRecommendation_summary),
                checked = otherState.hidePackageInstallerRecommendationEnabled,
                onCheckedChange = { enabled -> onOtherChange { it.copy(hidePackageInstallerRecommendationEnabled = enabled) } },
            )
        }))

}

private fun List<PowerMenuAction>.move(
    fromIndex: Int,
    toIndex: Int,
): List<PowerMenuAction> = toMutableList().apply {
    add(toIndex, removeAt(fromIndex))
}

@StringRes
private fun powerMenuActionTitle(actionName: String): Int = when (actionName) {
    PowerMenuAction.POWER -> R.string.oneuix_powerMenuAction_power
    PowerMenuAction.DATA_MODE -> R.string.oneuix_powerMenuAction_dataMode
    PowerMenuAction.RESTART -> R.string.oneuix_powerMenuAction_restart
    PowerMenuAction.SAFE_MODE -> R.string.oneuix_powerMenuAction_safeMode
    PowerMenuAction.LOCK_DOWN_MODE -> R.string.oneuix_powerMenuAction_lockDownMode
    PowerMenuAction.EMERGENCY_CALL -> R.string.oneuix_powerMenuAction_emergencyCall
    PowerMenuAction.MEDICAL_INFO -> R.string.oneuix_powerMenuAction_medicalInfo
    PowerMenuAction.SIDE_KEY_SETTINGS -> R.string.oneuix_sideKeySettings
    PowerMenuAction.FORCE_RESTART_MESSAGE -> R.string.oneuix_powerMenuAction_forceRestartMessage
    PowerMenuAction.RESTART_SYSTEMUI -> CommonR.string.restartSystemUI
    PowerMenuAction.RESTART_RECOVERY -> CommonR.string.restartRecovery
    PowerMenuAction.RESTART_DOWNLOAD -> CommonR.string.restartDownload
    else -> R.string.oneuix_other
}
