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
fun DetailPaneVideo(
    uiState: Preference.Other,
    onEvent: (VideoEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.fast_forward),
            title = stringResource(id = R.string.oneuialoha_showMorePlaybackSpeeds_title),
            summary = stringResource(id = R.string.oneuialoha_showMorePlaybackSpeeds_summary),
            checked = uiState.showMorePlaybackSpeeds,
            onCheckedChange = { onEvent(VideoEvent.ShowMorePlaybackSpeeds(it)) }
        )
    }
}

sealed interface VideoEvent {
    @JvmInline
    value class ShowMorePlaybackSpeeds(val value: Boolean) : VideoEvent
}

fun OneUIAlohaToolsViewModel.onVideoEvent(event: VideoEvent) {
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
