package io.github.soclear.oneuix.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
import com.oneuixp.app.ui.viewmodel.OneuiXPToolsViewModel
import io.github.soclear.oneuix.ui.component.SwitchItem

@Composable
fun DetailPaneVideo(
    uiState: Preference.Other,
    onEvent: (VideoEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.fast_forward),
            title = stringResource(id = R.string.oneuixp_showMorePlaybackSpeeds_title),
            summary = stringResource(id = R.string.oneuixp_showMorePlaybackSpeeds_summary),
            checked = uiState.showMorePlaybackSpeeds,
            onCheckedChange = { onEvent(VideoEvent.ShowMorePlaybackSpeeds(it)) }
        )
    }
}

sealed interface VideoEvent {
    @JvmInline
    value class ShowMorePlaybackSpeeds(val value: Boolean) : VideoEvent
}

fun OneuiXPToolsViewModel.onVideoEvent(event: VideoEvent) {
    updateData { preference ->
        when (event) {
            is VideoEvent.ShowMorePlaybackSpeeds -> preference.copy(
                other = preference.other.copy(
                    showMorePlaybackSpeeds = event.value
                )
            )
        }
    }
}
