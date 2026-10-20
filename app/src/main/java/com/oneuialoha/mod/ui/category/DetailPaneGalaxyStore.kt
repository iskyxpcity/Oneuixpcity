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
fun DetailPaneGalaxyStore(
    uiState: Preference.Other,
    onEvent: (GalaxyStoreEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.ad_off),
            title = stringResource(id = R.string.oneuialoha_blockGalaxyStoreAds_title),
            summary = stringResource(id = R.string.oneuialoha_blockGalaxyStoreAds_summary),
            checked = uiState.blockGalaxyStoreAds,
            onCheckedChange = { onEvent(GalaxyStoreEvent.BlockGalaxyStoreAds(it)) }
        )
    }
}

sealed interface GalaxyStoreEvent {
    @JvmInline
    value class BlockGalaxyStoreAds(val value: Boolean) : GalaxyStoreEvent
}

fun OneUIAlohaToolsViewModel.onGalaxyStoreEvent(event: GalaxyStoreEvent) {
    updateData { preference ->
        when (event) {
            is GalaxyStoreEvent.BlockGalaxyStoreAds -> preference.copy(
                other = preference.other.copy(
                    blockGalaxyStoreAds = event.value
                )
            )
        }
    }
}
