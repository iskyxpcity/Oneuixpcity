package com.oneuixp.app.ui.screen.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import io.github.soclear.oneuix.ui.component.SwitchItem

@Composable
fun StatusBluetoothCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    StatusThemeGroup(
        items = listOf(
            {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.bluetooth),
                    title = stringResource(id = R.string.oneuix_restoreBluetoothStatusBarIcon_title),
                    checked = uiState.restoreBluetoothStatusBarIcon,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.RestoreBluetoothStatusBarIcon(it))
                    },
                )
            },
        ),
    )
}
