package com.oneuixp.app.ui.screen.status

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import io.github.soclear.oneuix.ui.component.OneuiXActionButton
import io.github.soclear.oneuix.ui.component.OneuiXInlineTextField
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.SelectItem
import io.github.soclear.oneuix.ui.component.SwitchItem
import kotlin.math.roundToInt

private const val STATUS_SIGNAL_ESIM_BOTH = 2

@Composable
fun StatusSignalCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    var left by remember(uiState.statusBarLeftPaddingDp) {
        mutableFloatStateOf(uiState.statusBarLeftPaddingDp)
    }
    var leftExpanded by rememberSaveable { mutableStateOf(false) }
    var right by remember(uiState.statusBarRightPaddingDp) {
        mutableFloatStateOf(uiState.statusBarRightPaddingDp)
    }
    var rightExpanded by rememberSaveable { mutableStateOf(false) }
    var top by remember(uiState.statusBarTopPaddingDp) {
        mutableFloatStateOf(uiState.statusBarTopPaddingDp)
    }
    var topExpanded by rememberSaveable { mutableStateOf(false) }
    var bottom by remember(uiState.statusBarBottomPaddingDp) {
        mutableFloatStateOf(uiState.statusBarBottomPaddingDp)
    }
    var bottomExpanded by rememberSaveable { mutableStateOf(false) }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_statusBarLeftPaddingDp_title),
                    summary = if (uiState.modifyStatusBarLeftPadding) "%.1fdp".format(left) else null,
                    icon = ImageVector.vectorResource(id = R.drawable.padding),
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { leftExpanded = !leftExpanded },
                    checked = uiState.modifyStatusBarLeftPadding,
                    onCheckedChange = {
                        leftExpanded = it
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarLeftPadding(it))
                    },
                )
            }
            if (leftExpanded && uiState.modifyStatusBarLeftPadding) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = left,
                            onValueChange = { left = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..100f,
                            onValueChangeFinished = {
                                onEvent(SystemUIEvent.StatusBar.StatusBarLeftPaddingDp(left))
                            },
                        )
                    }
                }
            }

            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_statusBarRightPaddingDp_title),
                    summary = if (uiState.modifyStatusBarRightPadding) "%.1fdp".format(right) else null,
                    icon = ImageVector.vectorResource(id = R.drawable.padding),
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { rightExpanded = !rightExpanded },
                    checked = uiState.modifyStatusBarRightPadding,
                    onCheckedChange = {
                        rightExpanded = it
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarRightPadding(it))
                    },
                )
            }
            if (rightExpanded && uiState.modifyStatusBarRightPadding) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = right,
                            onValueChange = { right = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..100f,
                            onValueChangeFinished = {
                                onEvent(SystemUIEvent.StatusBar.StatusBarRightPaddingDp(right))
                            },
                        )
                    }
                }
            }

            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_statusBarTopPaddingDp_title),
                    summary = if (uiState.modifyStatusBarTopPadding) {
                        stringResource(id = R.string.oneuix_statusBarTopPaddingDp_summary, top)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.padding),
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { topExpanded = !topExpanded },
                    checked = uiState.modifyStatusBarTopPadding,
                    onCheckedChange = {
                        topExpanded = it
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarTopPadding(it))
                    },
                )
            }
            if (topExpanded && uiState.modifyStatusBarTopPadding) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = top,
                            onValueChange = { top = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..8f,
                            steps = 79,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.StatusBarTopPaddingDp(
                                        (top * 10f).roundToInt() / 10f
                                    )
                                )
                            },
                        )
                    }
                }
            }

            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_statusBarBottomPaddingDp_title),
                    summary = if (uiState.modifyStatusBarBottomPadding) {
                        stringResource(id = R.string.oneuix_statusBarBottomPaddingDp_summary, bottom)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.padding),
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { bottomExpanded = !bottomExpanded },
                    checked = uiState.modifyStatusBarBottomPadding,
                    onCheckedChange = {
                        bottomExpanded = it
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarBottomPadding(it))
                    },
                )
            }
            if (bottomExpanded && uiState.modifyStatusBarBottomPadding) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = bottom,
                            onValueChange = { bottom = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..8f,
                            steps = 79,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.StatusBarBottomPaddingDp(
                                        (bottom * 10f).roundToInt() / 10f
                                    )
                                )
                            },
                        )
                    }
                }
            }
        },
    )

    var carrierExpanded by rememberSaveable { mutableStateOf(false) }
    var carrier by remember(uiState.customCarrierName) { mutableStateOf(uiState.customCarrierName) }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.folder_managed),
                    title = stringResource(id = R.string.oneuix_hideSecureFolderStatusBarIcon_title),
                    checked = uiState.hideSecureFolderStatusBarIcon,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.HideSecureFolderStatusBarIcon(it))
                    },
                )
            }
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.bluetooth),
                    title = stringResource(id = R.string.oneuix_restoreBluetoothStatusBarIcon_title),
                    checked = uiState.restoreBluetoothStatusBarIcon,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.RestoreBluetoothStatusBarIcon(it))
                    },
                )
            }
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.sim_card),
                    title = stringResource(id = R.string.oneuix_physicalEsimAdapterWorkaround_title),
                    summary = stringResource(id = R.string.oneuix_physicalEsimAdapterWorkaround_summary),
                    showChildIndicator = true,
                    checked = uiState.physicalEsimAdapterWorkaround,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.PhysicalEsimAdapterWorkaround(it))
                    },
                )
            }
            if (uiState.physicalEsimAdapterWorkaround) {
                add {
                    SelectItem(
                        icon = ImageVector.vectorResource(id = R.drawable.sim_card),
                        title = stringResource(id = R.string.oneuix_physicalEsimAdapterSimSlot_title),
                        entries = listOf(
                            stringResource(id = R.string.oneuix_sim_slot_1),
                            stringResource(id = R.string.oneuix_sim_slot_2),
                            stringResource(id = R.string.oneuix_sim_slot_both),
                        ),
                        selectedIndex = uiState.physicalEsimAdapterSimSlot.coerceIn(
                            0,
                            STATUS_SIGNAL_ESIM_BOTH,
                        ),
                        onSelectedIndexChange = {
                            onEvent(SystemUIEvent.StatusBar.PhysicalEsimAdapterSimSlot(it))
                        },
                    )
                }
            }
            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_setCustomCarrierName_title),
                    summary = if (uiState.setCustomCarrierName) uiState.customCarrierName else null,
                    icon = ImageVector.vectorResource(id = R.drawable.sim_card),
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { carrierExpanded = !carrierExpanded },
                    checked = uiState.setCustomCarrierName,
                    onCheckedChange = {
                        if (it && uiState.customCarrierName.isEmpty()) carrierExpanded = true
                        if (!it) carrierExpanded = false
                        onEvent(SystemUIEvent.StatusBar.SetCustomCarrierName(it))
                    },
                )
            }
            if (carrierExpanded && uiState.setCustomCarrierName) {
                add {
                    StatusThemeChildItem {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        ) {
                            OneuiXInlineTextField(
                                value = carrier,
                                onValueChange = { carrier = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OneuiXActionButton(
                                text = stringResource(id = R.string.oneuix_confirm),
                                primary = true,
                                onClick = {
                                    onEvent(SystemUIEvent.StatusBar.CustomCarrierName(carrier))
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}
