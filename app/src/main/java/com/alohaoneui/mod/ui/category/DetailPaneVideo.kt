package com.alohaoneui.mod.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.alohaoneui.app.R
import com.alohaoneui.common.Preference
import com.alohaoneui.app.ui.viewmodel.AlohaOneUIToolsViewModel
import com.alohaoneui.mod.ui.component.SwitchItem

@Composable
fun DetailPaneVideo(
    uiState: Preference.Other,
    onEvent: (VideoEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.fast_forward),
            title = stringResource(id = R.string.alohaoneui_showMorePlaybackSpeeds_title),
            summary = stringResource(id = R.string.alohaoneui_showMorePlaybackSpeeds_summary),
            checked = uiState.showMorePlaybackSpeeds,
            onCheckedChange = { onEvent(VideoEvent.ShowMorePlaybackSpeeds(it)) }
        )
    }
}

sealed interface VideoEvent {
    @JvmInline
    value class ShowMorePlaybackSpeeds(val value: Boolean) : VideoEvent
}

fun AlohaOneUIToolsViewModel.onVideoEvent(event: VideoEvent) {
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
