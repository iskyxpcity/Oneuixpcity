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
fun DetailPaneNotes(
    uiState: Preference.Other,
    onEvent: (NotesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.notes),
            title = stringResource(id = R.string.oneuixp_supportAllNotesFeatures_title),
            summary = stringResource(id = R.string.oneuixp_supportAllNotesFeatures_summary),
            checked = uiState.supportAllNotesFeatures,
            onCheckedChange = { onEvent(NotesEvent.SupportAllNotesFeatures(it)) }
        )
    }
}

sealed interface NotesEvent {
    @JvmInline
    value class SupportAllNotesFeatures(val value: Boolean) : NotesEvent
}

fun OneuiXPToolsViewModel.onNotesEvent(event: NotesEvent) {
    updateData { preference ->
        when (event) {
            is NotesEvent.SupportAllNotesFeatures -> preference.copy(
                other = preference.other.copy(
                    supportAllNotesFeatures = event.value
                )
            )
        }
    }
}
