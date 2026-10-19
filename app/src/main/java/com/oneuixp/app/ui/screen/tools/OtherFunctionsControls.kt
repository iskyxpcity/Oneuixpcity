package com.oneuixp.app.ui.screen.tools

import androidx.annotation.StringRes
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import com.oneuixp.app.oneuiXPApp
import com.oneuixp.app.oneuix.OneuiXPreferenceBridge
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.component.dialog.rememberConfirmDialog
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import kotlin.math.roundToInt
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val applyCallReturnDialog = rememberConfirmDialog(
        onConfirm = {
            scope.launch {
                Toast.makeText(
                    context,
                    context.getString(R.string.oneuix_callReturnApplyStarted),
                    Toast.LENGTH_SHORT,
                ).show()
                val synchronized = OneuiXPreferenceBridge.synchronizeNow(oneuiXPApp)
                if (!synchronized) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.oneuix_preferenceSync_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                    return@launch
                }
                val restarted = withContext(Dispatchers.IO) {
                    runCatching {
                        val process = ProcessBuilder(
                            "su",
                            "-c",
                            "am force-stop com.samsung.android.incallui",
                        ).redirectErrorStream(true).start()
                        process.inputStream.use { it.readBytes() }
                        process.waitFor() == 0
                    }.getOrDefault(false)
                }
                Toast.makeText(
                    context,
                    context.getString(
                        if (restarted) R.string.oneuix_callReturnApplied
                        else R.string.oneuix_callReturnApplyFailed
                    ),
                    Toast.LENGTH_LONG,
                ).show()
            }
        },
    )
    val callReturnApplyTitle = stringResource(R.string.oneuix_callReturnApplyTitle)
    val callReturnApplyMessage = stringResource(R.string.oneuix_callReturnApplyMessage)
    val callReturnApplyNow = stringResource(R.string.oneuix_callReturnApplyNow)
    val callReturnApplyLater = stringResource(R.string.oneuix_callReturnApplyLater)
    val askApplyCallReturnNow = {
        applyCallReturnDialog.showConfirm(
            title = callReturnApplyTitle,
            content = callReturnApplyMessage,
            confirm = callReturnApplyNow,
            dismiss = callReturnApplyLater,
        )
    }

    var powerMenuExpanded by rememberSaveable { mutableStateOf(false) }

    // Power menu stays separate from screenshot sound.
    OtherFunctionsThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.power_settings_new),
                    title = stringResource(id = R.string.oneuix_customPowerMenu_title),
                    summary = stringResource(id = R.string.oneuix_customPowerMenu_summary),
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

    OtherFunctionsThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.screenshot),
                    title = stringResource(id = R.string.oneuix_disableScreenshotCaptureSound_title),
                    summary = stringResource(id = R.string.oneuix_disableScreenshotCaptureSound_summary),
                    checked = uiState.disableScreenshotCaptureSound,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.Other.DisableScreenshotCaptureSound(it))
                    },
                )
            }
        },
    )

    // This setting is a single native Material segmented group / Miuix Card.
    val persistedCallReturnDelay = otherState.callReturnDelaySeconds.coerceIn(0, 5)
    var callReturnDelaySeconds by remember(persistedCallReturnDelay) {
        mutableFloatStateOf(persistedCallReturnDelay.toFloat())
    }
    val callReturnDelayStatus = if (otherState.callReturnDelayEnabled) {
        stringResource(
            id = R.string.oneuix_callReturnStateEnabled,
            callReturnDelaySeconds.roundToInt().coerceIn(0, 5),
        )
    } else {
        stringResource(id = R.string.oneuix_callReturnStateDisabled)
    }

    OtherFunctionsThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.phone_forwarded),
                    title = stringResource(id = R.string.oneuix_callReturnDelay_title),
                    summary = stringResource(id = R.string.oneuix_callReturnDelay_summary),
                    checked = otherState.callReturnDelayEnabled,
                    onCheckedChange = { checked ->
                        onOtherChange { current -> current.copy(callReturnDelayEnabled = checked) }
                        askApplyCallReturnNow()
                    },
                )
            }
            add {
                OtherFunctionsThemeChildItem {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        when (LocalUiMode.current) {
                            UiMode.Material -> MaterialText(
                                text = callReturnDelayStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            UiMode.Miuix -> MiuixText(
                                text = callReturnDelayStatus,
                                color = colorScheme.onBackground,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        when (LocalUiMode.current) {
                            UiMode.Material -> MaterialText(
                                text = stringResource(R.string.oneuix_callReturnDelaySeconds_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            UiMode.Miuix -> MiuixText(
                                text = stringResource(R.string.oneuix_callReturnDelaySeconds_title),
                                color = colorScheme.onBackground,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        when (LocalUiMode.current) {
                            UiMode.Material -> MaterialText(
                                text = stringResource(
                                    R.string.oneuix_callReturnDelaySeconds_summary,
                                    callReturnDelaySeconds.roundToInt().coerceIn(0, 5),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            UiMode.Miuix -> MiuixText(
                                text = stringResource(
                                    R.string.oneuix_callReturnDelaySeconds_summary,
                                    callReturnDelaySeconds.roundToInt().coerceIn(0, 5),
                                ),
                                color = colorScheme.onBackground,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (LocalUiMode.current) {
                                UiMode.Material -> MaterialText(
                                    text = "0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                                UiMode.Miuix -> MiuixText(
                                    text = "0",
                                    color = colorScheme.onBackground,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            OneuiXSlider(
                                value = callReturnDelaySeconds,
                                onValueChange = { callReturnDelaySeconds = it },
                                modifier = Modifier.weight(1f),
                                valueRange = 0f..5f,
                                stepSize = 1f,
                                enabled = otherState.callReturnDelayEnabled,
                                onValueChangeFinished = {
                                    val finalSeconds = callReturnDelaySeconds.roundToInt().coerceIn(0, 5)
                                    onOtherChange { current ->
                                        current.copy(callReturnDelaySeconds = finalSeconds)
                                    }
                                    askApplyCallReturnNow()
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            when (LocalUiMode.current) {
                                UiMode.Material -> MaterialText(
                                    text = "5",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                                UiMode.Miuix -> MiuixText(
                                    text = "5",
                                    color = colorScheme.onBackground,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        when (LocalUiMode.current) {
                            UiMode.Material -> MaterialText(
                                text = stringResource(R.string.oneuix_callReturnDelayControlHint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            UiMode.Miuix -> MiuixText(
                                text = stringResource(R.string.oneuix_callReturnDelayControlHint),
                                color = colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
        },
    )

    OtherFunctionsThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.expand),
                    title = stringResource(id = R.string.oneuix_settingsTwoPane_title),
                    summary = stringResource(id = R.string.oneuix_settingsTwoPane_summary),
                    checked = otherState.settingsTwoPaneEnabled,
                    onCheckedChange = { checked ->
                        onOtherChange { current -> current.copy(settingsTwoPaneEnabled = checked) }
                    },
                )
            }
        },
    )

    OtherFunctionsThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.apk_document),
                    title = stringResource(id = R.string.oneuix_allowAppDowngrade_title),
                    summary = stringResource(id = R.string.oneuix_allowAppDowngrade_summary),
                    checked = otherState.allowAppDowngradeEnabled,
                    onCheckedChange = { checked ->
                        onOtherChange { current -> current.copy(allowAppDowngradeEnabled = checked) }
                    },
                )
            }
        },
    )

    OtherFunctionsThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.screenshot),
                    title = stringResource(id = R.string.oneuix_disableSecureScreenshotRestriction_title),
                    summary = stringResource(id = R.string.oneuix_disableSecureScreenshotRestriction_summary),
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

    OtherFunctionsThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ad_off),
                    title = stringResource(id = R.string.oneuix_hidePackageInstallerRecommendation_title),
                    summary = stringResource(id = R.string.oneuix_hidePackageInstallerRecommendation_summary),
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
