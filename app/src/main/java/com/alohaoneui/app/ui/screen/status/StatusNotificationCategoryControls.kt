package com.alohaoneui.app.ui.screen.status

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.R
import com.alohaoneui.common.Preference
import com.alohaoneui.mod.ui.category.SystemUIEvent
import com.alohaoneui.mod.ui.component.AlohaOneUISlider
import com.alohaoneui.mod.ui.component.SwitchItem
import kotlin.math.roundToInt

@Composable
fun StatusNotificationCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    var max by remember(uiState.statusBarMaxNotificationIcons) {
        mutableIntStateOf(uiState.statusBarMaxNotificationIcons)
    }
    var expanded by rememberSaveable { mutableStateOf(false) }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    title = stringResource(id = R.string.alohaoneui_setStatusBarMaxNotificationIcons_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.modifyStatusBarMaxNotificationIcons) " $max" else null,
                    icon = ImageVector.vectorResource(id = R.drawable.notifications),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.modifyStatusBarMaxNotificationIcons,
                    onCheckedChange = {
                        if (it && max == 4) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarMaxNotificationIcons(it))
                    },
                )
            }
            if (expanded && uiState.modifyStatusBarMaxNotificationIcons) {
                add {
                    StatusThemeChildItem {
                        AlohaOneUISlider(
                            value = max.toFloat(),
                            onValueChange = { max = it.roundToInt() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            valueRange = 4f..20f,
                            onValueChangeFinished = {
                                onEvent(SystemUIEvent.StatusBar.StatusBarMaxNotificationIcons(max))
                            },
                        )
                    }
                }
            }
        },
    )
}
