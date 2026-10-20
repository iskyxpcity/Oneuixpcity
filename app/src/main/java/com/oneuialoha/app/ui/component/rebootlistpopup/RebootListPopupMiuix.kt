package com.oneuialoha.app.ui.component.rebootlistpopup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.oneuialoha.app.R
import com.oneuialoha.app.ui.component.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun RebootListPopupMiuix(
    modifier: Modifier = Modifier,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.TopEnd
) {
    val showTopPopup = remember { mutableStateOf(false) }
    run {
        val onReboot = rememberRebootAction()
        IconButton(
            modifier = modifier,
            onClick = { showTopPopup.value = true },
            holdDownState = showTopPopup.value
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.power_settings_new),
                contentDescription = stringResource(id = R.string.reboot),
                tint = colorScheme.onBackground
            )
        }
        OverlayListPopup(
            show = showTopPopup.value,
            popupPositionProvider = ListPopupDefaults.MenuPositionProvider,
            alignment = alignment,
            onDismissRequest = {
                showTopPopup.value = false
            },
            content = {
                val rebootOptions = getRebootListOption()

                ListPopupColumn {
                    rebootOptions.forEachIndexed { idx, option ->
                        RebootDropdownItem(
                            option = option,
                            showTopPopup = showTopPopup,
                            optionSize = rebootOptions.size,
                            index = idx,
                            onReboot = onReboot
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun RebootDropdownItem(
    option: RebootListOption,
    showTopPopup: MutableState<Boolean>,
    optionSize: Int,
    index: Int,
    onReboot: (String) -> Unit,
) {
    com.oneuialoha.app.ui.component.miuix.DropdownItem(
        text = stringResource(option.labelRes),
        optionSize = optionSize,
        onSelectedIndexChange = {
            showTopPopup.value = false
            onReboot(option.reason)
        },
        index = index
    )
}
