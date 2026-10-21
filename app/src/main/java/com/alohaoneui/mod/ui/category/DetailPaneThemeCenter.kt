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
fun DetailPaneThemeCenter(
    uiState: Preference.Other,
    onEvent: (ThemeCenterEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.format_paint),
            title = stringResource(id = R.string.alohaoneui_setThemeTrialNeverExpired_title),
            checked = uiState.setThemeTrialNeverExpired,
            onCheckedChange = { onEvent(ThemeCenterEvent.SetThemeTrialNeverExpired(it)) }
        )
    }
}

sealed interface ThemeCenterEvent {
    @JvmInline
    value class SetThemeTrialNeverExpired(val value: Boolean) : ThemeCenterEvent
}

fun AlohaOneUIToolsViewModel.onThemeCenterEvent(event: ThemeCenterEvent) {
    updateData { preference ->
        when (event) {
            is ThemeCenterEvent.SetThemeTrialNeverExpired -> preference.copy(
                other = preference.other.copy(
                    setThemeTrialNeverExpired = event.value
                )
            )
        }
    }
}
