package com.oneuixp.mod.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuixp.app.R
import com.oneuixp.common.Preference
import com.oneuixp.app.ui.viewmodel.OneuiXPToolsViewModel
import com.oneuixp.mod.ui.component.SwitchItem

@Composable
fun DetailPaneBrowser(
    uiState: Preference.Other,
    onEvent: (BrowserEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.fast_forward),
            title = stringResource(id = R.string.oneuixp_showMorePlaybackSpeeds_title),
            summary = stringResource(id = R.string.oneuixp_showMorePlaybackSpeeds_summary),
            checked = uiState.showMorePlaybackSpeeds,
            onCheckedChange = { onEvent(BrowserEvent.ShowMorePlaybackSpeeds(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.tab_move),
            title = stringResource(id = R.string.oneuixp_redirect_custom_tab_title),
            summary = stringResource(id = R.string.oneuixp_redirect_custom_tab_summary),
            checked = uiState.redirectCustomTab,
            onCheckedChange = { onEvent(BrowserEvent.RedirectCustomTab(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.language_us),
            title = stringResource(id = R.string.oneuixp_spoofBrowserCountryCodeToUS_title),
            summary = stringResource(id = R.string.oneuixp_spoofBrowserCountryCodeToUS_summary),
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

fun OneuiXPToolsViewModel.onBrowserEvent(event: BrowserEvent) {
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
