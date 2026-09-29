package com.oneuixp.app.ui.component.configtransfer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.oneuixp.app.R
import com.oneuixp.app.ui.component.ListPopupDefaults
import com.oneuixp.app.ui.component.miuix.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun ConfigTransferPopupMiuix(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    val showPopup = remember { mutableStateOf(false) }

    IconButton(
        onClick = { showPopup.value = true },
        holdDownState = showPopup.value,
    ) {
        Icon(
            imageVector = MiuixIcons.Notes,
            contentDescription = stringResource(R.string.oneuix_backup_config),
            tint = colorScheme.onSurface,
        )
    }

    OverlayListPopup(
        show = showPopup.value,
        popupPositionProvider = ListPopupDefaults.MenuPositionProvider,
        alignment = PopupPositionProvider.Align.TopStart,
        onDismissRequest = { showPopup.value = false },
        content = {
            ListPopupColumn {
                DropdownItem(
                    text = stringResource(R.string.oneuix_backup_config),
                    optionSize = 2,
                    index = 0,
                    onSelectedIndexChange = {
                        showPopup.value = false
                        onBackup()
                    },
                )
                DropdownItem(
                    text = stringResource(R.string.oneuix_restore_config),
                    optionSize = 2,
                    index = 1,
                    onSelectedIndexChange = {
                        showPopup.value = false
                        onRestore()
                    },
                )
            }
        },
    )
}
