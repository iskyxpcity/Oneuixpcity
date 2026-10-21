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
fun DetailPaneWeather(
    uiState: Preference.Other,
    onEvent: (WeatherEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.partly_cloudy_day),
            title = stringResource(id = R.string.alohaoneui_setWeatherProviderCN_title),
            checked = uiState.setWeatherProviderCN,
            onCheckedChange = { onEvent(WeatherEvent.SetWeatherProviderCN(it)) }
        )
    }
}

sealed interface WeatherEvent {
    @JvmInline
    value class SetWeatherProviderCN(val value: Boolean) : WeatherEvent
}

fun AlohaOneUIToolsViewModel.onWeatherEvent(event: WeatherEvent) {
    updateData { preference ->
        when (event) {
            is WeatherEvent.SetWeatherProviderCN -> preference.copy(
                other = preference.other.copy(
                    setWeatherProviderCN = event.value
                )
            )
        }
    }
}
