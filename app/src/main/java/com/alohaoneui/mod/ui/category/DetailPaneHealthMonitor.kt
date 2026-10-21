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
fun DetailPaneHealthMonitor(
    uiState: Preference.Other,
    onEvent: (HealthMonitorEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.health_metrics),
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
