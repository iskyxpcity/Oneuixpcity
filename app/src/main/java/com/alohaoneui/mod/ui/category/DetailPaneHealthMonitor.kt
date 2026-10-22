package com.alohaoneui.mod.ui.category

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.alohaoneui.AlohaOneUICategory
import com.alohaoneui.app.ui.screen.tools.rememberAlohaOneUICategoryIcon

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alohaoneui.app.R
import com.alohaoneui.common.Preference
import com.alohaoneui.app.ui.viewmodel.AlohaOneUIToolsViewModel
import com.alohaoneui.mod.ui.component.SwitchItem

@Composable
fun DetailPaneHealthMonitor(
    uiState: Preference.Other,
    onEvent: (HealthMonitorEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            leadingContent = {
                Image(
                    bitmap = rememberAlohaOneUICategoryIcon(AlohaOneUICategory.HealthMonitor),
                    contentDescription = stringResource(id = R.string.alohaoneui_bypassHealthMonitorCountryCheck_title),
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit,
                )
            },
            title = stringResource(id = R.string.alohaoneui_bypassHealthMonitorCountryCheck_title),
            summary = stringResource(id = R.string.alohaoneui_bypassHealthMonitorCountryCheck_summary),
            checked = uiState.bypassHealthMonitorCountryCheck,
            onCheckedChange = { onEvent(HealthMonitorEvent.BypassHealthMonitorCountryCheck(it)) }
        )
    }
}

sealed interface HealthMonitorEvent {
    @JvmInline
    value class BypassHealthMonitorCountryCheck(val value: Boolean) : HealthMonitorEvent
}

fun AlohaOneUIToolsViewModel.onHealthMonitorEvent(event: HealthMonitorEvent) {
    updateData { preference ->
        when (event) {
            is HealthMonitorEvent.BypassHealthMonitorCountryCheck -> preference.copy(
                other = preference.other.copy(
                    bypassHealthMonitorCountryCheck = event.value
                )
            )
        }
    }
}
