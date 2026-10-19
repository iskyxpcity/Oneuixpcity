package com.oneuixp.mod.ui.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuixp.app.R
import com.oneuixp.common.Preference
import com.oneuixp.app.ui.viewmodel.OneuiXPToolsViewModel
import com.oneuixp.mod.ui.component.SwitchItem
import com.oneuixp.mod.ui.component.OneuiXPDetailGroup

@Composable
fun DetailPaneCamera(
    uiState: Preference.Camera,
    onEvent: (CameraEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        OneuiXPDetailGroup {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.camera),
            title = stringResource(id = R.string.oneuixp_supportAllCameraMenu_title),
            summary = stringResource(id = R.string.oneuixp_supportAllCameraMenu_summary),
            checked = uiState.supportAllCameraMenu,
            onCheckedChange = { onEvent(CameraEvent.SupportAllCameraMenu(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.camera),
            title = stringResource(id = R.string.oneuixp_disableCameraTemperatureCheck_title),
            checked = uiState.disableCameraTemperatureCheck,
            onCheckedChange = { onEvent(CameraEvent.DisableCameraTemperatureCheck(it)) }
        )
    
        }}
}

sealed interface CameraEvent {
    @JvmInline
    value class SupportAllCameraMenu(val value: Boolean) : CameraEvent

    @JvmInline
    value class DisableCameraTemperatureCheck(val value: Boolean) : CameraEvent
}

fun OneuiXPToolsViewModel.onCameraEvent(event: CameraEvent) {
    updateData { preference ->
        when (event) {
            is CameraEvent.SupportAllCameraMenu -> {
                preference.copy(
                    camera = preference.camera.copy(
                        supportAllCameraMenu = event.value
                    )
                )
            }

            is CameraEvent.DisableCameraTemperatureCheck -> {
                preference.copy(
                    camera = preference.camera.copy(
                        disableCameraTemperatureCheck = event.value
                    )
                )
            }
        }
    }
}
