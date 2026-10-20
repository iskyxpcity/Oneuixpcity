package com.oneuialoha.app.ui.screen.status

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuialoha.app.R
import com.oneuialoha.common.Preference
import com.oneuialoha.mod.ui.category.SystemUIEvent
import com.oneuialoha.mod.ui.component.SwitchItem

@Composable
fun StatusLockscreenCategoryControls(
    statusBar: Preference.SystemUI.StatusBar,
    aod: Preference.SystemUI.AOD,
    onEvent: (SystemUIEvent) -> Unit,
) {
    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.mobile_screensaver),
                    title = stringResource(id = R.string.oneuialoha_doubleTapStatusBarToSleep_title),
                    checked = statusBar.doubleTapStatusBarToSleep,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.DoubleTapStatusBarToSleep(it))
                    },
                )
            }
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.mobile_screensaver),
                    title = stringResource(id = R.string.oneuialoha_hideLockscreenStatusBar_title),
                    checked = statusBar.hideLockscreenStatusBar,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.HideLockscreenStatusBar(it))
                    },
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                add {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.battery),
                        title = stringResource(id = R.string.oneuialoha_hideAODStatusBar_title),
                        checked = aod.hideAODStatusBar,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.AOD.HideAODStatusBar(it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.today),
                        title = stringResource(id = R.string.oneuialoha_aodLockSupportLunar_title),
                        checked = aod.aodLockSupportLunar,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.AOD.AODLockSupportLunar(it))
                        },
                    )
                }
            }
        },
    )
}
