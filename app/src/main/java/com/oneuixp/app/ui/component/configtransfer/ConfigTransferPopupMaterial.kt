package com.oneuixp.app.ui.component.configtransfer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.oneuixp.app.R

@Composable
fun ConfigTransferPopupMaterial(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Article,
            contentDescription = stringResource(R.string.oneuixp_backup_config),
        )
    }

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
            SelectableDropdownMenuItem(
                selected = false,
                onClick = {
                    expanded = false
                    onBackup()
                },
                text = { Text(stringResource(R.string.oneuixp_backup_config)) },
                shapes = MenuDefaults.itemShape(index = 0, count = 2),
            )
            SelectableDropdownMenuItem(
                selected = false,
                onClick = {
                    expanded = false
                    onRestore()
                },
                text = { Text(stringResource(R.string.oneuixp_restore_config)) },
                shapes = MenuDefaults.itemShape(index = 1, count = 2),
            )
        }
    }
}
