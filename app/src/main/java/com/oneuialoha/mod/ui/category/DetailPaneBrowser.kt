package com.oneuialoha.mod.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuialoha.app.R
import com.oneuialoha.common.Preference
import com.oneuialoha.app.ui.viewmodel.OneUIAlohaToolsViewModel
import com.oneuialoha.mod.ui.component.SwitchItem

@Composable
fun DetailPaneBrowser(
    uiState: Preference.Other,
    onEvent: (BrowserEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.fast_forward),
            title = stringResource(id = R.string.oneuialoha_showMorePlaybackSpeeds_title),
            summary = stringResource(id = R.string.oneuialoha_showMorePlaybackSpeeds_summary),
            checked = uiState.showMorePlaybackSpeeds,
            onCheckedChange = { onEvent(BrowserEvent.ShowMorePlaybackSpeeds(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.tab_move),
            title = stringResource(id = R.string.oneuialoha_redirect_custom_tab_title),
            summary = stringResource(id = R.string.oneuialoha_redirect_custom_tab_summary),
            checked = uiState.redirectCustomTab,
            onCheckedChange = { onEvent(BrowserEvent.RedirectCustomTab(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.language_us),
            title = stringResource(id = R.string.oneuialoha_spoofBrowserCountryCodeToUS_title),
            summary = stringResource(id = R.string.oneuialoha_spoofBrowserCountryCodeToUS_summary),
            checked = uiState.spoofBrowserCountryCodeToUS,
            onCheckedChange = { onEvent(BrowserEvent.SpoofBrowserCountryCodeToUS(it)) }
        )
    }
}

sealed interface BrowserEvent {
    @JvmInline
    value class ShowMorePlaybackSpeeds(val value: Boolean) : BrowserEvent

    @JvmInline
    value class RedirectCustomTab(val value: Boolean) : BrowserEvent

    @JvmInline
    value class SpoofBrowserCountryCodeToUS(val value: Boolean) : BrowserEvent
}

fun OneUIAlohaToolsViewModel.onBrowserEvent(event: BrowserEvent) {
    updateData { preference ->
        when (event) {
            is BrowserEvent.ShowMorePlaybackSpeeds -> preference.copy(
                other = preference.other.copy(
                    showMorePlaybackSpeeds = event.value
                )
            )

            is BrowserEvent.RedirectCustomTab -> preference.copy(
                other = preference.other.copy(
                    redirectCustomTab = event.value
                )
            )

            is BrowserEvent.SpoofBrowserCountryCodeToUS -> preference.copy(
                other = preference.other.copy(
                    spoofBrowserCountryCodeToUS = event.value
                )
            )
        }
    }
}
