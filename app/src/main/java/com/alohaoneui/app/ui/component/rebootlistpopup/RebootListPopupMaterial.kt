package com.alohaoneui.app.ui.component.rebootlistpopup

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import com.alohaoneui.app.ui.component.material.ExpressiveIconButton
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
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowContainers

@Composable
fun RebootDropdownItems(onItemClick: (String) -> Unit) {
    val options = getRebootListOption()
    options.forEach { option ->
        DropdownMenuItem(
            onClick = { onItemClick(option.reason) },
            text = { Text("  " + stringResource(option.labelRes)) },
        )
    }
}

@Composable
fun RebootListPopupMaterial() {
    var expanded by remember { mutableStateOf(false) }

    run {
        val onReboot = rememberRebootAction()

        ExpressiveIconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = stringResource(id = R.string.reboot)
            )
        }

        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
                RebootDropdownItems { reason ->
                    expanded = false
                    onReboot(reason)
                }
            }
        }
    }
}
