package com.alohaoneui.app.ui.screen.tools

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
import com.alohaoneui.app.alohaOneUIApp
import com.alohaoneui.app.alohaoneui.AlohaOneUIPreferenceBridge
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.navigation3.LocalNavigator
import com.alohaoneui.app.ui.navigation3.Route
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.component.dialog.rememberConfirmDialog
import com.alohaoneui.mod.ui.component.AlohaOneUISlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import kotlin.math.roundToInt
import com.alohaoneui.app.R
import com.alohaoneui.common.PowerMenuAction
import com.alohaoneui.common.Preference
import com.alohaoneui.mod.ui.category.SystemUIEvent
import com.alohaoneui.mod.ui.component.AlohaOneUIActionButton
import com.alohaoneui.mod.ui.component.AlohaOneUIInfoItem
import com.alohaoneui.mod.ui.component.AlohaOneUISwitch
import com.alohaoneui.app.ui.component.material.SegmentedListItem
import com.alohaoneui.app.ui.component.material.alohaOneUISegmentedColors
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import com.alohaoneui.mod.ui.component.SwitchItem
import com.alohaoneui.common.R as CommonR

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
        stringResource(R.string.alohaoneui_callReturnEntryStateEnabled, persistedCallReturnDelay)
    } else {
        stringResource(R.string.alohaoneui_callReturnStateDisabled)
    }
    val callReturnDelaySummary = callReturnDelayStatus +
        "\n" + stringResource(R.string.alohaoneui_callReturnDelay_summary)

    var powerMenuExpanded by rememberSaveable { mutableStateOf(false) }

    OtherFunctionsThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.power_settings_new),
                    title = stringResource(id = R.string.alohaoneui_customPowerMenu_title),
                    summary = stringResource(id = R.string.alohaoneui_customPowerMenu_summary),
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
                        AlohaOneUIInfoItem(
                            title = stringResource(id = powerMenuActionTitle(action.name)),
                            summary = if (action.visible) {
                                stringResource(
                                    id = R.string.alohaoneui_powerMenuActionVisible_summary,
                                    visiblePosition,
                                )
                            } else {
                                stringResource(id = R.string.alohaoneui_powerMenuActionHidden_summary)
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AlohaOneUIActionButton(
                                        text = stringResource(id = R.string.alohaoneui_moveUp),
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
                                    AlohaOneUIActionButton(
                                        text = stringResource(id = R.string.alohaoneui_moveDown),
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
                                    AlohaOneUISwitch(
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
                    title = stringResource(id = R.string.alohaoneui_disableScreenshotCaptureSound_title),
                    summary = stringResource(id = R.string.alohaoneui_disableScreenshotCaptureSound_summary),
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
                        colors = alohaOneUISegmentedColors(),
                        headlineContent = {
                            MaterialText(text = stringResource(R.string.alohaoneui_callReturnDelay_title))
                        },
                        supportingContent = {
                            MaterialText(text = callReturnDelaySummary)
                        },
                        leadingContent = {
                            MaterialIcon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.phone_forwarded),
                                contentDescription = stringResource(R.string.alohaoneui_callReturnDelay_title),
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
                        title = stringResource(R.string.alohaoneui_callReturnDelay_title),
                        summary = callReturnDelaySummary,
                        startAction = {
                            MiuixIcon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.phone_forwarded),
                                contentDescription = stringResource(R.string.alohaoneui_callReturnDelay_title),
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
                    title = stringResource(id = R.string.alohaoneui_settingsTwoPane_title),
                    summary = stringResource(id = R.string.alohaoneui_settingsTwoPane_summary),
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
                    title = stringResource(id = R.string.alohaoneui_allowAppDowngrade_title),
                    summary = stringResource(id = R.string.alohaoneui_allowAppDowngrade_summary),
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
                    title = stringResource(id = R.string.alohaoneui_disableSecureScreenshotRestriction_title),
                    summary = stringResource(id = R.string.alohaoneui_disableSecureScreenshotRestriction_summary),
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
                    title = stringResource(id = R.string.alohaoneui_hidePackageInstallerRecommendation_title),
                    summary = stringResource(id = R.string.alohaoneui_hidePackageInstallerRecommendation_summary),
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
    PowerMenuAction.POWER -> R.string.alohaoneui_powerMenuAction_power
    PowerMenuAction.DATA_MODE -> R.string.alohaoneui_powerMenuAction_dataMode
    PowerMenuAction.RESTART -> R.string.alohaoneui_powerMenuAction_restart
    PowerMenuAction.SAFE_MODE -> R.string.alohaoneui_powerMenuAction_safeMode
    PowerMenuAction.LOCK_DOWN_MODE -> R.string.alohaoneui_powerMenuAction_lockDownMode
    PowerMenuAction.EMERGENCY_CALL -> R.string.alohaoneui_powerMenuAction_emergencyCall
    PowerMenuAction.MEDICAL_INFO -> R.string.alohaoneui_powerMenuAction_medicalInfo
    PowerMenuAction.FORCE_RESTART_MESSAGE -> R.string.alohaoneui_powerMenuAction_forceRestartMessage
    PowerMenuAction.RESTART_SYSTEMUI -> CommonR.string.restartSystemUI
    PowerMenuAction.RESTART_RECOVERY -> CommonR.string.restartRecovery
    PowerMenuAction.RESTART_DOWNLOAD -> CommonR.string.restartDownload
    else -> R.string.alohaoneui_other
}
