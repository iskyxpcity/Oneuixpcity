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
fun DetailPanePhotoRetouching(
    uiState: Preference.Other,
    onEvent: (PhotoRetouchingEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.branding_watermark),
            title = stringResource(id = R.string.alohaoneui_noAIWatermark_title),
            summary = stringResource(id = R.string.alohaoneui_noAIWatermark_summary),
            checked = uiState.noAIWatermark,
            onCheckedChange = { onEvent(PhotoRetouchingEvent.NoAIWatermark(it)) }
        )
    }
}

sealed interface PhotoRetouchingEvent {
    @JvmInline
    value class NoAIWatermark(val value: Boolean) : PhotoRetouchingEvent
}

fun AlohaOneUIToolsViewModel.onPhotoRetouchingEvent(event: PhotoRetouchingEvent) {
    updateData { preference ->
        when (event) {
            is PhotoRetouchingEvent.NoAIWatermark -> preference.copy(
                other = preference.other.copy(
                    noAIWatermark = event.value
                )
            )
        }
    }
}
