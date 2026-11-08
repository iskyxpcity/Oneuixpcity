package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth

/**
 * Default Material snackbar styled by the same Aloha shape and outline tokens
 * used by the rest of the shared Material component layer.
 */
@Composable
private fun ExpressiveSnackbar(data: SnackbarData) {
    val shape = voyageCornerShape(20.dp)
    val outlineWidth = LocalMaterialOutlineWidth.current
    Snackbar(
        snackbarData = data,
        modifier = Modifier.then(
            outlineWidth.takeIf { it > 0f }?.let {
                Modifier.border(
                    BorderStroke(it.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape,
                )
            } ?: Modifier
        ),
        shape = shape,
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        actionColor = MaterialTheme.colorScheme.inversePrimary,
        dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface,
    )
}

@Composable
fun SnackBarHost(
    modifier: Modifier = Modifier,
    hostState: SnackbarHostState,
    snackBar: @Composable (SnackbarData) -> Unit = { ExpressiveSnackbar(it) },
) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        backgroundContent = {},
        onDismiss = {
            hostState.currentSnackbarData?.dismiss()
        }
    ) {
        SnackbarHost(
            modifier = modifier,
            hostState = hostState,
            snackbar = snackBar,
        )
    }
    LaunchedEffect(hostState.currentSnackbarData) {
        if (hostState.currentSnackbarData == null) return@LaunchedEffect
        state.snapTo(SwipeToDismissBoxValue.Settled)
    }
}
