package io.github.soclear.oneuix.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
import com.oneuixp.app.ui.viewmodel.OneuiXPToolsViewModel
import io.github.soclear.oneuix.ui.component.SwitchItem

@Composable
fun DetailPaneMessaging(
    uiState: Preference.Other,
    onEvent: (MessagingEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.sms),
            title = stringResource(id = R.string.oneuixp_supportBlockMessage_title),
            checked = uiState.supportBlockMessage,
            onCheckedChange = { onEvent(MessagingEvent.SupportBlockMessage(it)) }
        )
    }
}

sealed interface MessagingEvent {
    @JvmInline
    value class SupportBlockMessage(val value: Boolean) : MessagingEvent
}

fun OneuiXPToolsViewModel.onMessagingEvent(event: MessagingEvent) {
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
