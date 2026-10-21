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
fun DetailPaneDualApp(
    uiState: Preference.Other,
    onEvent: (DualAppEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.apk_document),
            title = stringResource(id = R.string.oneuialoha_makeAllUserAppsAvailable_title),
            checked = uiState.makeAllUserAppsAvailable,
            onCheckedChange = { onEvent(DualAppEvent.MakeAllUserAppsAvailable(it)) }
        )
    }
}

sealed interface DualAppEvent {
    @JvmInline
    value class MakeAllUserAppsAvailable(val value: Boolean) : DualAppEvent
}

fun OneUIAlohaToolsViewModel.onDualAppEvent(event: DualAppEvent) {
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
