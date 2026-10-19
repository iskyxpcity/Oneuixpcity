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
fun DetailPanePhotoRetouching(
    uiState: Preference.Other,
    onEvent: (PhotoRetouchingEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.branding_watermark),
            title = stringResource(id = R.string.oneuixp_noAIWatermark_title),
            summary = stringResource(id = R.string.oneuixp_noAIWatermark_summary),
            checked = uiState.noAIWatermark,
            onCheckedChange = { onEvent(PhotoRetouchingEvent.NoAIWatermark(it)) }
        )
    }
}

sealed interface PhotoRetouchingEvent {
    @JvmInline
    value class NoAIWatermark(val value: Boolean) : PhotoRetouchingEvent
}

fun OneuiXPToolsViewModel.onPhotoRetouchingEvent(event: PhotoRetouchingEvent) {
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
