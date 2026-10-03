package io.github.soclear.oneuix.hook.systemui

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarRuntimeBridge
import io.github.soclear.oneuix.hook.util.PreferenceProvider
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/** Flux-style Bluetooth status icon controls adapted to Samsung SystemUI. */
internal object StatusBarBluetoothRuntime {
    private const val PREFERENCE_CHANGED_URI =
        "content://com.oneuixp.app.statusbridge/preferences"

    private data class NativeState(
        val alpha: Float,
        val visibility: Int,
        val scaleX: Float,
        val scaleY: Float,
        val translationX: Float,
        val translationY: Float,
        val marginStart: Int?,
        val marginEnd: Int?,
    )

    private data class Holder(
        val statusBarView: ViewGroup,
        val overlay: BluetoothGlyphView,
        var nativeView: View? = null,
        var nativeState: NativeState? = null,
    )

    private val installed = AtomicBoolean(false)
    private val watcherStarted = AtomicBoolean(false)
    private val bluetoothReceiverStarted = AtomicBoolean(false)
    private val holders = WeakHashMap<View, Holder>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var config = Preference.SystemUI.StatusBarBluetoothRuntime()

    private var preferenceThread: HandlerThread? = null
    private var preferenceObserver: ContentObserver? = null
    private var runtimeReceiver: BroadcastReceiver? = null
    private var bluetoothReceiver: BroadcastReceiver? = null

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
                            ensureBluetoothReceiver(statusBarView.context)
                        }
                    }
                    result
                }
            }
        }
    }

    private fun ensurePreferenceWatcher(module: XposedModule, context: Context) {
        if (!watcherStarted.compareAndSet(false, true)) return
        val worker = HandlerThread("OneuiXPStatusBluetoothPrefs").apply { start() }
        preferenceThread = worker
        val background = Handler(worker.looper)
        var previous: Preference.SystemUI.StatusBarBluetoothRuntime? = null

        fun refreshOnce() {
            background.post {
                val current = runCatching {
                    with(module) { PreferenceProvider.loadPreference() }
                        ?.systemUI?.statusBar?.bluetoothRuntime
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
                    val current = statusBar.bluetoothRuntime
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

    @Suppress("DEPRECATION")
    private fun ensureBluetoothReceiver(context: Context) {
        if (!bluetoothReceiverStarted.compareAndSet(false, true)) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                reapplyAll()
            }
        }
        bluetoothReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
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

        val host = statusBarView as? ViewGroup ?: return
        val holder = synchronized(holders) {
            holders[statusBarView] ?: Holder(
                statusBarView = host,
                overlay = BluetoothGlyphView(host.context).also { host.overlay.add(it) },
            ).also { newHolder ->
                holders[statusBarView] = newHolder
                host.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    // Shade/QS expansion can generate many layout passes. Never rescan
                    // or rewrite native layout params here; only move our overlay.
                    repositionOverlayOnLayout(newHolder)
                }
            }
        }
        applyConfig(holder, config)
    }

    private fun updateRuntime(newConfig: Preference.SystemUI.StatusBarBluetoothRuntime) {
        config = sanitize(newConfig)
        reapplyAll()
    }

    private fun reapplyAll() {
        mainHandler.post {
            val active = synchronized(holders) { holders.values.toList() }
            active.forEach { holder -> applyConfig(holder, config) }
        }
    }

    private fun sanitize(
        source: Preference.SystemUI.StatusBarBluetoothRuntime,
    ): Preference.SystemUI.StatusBarBluetoothRuntime = source.copy(
        portraitSizeDp = source.portraitSizeDp.coerceIn(6f, 32f),
        landscapeSizeDp = source.landscapeSizeDp.coerceIn(6f, 32f),
        horizontalOffsetDp = source.horizontalOffsetDp.coerceIn(-300f, 300f),
        verticalOffsetDp = source.verticalOffsetDp.coerceIn(-60f, 60f),
        startMarginDp = source.startMarginDp.coerceIn(-24f, 48f),
        endMarginDp = source.endMarginDp.coerceIn(-24f, 48f),
    )

    private fun resolveNative(holder: Holder): View? {
        holder.nativeView?.takeIf { it.parent != null }?.let { return it }
        val found = findBluetoothView(holder.statusBarView) ?: return null
        holder.nativeView = found
        holder.nativeState = captureNativeState(found)
        return found
    }

    private fun captureNativeState(view: View): NativeState {
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams
        return NativeState(
            alpha = view.alpha,
            visibility = view.visibility,
            scaleX = view.scaleX,
            scaleY = view.scaleY,
            translationX = view.translationX,
            translationY = view.translationY,
            marginStart = params?.marginStart,
            marginEnd = params?.marginEnd,
        )
    }

    private fun restoreNative(holder: Holder) {
        val view = holder.nativeView ?: return
        val state = holder.nativeState ?: return
        if (view.parent == null) return
        view.alpha = state.alpha
        view.visibility = state.visibility
        view.scaleX = state.scaleX
        view.scaleY = state.scaleY
        view.translationX = state.translationX
        view.translationY = state.translationY
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams
        if (params != null) {
            state.marginStart?.let { params.marginStart = it }
            state.marginEnd?.let { params.marginEnd = it }
            view.layoutParams = params
        }
    }

    private fun currentSizePx(
        holder: Holder,
        current: Preference.SystemUI.StatusBarBluetoothRuntime,
    ): Int {
        val density = holder.statusBarView.resources.displayMetrics.density
        val sizeDp = if (
            holder.statusBarView.resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
        ) current.landscapeSizeDp else current.portraitSizeDp
        return max(1, (sizeDp * density).toInt())
    }

    private fun applyConfig(
        holder: Holder,
        current: Preference.SystemUI.StatusBarBluetoothRuntime,
    ) {
        val native = resolveNative(holder)
        restoreNative(holder)

        if (!current.enabled || native == null) {
            holder.overlay.visibility = View.GONE
            return
        }

        val state = holder.nativeState ?: captureNativeState(native).also {
            holder.nativeState = it
        }
        val density = holder.statusBarView.resources.displayMetrics.density
        val sizePx = currentSizePx(holder, current)

        if (current.showLeftSide || current.customIconEnabled) {
            configureOverlaySource(holder.overlay, native, current.customIconEnabled)
            positionOverlay(holder, native, current, sizePx)
            native.alpha = 0f
            if (current.showLeftSide) native.visibility = View.GONE
        } else {
            holder.overlay.visibility = View.GONE
            val baseHeight = native.height.takeIf { it > 0 }
                ?: native.measuredHeight.takeIf { it > 0 }
                ?: sizePx
            val scale = (sizePx.toFloat() / baseHeight.toFloat()).coerceIn(0.35f, 3f)
            native.scaleX = state.scaleX * scale
            native.scaleY = state.scaleY * scale
            native.translationX = state.translationX + current.horizontalOffsetDp * density
            native.translationY = state.translationY + current.verticalOffsetDp * density
            applyNativeMargins(native, state, current, density)
        }
    }

    private fun repositionOverlayOnLayout(holder: Holder) {
        val current = config
        if (!current.enabled || (!current.showLeftSide && !current.customIconEnabled)) return
        val native = holder.nativeView?.takeIf { it.parent != null } ?: return
        positionOverlay(holder, native, current, currentSizePx(holder, current))
    }

    private fun applyNativeMargins(
        view: View,
        state: NativeState,
        current: Preference.SystemUI.StatusBarBluetoothRuntime,
        density: Float,
    ) {
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val baseStart = state.marginStart ?: params.marginStart
        val baseEnd = state.marginEnd ?: params.marginEnd
        val targetStart = baseStart + (current.startMarginDp * density).toInt()
        val targetEnd = baseEnd + (current.endMarginDp * density).toInt()
        if (params.marginStart != targetStart || params.marginEnd != targetEnd) {
            params.marginStart = targetStart
            params.marginEnd = targetEnd
            view.layoutParams = params
        }
    }

    private fun configureOverlaySource(
        overlay: BluetoothGlyphView,
        native: View,
        custom: Boolean,
    ) {
        overlay.customIcon = custom
        val image = native as? ImageView
            ?: collectDescendants(native as? ViewGroup).filterIsInstance<ImageView>().firstOrNull()
        overlay.sourceDrawable = image?.drawable?.constantState
            ?.newDrawable(native.resources)?.mutate()
        overlay.tintColor = image?.imageTintList?.getColorForState(
            image.drawableState,
            image.imageTintList?.defaultColor ?: Color.WHITE,
        ) ?: Color.WHITE
    }

    private fun positionOverlay(
        holder: Holder,
        native: View,
        current: Preference.SystemUI.StatusBarBluetoothRuntime,
        sizePx: Int,
    ) {
        if (holder.statusBarView.width <= 0 || holder.statusBarView.height <= 0) return
        val density = holder.statusBarView.resources.displayMetrics.density
        val barLocation = IntArray(2)
        holder.statusBarView.getLocationOnScreen(barLocation)

        val baseLeft = if (current.showLeftSide) {
            resolveLeftAnchorX(holder.statusBarView, barLocation)
        } else {
            if (native.width <= 0) return
            val nativeLocation = IntArray(2)
            native.getLocationOnScreen(nativeLocation)
            nativeLocation[0] - barLocation[0] + native.width / 2 - sizePx / 2
        }

        val left = baseLeft +
            ((current.startMarginDp - current.endMarginDp + current.horizontalOffsetDp) * density).toInt()
        val top = holder.statusBarView.height / 2 - sizePx / 2 +
            (current.verticalOffsetDp * density).toInt()

        holder.overlay.runtime = current
        holder.overlay.measure(
            View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY),
        )
        holder.overlay.layout(left, top, left + sizePx, top + sizePx)
        holder.overlay.visibility = View.VISIBLE
        holder.overlay.invalidate()
    }

    private fun resolveLeftAnchorX(root: ViewGroup, barLocation: IntArray): Int {
        val resources = root.resources
        listOf("clock", "status_bar_clock").forEach { name ->
            val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
            if (id != 0) {
                root.findViewById<View>(id)?.let { clock ->
                    if (clock.width > 0) {
                        val location = IntArray(2)
                        clock.getLocationOnScreen(location)
                        return location[0] - barLocation[0] + clock.width
                    }
                }
            }
        }
        listOf(
            "status_bar_left_side",
            "status_bar_start_side",
            "status_bar_start_side_content",
            "notification_icon_area",
        ).forEach { name ->
            val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
            if (id != 0) {
                root.findViewById<View>(id)?.let { area ->
                    val location = IntArray(2)
                    area.getLocationOnScreen(location)
                    return location[0] - barLocation[0] + area.width
                }
            }
        }
        return 0
    }

    private fun findBluetoothView(root: ViewGroup): View? {
        var best: View? = null
        var bestScore = 0

        fun visit(view: View, depth: Int) {
            if (depth > 10 || view is BluetoothGlyphView) return
            val name = resourceEntryName(view).lowercase()
            val className = view.javaClass.simpleName.lowercase()
            val description = view.contentDescription?.toString()?.lowercase().orEmpty()
            val tag = view.tag?.toString()?.lowercase().orEmpty()
            val slot = readSlotName(view).lowercase()
            var score = 0
            if (name.contains("bluetooth")) score += 120
            if (slot.contains("bluetooth")) score += 110
            if (description.contains("bluetooth") || description.contains("蓝牙")) score += 100
            if (tag.contains("bluetooth")) score += 80
            if (className.contains("bluetooth")) score += 70
            if (view is ImageView) score += 10
            score -= depth
            if (score > bestScore && score >= 70) {
                best = view
                bestScore = score
            }
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    visit(view.getChildAt(index), depth + 1)
                }
            }
        }

        visit(root, 0)
        return best
    }

    private fun readSlotName(view: View): String {
        var clazz: Class<*>? = view.javaClass
        while (clazz != null) {
            listOf("mSlot", "slot").forEach { fieldName ->
                runCatching {
                    val field = clazz!!.getDeclaredField(fieldName)
                    field.isAccessible = true
                    val value = field.get(view) as? String
                    if (!value.isNullOrBlank()) return value
                }
            }
            clazz = clazz.superclass
        }
        return ""
    }

    private fun resourceEntryName(view: View): String = runCatching {
        if (view.id == View.NO_ID || view.id == 0) ""
        else view.resources.getResourceEntryName(view.id)
    }.getOrDefault("")

    private fun collectDescendants(root: ViewGroup?): List<View> {
        if (root == null) return emptyList()
        val result = ArrayList<View>()
        fun visit(group: ViewGroup) {
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                result += child
                if (child is ViewGroup) visit(child)
            }
        }
        visit(root)
        return result
    }

    private class BluetoothGlyphView(context: Context) : View(context) {
        var runtime = Preference.SystemUI.StatusBarBluetoothRuntime()
        var sourceDrawable: Drawable? = null
        var customIcon: Boolean = false
        var tintColor: Int = Color.WHITE

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val path = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (!customIcon) {
                sourceDrawable?.let { drawable ->
                    drawable.setTint(tintColor)
                    val inset = (width * 0.08f).toInt()
                    drawable.setBounds(inset, inset, width - inset, height - inset)
                    drawable.draw(canvas)
                    return
                }
            }

            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 1f || h <= 1f) return
            paint.color = tintColor
            paint.strokeWidth = max(1.4f, minOf(w, h) * 0.105f)

            val cx = w * 0.45f
            val top = h * 0.13f
            val mid = h * 0.50f
            val bottom = h * 0.87f
            val right = w * 0.76f
            val left = w * 0.25f

            path.reset()
            path.moveTo(cx, top)
            path.lineTo(cx, bottom)
            path.moveTo(cx, top)
            path.lineTo(right, h * 0.31f)
            path.lineTo(cx, mid)
            path.lineTo(right, h * 0.69f)
            path.lineTo(cx, bottom)
            path.moveTo(left, h * 0.32f)
            path.lineTo(right, h * 0.69f)
            path.moveTo(left, h * 0.68f)
            path.lineTo(right, h * 0.31f)
            canvas.drawPath(path, paint)
        }
    }
}
