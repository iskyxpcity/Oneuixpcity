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
fun DetailPaneDualApp(
    uiState: Preference.Other,
    onEvent: (DualAppEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.apk_document),
            title = stringResource(id = R.string.alohaoneui_makeAllUserAppsAvailable_title),
            checked = uiState.makeAllUserAppsAvailable,
            onCheckedChange = { onEvent(DualAppEvent.MakeAllUserAppsAvailable(it)) }
        )
    }
}

sealed interface DualAppEvent {
    @JvmInline
    value class MakeAllUserAppsAvailable(val value: Boolean) : DualAppEvent
}

fun AlohaOneUIToolsViewModel.onDualAppEvent(event: DualAppEvent) {
    updateData { preference ->
        when (event) {
            is DualAppEvent.MakeAllUserAppsAvailable -> preference.copy(
                other = preference.other.copy(
                    makeAllUserAppsAvailable = event.value
                )
            )
        }
    }
}
