package io.github.soclear.oneuix.ui.category

import androidx.compose.animation.animateContentSize
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
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.ui.component.OneuiXChildVisibility
import io.github.soclear.oneuix.ui.component.OneuiXPreferenceGroup
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.SwitchItem
import kotlin.math.roundToInt

/**
 * Canonical status-bar notification controls shared by the legacy SystemUI page
 * and the new Status > Notification category. Theme-specific components only
 * change rendering; preference state and events remain identical.
 */
@Composable
fun StatusBarNotificationControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    OneuiXPreferenceGroup {
        var max by remember(uiState.statusBarMaxNotificationIcons) {
            mutableIntStateOf(uiState.statusBarMaxNotificationIcons)
        }
        var expanded by rememberSaveable { mutableStateOf(false) }

        SwitchItem(
            title = stringResource(id = R.string.oneuix_setStatusBarMaxNotificationIcons_title),
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
        OneuiXChildVisibility(expanded && uiState.modifyStatusBarMaxNotificationIcons) {
            OneuiXSlider(
                value = max.toFloat(),
                onValueChange = { max = it.roundToInt() },
                modifier = Modifier.padding(horizontal = 16.dp),
                valueRange = 4f..20f,
                steps = 15,
                onValueChangeFinished = {
                    onEvent(SystemUIEvent.StatusBar.StatusBarMaxNotificationIcons(max))
                },
            )
        }
    }
}
