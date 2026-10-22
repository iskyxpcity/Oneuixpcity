package com.alohaoneui.app.ui.screen.home

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Monitor
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text as MaterialText
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.component.material.TonalCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TabRowWithContour as MiuixTabRowWithContour
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private enum class DeviceTextKind { Title, Section, Label, Value, Large, Caption }
private data class CpuFrequencyEntry(
    val label: String,
    val currentKHz: Long,
    val maxKHz: Long?,
)

private data class CpuFrequencySnapshot(
    val entries: List<CpuFrequencyEntry>,
    val perCore: Boolean,
) {
    val averageKHz: Double
        get() = entries.map { it.currentKHz.toDouble() }.average()

    val maxSupportedKHz: Long?
        get() = entries.mapNotNull { it.maxKHz }.maxOrNull()

    val progress: Float
        get() {
            val denominator = maxSupportedKHz ?: entries.maxOfOrNull { it.currentKHz } ?: 0L
            return if (denominator > 0L) (averageKHz / denominator).toFloat().coerceIn(0f, 1f) else 0f
        }
}

private data class HomeDeviceSnapshot(
    val cpuFrequency: CpuFrequencySnapshot? = null,
    val ramUsed: Long = 0L,
    val ramTotal: Long = 0L,
    val storageUsed: Long = 0L,
    val storageTotal: Long = 0L,
    val batteryPercent: Int? = null,
    val batteryTemperatureC: Float? = null,
    val batteryVoltageMv: Int? = null,
    val charging: Boolean = false,
    val uptime: String = "读取中…",
    val resolution: String = "读取中…",
    val logicalSize: String = "—",
    val refreshRate: String = "—",
    val density: String = "—",
    val physicalSize: String = "—",
    val brightness: String = "—",
    val orientation: String = "—",
    val model: String = "—",
    val androidVersion: String = "—",
    val oneUiVersion: String = "—",
    val deviceCode: String = "—",
    val soc: String = "—",
    val cpuArchitecture: String = "—",
    val cpuCores: String = "—",
    val buildId: String = "—",
    val kernel: String = "—",
    val securityPatch: String = "—",
)

@Composable
fun HomeDeviceToolsCard() {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var selectedSection by rememberSaveable { mutableStateOf(0) }
    val snapshot = rememberHomeDeviceSnapshot(expanded)

    DeviceToolsSurface {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .semantics {
                        stateDescription = if (expanded) "已展开，点击收起" else "已收起，点击展开"
                    }
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeviceToolsIcon(Icons.Outlined.Devices, null, Modifier.size(26.dp))
                Column(
                    modifier = Modifier.weight(1f).padding(start = 12.dp, end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    DeviceToolsText("设备状态信息", DeviceTextKind.Title)
                    DeviceToolsText("系统监视器 · 屏幕信息 · 设备信息", DeviceTextKind.Caption)
                }
                DeviceToolsIcon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    if (expanded) "收起" else "展开",
                    Modifier.size(24.dp),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    DeviceInfoSectionSelector(
                        selectedSection = selectedSection,
                        onSelect = { selectedSection = it },
                    )
                    when (selectedSection) {
                        0 -> {
                            DeviceToolsDivider()
                            DeviceSectionHeading(Icons.Outlined.Speed, "系统监视器", "CPU、运行内存、内部存储与电池")
                            SystemMonitorContent(snapshot)
                        }
                        1 -> {
                            DeviceToolsDivider()
                            DeviceSectionHeading(Icons.Outlined.Monitor, "屏幕信息", "显示参数、刷新率、像素密度与亮度")
                            ScreenInformationContent(snapshot)
                        }
                        else -> {
                            DeviceToolsDivider()
                            DeviceSectionHeading(Icons.Outlined.Memory, "设备信息", "本机型号、系统版本与硬件环境")
                            DeviceInformationContent(snapshot)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceInfoSectionSelector(
    selectedSection: Int,
    onSelect: (Int) -> Unit,
) {
    val options = listOf("系统监视器", "屏幕信息", "设备信息")
    when (LocalUiMode.current) {
        UiMode.Material -> SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedSection == index,
                    onClick = { onSelect(index) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                    ),
                    label = {
                        MaterialText(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                )
            }
        }
        UiMode.Miuix -> MiuixTabRowWithContour(
            tabs = options,
            selectedTabIndex = selectedSection,
            onTabSelected = onSelect,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DeviceToolsSurface(content: @Composable () -> Unit) {
    when (LocalUiMode.current) {
        UiMode.Material -> TonalCard(modifier = Modifier.fillMaxWidth()) { content() }
        UiMode.Miuix -> MiuixCard(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun DeviceSectionHeading(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DeviceToolsIcon(icon, null, Modifier.size(21.dp))
        Column(
            modifier = Modifier.weight(1f).padding(start = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            DeviceToolsText(title, DeviceTextKind.Section)
            DeviceToolsText(subtitle, DeviceTextKind.Caption)
        }
    }
}

@Composable
private fun SystemMonitorContent(snapshot: HomeDeviceSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val cpuFrequency = snapshot.cpuFrequency
        val cpuFrequencyValue = cpuFrequency?.let { frequency ->
            if (frequency.perCore) {
                val average = formatCpuFrequency(frequency.averageKHz)
                val max = frequency.maxSupportedKHz?.let { " · 上限 ${formatCpuFrequency(it)}" }.orEmpty()
                "当前均值 $average$max"
            } else {
                frequency.entries.joinToString(" · ") { "${it.label} ${formatCpuFrequency(it.currentKHz)}" }
            }
        } ?: "系统未开放读取"
        DeviceProgressMetric(
            "CPU 实时频率",
            cpuFrequencyValue,
            cpuFrequency?.progress ?: 0f,
        )
        cpuFrequency?.let { frequency ->
            DeviceToolsText(
                if (frequency.perCore) "各核心频率" else "各簇频率",
                DeviceTextKind.Label,
            )
            CpuFrequencyGrid(frequency.entries)
        }
        val ramPercent = usagePercent(snapshot.ramUsed, snapshot.ramTotal)
        DeviceProgressMetric(
            "运行内存",
            if (snapshot.ramTotal > 0L) "${formatBytes(snapshot.ramUsed)} / ${formatBytes(snapshot.ramTotal)} · $ramPercent%"
            else "读取中…",
            ramPercent / 100f,
        )
        val storagePercent = usagePercent(snapshot.storageUsed, snapshot.storageTotal)
        DeviceProgressMetric(
            "内部存储",
            if (snapshot.storageTotal > 0L) "${formatBytes(snapshot.storageUsed)} / ${formatBytes(snapshot.storageTotal)} · $storagePercent%"
            else "读取中…",
            storagePercent / 100f,
        )
        val batteryPercent = snapshot.batteryPercent
        DeviceProgressMetric(
            label = "电池电量",
            value = batteryPercent?.let { "$it%" } ?: "暂不可用",
            progress = (batteryPercent ?: 0) / 100f,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DeviceValueTile(
                title = "电池电压",
                value = snapshot.batteryVoltageMv?.takeIf { it > 0 }?.let {
                    String.format(Locale.getDefault(), "%.2f V", it / 1000f)
                } ?: "暂不可用",
                modifier = Modifier.weight(1f),
            )
            DeviceValueTile(
                title = "电池温度",
                value = snapshot.batteryTemperatureC?.let {
                    String.format(Locale.getDefault(), "%.1f °C", it)
                } ?: "暂不可用",
                modifier = Modifier.weight(1f),
            )
        }
        DeviceDataRow(
            "电池状态",
            buildString {
                append(if (snapshot.charging) "充电中" else "未充电")
            },
        )
        DeviceDataRow("设备运行时间", snapshot.uptime)
        DeviceToolsText("展开时约每 2 秒刷新；系统不允许读取的指标会显示为不可读取。", DeviceTextKind.Caption)
    }
}

@Composable
private fun ScreenInformationContent(snapshot: HomeDeviceSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DeviceDataRow("物理分辨率", snapshot.resolution)
        DeviceDataRow("逻辑尺寸", snapshot.logicalSize)
        DeviceDataRow("显示刷新率", snapshot.refreshRate)
        DeviceDataRow("像素密度", snapshot.density)
        DeviceDataRow("估算屏幕尺寸", snapshot.physicalSize)
        DeviceDataRow("屏幕亮度", snapshot.brightness)
        DeviceDataRow("屏幕方向", snapshot.orientation)
    }
}

@Composable
private fun DeviceInformationContent(snapshot: HomeDeviceSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DeviceDataRow("手机型号", snapshot.model)
        DeviceDataRow("Android 版本", snapshot.androidVersion)
        DeviceDataRow("One UI 版本", snapshot.oneUiVersion)
        DeviceDataRow("设备代号", snapshot.deviceCode)
        DeviceDataRow("SoC / 芯片", snapshot.soc)
        DeviceDataRow("CPU 架构", snapshot.cpuArchitecture)
        DeviceDataRow("CPU 核心数", snapshot.cpuCores)
        DeviceDataRow("系统构建号", snapshot.buildId)
        DeviceDataRow("内核版本", snapshot.kernel)
        DeviceDataRow("安全补丁", snapshot.securityPatch)
    }
}

@Composable
private fun DeviceProgressMetric(label: String, value: String, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DeviceToolsText(label, DeviceTextKind.Label, Modifier.weight(1f))
            DeviceToolsText(value, DeviceTextKind.Value, Modifier.weight(2f), TextAlign.End)
        }

        val animatedProgress by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 550),
            label = "deviceMetricProgress",
        )
        val infiniteTransition = rememberInfiniteTransition(label = "deviceWavyProgress")
        val wavePhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_700, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "deviceWavyProgressPhase",
        )
        val accent = deviceAccentColor()
        val track = deviceSecondaryColor().copy(alpha = 0.16f)

        Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
            val strokeWidth = 3.dp.toPx()
            val centerY = size.height / 2f
            drawLine(
                color = track,
                start = Offset(strokeWidth / 2f, centerY),
                end = Offset(size.width - strokeWidth / 2f, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            val progressEnd = size.width * animatedProgress
            if (progressEnd > 0f) {
                val wavelength = 20.dp.toPx()
                val amplitude = 2.dp.toPx()
                val step = 1.5.dp.toPx().coerceAtLeast(1f)
                val path = Path()
                var x = 0f
                var isFirst = true
                while (x < progressEnd) {
                    val y = centerY + sin((x / wavelength * 2.0 * PI) + wavePhase) * amplitude
                    if (isFirst) {
                        path.moveTo(x, y.toFloat())
                        isFirst = false
                    } else {
                        path.lineTo(x, y.toFloat())
                    }
                    x += step
                }
                val endY = centerY + sin((progressEnd / wavelength * 2.0 * PI) + wavePhase) * amplitude
                if (isFirst) {
                    path.moveTo(0f, endY.toFloat())
                } else {
                    path.lineTo(progressEnd, endY.toFloat())
                }
                clipRect(right = progressEnd) {
                    drawPath(
                        path = path,
                        color = accent,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}

@Composable
private fun CpuFrequencyGrid(entries: List<CpuFrequencyEntry>) {
    // Keep a constant four-column grid so changing clock digits never shifts
    // neighboring CPU frequencies horizontally.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.chunked(4).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowEntries.forEach { entry ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(deviceTileColor())
                            .border(1.dp, deviceTileOutlineColor(), RoundedCornerShape(12.dp))
                            .padding(horizontal = 3.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        DeviceToolsText(
                            entry.label,
                            DeviceTextKind.Caption,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        DeviceToolsText(
                            formatCpuFrequency(entry.currentKHz),
                            DeviceTextKind.Value,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                repeat(4 - rowEntries.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DeviceValueTile(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(deviceTileColor())
            .border(1.dp, deviceTileOutlineColor(), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DeviceToolsText(title, DeviceTextKind.Caption)
        DeviceToolsText(value, DeviceTextKind.Large)
    }
}

@Composable
private fun DeviceDataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        DeviceToolsText(label, DeviceTextKind.Label, Modifier.weight(0.42f))
        DeviceToolsText(value, DeviceTextKind.Value, Modifier.weight(0.58f), TextAlign.End)
    }
}

@Composable
private fun DeviceToolsDivider() {
    Box(
        modifier = Modifier.fillMaxWidth().height(1.dp)
            .background(deviceDividerColor()),
    )
}

@Composable
private fun DeviceToolsIcon(
    imageVector: ImageVector,
    description: String?,
    modifier: Modifier = Modifier,
) {
    val tint = deviceAccentColor()
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialIcon(imageVector, description, modifier, tint = tint)
        UiMode.Miuix -> MiuixIcon(imageVector, description, modifier, tint = tint)
    }
}

@Composable
private fun DeviceToolsText(
    text: String,
    kind: DeviceTextKind,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    val color = when (kind) {
        DeviceTextKind.Label, DeviceTextKind.Caption -> deviceSecondaryColor()
        else -> deviceContentColor()
    }
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialText(
            text = text,
            modifier = modifier,
            style = when (kind) {
                DeviceTextKind.Title -> MaterialTheme.typography.titleMedium
                DeviceTextKind.Section -> MaterialTheme.typography.titleSmall
                DeviceTextKind.Label -> MaterialTheme.typography.labelMedium
                DeviceTextKind.Value -> MaterialTheme.typography.bodyMedium
                DeviceTextKind.Large -> MaterialTheme.typography.titleLarge
                DeviceTextKind.Caption -> MaterialTheme.typography.bodySmall
            },
            color = color,
            fontWeight = when (kind) {
                DeviceTextKind.Title, DeviceTextKind.Section, DeviceTextKind.Large -> FontWeight.SemiBold
                else -> FontWeight.Normal
            },
            textAlign = textAlign,
            overflow = TextOverflow.Ellipsis,
        )
        UiMode.Miuix -> MiuixText(
            text = text,
            modifier = modifier,
            fontSize = when (kind) {
                DeviceTextKind.Title -> 18.sp
                DeviceTextKind.Section -> 16.sp
                DeviceTextKind.Label -> 13.sp
                DeviceTextKind.Value -> 14.sp
                DeviceTextKind.Large -> 20.sp
                DeviceTextKind.Caption -> 12.sp
            },
            fontWeight = when (kind) {
                DeviceTextKind.Title, DeviceTextKind.Section, DeviceTextKind.Large -> FontWeight.SemiBold
                DeviceTextKind.Label -> FontWeight.Medium
                else -> FontWeight.Normal
            },
            color = color,
            textAlign = textAlign,
        )
    }
}

@Composable
private fun deviceContentColor(): Color = when (LocalUiMode.current) {
    UiMode.Material -> MaterialTheme.colorScheme.onSurface
    UiMode.Miuix -> MiuixTheme.colorScheme.onSurface
}

@Composable
private fun deviceSecondaryColor(): Color = when (LocalUiMode.current) {
    UiMode.Material -> MaterialTheme.colorScheme.onSurfaceVariant
    UiMode.Miuix -> MiuixTheme.colorScheme.onSurfaceVariantSummary
}

@Composable
private fun deviceAccentColor(): Color = when (LocalUiMode.current) {
    UiMode.Material -> MaterialTheme.colorScheme.primary
    UiMode.Miuix -> MiuixTheme.colorScheme.primary
}

@Composable
private fun deviceTileColor(): Color = when (LocalUiMode.current) {
    // Material's tonal surface container and Miuix's own surface variant keep
    // the telemetry tiles visibly tied to each theme rather than a tinted overlay.
    UiMode.Material -> MaterialTheme.colorScheme.surfaceContainerLow
    UiMode.Miuix -> MiuixTheme.colorScheme.surfaceVariant
}

@Composable
private fun deviceTileOutlineColor(): Color = when (LocalUiMode.current) {
    UiMode.Material -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
    UiMode.Miuix -> MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.22f)
}

@Composable
private fun deviceDividerColor(): Color = when (LocalUiMode.current) {
    UiMode.Material -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f)
    UiMode.Miuix -> MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.28f)
}

@Composable
private fun rememberHomeDeviceSnapshot(expanded: Boolean): HomeDeviceSnapshot {
    val context = LocalContext.current.applicationContext
    var snapshot by remember(context) { mutableStateOf(HomeDeviceSnapshot()) }

    LaunchedEffect(context, expanded) {
        if (!expanded) return@LaunchedEffect
        while (isActive) {
            snapshot = withContext(Dispatchers.IO) {
                readHomeDeviceSnapshot(context, readCpuFrequency())
            }
            delay(2_000)
        }
    }
    return snapshot
}

/**
 * Reads live CPU clocks in kHz from the kernel CPUFreq sysfs interface.
 * Prefer per-core values and use policy-level values only when per-core paths
 * are unavailable on this device/kernel.
 */
private fun readCpuFrequency(): CpuFrequencySnapshot? {
    val cpuRoot = File("/sys/devices/system/cpu")
    val cpuDirectories = cpuRoot.listFiles()
        .orEmpty()
        .filter { it.isDirectory && it.name.matches(Regex("^cpu\\d+$")) }
        .sortedBy { it.name.removePrefix("cpu").toIntOrNull() ?: Int.MAX_VALUE }

    val coreFrequencies = cpuDirectories.mapNotNull { cpuDirectory ->
        val cpuId = cpuDirectory.name.removePrefix("cpu")
        val current = readFrequencyKHz(
            File(cpuDirectory, "cpufreq/scaling_cur_freq"),
            File(cpuDirectory, "cpufreq/cpuinfo_cur_freq"),
        ) ?: return@mapNotNull null
        val max = readFrequencyKHz(
            File(cpuDirectory, "cpufreq/scaling_max_freq"),
            File(cpuDirectory, "cpufreq/cpuinfo_max_freq"),
        )
        CpuFrequencyEntry(label = "核心$cpuId", currentKHz = current, maxKHz = max)
    }
    if (coreFrequencies.isNotEmpty()) return CpuFrequencySnapshot(coreFrequencies, perCore = true)

    val policyRoot = File("/sys/devices/system/cpu/cpufreq")
    val policyDirectories = policyRoot.listFiles()
        .orEmpty()
        .filter { it.isDirectory && it.name.matches(Regex("^policy\\d+$")) }
        .sortedBy { it.name.removePrefix("policy").toIntOrNull() ?: Int.MAX_VALUE }

    val policyFrequencies = policyDirectories.mapNotNull { policyDirectory ->
        val current = readFrequencyKHz(
            File(policyDirectory, "scaling_cur_freq"),
            File(policyDirectory, "cpuinfo_cur_freq"),
        ) ?: return@mapNotNull null
        val max = readFrequencyKHz(
            File(policyDirectory, "scaling_max_freq"),
            File(policyDirectory, "cpuinfo_max_freq"),
        )
        CpuFrequencyEntry(
            label = "簇${policyDirectory.name.removePrefix("policy")}",
            currentKHz = current,
            maxKHz = max,
        )
    }
    return policyFrequencies.takeIf { it.isNotEmpty() }
        ?.let { CpuFrequencySnapshot(it, perCore = false) }
}

private fun readFrequencyKHz(vararg candidates: File): Long? {
    for (file in candidates) {
        val value = runCatching { file.readText().trim().toLongOrNull() }.getOrNull()
        if (value != null && value > 0L) return value
    }
    return null
}

private fun formatCpuFrequency(kHz: Double): String {
    return if (kHz >= 1_000_000.0) {
        String.format(Locale.getDefault(), "%.2f GHz", kHz / 1_000_000.0)
    } else {
        String.format(Locale.getDefault(), "%.0f MHz", kHz / 1_000.0)
    }
}

private fun formatCpuFrequency(kHz: Long): String = formatCpuFrequency(kHz.toDouble())

private fun readHomeDeviceSnapshot(context: Context, cpuFrequency: CpuFrequencySnapshot?): HomeDeviceSnapshot {
    val memoryInfo = ActivityManager.MemoryInfo()
    runCatching {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
    }
    val ramTotal = memoryInfo.totalMem.coerceAtLeast(0L)
    val ramAvailable = memoryInfo.availMem.coerceAtLeast(0L)

    val (storageTotal, storageAvailable) = runCatching {
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        stat.totalBytes to stat.availableBytes
    }.getOrDefault(0L to 0L)

    val batteryIntent = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }.getOrNull()
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val batteryFromManager = runCatching {
        batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }.getOrNull()?.takeIf { it in 0..100 }
    val batteryFromBroadcast = batteryIntent?.let { intent ->
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) (level * 100f / scale).roundToInt().coerceIn(0, 100) else null
    }
    val batteryTemperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        ?.takeIf { it >= 0 }?.div(10f)
    val batteryVoltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }
    val batteryStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
        batteryStatus == BatteryManager.BATTERY_STATUS_FULL
    val screen = readScreenInformation(context)
    val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Build.SOC_MODEL.takeIf { it.isNotBlank() } ?: Build.HARDWARE
    } else Build.HARDWARE

    return HomeDeviceSnapshot(
        cpuFrequency = cpuFrequency,
        ramUsed = (ramTotal - ramAvailable).coerceAtLeast(0L),
        ramTotal = ramTotal,
        storageUsed = (storageTotal - storageAvailable).coerceAtLeast(0L),
        storageTotal = storageTotal,
        batteryPercent = batteryFromManager ?: batteryFromBroadcast,
        batteryTemperatureC = batteryTemperature,
        batteryVoltageMv = batteryVoltage,
        charging = charging,
        uptime = formatUptime(SystemClock.elapsedRealtime()),
        resolution = screen.resolution,
        logicalSize = screen.logicalSize,
        refreshRate = screen.refreshRate,
        density = screen.density,
        physicalSize = screen.physicalSize,
        brightness = screen.brightness,
        orientation = screen.orientation,
        model = listOf(Build.MANUFACTURER, Build.MODEL).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "未知设备" },
        androidVersion = "Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}",
        oneUiVersion = formatOneUiVersion(readSystemProperty("ro.build.version.oneui")).ifBlank { "—" },
        deviceCode = Build.DEVICE.ifBlank { Build.PRODUCT.ifBlank { "—" } },
        soc = soc.ifBlank { "—" },
        cpuArchitecture = Build.SUPPORTED_ABIS.joinToString(" / ").ifBlank { System.getProperty("os.arch") ?: "—" },
        cpuCores = "${Runtime.getRuntime().availableProcessors()} 核",
        buildId = Build.DISPLAY.ifBlank { Build.ID.ifBlank { "—" } },
        kernel = System.getProperty("os.version")?.ifBlank { "—" } ?: "—",
        securityPatch = Build.VERSION.SECURITY_PATCH.ifBlank { "—" },
    )
}

private data class ScreenInformation(
    val resolution: String = "—",
    val logicalSize: String = "—",
    val refreshRate: String = "—",
    val density: String = "—",
    val physicalSize: String = "—",
    val brightness: String = "—",
    val orientation: String = "—",
)

@Suppress("DEPRECATION")
private fun readScreenInformation(context: Context): ScreenInformation = runCatching {
    val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = manager.defaultDisplay
    val metrics = DisplayMetrics()
    display.getRealMetrics(metrics)
    val width = metrics.widthPixels
    val height = metrics.heightPixels
    val densityDpi = metrics.densityDpi.takeIf { it > 0 }
        ?: context.resources.displayMetrics.densityDpi.coerceAtLeast(1)
    val densityScale = densityDpi / 160f
    val widthDp = (width / densityScale).roundToInt()
    val heightDp = (height / densityScale).roundToInt()
    val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        runCatching { display.mode.refreshRate }.getOrNull()?.takeIf { it > 0f } ?: display.refreshRate
    } else display.refreshRate
    val diagonalInches = if (metrics.xdpi > 0f && metrics.ydpi > 0f) {
        sqrt((width / metrics.xdpi) * (width / metrics.xdpi) +
            (height / metrics.ydpi) * (height / metrics.ydpi))
    } else 0f
    val brightness = runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            .let { (it * 100f / 255f).roundToInt().coerceIn(0, 100) }
    }.getOrNull()
    ScreenInformation(
        resolution = "${width} × ${height} px",
        logicalSize = "${widthDp} × ${heightDp} dp",
        refreshRate = String.format(Locale.getDefault(), "%.2f Hz", refreshRate),
        density = "$densityDpi dpi",
        physicalSize = if (diagonalInches > 0f) {
            String.format(Locale.getDefault(), "约 %.2f 英寸", diagonalInches)
        } else "设备未提供有效数据",
        brightness = brightness?.let { "系统亮度设置 $it%" } ?: "系统未提供亮度值",
        orientation = if (width >= height) "横屏" else "竖屏",
    )
}.getOrDefault(ScreenInformation())

private fun readSystemProperty(name: String): String = runCatching {
    val properties = Class.forName("android.os.SystemProperties")
    val getter = properties.getMethod("get", String::class.java, String::class.java)
    getter.invoke(null, name, "") as? String ?: ""
}.getOrDefault("")

private fun formatOneUiVersion(rawValue: String): String {
    val raw = rawValue.trim()
    if (raw.isBlank()) return ""
    if (raw.contains('.')) return raw
    val encoded = raw.toLongOrNull() ?: return raw
    if (encoded >= 10_000L) return String.format(Locale.US, "%.1f", encoded / 10_000.0)
    return raw
}

private fun usagePercent(used: Long, total: Long): Int =
    if (total > 0L) ((used.toDouble() / total.toDouble()) * 100.0).roundToInt().coerceIn(0, 100) else 0

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val gib = bytes / 1073741824.0
    return if (gib >= 1.0) String.format(Locale.getDefault(), "%.1f GB", gib)
    else String.format(Locale.getDefault(), "%.0f MB", bytes / 1048576.0)
}

private fun formatUptime(elapsedMillis: Long): String {
    val totalMinutes = elapsedMillis / 60_000L
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60
    return buildString {
        if (days > 0) append(days).append(" 天 ")
        if (hours > 0 || days > 0) append(hours).append(" 小时 ")
        append(minutes).append(" 分钟")
    }.trim()
}
