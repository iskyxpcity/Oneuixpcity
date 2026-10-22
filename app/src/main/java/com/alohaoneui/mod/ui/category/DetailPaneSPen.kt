package com.alohaoneui.mod.ui.category

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.alohaoneui.AlohaOneUICategory
import com.alohaoneui.app.ui.screen.tools.rememberAlohaOneUICategoryIcon

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alohaoneui.app.R
import com.alohaoneui.common.Preference
import com.alohaoneui.app.ui.viewmodel.AlohaOneUIToolsViewModel
import com.alohaoneui.mod.ui.component.SwitchItem

@Composable
fun DetailPaneSPen(
    uiState: Preference.Other,
    onEvent: (SPenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            leadingContent = {
                Image(
                    bitmap = rememberAlohaOneUICategoryIcon(AlohaOneUICategory.SPen),
                    contentDescription = stringResource(id = R.string.alohaoneui_useSPenGoogleTranslate_title),
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit,
                )
            },
            title = stringResource(id = R.string.alohaoneui_useSPenGoogleTranslate_title),
            summary = stringResource(id = R.string.alohaoneui_useSPenGoogleTranslate_summary),
            checked = uiState.useSPenGoogleTranslate,
            onCheckedChange = { onEvent(SPenEvent.UseSPenGoogleTranslate(it)) }
        )
    }
}

sealed interface SPenEvent {
    @JvmInline
    value class UseSPenGoogleTranslate(val value: Boolean) : SPenEvent
}

fun AlohaOneUIToolsViewModel.onSPenEvent(event: SPenEvent) {
    updateData { preference ->
        when (event) {
            is SPenEvent.UseSPenGoogleTranslate -> preference.copy(
                other = preference.other.copy(
                    useSPenGoogleTranslate = event.value
                )
            )
        }
    }
}
