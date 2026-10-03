package io.github.soclear.oneuix.hook.systemui

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarRuntimeBridge
import io.github.soclear.oneuix.hook.util.PreferenceProvider
import java.io.File
import java.util.Locale
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt

/** Samsung SystemUI runtime for Flux-style status-bar power/thermal information. */
internal object StatusBarPowerRuntime {
    private const val PREFERENCE_CHANGED_URI =
        "content://com.oneuixp.app.statusbridge/preferences"

    private data class Holder(
        val statusBarView: ViewGroup,
        var inlineParent: ViewGroup,
        val root: LinearLayout,
        val row1: LinearLayout,
        val row2: LinearLayout,
        val slots: List<TextView>,
        var mountedSide: String = "right",
        var overlayMode: Boolean = false,
        var overlayAnchorX: Float = 0f,
        var overlayAnchorUsesRightEdge: Boolean = true,
        var overlayHostWidth: Int = 0,
        var overlayHostHeight: Int = 0,
        var overlayOrientation: Int = Configuration.ORIENTATION_UNDEFINED,
        var lastLayoutKey: String = "",
    )

    private data class Sample(
        val powerW: Double?,
        val currentMa: Double?,
        val batteryTempC: Double?,
        val cpuTempC: Double?,
        val voltageV: Double?,
        val batteryLevel: Double?,
    )

    private val installed = AtomicBoolean(false)
    private val watcherStarted = AtomicBoolean(false)
    private val configurationReceiverStarted = AtomicBoolean(false)
    private val holders = WeakHashMap<View, Holder>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var config = Preference.SystemUI.StatusBarPowerRuntime()

    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null
    private var preferenceThread: HandlerThread? = null
    private var thermalCandidates: List<File>? = null
    private var preferenceObserver: ContentObserver? = null
    private var runtimeReceiver: BroadcastReceiver? = null
    private var configurationReceiver: BroadcastReceiver? = null

    private val configurationReanchorRunnable = Runnable {
        val active = synchronized(holders) { holders.values.toList() }
        active.forEach { holder ->
            if (!holder.overlayMode) return@forEach
            val host = holder.statusBarView
            if (host.width <= 0 || host.height <= 0) return@forEach
            val orientation = host.resources.configuration.orientation
            val geometryChanged = orientation != holder.overlayOrientation ||
                host.width != holder.overlayHostWidth ||
                host.height != holder.overlayHostHeight
            if (!geometryChanged) return@forEach

            refreshOverlayAnchor(holder, config, force = true)
            holder.overlayOrientation = orientation
            positionTrueOverlay(holder, config)
        }
    }

    private val sampleRunnable = object : Runnable {
        override fun run() {
            val currentConfig = config
            if (!currentConfig.enabled) return

            val context = synchronized(holders) {
                holders.values.firstOrNull()?.statusBarView?.context
            } ?: return

            val sample = readSample(context, currentConfig)
            mainHandler.post { renderSample(sample, currentConfig) }
            workerHandler?.postDelayed(
                this,
                currentConfig.refreshIntervalMs.coerceIn(500, 5000).toLong(),
            )
        }
    }

    fun install(
        module: XposedModule,
        param: XposedModuleInterface.PackageReadyParam,
    ) {
        if (param.packageName != Package.SYSTEMUI) return
        if (!installed.compareAndSet(false, true)) return

        runCatching {
            val clazz = param.classLoader.loadClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView"
            )
            clazz.declaredConstructors.forEach { constructor ->
                module.hook(constructor).intercept { chain ->
                    val result = chain.proceed()
                    (chain.thisObject as? View)?.let { statusBarView ->
                        statusBarView.post {
                            registerStatusBarView(statusBarView)
                            ensurePreferenceWatcher(module, statusBarView.context)
                            ensureConfigurationReceiver(statusBarView.context)
                        }
                    }
                    result
                }
            }
        }
    }

    private fun ensurePreferenceWatcher(module: XposedModule, context: Context) {
        if (!watcherStarted.compareAndSet(false, true)) return
        val worker = HandlerThread("OneuiXPStatusPowerPrefs").apply { start() }
        preferenceThread = worker
        val background = Handler(worker.looper)
        var previous: Preference.SystemUI.StatusBarPowerRuntime? = null

        fun refreshOnce() {
            background.post {
                val current = runCatching {
                    with(module) { PreferenceProvider.loadPreference() }
                        ?.systemUI?.statusBar?.powerRuntime
                }.getOrNull() ?: return@post
                if (current == previous) return@post
                previous = current
                mainHandler.post { updateRuntime(current) }
            }
        }

        val observer = object : ContentObserver(background) {
            override fun onChange(selfChange: Boolean) = refreshOnce()
            override fun onChange(selfChange: Boolean, uri: Uri?) = refreshOnce()
        }
        preferenceObserver = observer
        runCatching {
            context.contentResolver.registerContentObserver(
                Uri.parse(PREFERENCE_CHANGED_URI),
                false,
                observer,
            )
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != StatusBarRuntimeBridge.ACTION) return
                val payload = intent.getStringExtra(
                    StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON
                ) ?: return
                background.post {
                    val statusBar = runCatching {
                        IgnoreUnknownKeysJson.decodeFromString(
                            Preference.SystemUI.StatusBar.serializer(),
                            payload,
                        )
                    }.getOrNull() ?: return@post
                    val current = statusBar.powerRuntime
                    previous = current
                    mainHandler.post { updateRuntime(current) }
                }
            }
        }
        runtimeReceiver = receiver
        runCatching {
            val filter = IntentFilter(StatusBarRuntimeBridge.ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
        }

        refreshOnce()
    }

    private fun ensureConfigurationReceiver(context: Context) {
        if (!configurationReceiverStarted.compareAndSet(false, true)) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_CONFIGURATION_CHANGED) return
                mainHandler.removeCallbacks(configurationReanchorRunnable)
                mainHandler.postDelayed(configurationReanchorRunnable, 300L)
            }
        }
        configurationReceiver = receiver
        val filter = IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
        }
    }

    private fun registerStatusBarView(statusBarView: View) {
        if (!statusBarView.isLaidOut) {
            statusBarView.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int,
                ) {
                    if (!v.isLaidOut) return
                    v.removeOnLayoutChangeListener(this)
                    registerStatusBarView(v)
                }
            })
            return
        }

        val holder = synchronized(holders) {
            holders[statusBarView] ?: createHolder(statusBarView)?.also {
                holders[statusBarView] = it
            }
        } ?: return
        applyConfig(holder, config)
        refreshSampling()
    }

    private fun updateRuntime(newConfig: Preference.SystemUI.StatusBarPowerRuntime) {
        config = sanitize(newConfig)
        val active = synchronized(holders) { holders.values.toList() }
        active.forEach { holder -> holder.root.post { applyConfig(holder, config) } }
        refreshSampling()
    }

    private fun sanitize(
        source: Preference.SystemUI.StatusBarPowerRuntime,
    ): Preference.SystemUI.StatusBarPowerRuntime = source.copy(
        placementSide = if (source.placementSide == "left") "left" else "right",
        slot1Decimals = source.slot1Decimals.coerceIn(0, 2),
        slot2Decimals = source.slot2Decimals.coerceIn(0, 2),
        slot3Decimals = source.slot3Decimals.coerceIn(0, 2),
        slot4Decimals = source.slot4Decimals.coerceIn(0, 2),
        refreshIntervalMs = source.refreshIntervalMs.coerceIn(500, 5000),
        textSizeSp = source.textSizeSp.coerceIn(6f, 18f),
        fontWeight = source.fontWeight.coerceIn(100, 900),
        columnGapDp = source.columnGapDp.coerceIn(0f, 12f),
        rowGapDp = source.rowGapDp.coerceIn(-4f, 8f),
        horizontalOffsetDp = source.horizontalOffsetDp.coerceIn(-300f, 300f),
        verticalOffsetDp = source.verticalOffsetDp.coerceIn(-60f, 60f),
        startPaddingDp = source.startPaddingDp.coerceIn(0f, 12f),
        endPaddingDp = source.endPaddingDp.coerceIn(0f, 12f),
    )

    private fun createHolder(statusBarView: View): Holder? {
        val host = statusBarView as? ViewGroup ?: return null
        val resolved = resolveInlineParent(statusBarView, config.placementSide) ?: return null
        val initialSide = resolved.first
        val inlineParent = resolved.second
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
            isClickable = false
            isFocusable = false
            addView(row1)
            addView(row2)
        }
        val slots = List(4) {
            TextView(context).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER
                setSingleLine(true)
                maxLines = 1
                isClickable = false
                isFocusable = false
            }
        }

        inlineParent.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        val holder = Holder(
            statusBarView = host,
            inlineParent = inlineParent,
            root = root,
            row1 = row1,
            row2 = row2,
            slots = slots,
            mountedSide = initialSide,
            overlayOrientation = host.resources.configuration.orientation,
        )
        addInlineAtEdge(holder)
        inlineParent.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (!holder.overlayMode && holder.root.parent === holder.inlineParent) {
                holder.root.post { addInlineAtEdge(holder) }
            }
        }
        return holder
    }

    private fun findRightIndicatorArea(statusBarView: View): ViewGroup? {
        val resources = statusBarView.resources
        listOf(
            "system_icon_area",
            "status_bar_right_side",
            "status_bar_end_side_content",
            "status_icon_area",
            "status_bar_end_side",
        ).forEach { name ->
            val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
            if (id != 0) {
                (statusBarView.findViewById<View>(id) as? ViewGroup)?.let { return it }
            }
        }
        return null
    }

    private fun findLeftIndicatorArea(statusBarView: View): ViewGroup? {
        val resources = statusBarView.resources
        listOf(
            "status_bar_left_side",
            "status_bar_start_side",
            "status_bar_start_side_content",
            "status_bar_start_side_except_heads_up",
            "notification_icon_area",
            "notification_icon_area_inner",
            "left_clock_container",
        ).forEach { name ->
            val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
            if (id != 0) {
                (statusBarView.findViewById<View>(id) as? ViewGroup)?.let { return it }
            }
        }

        listOf("clock", "status_bar_clock").forEach { name ->
            val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
            if (id != 0) {
                val clock = statusBarView.findViewById<View>(id)
                (clock?.parent as? ViewGroup)?.let { return it }
            }
        }
        return null
    }

    private fun resolveInlineParent(
        statusBarView: View,
        requestedSide: String,
    ): Pair<String, ViewGroup>? {
        if (requestedSide == "left") {
            findLeftIndicatorArea(statusBarView)?.let { return "left" to it }
            findRightIndicatorArea(statusBarView)?.let { return "right" to it }
        } else {
            findRightIndicatorArea(statusBarView)?.let { return "right" to it }
            findLeftIndicatorArea(statusBarView)?.let { return "left" to it }
        }
        return null
    }

    /**
     * Find the direct child in [parent] that owns Samsung's Wi-Fi/Bluetooth icon.
     * Power is inserted immediately before this child, giving the visual order:
     * network speed/location -> power -> Wi-Fi/Bluetooth -> remaining right icons.
     */
    private fun findRightInlineAnchor(
        statusBarView: ViewGroup,
        parent: ViewGroup,
    ): View? {
        val resources = statusBarView.resources
        val names = listOf(
            "wifi_group",
            "wifi_combo",
            "status_bar_wifi",
            "status_bar_wifi_group",
            "wifi_container",
            "modern_status_bar_wifi",
            "bluetooth",
            "bluetooth_icon",
            "status_bar_bluetooth",
        )
        names.forEach { name ->
            val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
            if (id == 0) return@forEach
            val found = statusBarView.findViewById<View>(id) ?: return@forEach
            var direct: View = found
            var owner = direct.parent as? ViewGroup
            while (owner != null && owner !== parent) {
                direct = owner
                owner = direct.parent as? ViewGroup
            }
            if (owner === parent && direct !== parent && direct !== statusBarView) {
                return direct
            }
        }
        return null
    }

    private fun addInlineAtEdge(holder: Holder) {
        val parent = holder.inlineParent
        if (holder.mountedSide == "right") {
            val anchor = findRightInlineAnchor(holder.statusBarView, parent)
            if (anchor != null) {
                val rootIndex = parent.indexOfChild(holder.root)
                val anchorIndex = parent.indexOfChild(anchor)
                if (rootIndex >= 0 && anchorIndex >= 0 && rootIndex == anchorIndex - 1) return

                if (holder.root.parent === parent) {
                    parent.removeView(holder.root)
                } else {
                    (holder.root.parent as? ViewGroup)?.removeView(holder.root)
                }
                val refreshedAnchor = findRightInlineAnchor(holder.statusBarView, parent)
                val targetIndex = refreshedAnchor
                    ?.let(parent::indexOfChild)
                    ?.takeIf { it >= 0 }
                    ?: parent.childCount
                parent.addView(
                    holder.root,
                    targetIndex,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                return
            }
        }

        if (holder.root.parent === parent) {
            val targetIndex = if (holder.mountedSide == "left") 0 else parent.childCount - 1
            if (parent.indexOfChild(holder.root) == targetIndex) return
            parent.removeView(holder.root)
        } else {
            (holder.root.parent as? ViewGroup)?.removeView(holder.root)
        }
        parent.addView(
            holder.root,
            if (holder.mountedSide == "left") 0 else parent.childCount,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    private fun ensureInlineAnchor(
        holder: Holder,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ): Boolean {
        val resolved = resolveInlineParent(holder.statusBarView, current.placementSide) ?: return false
        val desiredSide = resolved.first
        val desiredParent = resolved.second
        if (holder.inlineParent === desiredParent && holder.mountedSide == desiredSide) return true

        if (holder.overlayMode) {
            holder.statusBarView.overlay.remove(holder.root)
            holder.overlayMode = false
        }
        (holder.root.parent as? ViewGroup)?.removeView(holder.root)
        holder.inlineParent = desiredParent
        holder.mountedSide = desiredSide
        holder.root.translationX = 0f
        holder.root.translationY = 0f
        addInlineAtEdge(holder)
        holder.root.post { applyConfig(holder, config) }
        return false
    }

    private fun captureInlineOverlayAnchor(
        holder: Holder,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ) {
        val hostLocation = IntArray(2)
        val rootLocation = IntArray(2)
        holder.statusBarView.getLocationOnScreen(hostLocation)
        holder.root.getLocationOnScreen(rootLocation)
        val density = holder.root.resources.displayMetrics.density
        holder.overlayAnchorUsesRightEdge = holder.mountedSide != "left"
        holder.overlayAnchorX = if (holder.overlayAnchorUsesRightEdge) {
            rootLocation[0] - hostLocation[0] + holder.root.width -
                current.horizontalOffsetDp * density
        } else {
            rootLocation[0] - hostLocation[0] -
                current.horizontalOffsetDp * density
        }
        holder.overlayHostWidth = holder.statusBarView.width
        holder.overlayHostHeight = holder.statusBarView.height
        holder.overlayOrientation = holder.statusBarView.resources.configuration.orientation
    }

    private fun refreshOverlayAnchor(
        holder: Holder,
        current: Preference.SystemUI.StatusBarPowerRuntime,
        force: Boolean = false,
    ) {
        val host = holder.statusBarView
        if (host.width <= 0 || host.height <= 0) return
        if (
            !force && host.width == holder.overlayHostWidth &&
            host.height == holder.overlayHostHeight
        ) return

        val hostLocation = IntArray(2)
        host.getLocationOnScreen(hostLocation)
        var anchored = false

        if (holder.mountedSide == "right") {
            val iconAnchor = findRightInlineAnchor(host, holder.inlineParent)
            if (iconAnchor != null && iconAnchor.isLaidOut) {
                val anchorLocation = IntArray(2)
                iconAnchor.getLocationOnScreen(anchorLocation)
                holder.overlayAnchorUsesRightEdge = true
                holder.overlayAnchorX = (anchorLocation[0] - hostLocation[0]).toFloat()
                anchored = true
            }
        } else if (holder.inlineParent.isLaidOut) {
            val parentLocation = IntArray(2)
            holder.inlineParent.getLocationOnScreen(parentLocation)
            holder.overlayAnchorUsesRightEdge = false
            holder.overlayAnchorX = (parentLocation[0] - hostLocation[0]).toFloat()
            anchored = true
        }

        if (!anchored && holder.overlayHostWidth > 0 && holder.overlayAnchorUsesRightEdge) {
            holder.overlayAnchorX += host.width - holder.overlayHostWidth
        }

        holder.overlayHostWidth = host.width
        holder.overlayHostHeight = host.height
        holder.overlayOrientation = host.resources.configuration.orientation
    }

    private fun ensureMountMode(
        holder: Holder,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ): Boolean {
        if (!ensureInlineAnchor(holder, current)) return false
        val density = holder.root.resources.displayMetrics.density
        if (current.overlayOnTop) {
            if (!holder.overlayMode) {
                if (holder.root.width <= 0 || holder.statusBarView.width <= 0) {
                    holder.root.post { applyConfig(holder, config) }
                    return false
                }

                captureInlineOverlayAnchor(holder, current)
                holder.statusBarView.overlay.add(holder.root)
                holder.overlayMode = true
            }
            positionTrueOverlay(holder, current)
        } else {
            if (holder.overlayMode) {
                holder.statusBarView.overlay.remove(holder.root)
                holder.overlayMode = false
                holder.overlayHostWidth = 0
                holder.overlayHostHeight = 0
                addInlineAtEdge(holder)
            } else {
                addInlineAtEdge(holder)
            }
            holder.root.translationX = current.horizontalOffsetDp * density
            holder.root.translationY = current.verticalOffsetDp * density
        }
        return true
    }

    private fun positionTrueOverlay(
        holder: Holder,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ) {
        if (!holder.overlayMode) return
        val host = holder.statusBarView
        if (host.width <= 0 || host.height <= 0) return

        val density = holder.root.resources.displayMetrics.density
        val widthSpec = View.MeasureSpec.makeMeasureSpec(host.width, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(host.height, View.MeasureSpec.AT_MOST)
        holder.root.measure(widthSpec, heightSpec)

        val width = holder.root.measuredWidth.coerceAtLeast(1)
        val height = holder.root.measuredHeight.coerceAtLeast(1)
        val anchor = holder.overlayAnchorX + current.horizontalOffsetDp * density
        val left: Int
        val right: Int
        if (holder.overlayAnchorUsesRightEdge) {
            right = anchor.roundToInt()
            left = right - width
        } else {
            left = anchor.roundToInt()
            right = left + width
        }
        val top = (
            (host.height - height) / 2f + current.verticalOffsetDp * density
        ).roundToInt()

        holder.root.translationX = 0f
        holder.root.translationY = 0f
        if (
            holder.root.left != left || holder.root.top != top ||
            holder.root.right != right || holder.root.bottom != top + height
        ) {
            holder.root.layout(left, top, right, top + height)
        }
    }

    private fun applyConfig(
        holder: Holder,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ) {
        holder.root.visibility = if (
            current.enabled && !shouldHideForLockscreen(holder.root.context, current)
        ) View.VISIBLE else View.GONE
        if (!current.enabled) return

        if (!ensureMountMode(holder, current)) return

        rebuildRows(holder, current)
        val density = holder.root.resources.displayMetrics.density
        holder.root.setPaddingRelative(
            (current.startPaddingDp * density).roundToInt(),
            0,
            (current.endPaddingDp * density).roundToInt(),
            0,
        )

        val rowGapPx = if (current.doubleLine) {
            (current.rowGapDp * density).roundToInt()
        } else {
            0
        }
        (holder.row2.layoutParams as? LinearLayout.LayoutParams)?.let { params ->
            if (params.topMargin != rowGapPx) {
                params.topMargin = rowGapPx
                holder.row2.layoutParams = params
            }
        }

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

        if (holder.overlayMode) {
            holder.root.post { positionTrueOverlay(holder, current) }
        }
    }

    private fun rebuildRows(
        holder: Holder,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ) {
        val layoutKey = listOf(
            current.doubleLine.toString(),
            current.slot1,
            current.slot2,
            current.slot3,
            current.slot4,
        ).joinToString("|")
        if (holder.lastLayoutKey == layoutKey) return
        holder.lastLayoutKey = layoutKey

        holder.row1.removeAllViews()
        holder.row2.removeAllViews()
        if (current.doubleLine) {
            holder.row1.addView(holder.slots[0])
            holder.row1.addView(holder.slots[1])
            holder.row2.addView(holder.slots[2])
            holder.row2.addView(holder.slots[3])
            holder.row2.visibility = View.VISIBLE
        } else {
            holder.slots.forEach { holder.row1.addView(it) }
            holder.row2.visibility = View.GONE
        }
    }

    private fun refreshSampling() {
        if (!config.enabled) {
            workerHandler?.removeCallbacks(sampleRunnable)
            return
        }
        val handler = ensureWorker()
        handler.removeCallbacks(sampleRunnable)
        handler.post(sampleRunnable)
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
        } else null
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
        val zones = File("/sys/class/thermal").listFiles()
            ?.filter { it.name.startsWith("thermal_zone") && it.isDirectory }
            .orEmpty()
        val preferred = zones.filter { zone ->
            val type = runCatching {
                File(zone, "type").readText().trim().lowercase()
            }.getOrDefault("")
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
            val desiredVisibility = if (
                current.enabled && !shouldHideForLockscreen(holder.root.context, current)
            ) View.VISIBLE else View.GONE
            if (holder.root.visibility != desiredVisibility) {
                holder.root.visibility = desiredVisibility
            }

            val keys = listOf(current.slot1, current.slot2, current.slot3, current.slot4)
            val decimals = listOf(
                current.slot1Decimals,
                current.slot2Decimals,
                current.slot3Decimals,
                current.slot4Decimals,
            )
            holder.slots.forEachIndexed { index, textView ->
                val nextText = renderValue(
                    keys[index], decimals[index], sample, current.hideUnit
                )
                if (textView.text.toString() != nextText) {
                    textView.text = nextText
                }
                val nextVisibility = if (keys[index] == "none") View.GONE else View.VISIBLE
                if (textView.visibility != nextVisibility) {
                    textView.visibility = nextVisibility
                }
            }

            if (desiredVisibility == View.VISIBLE && holder.overlayMode) {
                positionTrueOverlay(holder, current)
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
            "current" -> { value = sample.currentMa; unit = "mA" }
            "battery_temp" -> { value = sample.batteryTempC; unit = "°C" }
            "cpu_temp" -> { value = sample.cpuTempC; unit = "°C" }
            "voltage" -> { value = sample.voltageV; unit = "V" }
            "battery_level" -> { value = sample.batteryLevel; unit = "%" }
            "none" -> return ""
            else -> { value = sample.powerW; unit = "W" }
        }
        if (value == null) return "--"
        val number = String.format(Locale.US, "%.${safeDecimals}f", value)
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
