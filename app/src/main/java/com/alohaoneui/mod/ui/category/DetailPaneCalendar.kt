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
fun DetailPaneCalendar(
    uiState: Preference.Other,
    onEvent: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(
        modifier = modifier,
        content = buildList {
            add {
                SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.today),
                            title = stringResource(id = R.string.alohaoneui_enableChineseHolidayDisplay_title),
                            checked = uiState.enableChineseHolidayDisplay,
                            onCheckedChange = { onEvent(CalendarEvent.EnableChineseHolidayDisplay(it)) }
                        )
            }
        }
    )
}

sealed interface CalendarEvent {
    @JvmInline
    value class EnableChineseHolidayDisplay(val value: Boolean) : CalendarEvent
}

fun AlohaOneUIToolsViewModel.onCalendarEvent(event: CalendarEvent) {
    updateData { preference ->
        when (event) {
            is CalendarEvent.EnableChineseHolidayDisplay -> preference.copy(
                other = preference.other.copy(
                    enableChineseHolidayDisplay = event.value
                )
            )
        }
    }
}PackagePane(
        modifier = modifier,
        content = buildList {
            add {
                .mod.ui.category
            }
            add {
                import androidx.compose.runtime.Composable
            }
            add {
                import androidx.compose.ui.Modifier
            }
            add {
                import androidx.compose.ui.graphics.vector.ImageVector
            }
            add {
                import androidx.compose.ui.res.stringResource
            }
            add {
                import androidx.compose.ui.res.vectorResource
            }
            add {
                import com.alohaoneui.app.R
            }
            add {
                import com.alohaoneui.common.Preference
            }
            add {
                import com.alohaoneui.app.ui.viewmodel.AlohaOneUIToolsViewModel
            }
            add {
                import com.alohaoneui.mod.ui.component.SwitchItem
            }
            add {
                @Composable
            }
            add {
                fun DetailPaneCalendar(
                    uiState: Preference.Other,
                    onEvent: (CalendarEvent) -> Unit,
                    modifier: Modifier = Modifier
                ) {
                    PackagePane(
                        modifier = modifier,
                        content = buildList {
                            add {
                                SwitchItem(
                                            icon = ImageVector.vectorResource(id = R.drawable.today),
                                            title = stringResource(id = R.string.alohaoneui_enableChineseHolidayDisplay_title),
                                            checked = uiState.enableChineseHolidayDisplay,
                                            onCheckedChange = { onEvent(CalendarEvent.EnableChineseHolidayDisplay(it)) }
                                        )
                            }
                        }
                    )
            }
        }
    )

sealed interface CalendarEvent {
    @JvmInline
    value class EnableChineseHolidayDisplay(val value: Boolean) : CalendarEvent
}

fun AlohaOneUIToolsViewModel.onCalendarEvent(event: CalendarEvent) {
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
