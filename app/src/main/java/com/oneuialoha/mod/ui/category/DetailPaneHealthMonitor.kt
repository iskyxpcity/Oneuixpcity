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
fun DetailPaneHealthMonitor(
    uiState: Preference.Other,
    onEvent: (HealthMonitorEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.health_metrics),
            title = stringResource(id = R.string.oneuialoha_bypassHealthMonitorCountryCheck_title),
            summary = stringResource(id = R.string.oneuialoha_bypassHealthMonitorCountryCheck_summary),
            checked = uiState.bypassHealthMonitorCountryCheck,
            onCheckedChange = { onEvent(HealthMonitorEvent.BypassHealthMonitorCountryCheck(it)) }
        )
    }
}

sealed interface HealthMonitorEvent {
    @JvmInline
    value class BypassHealthMonitorCountryCheck(val value: Boolean) : HealthMonitorEvent
}

fun OneUIAlohaToolsViewModel.onHealthMonitorEvent(event: HealthMonitorEvent) {
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
