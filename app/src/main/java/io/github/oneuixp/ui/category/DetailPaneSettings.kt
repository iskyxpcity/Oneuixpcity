package io.github.oneuixp.ui.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuixp.app.R
import io.github.oneuixp.common.Preference
import com.oneuixp.app.ui.viewmodel.OneuiXPToolsViewModel
import io.github.oneuixp.ui.component.SwitchItem
import io.github.oneuixp.ui.component.OneuiXPDetailGroup

@Composable
fun DetailPaneSettings(
    uiState: Preference.Settings,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        OneuiXPDetailGroup {
        SwitchItem(
            title = stringResource(id = R.string.oneuixp_showForcePeakRefreshRatePreference_title),
            summary = stringResource(id = R.string.oneuixp_showForcePeakRefreshRatePreference_summary),
            icon = ImageVector.vectorResource(id = R.drawable.logo_dev),
            checked = uiState.showForcePeakRefreshRatePreference,
            onCheckedChange = { onEvent(SettingsEvent.ShowForcePeakRefreshRatePreference(it)) },
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.light_mode),
            title = stringResource(id = R.string.oneuixp_supportOutdoorMode_title),
            checked = uiState.supportOutdoorMode,
            onCheckedChange = { onEvent(SettingsEvent.SupportOutdoorMode(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.battery),
            title = stringResource(id = R.string.oneuixp_showMoreBatteryInfo_title),
            summary = stringResource(id = R.string.oneuixp_showMoreBatteryInfo_summary),
            checked = uiState.showMoreBatteryInfo,
            onCheckedChange = { onEvent(SettingsEvent.ShowMoreBatteryInfo(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.apk_document),
            title = stringResource(id = R.string.oneuixp_showPackageInfo_title),
            summary = stringResource(id = R.string.oneuixp_showPackageInfo_summary),
            checked = uiState.showPackageInfo,
            onCheckedChange = { onEvent(SettingsEvent.ShowPackageInfo(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.wifi_link_speed),
            title = stringResource(id = R.string.oneuixp_showWiFiLinkSpeed_title),
            summary = stringResource(id = R.string.oneuixp_showWiFiLinkSpeed_summary),
            checked = uiState.showWiFiLinkSpeed,
            onCheckedChange = { onEvent(SettingsEvent.ShowWiFiLinkSpeed(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.font),
            title = stringResource(id = R.string.oneuixp_supportAnyFont_title),
            checked = uiState.supportAnyFont,
            onCheckedChange = { onEvent(SettingsEvent.SupportAnyFont(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.power_settings_new),
            title = stringResource(id = R.string.oneuixp_supportAutoPowerOnOff_title),
            summary = stringResource(id = R.string.oneuixp_supportAutoPowerOnOff_summary),
            checked = uiState.supportAutoPowerOnOff,
            onCheckedChange = { onEvent(SettingsEvent.SupportAutoPowerOnOff(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.mobile_screensaver),
            title = stringResource(id = R.string.oneuixp_spoofPhoneStatusAsOfficial_title),
            summary = stringResource(id = R.string.oneuixp_spoofPhoneStatusAsOfficial_summary),
            checked = uiState.spoofPhoneStatusAsOfficial,
            onCheckedChange = { onEvent(SettingsEvent.SpoofPhoneStatusAsOfficial(it)) }
        )
    
        }}
}

sealed interface SettingsEvent {
    @JvmInline
    value class ShowForcePeakRefreshRatePreference(val value: Boolean) : SettingsEvent

    @JvmInline
    value class SupportOutdoorMode(val value: Boolean) : SettingsEvent

    @JvmInline
    value class ShowMoreBatteryInfo(val value: Boolean) : SettingsEvent

    @JvmInline
    value class ShowPackageInfo(val value: Boolean) : SettingsEvent

    @JvmInline
    value class ShowWiFiLinkSpeed(val value: Boolean) : SettingsEvent

    @JvmInline
    value class SupportAnyFont(val value: Boolean) : SettingsEvent

    @JvmInline
    value class SupportAutoPowerOnOff(val value: Boolean) : SettingsEvent

    @JvmInline
    value class SpoofPhoneStatusAsOfficial(val value: Boolean) : SettingsEvent
}

fun OneuiXPToolsViewModel.onSettingsEvent(event: SettingsEvent) {
    updateData { preference ->
        when (event) {
            is SettingsEvent.ShowForcePeakRefreshRatePreference -> preference.copy(
                settings = preference.settings.copy(
                    showForcePeakRefreshRatePreference = event.value
                )
            )

            is SettingsEvent.SupportOutdoorMode -> preference.copy(
                settings = preference.settings.copy(
                    supportOutdoorMode = event.value
                )
            )

            is SettingsEvent.ShowMoreBatteryInfo -> preference.copy(
                settings = preference.settings.copy(
                    showMoreBatteryInfo = event.value
                )
            )

            is SettingsEvent.ShowPackageInfo -> preference.copy(
                settings = preference.settings.copy(
                    showPackageInfo = event.value
                )
            )

            is SettingsEvent.ShowWiFiLinkSpeed -> preference.copy(
                settings = preference.settings.copy(
                    showWiFiLinkSpeed = event.value
                )
            )

            is SettingsEvent.SupportAnyFont -> preference.copy(
                settings = preference.settings.copy(
                    supportAnyFont = event.value
                )
            )
            is SettingsEvent.SupportAutoPowerOnOff -> preference.copy(
                settings = preference.settings.copy(
                    supportAutoPowerOnOff = event.value
                )
            )

            is SettingsEvent.SpoofPhoneStatusAsOfficial -> preference.copy(
                settings = preference.settings.copy(
                    spoofPhoneStatusAsOfficial = event.value
                )
            )
        }
    }
}
