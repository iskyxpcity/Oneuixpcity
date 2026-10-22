package com.alohaoneui.mod.ui.category

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
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
fun DetailPaneMessaging(
    uiState: Preference.Other,
    onEvent: (MessagingEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            leadingContent = {
                Image(
                    bitmap = rememberAlohaOneUICategoryIcon(AlohaOneUICategory.Messaging),
                    contentDescription = stringResource(id = R.string.alohaoneui_supportBlockMessage_title),
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit,
                )
            },
            title = stringResource(id = R.string.alohaoneui_supportBlockMessage_title),
            checked = uiState.supportBlockMessage,
            onCheckedChange = { onEvent(MessagingEvent.SupportBlockMessage(it)) }
        )
    }
}

sealed interface MessagingEvent {
    @JvmInline
    value class SupportBlockMessage(val value: Boolean) : MessagingEvent
}

fun AlohaOneUIToolsViewModel.onMessagingEvent(event: MessagingEvent) {
    updateData { preference ->
        when (event) {
            is MessagingEvent.SupportBlockMessage -> preference.copy(
                other = preference.other.copy(
                    supportBlockMessage = event.value
                )
            )
        }
    }
}
