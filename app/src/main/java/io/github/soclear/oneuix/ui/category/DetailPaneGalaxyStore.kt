package io.github.soclear.oneuix.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
import com.oneuixp.app.ui.viewmodel.OneuiXToolsViewModel
import io.github.soclear.oneuix.ui.component.SwitchItem

@Composable
fun DetailPaneGalaxyStore(
    uiState: Preference.Other,
    onEvent: (GalaxyStoreEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.ad_off),
            title = stringResource(id = R.string.oneuix_blockGalaxyStoreAds_title),
            summary = stringResource(id = R.string.oneuix_blockGalaxyStoreAds_summary),
            checked = uiState.blockGalaxyStoreAds,
            onCheckedChange = { onEvent(GalaxyStoreEvent.BlockGalaxyStoreAds(it)) }
        )
    }
}

sealed interface GalaxyStoreEvent {
    @JvmInline
    value class BlockGalaxyStoreAds(val value: Boolean) : GalaxyStoreEvent
}

fun OneuiXToolsViewModel.onGalaxyStoreEvent(event: GalaxyStoreEvent) {
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
