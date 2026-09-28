package com.oneuixp.app.ui.screen.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuixp.app.R
import io.github.soclear.oneuix.ui.component.OneuiXInfoItem

@Composable
fun StatusWifiCategoryControls() {
    StatusThemeGroup(
        items = listOf(
            {
                OneuiXInfoItem(
                    title = stringResource(id = R.string.status_wifi_no_migrated_feature_title),
                    summary = stringResource(id = R.string.status_wifi_no_migrated_feature_summary),
                    icon = ImageVector.vectorResource(id = R.drawable.flux_status_wifi),
                )
            },
        ),
    )
}
