package com.alohaoneui.app.ui.component.configtransfer

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.component.material.ExpressiveIconButton
import com.alohaoneui.app.ui.component.material.alohaCornerShape
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowContainers

@Composable
fun ConfigTransferPopupMaterial(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExpressiveIconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Article,
            contentDescription = stringResource(R.string.alohaoneui_backup_config),
        )
    }

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShapes(
                shape = alohaCornerShape(20.dp),
                inactiveShape = alohaCornerShape(20.dp),
            ),
            shadowElevation = if (LocalMaterialShadowContainers.current) {
                MenuDefaults.ShadowElevation
            } else 0.dp,
            border = LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                BorderStroke(it.dp, MaterialTheme.colorScheme.outlineVariant)
            },
        ) {
            SelectableDropdownMenuItem(
                selected = false,
                onClick = {
                    expanded = false
                    onBackup()
                },
                text = { Text(stringResource(R.string.alohaoneui_backup_config)) },
                shapes = MenuDefaults.itemShape(index = 0, count = 2),
            )
            SelectableDropdownMenuItem(
                selected = false,
                onClick = {
                    expanded = false
                    onRestore()
                },
                text = { Text(stringResource(R.string.alohaoneui_restore_config)) },
                shapes = MenuDefaults.itemShape(index = 1, count = 2),
            )
        }
    }
}
