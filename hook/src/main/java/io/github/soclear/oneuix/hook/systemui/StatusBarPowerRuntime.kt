package io.github.soclear.oneuix.hook.systemui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import java.io.File
import java.util.Locale
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Samsung SystemUI runtime for Flux-style status-bar power/thermal information.
 *
 * The view is injected into Samsung's right-side indicator container, while the
 * data collector only runs when the feature is enabled. Preference changes are
 * delivered by the existing status-bar runtime bridge, so slider previews do not
 * write preference.json repeatedly.
 */
internal object StatusBarPowerRuntime {
    private data class Holder(
        val statusBarView: View,
        val root: LinearLayout,
        val row1: LinearLayout,
        val row2: LinearLayout,
        val slots: List<TextView>,
    )

    private data class Sample(
        val powerW: Double?,
        val currentMa: Double?,
        val batteryTempC: Double?,
        val cpuTempC: Double?,
        val voltageV: Double?,
        val batteryLevel: Double?,
    )

    private val holders = WeakHashMap<View, Holder>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var config = Preference.SystemUI.StatusBarPowerRuntime()

    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null
    private var thermalCandidates: List<File>? = null

    private val sampleRunnable = object : Runnable {
        override fun run() {
            val currentConfig = config
            if (!currentConfig.enabled) return

            val context = synchronized(holders) {
                holders.values.firstOrNull()?.statusBarView?.context
            } ?: return

            val sample = readSample(context, currentConfig)
            mainHandler.post {
                renderSample(sample, currentConfig)
            }

            workerHandler?.postDelayed(
                this,
                currentConfig.refreshIntervalMs.coerceIn(500, 5000).toLong(),
            )
        }
    }

    fun registerStatusBarView(statusBarView: View) {
        statusBarView.post {
            val holder = synchronized(holders) {
                holders[statusBarView] ?: createHolder(statusBarView)?.also {
                    holders[statusBarView] = it
                }
            } ?: return@post
            applyConfig(holder, config)
            refreshSampling()
        }
    }

    fun updateRuntime(newConfig: Preference.SystemUI.StatusBarPowerRuntime) {
        config = sanitize(newConfig)
        val active = synchronized(holders) { holders.values.toList() }
        active.forEach { holder ->
            holder.root.post { applyConfig(holder, config) }
        }
        refreshSampling()
    }

    private fun sanitize(
        source: Preference.SystemUI.StatusBarPowerRuntime,
    ): Preference.SystemUI.StatusBarPowerRuntime = source.copy(
        slot1Decimals = source.slot1Decimals.coerceIn(0, 2),
        slot2Decimals = source.slot2Decimals.coerceIn(0, 2),
        slot3Decimals = source.slot3Decimals.coerceIn(0, 2),
        slot4Decimals = source.slot4Decimals.coerceIn(0, 2),
        refreshIntervalMs = source.refreshIntervalMs.coerceIn(500, 5000),
        textSizeSp = source.textSizeSp.coerceIn(6f, 14f),
        fontWeight = source.fontWeight.coerceIn(100, 900),
        columnGapDp = source.columnGapDp.coerceIn(0f, 12f),
        horizontalOffsetDp = source.horizontalOffsetDp.coerceIn(-30f, 30f),
        verticalOffsetDp = source.verticalOffsetDp.coerceIn(-12f, 12f),
        startPaddingDp = source.startPaddingDp.coerceIn(0f, 12f),
        endPaddingDp = source.endPaddingDp.coerceIn(0f, 12f),
    )

    private fun createHolder(statusBarView: View): Holder? {
        val parent = findRightIndicatorArea(statusBarView) ?: return null
        val context = statusBarView.context

        fun row() = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val row1 = row()
        val row2 = row()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            clipChildren = false
            clipToPadding = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            addView(row1)
            addView(row2)
        }

        val slots = List(4) {
            TextView(context).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER
                setSingleLine(true)
                maxLines = 1
            }
        }

        parent.addView(root, 0)
        return Holder(statusBarView, root, row1, row2, slots)
    }

    private fun findRightIndicatorArea(statusBarView: View): ViewGroup? {
        val resources = statusBarView.resources
        val names = listOf(
            "system_icon_area",
            "status_bar_right_side",
            "status_bar_end_side_content",
            "status_icon_area",
            "status_bar_end_side",
        )
        names.forEach { name ->
            val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
            if (id != 0) {
                (statusBarView.findViewById<View>(id) as? ViewGroup)?.let { return it }
            }
        }
        return null
    }

    private fun applyConfig(
        holder: Holder,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ) {
        holder.root.visibility = if (
            current.enabled && !shouldHideForLockscreen(holder.root.context, current)
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
        if (!current.enabled) return

        rebuildRows(holder, current)
        val density = holder.root.resources.displayMetrics.density
        holder.root.translationX = current.horizontalOffsetDp * density
        holder.root.translationY = current.verticalOffsetDp * density
        holder.root.setPaddingRelative(
            (current.startPaddingDp * density).roundToInt(),
            0,
            (current.endPaddingDp * density).roundToInt(),
            0,
        )

        val color = resolveIndicatorTextColor(holder.statusBarView)
        val gapPx = (current.columnGapDp * density).roundToInt()
        holder.slots.forEachIndexed { index, textView ->
            textView.setTextColor(color)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, current.textSizeSp)
            textView.typeface = Typeface.create(Typeface.DEFAULT, current.fontWeight, false)
            textView.setPaddingRelative(
                if (index % 2 == 1 || !current.doubleLine && index > 0) gapPx else 0,
                0,
                0,
                0,
            )
        }
    }

    private fun rebuildRows(
        holder: Holder,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ) {
        holder.row1.removeAllViews()
        holder.row2.removeAllViews()
        if (current.doubleLine) {
            holder.row1.addView(holder.slots[0])
            holder.row1.addView(holder.slots[1])
            holder.row2.addView(holder.slots[2])
            holder.row2.addView(holder.slots[3])
            holder.row2.visibility = View.VISIBLE
        } else {
            holder.slots.forEach(holder.row1::addView)
            holder.row2.visibility = View.GONE
        }
    }

    private fun refreshSampling() {
        val current = config
        if (!current.enabled) {
            workerHandler?.removeCallbacks(sampleRunnable)
            return
        }
        ensureWorker().removeCallbacks(sampleRunnable)
        ensureWorker().post(sampleRunnable)
    }

    private fun ensureWorker(): Handler {
        workerHandler?.let { return it }
        val thread = HandlerThread("OneuiXPStatusPower").apply { start() }
        workerThread = thread
        return Handler(thread.looper).also { workerHandler = it }
    }

    private fun readSample(
        context: Context,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ): Sample {
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val currentUa = batteryManager
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            ?.takeUnless { it == Int.MIN_VALUE }
        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val voltageMv = batteryIntent
            ?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            ?.takeIf { it > 0 }
        val tempTenths = batteryIntent
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?.takeUnless { it == Int.MIN_VALUE }
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100

        val signedCurrentMa = currentUa?.toDouble()?.div(1000.0)
        val currentMa = signedCurrentMa?.let { if (current.absoluteValue) abs(it) else it }
        val signedPowerW = if (currentUa != null && voltageMv != null) {
            currentUa.toDouble() / 1_000_000.0 * voltageMv.toDouble() / 1000.0
        } else {
            null
        }
        val powerW = signedPowerW?.let { if (current.absoluteValue) abs(it) else it }

        return Sample(
            powerW = powerW,
            currentMa = currentMa,
            batteryTempC = tempTenths?.toDouble()?.div(10.0),
            cpuTempC = readCpuTemperature(),
            voltageV = voltageMv?.toDouble()?.div(1000.0),
            batteryLevel = if (level >= 0 && scale > 0) level * 100.0 / scale else null,
        )
    }

    private fun readCpuTemperature(): Double? {
        val candidates = thermalCandidates ?: discoverCpuThermalFiles().also {
            thermalCandidates = it
        }
        return candidates.mapNotNull(::readThermalFile).maxOrNull()
    }

    private fun discoverCpuThermalFiles(): List<File> {
        val thermalRoot = File("/sys/class/thermal")
        val zones = thermalRoot.listFiles()
            ?.filter { it.name.startsWith("thermal_zone") && it.isDirectory }
            .orEmpty()

        val preferred = zones.filter { zone ->
            val type = runCatching { File(zone, "type").readText().trim().lowercase() }
                .getOrDefault("")
            listOf("cpu", "soc", "ap", "cluster", "big", "little").any(type::contains)
        }.map { File(it, "temp") }.filter(File::canRead)

        if (preferred.isNotEmpty()) return preferred
        return zones.map { File(it, "temp") }.filter(File::canRead)
    }

    private fun readThermalFile(file: File): Double? {
        val raw = runCatching { file.readText().trim().toDouble() }.getOrNull() ?: return null
        val value = if (abs(raw) > 1000.0) raw / 1000.0 else raw
        return value.takeIf { it in -20.0..150.0 }
    }

    private fun renderSample(
        sample: Sample,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ) {
        val active = synchronized(holders) { holders.values.toList() }
        active.forEach { holder ->
            applyConfig(holder, current)
            if (holder.root.visibility != View.VISIBLE) return@forEach
            val keys = listOf(current.slot1, current.slot2, current.slot3, current.slot4)
            val decimals = listOf(
                current.slot1Decimals,
                current.slot2Decimals,
                current.slot3Decimals,
                current.slot4Decimals,
            )
            holder.slots.forEachIndexed { index, textView ->
                val rendered = renderValue(keys[index], decimals[index], sample, current.hideUnit)
                textView.text = rendered
                textView.visibility = if (keys[index] == "none") View.GONE else View.VISIBLE
            }
        }
    }

    private fun renderValue(
        key: String,
        decimals: Int,
        sample: Sample,
        hideUnit: Boolean,
    ): String {
        val safeDecimals = decimals.coerceIn(0, 2)
        val value: Double?
        val unit: String
        when (key) {
            "current" -> {
                value = sample.currentMa
                unit = "mA"
            }
            "battery_temp" -> {
                value = sample.batteryTempC
                unit = "°C"
            }
            "cpu_temp" -> {
                value = sample.cpuTempC
                unit = "°C"
            }
            "voltage" -> {
                value = sample.voltageV
                unit = "V"
            }
            "battery_level" -> {
                value = sample.batteryLevel
                unit = "%"
            }
            "none" -> return ""
            else -> {
                value = sample.powerW
                unit = "W"
            }
        }
        if (value == null) return "--"
        val number = String.format(Locale.US, ".${safeDecimals}f", value)
        return if (hideUnit) number else number + unit
    }

    private fun shouldHideForLockscreen(
        context: Context,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ): Boolean {
        if (!current.hideOnLockscreen) return false
        return context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
    }

    private fun resolveIndicatorTextColor(statusBarView: View): Int {
        val resources = statusBarView.resources
        listOf("clock", "status_bar_clock").forEach { name ->
            val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
            if (id != 0) {
                (statusBarView.findViewById<View>(id) as? TextView)?.let {
                    return it.currentTextColor
                }
            }
        }
        return Color.WHITE
    }
}
