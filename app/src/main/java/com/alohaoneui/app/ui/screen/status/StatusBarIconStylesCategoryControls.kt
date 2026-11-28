package com.alohaoneui.app.ui.screen.status

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.common.Preference
import com.alohaoneui.mod.ui.component.AlohaOneUIActionButton
import com.alohaoneui.mod.ui.component.AlohaOneUISlider
import com.alohaoneui.mod.ui.component.SelectItem
import com.alohaoneui.mod.ui.component.SwitchItem
import org.json.JSONArray
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private enum class IconPackTarget { Battery, Wifi, Mobile, Bluetooth, Vpn }
private data class IconPackStyle(val id: String, val label: String, val assetPath: String, val tintPreview: Boolean)

@Composable
fun StatusBarIconStylesCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onChange: (Preference.SystemUI.StatusBar) -> Unit,
    onApply: () -> Unit,
) {
    val context = LocalContext.current
    val styles = remember(context) {
        mapOf(
            IconPackTarget.Battery to loadBatteryIconStyles(context),
            IconPackTarget.Wifi to loadSignalIconStyles(context, "wifi_signal_icons", "laobai.systemui.wifi_signal.", "WiFi 风格", "wifi"),
            IconPackTarget.Mobile to loadSignalIconStyles(context, "mobile_signal_icons", "laobai.systemui.mobile_signal.", "信号风格", "mobile"),
            IconPackTarget.Bluetooth to loadSignalIconStyles(context, "bluetooth_status_icons", "laobai.systemui.bluetooth_icon.", "蓝牙风格", "bluetooth"),
            IconPackTarget.Vpn to loadSignalIconStyles(context, "vpn_status_icons", "laobai.systemui.vpn_icon.", "VPN 风格", "vpn"),
        )
    }
    val current = uiState.iconStyles
    var batteryExpanded by rememberSaveable { mutableStateOf(false) }
    var wifiExpanded by rememberSaveable { mutableStateOf(false) }
    var mobileExpanded by rememberSaveable { mutableStateOf(false) }
    var bluetoothExpanded by rememberSaveable { mutableStateOf(false) }
    var vpnExpanded by rememberSaveable { mutableStateOf(false) }
    var batterySize by remember(current.batterySizePercent) { mutableFloatStateOf(current.batterySizePercent.toFloat()) }

    fun update(next: Preference.SystemUI.StatusBarIconStyles) {
        onChange(uiState.copy(iconStyles = next))
    }

    StatusThemeGroup(
        title = "图标样式",
        items = buildList {
            add {
                SwitchItem(
                    title = "电池图标样式",
                    summary = if (current.batteryEnabled) styles.getValue(IconPackTarget.Battery).firstOrNull { it.id == current.batteryStyleId }?.label?.let { "已启用：$it" } ?: "已启用"
                        else "使用 App 内置模块电池样式替换状态栏电池图标。",
                    icon = ImageVector.vectorResource(id = R.drawable.battery),
                    clickable = true, showChildIndicator = true, expanded = batteryExpanded,
                    onClick = { batteryExpanded = !batteryExpanded },
                    checked = current.batteryEnabled,
                    onCheckedChange = { enabled ->
                        val first = styles.getValue(IconPackTarget.Battery).firstOrNull()?.id.orEmpty()
                        update(current.copy(batteryEnabled = enabled, batteryStyleId = current.batteryStyleId.ifBlank { first }))
                        if (enabled) batteryExpanded = true
                    },
                )
            }
            if (batteryExpanded) {
                add { StatusThemeChildItem(title = "官方电池设置", summary = "未启用自定义电池时，百分比走系统原版；启用自定义电池后，可选择百分比在图标左侧或右侧。") {} }
                add {
                    SwitchItem(
                        title = "电池背景",
                        summary = when {
                            current.batteryEnabled -> "自定义电池已接管，系统胶囊暂不使用。"
                            current.batteryBackgroundEnabled -> "已开启，显示系统电池胶囊背景。"
                            else -> "已关闭，使用普通电池显示。"
                        },
                        checked = current.batteryBackgroundEnabled, enabled = !current.batteryEnabled,
                        onCheckedChange = { enabled -> update(current.copy(batteryBackgroundEnabled = enabled, batteryPercentEnabled = if (enabled) false else current.batteryPercentEnabled)) },
                    )
                }
                add {
                    SwitchItem(
                        title = "电量百分比",
                        summary = if (current.batteryEnabled) {
                            if (current.batteryPercentEnabled) "已开启，自定义百分比显示在图标${if (current.batteryPercentPosition == 1) "右侧" else "左侧"}。"
                            else "已关闭，自定义电池仅显示图标。"
                        } else if (current.batteryPercentEnabled) "已开启，使用系统原版电量百分比。" else "已关闭，仅显示电池图标。",
                        enabled = current.batteryEnabled || !current.batteryBackgroundEnabled,
                        checked = current.batteryPercentEnabled,
                        onCheckedChange = { update(current.copy(batteryPercentEnabled = it)) },
                    )
                }
                add {
                    SelectItem(
                        title = "百分比位置", summary = "仅启用自定义电池并开启电量百分比后生效。",
                        entries = listOf("左侧", "右侧"), selectedIndex = current.batteryPercentPosition.coerceIn(0, 1),
                        enabled = current.batteryEnabled && current.batteryPercentEnabled,
                        onSelectedIndexChange = { update(current.copy(batteryPercentPosition = it)) },
                    )
                }
                add {
                    SwitchItem(
                        title = "充电动画", summary = "充电时播放电池图标动画，关闭后显示静态电量。",
                        checked = current.batteryChargingAnimationEnabled, enabled = current.batteryEnabled,
                        onCheckedChange = { update(current.copy(batteryChargingAnimationEnabled = it)) },
                    )
                }
                add {
                    StatusThemeChildItem(title = "图标大小", summary = "${batterySize.toInt()}%") {
                        AlohaOneUISlider(
                            value = batterySize, onValueChange = { batterySize = it.roundToInt().toFloat() },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 80f..180f,
                            onValueChangeFinished = { update(current.copy(batterySizePercent = batterySize.roundToInt())) },
                        )
                    }
                }
                addAll(styleRows(styles.getValue(IconPackTarget.Battery), current.batteryStyleId, current.batteryEnabled) { id ->
                    update(current.copy(batteryEnabled = true, batteryStyleId = id))
                })
                add { ApplyIconStylesButton(onApply) }
            }

            add {
                SwitchItem(
                    title = "WiFi 信号图标",
                    summary = if (current.wifiEnabled) styles.getValue(IconPackTarget.Wifi).firstOrNull { it.id == current.wifiStyleId }?.label?.let { "已启用：$it" } ?: "已启用"
                        else "替换 WiFi 信号样式，支持 WiFi 5 / WiFi 6 / WiFi 7 图标和上下箭头隐藏。",
                    icon = ImageVector.vectorResource(id = R.drawable.flux_status_wifi),
                    clickable = true, showChildIndicator = true, expanded = wifiExpanded,
                    onClick = { wifiExpanded = !wifiExpanded }, checked = current.wifiEnabled,
                    onCheckedChange = { enabled ->
                        val first = styles.getValue(IconPackTarget.Wifi).firstOrNull()?.id.orEmpty()
                        update(current.copy(wifiEnabled = enabled, wifiStyleId = current.wifiStyleId.ifBlank { first }))
                        if (enabled) wifiExpanded = true
                    },
                )
            }
            if (wifiExpanded) {
                add {
                    SwitchItem(
                        title = "隐藏 WiFi 上下箭头",
                        summary = if (current.hideWifiActivityArrows) "已开启，只隐藏 WiFi 进/出流量箭头。" else "已关闭，保留系统 WiFi 进/出流量箭头。",
                        checked = current.hideWifiActivityArrows,
                        onCheckedChange = { update(current.copy(hideWifiActivityArrows = it)) },
                    )
                }
                addAll(styleRows(styles.getValue(IconPackTarget.Wifi), current.wifiStyleId, current.wifiEnabled) { id ->
                    update(current.copy(wifiEnabled = true, wifiStyleId = id))
                })
                add { ApplyIconStylesButton(onApply) }
            }

            add {
                SwitchItem(
                    title = "移动信号图标",
                    summary = if (current.mobileEnabled) styles.getValue(IconPackTarget.Mobile).firstOrNull { it.id == current.mobileStyleId }?.label?.let { "已启用：$it" } ?: "已启用"
                        else "替换移动网络信号图标，支持正常状态栏、下拉面板和双卡样式。",
                    icon = ImageVector.vectorResource(id = R.drawable.flux_status_signal),
                    clickable = true, showChildIndicator = true, expanded = mobileExpanded,
                    onClick = { mobileExpanded = !mobileExpanded }, checked = current.mobileEnabled,
                    onCheckedChange = { enabled ->
                        val first = styles.getValue(IconPackTarget.Mobile).firstOrNull()?.id.orEmpty()
                        update(current.copy(mobileEnabled = enabled, mobileStyleId = current.mobileStyleId.ifBlank { first }))
                        if (enabled) mobileExpanded = true
                    },
                )
            }
            if (mobileExpanded) {
                add {
                    SwitchItem(
                        title = "隐藏 4G / 5G 小标",
                        summary = if (current.hideMobileNetworkType) "已开启，隐藏移动信号旁的 4G / 4G+ / 5G / 5G+ 小图标。" else "已关闭，保留系统移动网络制式小图标。",
                        checked = current.hideMobileNetworkType,
                        onCheckedChange = { update(current.copy(hideMobileNetworkType = it)) },
                    )
                }
                addAll(styleRows(styles.getValue(IconPackTarget.Mobile), current.mobileStyleId, current.mobileEnabled) { id ->
                    update(current.copy(mobileEnabled = true, mobileStyleId = id))
                })
                add { ApplyIconStylesButton(onApply) }
            }

            add {
                SwitchItem(
                    title = "蓝牙图标",
                    summary = if (current.bluetoothEnabled) styles.getValue(IconPackTarget.Bluetooth).firstOrNull { it.id == current.bluetoothStyleId }?.label?.let { "已启用：$it" } ?: "已启用"
                        else "替换状态栏蓝牙未连接和已连接图标。",
                    icon = ImageVector.vectorResource(id = R.drawable.bluetooth),
                    clickable = true, showChildIndicator = true, expanded = bluetoothExpanded,
                    onClick = { bluetoothExpanded = !bluetoothExpanded }, checked = current.bluetoothEnabled,
                    onCheckedChange = { enabled ->
                        val first = styles.getValue(IconPackTarget.Bluetooth).firstOrNull()?.id.orEmpty()
                        update(current.copy(bluetoothEnabled = enabled, bluetoothStyleId = current.bluetoothStyleId.ifBlank { first }))
                        if (enabled) bluetoothExpanded = true
                    },
                )
            }
            if (bluetoothExpanded) {
                addAll(styleRows(styles.getValue(IconPackTarget.Bluetooth), current.bluetoothStyleId, current.bluetoothEnabled) { id ->
                    update(current.copy(bluetoothEnabled = true, bluetoothStyleId = id))
                })
                add { ApplyIconStylesButton(onApply) }
            }

            add {
                SwitchItem(
                    title = "VPN 图标",
                    summary = if (current.vpnEnabled) styles.getValue(IconPackTarget.Vpn).firstOrNull { it.id == current.vpnStyleId }?.label?.let { "已启用：$it" } ?: "已启用"
                        else "替换状态栏 VPN 图标。",
                    icon = ImageVector.vectorResource(id = R.drawable.flux_status_lockscreen),
                    clickable = true, showChildIndicator = true, expanded = vpnExpanded,
                    onClick = { vpnExpanded = !vpnExpanded }, checked = current.vpnEnabled,
                    onCheckedChange = { enabled ->
                        val first = styles.getValue(IconPackTarget.Vpn).firstOrNull()?.id.orEmpty()
                        update(current.copy(vpnEnabled = enabled, vpnStyleId = current.vpnStyleId.ifBlank { first }))
                        if (enabled) vpnExpanded = true
                    },
                )
            }
            if (vpnExpanded) {
                addAll(styleRows(styles.getValue(IconPackTarget.Vpn), current.vpnStyleId, current.vpnEnabled) { id ->
                    update(current.copy(vpnEnabled = true, vpnStyleId = id))
                })
                add { ApplyIconStylesButton(onApply) }
            }
        },
    )
}

@Composable
private fun ApplyIconStylesButton(onApply: () -> Unit) {
    StatusThemeChildItem {
        AlohaOneUIActionButton(
            text = "立即应用", onClick = onApply,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), fillWidth = true,
        )
    }
}

private fun styleRows(
    styles: List<IconPackStyle>,
    selectedId: String,
    enabled: Boolean,
    onSelected: (String) -> Unit,
): List<@Composable () -> Unit> = styles.map { style ->
    {
        StatusThemeChildItem(modifier = Modifier.clickable(enabled = enabled) { onSelected(style.id) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconAssetPreview(style)
                Text(
                    text = style.label, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (LocalUiMode.current == UiMode.Material) MaterialTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (selectedId == style.id) "已选" else "选择",
                    color = if (selectedId == style.id) {
                        if (LocalUiMode.current == UiMode.Material) MaterialTheme.colorScheme.primary else MiuixTheme.colorScheme.primary
                    } else if (LocalUiMode.current == UiMode.Material) MaterialTheme.colorScheme.onSurfaceVariant else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun IconAssetPreview(style: IconPackStyle) {
    val context = LocalContext.current
    val bitmap = remember(style.assetPath) {
        runCatching { context.assets.open(style.assetPath).use { BitmapFactory.decodeStream(it) } }.getOrNull()
    }
    val tint = if (LocalUiMode.current == UiMode.Material) MaterialTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurface
    val accent = if (LocalUiMode.current == UiMode.Material) MaterialTheme.colorScheme.primary else MiuixTheme.colorScheme.primary
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(), contentDescription = style.label,
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(if (style.tintPreview) accent else tint),
        )
    } else {
        Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) { Text("—") }
    }
}

private fun loadBatteryIconStyles(context: android.content.Context): List<IconPackStyle> =
    runCatching {
        val raw = context.assets.open("battery_icons/manifest.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val id = item.optString("id")
                val dir = item.optString("dir")
                if (!id.startsWith("laobai.systemui.battery.") || dir.isBlank()) continue
                add(IconPackStyle(id, "电池风格 ${size + 1}", "battery_icons/$dir/battery_100.png", true))
            }
        }
    }.getOrDefault(emptyList())

private fun loadSignalIconStyles(
    context: android.content.Context,
    assetDirectory: String,
    prefix: String,
    labelPrefix: String,
    filePrefix: String,
): List<IconPackStyle> {
    val dirs = runCatching { context.assets.list(assetDirectory).orEmpty() }.getOrDefault(emptyArray())
    return dirs.filter(String::isNotBlank).sorted().mapIndexed { index, dir ->
        val maxLevel = if (filePrefix == "bluetooth") 1 else if (filePrefix == "vpn") 0 else {
            runCatching { context.assets.list("$assetDirectory/$dir").orEmpty() }.getOrDefault(emptyArray())
                .mapNotNull { Regex("^${Regex.escape(filePrefix)}_(\\d)\\.png$").find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
                .maxOrNull()?.coerceIn(0, 5) ?: 0
        }
        val previewName = when (filePrefix) {
            "wifi" -> "wifi_$maxLevel.png"
            "mobile" -> "mobile_$maxLevel.png"
            "bluetooth" -> "bluetooth_connected.png"
            else -> "vpn.png"
        }
        val number = Regex("^style0*(\\d+)$").find(dir)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: index + 1
        IconPackStyle(prefix + dir, "$labelPrefix $number", "$assetDirectory/$dir/$previewName", false)
    }
}
