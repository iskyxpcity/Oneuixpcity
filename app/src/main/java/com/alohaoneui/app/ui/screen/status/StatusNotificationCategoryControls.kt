package com.alohaoneui.app.ui.screen.status

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.font.FontWeight
import com.alohaoneui.app.R
import com.alohaoneui.common.Preference
import com.alohaoneui.mod.ui.category.SystemUIEvent
import com.alohaoneui.mod.ui.component.AlohaOneUISlider
import com.alohaoneui.mod.ui.component.SwitchItem
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.navigation3.LocalNavigator
import com.alohaoneui.app.ui.navigation3.Route
import com.alohaoneui.app.ui.util.ResponsiveCardGrid
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import org.json.JSONArray

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
                    title = androidx.compose.ui.res.stringResource(id = R.string.alohaoneui_setStatusBarMaxNotificationIcons_title),
                    summary = if (uiState.modifyStatusBarMaxNotificationIcons) " $max" else null,
                    icon = ImageVector.vectorResource(id = R.drawable.notifications),
                    clickable = true,
                    showChildIndicator = true,
                    expanded = expanded && uiState.modifyStatusBarMaxNotificationIcons,
                    onClick = { expanded = !expanded },
                    checked = uiState.modifyStatusBarMaxNotificationIcons,
                    onCheckedChange = {
                        if (it && max == 4) expanded = true else if (!it) expanded = false
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarMaxNotificationIcons(it))
                    },
                )
            }
            if (expanded && uiState.modifyStatusBarMaxNotificationIcons) {
                add {
                    StatusThemeChildItem {
                        AlohaOneUISlider(
                            value = max.toFloat(),
                            onValueChange = { max = it.toInt() },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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

    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val iconStyles = uiState.iconStyles
    val batteryStyleLabels = remember(context) {
        runCatching {
            val raw = context.assets.open("battery_icons/manifest.json")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            val array = JSONArray(raw)
            buildMap {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    put(item.optString("id"), item.optString("label"))
                }
            }
        }.getOrDefault(emptyMap())
    }
    when (LocalUiMode.current) {
        UiMode.Material -> Text(
            text = "图标样式",
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        UiMode.Miuix -> MiuixText(
            text = "图标样式",
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 20.dp, bottom = 8.dp),
            fontWeight = FontWeight(550),
            color = MiuixTheme.colorScheme.onBackground,
        )
    }

    ResponsiveCardGrid(
        content = StatusIconStyleTarget.entries.map { target ->
            {
                val stateText = when (target) {
                    StatusIconStyleTarget.Battery -> if (iconStyles.batteryEnabled && iconStyles.batteryStyleId.isNotBlank()) {
                        "已启用：" + (batteryStyleLabels[iconStyles.batteryStyleId]?.takeIf { it.isNotBlank() } ?: "电池样式")
                    } else "未启用"
                    StatusIconStyleTarget.Wifi -> if (iconStyles.wifiEnabled && iconStyles.wifiStyleId.isNotBlank()) {
                        "已启用：" + signalStyleLabel(iconStyles.wifiStyleId, "laobai.systemui.wifi_signal.", "WiFi 风格")
                    } else if (iconStyles.hideWifiActivityArrows) "已隐藏 WiFi 箭头" else "未启用"
                    StatusIconStyleTarget.Mobile -> if (iconStyles.mobileEnabled && iconStyles.mobileStyleId.isNotBlank()) {
                        "已启用：" + signalStyleLabel(iconStyles.mobileStyleId, "laobai.systemui.mobile_signal.", "信号风格")
                    } else if (iconStyles.hideMobileNetworkType) "已隐藏 4G / 5G 小标" else "未启用"
                    StatusIconStyleTarget.Bluetooth -> if (iconStyles.bluetoothEnabled && iconStyles.bluetoothStyleId.isNotBlank()) {
                        "已启用：" + signalStyleLabel(iconStyles.bluetoothStyleId, "laobai.systemui.bluetooth_icon.", "蓝牙风格")
                    } else "未启用"
                    StatusIconStyleTarget.Vpn -> if (iconStyles.vpnEnabled && iconStyles.vpnStyleId.isNotBlank()) {
                        "已启用：" + signalStyleLabel(iconStyles.vpnStyleId, "laobai.systemui.vpn_icon.", "VPN 风格")
                    } else "未启用"
                }
                val active = stateText != "未启用"
                Box {
                    StatusThemeGroup(
                        miuixInsideMargin = PaddingValues(10.dp),
                        items = listOf({
                            StatusThemeChildItem(
                                title = target.title,
                                summary = target.description,
                                icon = ImageVector.vectorResource(id = target.iconRes),
                                modifier = Modifier.fillMaxWidth().clickable {
                                    navigator.push(Route.StatusIconStyle(target.key))
                                },
                            ) {
                                when (LocalUiMode.current) {
                                    UiMode.Material -> Text(
                                        text = stateText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    UiMode.Miuix -> MiuixText(
                                        text = stateText,
                                        color = if (active) MiuixTheme.colorScheme.primary
                                        else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                        }),
                    )
                }
            }
        },
    )
}


private fun signalStyleLabel(styleId: String, prefix: String, label: String): String {
    val dir = styleId.removePrefix(prefix)
    val number = Regex("^style0*(\\d+)$").find(dir)?.groupValues?.getOrNull(1)
    return if (number != null) label + " " + number else label
}
