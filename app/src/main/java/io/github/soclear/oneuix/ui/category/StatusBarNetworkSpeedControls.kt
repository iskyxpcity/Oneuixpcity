package io.github.soclear.oneuix.ui.category

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import io.github.soclear.oneuix.ui.component.OneuiXActionButton
import io.github.soclear.oneuix.ui.component.OneuiXChildVisibility
import io.github.soclear.oneuix.ui.component.OneuiXInlineTextField
import io.github.soclear.oneuix.ui.component.OneuiXPreferenceGroup
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.SwitchItem
import kotlin.math.roundToInt

/**
 * Canonical status-bar network-speed controls shared by the legacy SystemUI
 * page and Status > Network speed. Both themes share the same preference
 * values and SystemUI events; only their widgets render differently.
 */
@Composable
fun StatusBarNetworkSpeedControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    SwitchItem(
        icon = ImageVector.vectorResource(id = R.drawable.net_speed),
        title = stringResource(id = R.string.oneuix_supportRealTimeNetworkSpeed_title),
        summary = stringResource(id = R.string.oneuix_supportRealTimeNetworkSpeed_summary),
        checked = uiState.supportRealTimeNetworkSpeed,
        onCheckedChange = {
            onEvent(SystemUIEvent.StatusBar.SupportRealTimeNetworkSpeed(it))
        },
    )
    SwitchItem(
        icon = ImageVector.vectorResource(id = R.drawable.net_speed),
        title = stringResource(id = R.string.oneuix_showSeparateUpDownNetworkSpeeds_title),
        summary = stringResource(id = R.string.oneuix_showSeparateUpDownNetworkSpeeds_summary),
        checked = uiState.showSeparateUpDownNetworkSpeeds,
        onCheckedChange = {
            onEvent(SystemUIEvent.StatusBar.ShowSeparateUpDownNetworkSpeeds(it))
        },
    )

    if (uiState.supportRealTimeNetworkSpeed || uiState.showSeparateUpDownNetworkSpeeds) {
        OneuiXPreferenceGroup {
            var expanded by rememberSaveable { mutableStateOf(false) }
            var lineGapDp by remember(uiState.networkSpeedLineGapDp) {
            mutableFloatStateOf(uiState.networkSpeedLineGapDp)
        }
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.format_letter_spacing),
            title = stringResource(id = R.string.oneuix_networkSpeedLineGap_title),
            summary = if (uiState.modifyNetworkSpeedLineGap) {
                stringResource(id = R.string.oneuix_networkSpeedLineGap_summary, lineGapDp)
            } else {
                stringResource(id = R.string.oneuix_networkSpeedDoubleLineOnly_summary)
            },
            modifier = Modifier.animateContentSize(),
            clickable = true,
            onClick = { expanded = !expanded },
            checked = uiState.modifyNetworkSpeedLineGap,
            onCheckedChange = {
                expanded = it
                onEvent(SystemUIEvent.StatusBar.ModifyNetworkSpeedLineGap(it))
            },
        )
        OneuiXChildVisibility(expanded && uiState.modifyNetworkSpeedLineGap) {
            OneuiXSlider(
                value = lineGapDp,
                onValueChange = { lineGapDp = (it * 10f).roundToInt() / 10f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                valueRange = -4f..4f,
                onValueChangeFinished = {
                    onEvent(SystemUIEvent.StatusBar.NetworkSpeedLineGapDp(lineGapDp))
                },
            )
        }
    }

    OneuiXPreferenceGroup {
        var expanded by rememberSaveable { mutableStateOf(false) }
        var uploadScale by remember(uiState.networkSpeedUploadTextScale) {
            mutableFloatStateOf(uiState.networkSpeedUploadTextScale)
        }
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.format_size),
            title = stringResource(id = R.string.oneuix_networkSpeedUploadTextScale_title),
            summary = if (uiState.setNetworkSpeedUploadTextScale) {
                stringResource(id = R.string.oneuix_networkSpeedUploadTextScale_summary, uploadScale)
            } else {
                stringResource(id = R.string.oneuix_networkSpeedDoubleLineOnly_summary)
            },
            modifier = Modifier.animateContentSize(),
            clickable = true,
            onClick = { expanded = !expanded },
            checked = uiState.setNetworkSpeedUploadTextScale,
            onCheckedChange = {
                expanded = it
                onEvent(SystemUIEvent.StatusBar.SetNetworkSpeedUploadTextScale(it))
            },
        )
        OneuiXChildVisibility(expanded && uiState.setNetworkSpeedUploadTextScale) {
            OneuiXSlider(
                value = uploadScale,
                onValueChange = { uploadScale = (it * 100f).roundToInt() / 100f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                valueRange = 0.5f..2f,
                onValueChangeFinished = {
                    onEvent(SystemUIEvent.StatusBar.NetworkSpeedUploadTextScale(uploadScale))
                },
            )
        }
    }

    OneuiXPreferenceGroup {
        var expanded by rememberSaveable { mutableStateOf(false) }
        var downloadScale by remember(uiState.networkSpeedDownloadTextScale) {
            mutableFloatStateOf(uiState.networkSpeedDownloadTextScale)
        }
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.format_size),
            title = stringResource(id = R.string.oneuix_networkSpeedDownloadTextScale_title),
            summary = if (uiState.setNetworkSpeedDownloadTextScale) {
                stringResource(id = R.string.oneuix_networkSpeedDownloadTextScale_summary, downloadScale)
            } else {
                stringResource(id = R.string.oneuix_networkSpeedDoubleLineOnly_summary)
            },
            modifier = Modifier.animateContentSize(),
            clickable = true,
            onClick = { expanded = !expanded },
            checked = uiState.setNetworkSpeedDownloadTextScale,
            onCheckedChange = {
                expanded = it
                onEvent(SystemUIEvent.StatusBar.SetNetworkSpeedDownloadTextScale(it))
            },
        )
        OneuiXChildVisibility(expanded && uiState.setNetworkSpeedDownloadTextScale) {
            OneuiXSlider(
                value = downloadScale,
                onValueChange = { downloadScale = (it * 100f).roundToInt() / 100f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                valueRange = 0.5f..2f,
                onValueChangeFinished = {
                    onEvent(SystemUIEvent.StatusBar.NetworkSpeedDownloadTextScale(downloadScale))
                },
            )
        }
    }

    }

    OneuiXPreferenceGroup {
        var expanded by rememberSaveable { mutableStateOf(false) }
        var threshold by remember(uiState.networkSpeedThresholdKb) {
            mutableIntStateOf(uiState.networkSpeedThresholdKb)
        }

        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.net_speed),
            title = stringResource(id = R.string.oneuix_networkSpeedThreshold_title),
            summary = if (uiState.networkSpeedThresholdKb > 0) {
                "${uiState.networkSpeedThresholdKb} KB/s"
            } else {
                stringResource(id = R.string.oneuix_networkSpeedThreshold_scope_summary)
            },
            modifier = Modifier.animateContentSize(),
            clickable = true,
            onClick = { expanded = !expanded },
            checked = uiState.networkSpeedThresholdKb > 0,
            onCheckedChange = {
                if (it && threshold == 0) threshold = 1
                if (!it) {
                    threshold = 0
                    expanded = false
                } else {
                    expanded = true
                }
                onEvent(SystemUIEvent.StatusBar.NetworkSpeedThreshold(threshold))
            },
        )
        OneuiXChildVisibility(expanded && uiState.networkSpeedThresholdKb > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                OneuiXInlineTextField(
                    value = threshold.toString(),
                    onValueChange = {
                        threshold = it.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    },
                    modifier = Modifier.weight(1f),
                    labelText = stringResource(id = R.string.oneuix_networkSpeedThreshold_label),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                OneuiXActionButton(
                    text = stringResource(id = R.string.oneuix_confirm),
                    primary = true,
                    onClick = {
                        onEvent(SystemUIEvent.StatusBar.NetworkSpeedThreshold(threshold))
                    },
                )
            }
        }
    }
}
