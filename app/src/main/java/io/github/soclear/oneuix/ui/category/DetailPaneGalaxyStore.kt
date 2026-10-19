package io.github.soclear.oneuix.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
import com.oneuixp.app.ui.viewmodel.OneuiXToolsViewModel
import io.github.soclear.oneuix.ui.component.OneuiXInfoItem

@Suppress("UNUSED_PARAMETER")
@Composable
fun DetailPaneGalaxyStore(
    uiState: Preference.Other,
    onEvent: (GalaxyStoreEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        OneuiXInfoItem(
            title = stringResource(id = R.string.oneuix_disableGalaxyStoreRecommendations_moved_title),
            summary = stringResource(id = R.string.oneuix_disableGalaxyStoreRecommendations_moved_summary),
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
