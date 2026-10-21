package com.oneuialoha.mod.ui.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuialoha.app.R
import com.oneuialoha.common.Preference
import com.oneuialoha.app.ui.viewmodel.OneUIAlohaToolsViewModel
import com.oneuialoha.mod.ui.component.SwitchItem
import com.oneuialoha.mod.ui.component.OneUIAlohaDetailGroup

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
        OneUIAlohaDetailGroup {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.call_recording),
            title = stringResource(id = R.string.oneuialoha_supportVoiceCallRecording_title),
            checked = uiState.supportVoiceCallRecording,
            onCheckedChange = { onEvent(CallEvent.SupportVoiceCallRecording(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.call_recording),
            title = stringResource(id = R.string.oneuialoha_preferRecordingButton_title),
            checked = uiState.preferRecordingButton,
            onCheckedChange = { onEvent(CallEvent.PreferRecordingButton(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.person_pin_circle),
            title = stringResource(id = R.string.oneuialoha_showGeocodedLocationInRecentCall_title),
            checked = uiState.showGeocodedLocationInRecentCall,
            onCheckedChange = { onEvent(CallEvent.ShowGeocodedLocationInRecentCall(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.call_log),
            title = stringResource(id = R.string.oneuialoha_isOpStyleCHN_title),
            checked = uiState.isOpStyleCHN,
            onCheckedChange = { onEvent(CallEvent.IsOpStyleCHN(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.phone_forwarded),
            title = stringResource(id = R.string.oneuialoha_supportCallAndTextOnOtherDevices_title),
            summary = stringResource(id = R.string.oneuialoha_supportCallAndTextOnOtherDevices_summary),
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

fun OneUIAlohaToolsViewModel.onCallEvent(event: CallEvent) {
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
