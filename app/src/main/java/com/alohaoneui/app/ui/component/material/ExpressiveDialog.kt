package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowContainers
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun ExpressiveDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape? = null,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
 ) {
    val dialogShape = shape ?: voyageCornerShape(28.dp)
    val dialogModifier = modifier.then(
        LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
            Modifier.border(it.dp, MaterialTheme.colorScheme.outlineVariant, dialogShape)
        } ?: Modifier
    )
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = dialogModifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = dialogShape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = if (LocalMaterialShadowContainers.current) tonalElevation else 0.dp,
        properties = properties,
    )
}

@Composable
fun ExpressiveConfirmDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    dismissText: String? = null,
) {
    ExpressiveDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = message?.let { { Text(it) } },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = dismissText?.let {
            { TextButton(onClick = onDismiss) { Text(it) } }
        },
    )
}

/**
 * Theme-native dialog frame for settings popups that need arbitrary interactive content.
 *
 * Keep this alongside ExpressiveDialog so custom pickers use the same Material 3 shape,
 * container, typography colors, and tonal elevation as the app's standard dialogs.
 */
@Composable
fun ExpressiveContentDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val dialogShape = voyageCornerShape(28.dp)
    val dialogModifier = modifier.then(
        LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
            Modifier.border(it.dp, MaterialTheme.colorScheme.outlineVariant, dialogShape)
        } ?: Modifier
    )
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = dialogModifier,
        properties = properties,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = dialogShape,
            color = AlertDialogDefaults.containerColor,
            contentColor = AlertDialogDefaults.textContentColor,
            tonalElevation = if (LocalMaterialShadowContainers.current) AlertDialogDefaults.TonalElevation else 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                content = content,
            )
        }
    }
}
