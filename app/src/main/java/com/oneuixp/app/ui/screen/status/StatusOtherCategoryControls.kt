package com.oneuixp.app.ui.screen.status

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.PowerMenuAction
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import io.github.soclear.oneuix.ui.component.OneuiXActionButton
import io.github.soclear.oneuix.ui.component.OneuiXInfoItem
import io.github.soclear.oneuix.ui.component.OneuiXSwitch
import io.github.soclear.oneuix.ui.component.SwitchItem
import io.github.soclear.oneuix.common.R as CommonR

/**
 * Independent Status > Other controls extracted from the legacy System UI page.
 *
 * UI ownership is third-page-only; Preference, SystemUIEvent and hook logic stay
 * shared with the legacy implementation. The legacy System UI category stays
 * in source and is hidden only from the second-page category list.
 */
@Composable
fun StatusOtherCategoryControls(
    uiState: Preference.SystemUI.Other,
    onEvent: (SystemUIEvent) -> Unit,
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

            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.screenshot),
                    title = stringResource(
                        id = R.string.oneuix_disableScreenshotCaptureSound_title
                    ),
                    checked = uiState.disableScreenshotCaptureSound,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.Other.DisableScreenshotCaptureSound(it))
                    },
                )
            }
        },
    )
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
