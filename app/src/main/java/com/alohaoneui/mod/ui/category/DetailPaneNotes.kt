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
fun DetailPaneNotes(
    uiState: Preference.Other,
    onEvent: (NotesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(
        modifier = modifier,
        content = buildList {
            add {
                SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.notes),
                            title = stringResource(id = R.string.alohaoneui_supportAllNotesFeatures_title),
                            summary = stringResource(id = R.string.alohaoneui_supportAllNotesFeatures_summary),
                            checked = uiState.supportAllNotesFeatures,
                            onCheckedChange = { onEvent(NotesEvent.SupportAllNotesFeatures(it)) }
                        )
            }
        }
    )
}

sealed interface NotesEvent {
    @JvmInline
    value class SupportAllNotesFeatures(val value: Boolean) : NotesEvent
}

fun AlohaOneUIToolsViewModel.onNotesEvent(event: NotesEvent) {
    updateData { preference ->
        when (event) {
            is NotesEvent.SupportAllNotesFeatures -> preference.copy(
                other = preference.other.copy(
                    supportAllNotesFeatures = event.value
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
                fun DetailPaneNotes(
                    uiState: Preference.Other,
                    onEvent: (NotesEvent) -> Unit,
                    modifier: Modifier = Modifier
                ) {
                    PackagePane(
                        modifier = modifier,
                        content = buildList {
                            add {
                                SwitchItem(
                                            icon = ImageVector.vectorResource(id = R.drawable.notes),
                                            title = stringResource(id = R.string.alohaoneui_supportAllNotesFeatures_title),
                                            summary = stringResource(id = R.string.alohaoneui_supportAllNotesFeatures_summary),
                                            checked = uiState.supportAllNotesFeatures,
                                            onCheckedChange = { onEvent(NotesEvent.SupportAllNotesFeatures(it)) }
                                        )
                            }
                        }
                    )
            }
        }
    )

sealed interface NotesEvent {
    @JvmInline
    value class SupportAllNotesFeatures(val value: Boolean) : NotesEvent
}

fun AlohaOneUIToolsViewModel.onNotesEvent(event: NotesEvent) {
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
