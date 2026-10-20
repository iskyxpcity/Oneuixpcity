package com.oneuialoha.app.ui.screen.tools

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import com.oneuialoha.app.oneUIAlohaApp
import com.oneuialoha.app.oneuixp.OneUIAlohaPreferenceBridge
import com.oneuialoha.app.ui.LocalUiMode
import com.oneuialoha.app.ui.navigation3.LocalNavigator
import com.oneuialoha.app.ui.navigation3.Route
import com.oneuialoha.app.ui.UiMode
import com.oneuialoha.app.ui.component.dialog.rememberConfirmDialog
import com.oneuialoha.mod.ui.component.OneUIAlohaSlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import kotlin.math.roundToInt
import com.oneuialoha.app.R
import com.oneuialoha.common.PowerMenuAction
import com.oneuialoha.common.Preference
import com.oneuialoha.mod.ui.category.SystemUIEvent
import com.oneuialoha.mod.ui.component.OneUIAlohaActionButton
import com.oneuialoha.mod.ui.component.OneUIAlohaInfoItem
import com.oneuialoha.mod.ui.component.OneUIAlohaSwitch
import com.oneuialoha.app.ui.component.material.SegmentedListItem
import com.oneuialoha.app.ui.component.material.oneUIAlohaSegmentedColors
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import com.oneuialoha.mod.ui.component.SwitchItem
import com.oneuialoha.common.R as CommonR

@Composable
fun OtherFunctionsControls(
    uiState: Preference.SystemUI.Other,
    otherState: Preference.Other,
    onEvent: (SystemUIEvent) -> Unit,
    onOtherChange: ((Preference.Other) -> Preference.Other) -> Unit,
) {
    val navigator = LocalNavigator.current
    val persistedCallReturnDelay = otherState.callReturnDelaySeconds.coerceIn(0, 5)
    val callReturnDelayStatus = if (otherState.callReturnDelayEnabled) {
        stringResource(R.string.oneuialoha_callReturnEntryStateEnabled, persistedCallReturnDelay)
    } else {
        stringResource(R.string.oneuialoha_callReturnStateDisabled)
    }
    val callReturnDelaySummary = callReturnDelayStatus +
        "\n" + stringResource(R.string.oneuialoha_callReturnDelay_summary)

    var powerMenuExpanded by rememberSaveable { mutableStateOf(false) }

    OtherFunctionsThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.power_settings_new),
                    title = stringResource(id = R.string.oneuialoha_customPowerMenu_title),
                    summary = stringResource(id = R.string.oneuialoha_customPowerMenu_summary),
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
                        OneUIAlohaInfoItem(
                            title = stringResource(id = powerMenuActionTitle(action.name)),
                            summary = if (action.visible) {
                                stringResource(
                                    id = R.string.oneuialoha_powerMenuActionVisible_summary,
                                    visiblePosition,
                                )
                            } else {
                                stringResource(id = R.string.oneuialoha_powerMenuActionHidden_summary)
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OneUIAlohaActionButton(
                                        text = stringResource(id = R.string.oneuialoha_moveUp),
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
                                    OneUIAlohaActionButton(
                                        text = stringResource(id = R.string.oneuialoha_moveDown),
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
                                    OneUIAlohaSwitch(
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
                    title = stringResource(id = R.string.oneuialoha_disableScreenshotCaptureSound_title),
                    summary = stringResource(id = R.string.oneuialoha_disableScreenshotCaptureSound_summary),
                    checked = uiState.disableScreenshotCaptureSound,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.Other.DisableScreenshotCaptureSound(it))
                    },
                )
            }
        },
    )

    // 「通话返回」只在独立页里实现，这里只做入口，避免两份逻辑各改各的。
    OtherFunctionsThemeGroup(
        items = buildList {
            add {
                when (LocalUiMode.current) {
                    UiMode.Material -> SegmentedListItem(
                        onClick = { navigator.push(Route.CallReturnDelay) },
                        colors = oneUIAlohaSegmentedColors(),
                        headlineContent = {
                            MaterialText(text = stringResource(R.string.oneuialoha_callReturnDelay_title))
                        },
                        supportingContent = {
                            MaterialText(text = callReturnDelaySummary)
                        },
                        leadingContent = {
                            MaterialIcon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.phone_forwarded),
                                contentDescription = stringResource(R.string.oneuialoha_callReturnDelay_title),
                            )
                        },
                        trailingContent = {
                            MaterialIcon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                            )
                        },
                    )
                    UiMode.Miuix -> BasicComponent(
                        title = stringResource(R.string.oneuialoha_callReturnDelay_title),
                        summary = callReturnDelaySummary,
                        startAction = {
                            MiuixIcon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.phone_forwarded),
                                contentDescription = stringResource(R.string.oneuialoha_callReturnDelay_title),
                                tint = colorScheme.onSurface,
                            )
                        },
                        endActions = {
                            MiuixIcon(
                                imageVector = MiuixIcons.Basic.ArrowRight,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariantActions,
                            )
                        },
                        onClick = { navigator.push(Route.CallReturnDelay) },
                    )
                }
            }
        },
    )

    OtherFunctionsThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.expand),
                    title = stringResource(id = R.string.oneuialoha_settingsTwoPane_title),
                    summary = stringResource(id = R.string.oneuialoha_settingsTwoPane_summary),
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
                    title = stringResource(id = R.string.oneuialoha_allowAppDowngrade_title),
                    summary = stringResource(id = R.string.oneuialoha_allowAppDowngrade_summary),
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
                    title = stringResource(id = R.string.oneuialoha_disableSecureScreenshotRestriction_title),
                    summary = stringResource(id = R.string.oneuialoha_disableSecureScreenshotRestriction_summary),
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
                    title = stringResource(id = R.string.oneuialoha_hidePackageInstallerRecommendation_title),
                    summary = stringResource(id = R.string.oneuialoha_hidePackageInstallerRecommendation_summary),
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
    PowerMenuAction.POWER -> R.string.oneuialoha_powerMenuAction_power
    PowerMenuAction.DATA_MODE -> R.string.oneuialoha_powerMenuAction_dataMode
    PowerMenuAction.RESTART -> R.string.oneuialoha_powerMenuAction_restart
    PowerMenuAction.SAFE_MODE -> R.string.oneuialoha_powerMenuAction_safeMode
    PowerMenuAction.LOCK_DOWN_MODE -> R.string.oneuialoha_powerMenuAction_lockDownMode
    PowerMenuAction.EMERGENCY_CALL -> R.string.oneuialoha_powerMenuAction_emergencyCall
    PowerMenuAction.MEDICAL_INFO -> R.string.oneuialoha_powerMenuAction_medicalInfo
    PowerMenuAction.FORCE_RESTART_MESSAGE -> R.string.oneuialoha_powerMenuAction_forceRestartMessage
    PowerMenuAction.RESTART_SYSTEMUI -> CommonR.string.restartSystemUI
    PowerMenuAction.RESTART_RECOVERY -> CommonR.string.restartRecovery
    PowerMenuAction.RESTART_DOWNLOAD -> CommonR.string.restartDownload
    else -> R.string.oneuialoha_other
}
