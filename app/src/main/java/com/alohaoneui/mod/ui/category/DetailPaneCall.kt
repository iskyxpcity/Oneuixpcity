package com.alohaoneui.mod.ui.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.alohaoneui.app.R
import com.alohaoneui.common.Preference
import com.alohaoneui.app.ui.viewmodel.AlohaOneUIToolsViewModel
import com.alohaoneui.mod.ui.component.SwitchItem
import com.alohaoneui.mod.ui.component.AlohaOneUIDetailGroup

@Composable
fun DetailPaneCall(
    uiState: Preference.Call,
    onEvent: (CallEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AlohaOneUIDetailGroup {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.call_recording),
            title = stringResource(id = R.string.alohaoneui_supportVoiceCallRecording_title),
            checked = uiState.supportVoiceCallRecording,
            onCheckedChange = { onEvent(CallEvent.SupportVoiceCallRecording(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.call_recording),
            title = stringResource(id = R.string.alohaoneui_preferRecordingButton_title),
            checked = uiState.preferRecordingButton,
            onCheckedChange = { onEvent(CallEvent.PreferRecordingButton(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.person_pin_circle),
            title = stringResource(id = R.string.alohaoneui_showGeocodedLocationInRecentCall_title),
            checked = uiState.showGeocodedLocationInRecentCall,
            onCheckedChange = { onEvent(CallEvent.ShowGeocodedLocationInRecentCall(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.call_log),
            title = stringResource(id = R.string.alohaoneui_isOpStyleCHN_title),
            checked = uiState.isOpStyleCHN,
            onCheckedChange = { onEvent(CallEvent.IsOpStyleCHN(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.phone_forwarded),
            title = stringResource(id = R.string.alohaoneui_supportCallAndTextOnOtherDevices_title),
            summary = stringResource(id = R.string.alohaoneui_supportCallAndTextOnOtherDevices_summary),
            checked = uiState.supportCallAndTextOnOtherDevices,
            onCheckedChange = { onEvent(CallEvent.SupportCallAndTextOnOtherDevices(it)) }
        )
    
        }}
}


sealed interface CallEvent {
    @JvmInline
    value class SupportVoiceCallRecording(val value: Boolean) : CallEvent

    @JvmInline
    value class PreferRecordingButton(val value: Boolean) : CallEvent

    @JvmInline
    value class ShowGeocodedLocationInRecentCall(val value: Boolean) : CallEvent

    @JvmInline
    value class IsOpStyleCHN(val value: Boolean) : CallEvent

    @JvmInline
    value class SupportCallAndTextOnOtherDevices(val value: Boolean) : CallEvent
}

fun AlohaOneUIToolsViewModel.onCallEvent(event: CallEvent) {
    updateData {
        when (event) {
            is CallEvent.SupportVoiceCallRecording -> {
                it.copy(call = it.call.copy(supportVoiceCallRecording = event.value))
            }

            is CallEvent.PreferRecordingButton -> {
                it.copy(call = it.call.copy(preferRecordingButton = event.value))
            }

            is CallEvent.ShowGeocodedLocationInRecentCall -> {
                it.copy(call = it.call.copy(showGeocodedLocationInRecentCall = event.value))
            }

            is CallEvent.IsOpStyleCHN -> {
                it.copy(call = it.call.copy(isOpStyleCHN = event.value))
            }

            is CallEvent.SupportCallAndTextOnOtherDevices -> {
                it.copy(call = it.call.copy(supportCallAndTextOnOtherDevices = event.value))
            }
        }
    }
}
