package com.alohaoneui.app.ui.screen.status

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
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
import com.alohaoneui.app.ui.util.ResponsiveCardGrid
import com.alohaoneui.app.ui.util.responsiveGridFullSpan
import com.alohaoneui.common.Preference
import com.alohaoneui.mod.ui.component.AlohaOneUIActionButton
import com.alohaoneui.mod.ui.component.AlohaOneUISlider
import com.alohaoneui.mod.ui.component.SwitchItem
import org.json.JSONArray
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private data class IconPackStyle(
    val id: String,
    val dir: String,
    val label: String,
    val assetDir: String,
    val previewName: String,
    val batteryPreview: Boolean = false,
)

@Composable
fun StatusBarIconStylesCategoryControls(
    target: StatusIconStyleTarget,
    uiState: Preference.SystemUI.StatusBar,
    onChange: (Preference.SystemUI.StatusBar) -> Unit,
    onApply: () -> Unit,
) {
    val context = LocalContext.current
    val styles = remember(context, target) {
        when (target) {
            StatusIconStyleTarget.Battery -> loadBatteryIconStyles(context)
            StatusIconStyleTarget.Wifi,
            StatusIconStyleTarget.Mobile,
            StatusIconStyleTarget.Bluetooth,
            StatusIconStyleTarget.Vpn -> loadSignalIconStyles(context, target)
        }
    }
    val config = uiState.iconStyles

    ResponsiveCardGrid {
        Box(Modifier.responsiveGridFullSpan()) {
            when (LocalUiMode.current) {
                UiMode.Material -> Text(
                    text = target.detailDescription,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                UiMode.Miuix -> top.yukonga.miuix.kmp.basic.Text(
                    text = target.description,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        when (target) {
            StatusIconStyleTarget.Battery -> BatteryIconStyleCards(
                config = config,
                styles = styles,
                onChange = { onChange(uiState.copy(iconStyles = it)) },
                onApply = onApply,
            )
            StatusIconStyleTarget.Wifi -> SignalIconStyleCards(
                target = target,
                enabled = config.wifiEnabled,
                selectedStyleId = config.wifiStyleId,
                hideAuxiliary = config.hideWifiActivityArrows,
                styles = styles,
                onEnabledChange = { onChange(uiState.copy(iconStyles = config.copy(wifiEnabled = it, wifiStyleId = config.wifiStyleId.ifBlank { styles.firstOrNull()?.id.orEmpty() }))) },
                onAuxiliaryChange = { onChange(uiState.copy(iconStyles = config.copy(hideWifiActivityArrows = it))) },
                onStyleChange = { onChange(uiState.copy(iconStyles = config.copy(wifiEnabled = true, wifiStyleId = it))) },
                onApply = onApply,
            )
            StatusIconStyleTarget.Mobile -> SignalIconStyleCards(
                target = target,
                enabled = config.mobileEnabled,
                selectedStyleId = config.mobileStyleId,
                hideAuxiliary = config.hideMobileNetworkType,
                styles = styles,
                onEnabledChange = { onChange(uiState.copy(iconStyles = config.copy(mobileEnabled = it, mobileStyleId = config.mobileStyleId.ifBlank { styles.firstOrNull()?.id.orEmpty() }))) },
                onAuxiliaryChange = { onChange(uiState.copy(iconStyles = config.copy(hideMobileNetworkType = it))) },
                onStyleChange = { onChange(uiState.copy(iconStyles = config.copy(mobileEnabled = true, mobileStyleId = it))) },
                onApply = onApply,
            )
            StatusIconStyleTarget.Bluetooth -> SignalIconStyleCards(
                target = target,
                enabled = config.bluetoothEnabled,
                selectedStyleId = config.bluetoothStyleId,
                styles = styles,
                onEnabledChange = { onChange(uiState.copy(iconStyles = config.copy(bluetoothEnabled = it, bluetoothStyleId = config.bluetoothStyleId.ifBlank { styles.firstOrNull()?.id.orEmpty() }))) },
                onStyleChange = { onChange(uiState.copy(iconStyles = config.copy(bluetoothEnabled = true, bluetoothStyleId = it))) },
                onApply = onApply,
            )
            StatusIconStyleTarget.Vpn -> SignalIconStyleCards(
                target = target,
                enabled = config.vpnEnabled,
                selectedStyleId = config.vpnStyleId,
                styles = styles,
                onEnabledChange = { onChange(uiState.copy(iconStyles = config.copy(vpnEnabled = it, vpnStyleId = config.vpnStyleId.ifBlank { styles.firstOrNull()?.id.orEmpty() }))) },
                onStyleChange = { onChange(uiState.copy(iconStyles = config.copy(vpnEnabled = true, vpnStyleId = it))) },
                onApply = onApply,
            )
        }
    }
}

@Composable
private fun BatteryIconStyleCards(
    config: Preference.SystemUI.StatusBarIconStyles,
    styles: List<IconPackStyle>,
    onChange: (Preference.SystemUI.StatusBarIconStyles) -> Unit,
    onApply: () -> Unit,
) {
    val selected = styles.firstOrNull { it.id == config.batteryStyleId }
    var size by remember(config.batterySizePercent) { mutableFloatStateOf(config.batterySizePercent.toFloat()) }

    Box {
        StatusThemeGroup(
            miuixInsideMargin = PaddingValues(10.dp),
            items = listOf(
                {
                    StatusThemeChildItem(
                        title = "官方电池设置",
                        summary = "未启用自定义电池时，百分比走系统原版；启用自定义电池后，可选择百分比在图标左侧或右侧。",
                    ) {}
                },
                {
                    SwitchItem(
                        title = "电池背景",
                        summary = when {
                            config.batteryEnabled -> "自定义电池已接管，系统胶囊暂不使用。"
                            config.batteryBackgroundEnabled -> "已开启，显示系统电池胶囊背景。"
                            else -> "已关闭，使用普通电池显示。"
                        },
                        checked = config.batteryBackgroundEnabled,
                        enabled = !config.batteryEnabled,
                        onCheckedChange = {
                            onChange(config.copy(
                                batteryBackgroundEnabled = it,
                                batteryPercentEnabled = if (it) false else config.batteryPercentEnabled,
                            ))
                        },
                    )
                },
                {
                    SwitchItem(
                        title = "电量百分比",
                        summary = if (config.batteryEnabled || !config.batteryBackgroundEnabled) {
                            if (config.batteryEnabled) {
                                if (config.batteryPercentEnabled) {
                                    val side = if (config.batteryPercentPosition == 1) "右侧" else "左侧"
                                    "已开启，自定义百分比显示在图标" + side + "。"
                                } else "已关闭，自定义电池仅显示图标。"
                            } else {
                                if (config.batteryPercentEnabled) "已开启，使用系统原版电量百分比。"
                                else "已关闭，仅显示电池图标。"
                            }
                        } else "电池背景开启时由系统胶囊样式接管。",
                        enabled = config.batteryEnabled || !config.batteryBackgroundEnabled,
                        checked = config.batteryPercentEnabled,
                        onCheckedChange = { onChange(config.copy(batteryPercentEnabled = it)) },
                    )
                },
            ),
        )
    }

    Box {
        StatusThemeGroup(
            miuixInsideMargin = PaddingValues(10.dp),
            items = listOf(
                {
                    SwitchItem(
                        title = "启用电池图标替换",
                        summary = if (config.batteryEnabled) "已启用：" + (selected?.label ?: "未选择") else "未启用",
                        checked = config.batteryEnabled,
                        onCheckedChange = {
                            onChange(config.copy(
                                batteryEnabled = it,
                                batteryStyleId = config.batteryStyleId.ifBlank { styles.firstOrNull()?.id.orEmpty() },
                            ))
                        },
                    )
                },
                {
                    BatteryPercentPositionSelector(
                        selectedPosition = config.batteryPercentPosition,
                        enabled = config.batteryEnabled && config.batteryPercentEnabled,
                        onSelected = { onChange(config.copy(batteryPercentPosition = it)) },
                    )
                },
                {
                    SwitchItem(
                        title = "充电动画",
                        summary = "充电时播放电池图标动画，关闭后显示静态电量。",
                        checked = config.batteryChargingAnimationEnabled,
                        enabled = config.batteryEnabled,
                        onCheckedChange = { onChange(config.copy(batteryChargingAnimationEnabled = it)) },
                    )
                },
                {
                    StatusThemeChildItem(
                        title = "图标大小",
                        summary = size.roundToInt().toString() + "%",
                        modifier = Modifier.alpha(if (config.batteryEnabled) 1f else 0.42f),
                    ) {
                        AlohaOneUISlider(
                            value = size,
                            onValueChange = { size = it.roundToInt().toFloat() },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 80f..180f,
                            enabled = config.batteryEnabled,
                            onValueChangeFinished = { onChange(config.copy(batterySizePercent = size.roundToInt())) },
                        )
                    }
                },
                { ApplyIconStyleButton(onApply) },
            ),
        )
    }

    Box(Modifier.responsiveGridFullSpan().alpha(if (config.batteryEnabled) 1f else 0.42f)) {
        when (LocalUiMode.current) {
            UiMode.Material -> Text(
                text = "内置样式",
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            UiMode.Miuix -> top.yukonga.miuix.kmp.basic.Text(
                text = "内置样式",
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 14.dp, bottom = 8.dp),
                color = MiuixTheme.colorScheme.onBackground,
            )
        }
    }
    styleRows(
        styles = styles,
        selectedId = config.batteryStyleId,
        enabled = config.batteryEnabled,
        emptyMessage = "没有找到内置电池图标资源",
    ) {
        onChange(config.copy(batteryEnabled = true, batteryStyleId = it))
    }
}

@Composable
private fun SignalIconStyleCards(
    target: StatusIconStyleTarget,
    enabled: Boolean,
    selectedStyleId: String,
    styles: List<IconPackStyle>,
    hideAuxiliary: Boolean = false,
    onEnabledChange: (Boolean) -> Unit,
    onAuxiliaryChange: ((Boolean) -> Unit)? = null,
    onStyleChange: (String) -> Unit,
    onApply: () -> Unit,
) {
    val selected = styles.firstOrNull { it.id == selectedStyleId }
    Box {
        StatusThemeGroup(
            miuixInsideMargin = PaddingValues(10.dp),
            items = listOf(
                {
                    SwitchItem(
                        title = "启用" + target.title + "替换",
                        summary = if (enabled) "已启用：" + (selected?.label ?: "未选择") else "未启用",
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                    )
                },
                { ApplyIconStyleButton(onApply) },
            ),
        )
    }

    if (target == StatusIconStyleTarget.Wifi && onAuxiliaryChange != null) {
        Box {
            StatusThemeGroup(
                miuixInsideMargin = PaddingValues(10.dp),
                items = listOf({
                    SwitchItem(
                        title = "隐藏 WiFi 上下箭头",
                        summary = if (hideAuxiliary) "已开启，只隐藏 WiFi 进/出流量箭头。"
                        else "已关闭，保留系统 WiFi 进/出流量箭头。",
                        checked = hideAuxiliary,
                        onCheckedChange = onAuxiliaryChange,
                    )
                }),
            )
        }
    }
    if (target == StatusIconStyleTarget.Mobile && onAuxiliaryChange != null) {
        Box {
            StatusThemeGroup(
                miuixInsideMargin = PaddingValues(10.dp),
                items = listOf({
                    SwitchItem(
                        title = "隐藏 4G / 5G 小标",
                        summary = if (hideAuxiliary) "已开启，隐藏移动信号旁的 4G / 4G+ / 5G / 5G+ 小图标。"
                        else "已关闭，保留系统移动网络制式小图标。",
                        checked = hideAuxiliary,
                        onCheckedChange = onAuxiliaryChange,
                    )
                }),
            )
        }
    }

    Box(Modifier.responsiveGridFullSpan().alpha(if (enabled) 1f else 0.42f)) {
        val title = when (target) {
            StatusIconStyleTarget.Wifi -> "WiFi 样式"
            StatusIconStyleTarget.Mobile -> "移动信号样式"
            StatusIconStyleTarget.Bluetooth -> "蓝牙样式"
            StatusIconStyleTarget.Vpn -> "VPN 样式"
            StatusIconStyleTarget.Battery -> "内置样式"
        }
        when (LocalUiMode.current) {
            UiMode.Material -> Text(
                text = title,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            UiMode.Miuix -> top.yukonga.miuix.kmp.basic.Text(
                text = title,
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 14.dp, bottom = 8.dp),
                color = MiuixTheme.colorScheme.onBackground,
            )
        }
    }
    styleRows(
        styles = styles,
        selectedId = selectedStyleId,
        enabled = enabled,
        emptyMessage = "没有找到内置图标资源",
        onSelected = onStyleChange,
    )
}

@Composable
private fun BatteryPercentPositionSelector(
    selectedPosition: Int,
    enabled: Boolean,
    onSelected: (Int) -> Unit,
) {
    StatusThemeChildItem(
        title = "百分比位置",
        summary = "仅启用自定义电池并开启电量百分比后生效。",
        modifier = Modifier.alpha(if (enabled) 1f else 0.42f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PercentPositionButton(
                label = "左侧",
                selected = selectedPosition == 0,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(0) },
            )
            PercentPositionButton(
                label = "右侧",
                selected = selectedPosition == 1,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(1) },
            )
        }
    }
}

@Composable
private fun PercentPositionButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val material = LocalUiMode.current == UiMode.Material
    val background = when {
        selected && material -> MaterialTheme.colorScheme.primary
        selected -> MiuixTheme.colorScheme.primary
        material -> MaterialTheme.colorScheme.surfaceVariant
        else -> MiuixTheme.colorScheme.surface
    }
    val foreground = when {
        selected && material -> MaterialTheme.colorScheme.onPrimary
        selected -> MiuixTheme.colorScheme.onPrimary
        material -> MaterialTheme.colorScheme.primary
        else -> MiuixTheme.colorScheme.primary
    }
    val outline = when {
        selected && material -> MaterialTheme.colorScheme.primary
        selected -> MiuixTheme.colorScheme.primary
        material -> MaterialTheme.colorScheme.outlineVariant
        else -> MiuixTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    }
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, outline, RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.55f),
        contentAlignment = Alignment.Center,
    ) {
        when (LocalUiMode.current) {
            UiMode.Material -> Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = foreground,
            )
            UiMode.Miuix -> top.yukonga.miuix.kmp.basic.Text(
                text = label,
                color = foreground,
            )
        }
    }
}

@Composable
private fun IconStyleSelectionBadge(selected: Boolean) {
    val material = LocalUiMode.current == UiMode.Material
    val background = when {
        selected && material -> MaterialTheme.colorScheme.primary
        selected -> MiuixTheme.colorScheme.primary
        material -> MaterialTheme.colorScheme.surfaceVariant
        else -> MiuixTheme.colorScheme.surface
    }
    val foreground = when {
        selected && material -> MaterialTheme.colorScheme.onPrimary
        selected -> MiuixTheme.colorScheme.onPrimary
        material -> MaterialTheme.colorScheme.primary
        else -> MiuixTheme.colorScheme.primary
    }
    val outline = when {
        selected && material -> MaterialTheme.colorScheme.primary
        selected -> MiuixTheme.colorScheme.primary
        material -> MaterialTheme.colorScheme.outlineVariant
        else -> MiuixTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    }
    Box(
        modifier = Modifier
            .size(58.dp, 34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, outline, RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when (LocalUiMode.current) {
            UiMode.Material -> Text(
                text = if (selected) "已选" else "选择",
                style = MaterialTheme.typography.labelMedium,
                color = foreground,
            )
            UiMode.Miuix -> top.yukonga.miuix.kmp.basic.Text(
                text = if (selected) "已选" else "选择",
                color = foreground,
            )
        }
    }
}

@Composable
private fun ApplyIconStyleButton(onApply: () -> Unit) {
    StatusThemeChildItem {
        AlohaOneUIActionButton(
            text = "立即应用",
            onClick = onApply,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            fillWidth = true,
        )
    }
}

@Composable
private fun styleRows(
    styles: List<IconPackStyle>,
    selectedId: String,
    enabled: Boolean,
    emptyMessage: String,
    onSelected: (String) -> Unit,
) {
    if (styles.isEmpty()) {
        Box {
            StatusThemeGroup(
                miuixInsideMargin = PaddingValues(10.dp),
                items = listOf({
                    StatusThemeChildItem(title = emptyMessage) {}
                }),
            )
        }
        return
    }
    styles.forEach { style ->
        Box(Modifier.alpha(if (enabled) 1f else 0.42f)) {
            StatusThemeGroup(
                modifier = Modifier.border(
                    width = if (selectedId == style.id) 1.dp else 0.5.dp,
                    color = if (selectedId == style.id) {
                        if (LocalUiMode.current == UiMode.Material) MaterialTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.primary
                    } else if (LocalUiMode.current == UiMode.Material) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(22.dp),
                ),
                miuixInsideMargin = PaddingValues(10.dp),
                items = listOf({
                    StatusThemeChildItem(
                        modifier = Modifier.clickable(enabled = enabled) { onSelected(style.id) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconAssetPreview(style)
                            Text(
                                text = style.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (LocalUiMode.current == UiMode.Material) MaterialTheme.colorScheme.onSurface
                                else MiuixTheme.colorScheme.onSurface,
                            )
                            IconStyleSelectionBadge(selected = selectedId == style.id)
                        }
                    }
                }),
            )
        }
    }
}

@Composable
private fun IconAssetPreview(style: IconPackStyle) {
    val context = LocalContext.current
    val bitmap = remember(style.assetDir, style.dir, style.previewName) {
        runCatching {
            context.assets.open(style.assetDir + "/" + style.dir + "/" + style.previewName)
                .use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
    if (bitmap == null) {
        Box(
            Modifier.size(if (style.batteryPreview) 34.dp else 78.dp, if (style.batteryPreview) 34.dp else 38.dp),
            contentAlignment = Alignment.Center,
        ) { Text("—") }
        return
    }
    val surface = if (LocalUiMode.current == UiMode.Material) MaterialTheme.colorScheme.surfaceVariant
    else MiuixTheme.colorScheme.surface
    val tint = if (LocalUiMode.current == UiMode.Material) MaterialTheme.colorScheme.onSurface
    else MiuixTheme.colorScheme.onSurface
    val accent = if (LocalUiMode.current == UiMode.Material) MaterialTheme.colorScheme.primary
    else MiuixTheme.colorScheme.primary
    val filter = when {
        style.batteryPreview && shouldTintBatteryPreview(bitmap) -> ColorFilter.tint(accent)
        style.batteryPreview -> null
        else -> ColorFilter.tint(tint)
    }
    Box(
        modifier = Modifier
            .size(if (style.batteryPreview) 34.dp else 78.dp, if (style.batteryPreview) 34.dp else 38.dp)
            .background(surface, RoundedCornerShape(12.dp))
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = style.label,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
            colorFilter = filter,
        )
    }
}

private fun shouldTintBatteryPreview(bitmap: Bitmap): Boolean {
    var colored = 0
    var lightNeutral = 0
    var visible = 0
    val stepX = maxOf(1, bitmap.width / 16)
    val stepY = maxOf(1, bitmap.height / 16)
    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = Color.alpha(pixel)
            if (alpha > 48) {
                visible++
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                val max = maxOf(red, green, blue)
                val min = minOf(red, green, blue)
                if (max - min > 42) colored++
                if (min >= 165 && max - min <= 30) lightNeutral++
            }
            x += stepX
        }
        y += stepY
    }
    if (visible == 0) return false
    return colored <= maxOf(2, visible / 12) && lightNeutral >= visible / 3
}

private fun loadBatteryIconStyles(context: android.content.Context): List<IconPackStyle> =
    runCatching {
        val raw = context.assets.open("battery_icons/manifest.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val id = item.optString("id")
                val dir = item.optString("dir")
                val label = item.optString("label")
                if (!id.startsWith("laobai.systemui.battery.") || dir.isBlank()) continue
                add(IconPackStyle(
                    id = id,
                    dir = dir,
                    label = label.ifBlank { "电池风格 " + (size + 1) },
                    assetDir = "battery_icons",
                    previewName = "battery_100.png",
                    batteryPreview = true,
                ))
            }
        }
    }.getOrDefault(emptyList())

private fun loadSignalIconStyles(
    context: android.content.Context,
    target: StatusIconStyleTarget,
): List<IconPackStyle> {
    val (assetDir, prefix, labelPrefix, filePrefix) = when (target) {
        StatusIconStyleTarget.Wifi -> listOf("wifi_signal_icons", "laobai.systemui.wifi_signal.", "WiFi 风格", "wifi")
        StatusIconStyleTarget.Mobile -> listOf("mobile_signal_icons", "laobai.systemui.mobile_signal.", "信号风格", "mobile")
        StatusIconStyleTarget.Bluetooth -> listOf("bluetooth_status_icons", "laobai.systemui.bluetooth_icon.", "蓝牙风格", "bluetooth")
        StatusIconStyleTarget.Vpn -> listOf("vpn_status_icons", "laobai.systemui.vpn_icon.", "VPN 风格", "vpn")
        StatusIconStyleTarget.Battery -> return emptyList()
    }
    val dirs = runCatching { context.assets.list(assetDir).orEmpty() }.getOrDefault(emptyArray())
    return dirs.filter(String::isNotBlank).sorted().mapIndexed { index, dir ->
        val maxLevel = when (filePrefix) {
            "bluetooth" -> 1
            "vpn" -> 0
            else -> runCatching { context.assets.list("$assetDir/$dir").orEmpty() }
                .getOrDefault(emptyArray())
                .mapNotNull {
                    Regex("^" + Regex.escape(filePrefix) + "_(\\d)\\.png$").find(it)
                        ?.groupValues?.getOrNull(1)?.toIntOrNull()
                }
                .maxOrNull()?.coerceIn(0, 5) ?: 0
        }
        val number = Regex("^style0*(\\d+)$").find(dir)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: index + 1
        val previewName = when (filePrefix) {
            "wifi" -> "wifi_" + maxLevel + ".png"
            "mobile" -> "mobile_" + maxLevel + ".png"
            "bluetooth" -> "bluetooth_connected.png"
            else -> "vpn.png"
        }
        IconPackStyle(prefix + dir, dir, labelPrefix + " " + number, assetDir, previewName)
    }
}
