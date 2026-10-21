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
fun DetailPaneCalendar(
    uiState: Preference.Other,
    onEvent: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.today),
            title = stringResource(id = R.string.oneuialoha_enableChineseHolidayDisplay_title),
            checked = uiState.enableChineseHolidayDisplay,
            onCheckedChange = { onEvent(CalendarEvent.EnableChineseHolidayDisplay(it)) }
        )
    }
}

sealed interface CalendarEvent {
    @JvmInline
    value class EnableChineseHolidayDisplay(val value: Boolean) : CalendarEvent
}

fun OneUIAlohaToolsViewModel.onCalendarEvent(event: CalendarEvent) {
    updateData { preference ->
        when (event) {
            is CalendarEvent.EnableChineseHolidayDisplay -> preference.copy(
                other = preference.other.copy(
                    enableChineseHolidayDisplay = event.value
                )
            )
        }
    }
}
