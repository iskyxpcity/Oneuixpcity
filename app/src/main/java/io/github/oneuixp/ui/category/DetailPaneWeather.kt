package io.github.oneuixp.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuixp.app.R
import io.github.oneuixp.common.Preference
import com.oneuixp.app.ui.viewmodel.OneuiXPToolsViewModel
import io.github.oneuixp.ui.component.SwitchItem

@Composable
fun DetailPaneWeather(
    uiState: Preference.Other,
    onEvent: (WeatherEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.partly_cloudy_day),
            title = stringResource(id = R.string.oneuixp_setWeatherProviderCN_title),
            checked = uiState.setWeatherProviderCN,
            onCheckedChange = { onEvent(WeatherEvent.SetWeatherProviderCN(it)) }
        )
    }
}

sealed interface WeatherEvent {
    @JvmInline
    value class SetWeatherProviderCN(val value: Boolean) : WeatherEvent
}

fun OneuiXPToolsViewModel.onWeatherEvent(event: WeatherEvent) {
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
