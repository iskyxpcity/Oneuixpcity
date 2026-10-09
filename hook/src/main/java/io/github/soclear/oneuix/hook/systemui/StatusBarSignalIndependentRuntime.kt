package io.github.soclear.oneuix.hook.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
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
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Independent collapsed-status-bar mobile signal renderer.
 *
 * Presentation is owned entirely by OneuiXP. Signal level comes from Android's
 * public Telephony / Subscription state inside the privileged SystemUI process;
 * no Samsung MobileIconBinder/HomeStatusBarSignalPolicy/Compose rendering hook
 * is used. Samsung's native signal presentation is only hidden after a compact,
 * strongly-identifiable signal/mobile child is found, and is restored when the
 * feature is disabled.
 */
internal object StatusBarSignalIndependentRuntime {
    /**
     * Current strongest/default-data cellular level for other OneuiXP-owned
     * status-bar renderers. Keeps all cellular presentations on one live source.
     */
    internal fun currentDefaultDataSignalLevel(): Int {
        val preferred = runCatching {
            SubscriptionManager.getDefaultDataSubscriptionId()
        }.getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        return synchronized(states) {
            val selected = states[preferred]?.level
            selected?.coerceIn(0, 4)
                ?: states.values.maxOfOrNull { it.level.coerceIn(0, 4) }
                ?: 0
        }
    }
    private const val PREFERENCE_CHANGED_URI =
        "content://com.oneuixp.app.statusbridge/preferences"
    private const val PHONE_STATUS_BAR_VIEW =
        "com.android.systemui.statusbar.phone.PhoneStatusBarView"
    private const val MIUI_PHONE_STATUS_BAR_VIEW =
        "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView"
    private const val STATUS_BAR_ICON_VIEW =
        "com.android.systemui.statusbar.StatusBarIconView"

    private data class SignalState(
        val subscriptionId: Int,
        val slotIndex: Int,
        val level: Int = 0,
        val inService: Boolean = false,
    )

    private data class NativeHiddenState(
        val alpha: Float,
        val visibility: Int,
    )

    private data class Holder(
        val statusBarView: ViewGroup,
        val parent: ViewGroup,
        val root: LinearLayout,
        val bars: IndependentSignalView,
        val hiddenNative: MutableMap<View, NativeHiddenState> = LinkedHashMap(),
    )

    private class IndependentSignalView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var states: List<SignalState> = emptyList()
        private var current = Preference.SystemUI.StatusBarSignalRuntime()
        private var indicatorColor: Int = Color.WHITE

        fun update(
            nextStates: List<SignalState>,
            nextConfig: Preference.SystemUI.StatusBarSignalRuntime,
            color: Int,
        ) {
            states = nextStates
            current = nextConfig
            indicatorColor = color
            visibility = if (nextConfig.enabled && visibleStates().isNotEmpty()) VISIBLE else GONE
            requestLayout()
            invalidate()
        }

        fun updateIndicatorColor(color: Int) {
            if (indicatorColor == color) return
            indicatorColor = color
            // Colour changes do not change view geometry.
            invalidate()
        }

        private fun visibleStates(): List<SignalState> = states
            .sortedBy { it.slotIndex }
            .filter {
                when (it.slotIndex) {
                    0 -> current.showSim1
                    1 -> current.showSim2
                    else -> false
                }
            }
            .take(2)

        private fun iconSizePx(): Float {
            val dp = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                current.landscapeSizeDp
            } else {
                current.portraitSizeDp
            }
            return dp.coerceIn(8f, 32f) * resources.displayMetrics.density
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val count = max(1, visibleStates().size)
            val size = iconSizePx()
            val gap = if (count > 1) 2f * resources.displayMetrics.density else 0f
            val desiredWidth = (size * count + gap * (count - 1)).roundToInt()
            val desiredHeight = size.roundToInt()
            setMeasuredDimension(
                resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec),
            )
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val visible = visibleStates()
            if (visible.isEmpty()) return
            val size = iconSizePx().coerceAtMost(height.toFloat().coerceAtLeast(1f))
            val gap = if (visible.size > 1) 2f * resources.displayMetrics.density else 0f
            val totalWidth = size * visible.size + gap * (visible.size - 1)
            var x = (width - totalWidth) / 2f
            val top = (height - size) / 2f
            visible.forEach { state ->
                drawBars(canvas, x, top, size, state)
                x += size + gap
            }
        }

        private fun drawBars(
            canvas: Canvas,
            left: Float,
            top: Float,
            size: Float,
            state: SignalState,
        ) {
            val level = if (state.inService) state.level.coerceIn(0, 4) else 0
            val barWidth = size * 0.13f
            val barGap = size * 0.075f
            val total = 4f * barWidth + 3f * barGap
            var x = left + (size - total) / 2f
            val bottom = top + size * 0.86f
            val heights = floatArrayOf(0.27f, 0.43f, 0.61f, 0.80f)
            for (index in 0..3) {
                val barHeight = size * heights[index]
                paint.color = indicatorColor
                paint.alpha = if (index < level) 255 else 58
                val radius = barWidth * 0.28f
                canvas.drawRoundRect(
                    x,
                    bottom - barHeight,
                    x + barWidth,
                    bottom,
                    radius,
                    radius,
                    paint,
                )
                x += barWidth + barGap
            }
        }
    }

    private class MobileCallback(
        private val subscriptionId: Int,
    ) : TelephonyCallback(),
        TelephonyCallback.SignalStrengthsListener,
        TelephonyCallback.ServiceStateListener {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            updateSignal(subscriptionId, level = signalStrength.level.coerceIn(0, 4))
        }

        override fun onServiceStateChanged(serviceState: ServiceState) {
            updateSignal(
                subscriptionId,
                inService = serviceState.state == ServiceState.STATE_IN_SERVICE,
            )
        }
    }

    private val installed = AtomicBoolean(false)
    private val watcherStarted = AtomicBoolean(false)
    private val holders = WeakHashMap<View, Holder>()
    // Cache both positive and negative classifications. StatusBarIconView is also
    // used by the expanded shade; after the first ancestry check, those shade icons
    // must not trigger another parent walk on every dark-intensity animation frame.
    private val collapsedTintViewCache =
        Collections.synchronizedMap(WeakHashMap<View, Boolean>())
    @Volatile private var statusBarIconViewClass: Class<*>? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val states = LinkedHashMap<Int, SignalState>()
    private val callbacks = LinkedHashMap<Int, Pair<TelephonyManager, TelephonyCallback>>()

    @Volatile
    private var config = Preference.SystemUI.StatusBarSignalRuntime()

    @Volatile
    private var indicatorColor: Int = Color.WHITE

    private val indicatorColorListeners = CopyOnWriteArrayList<(Int) -> Unit>()

    private var subscriptionManager: SubscriptionManager? = null
    private var subscriptionListener: SubscriptionManager.OnSubscriptionsChangedListener? = null
    private var preferenceThread: HandlerThread? = null
    private var preferenceObserver: ContentObserver? = null
    private var runtimeReceiver: BroadcastReceiver? = null


    fun addIndicatorColorListener(listener: (Int) -> Unit) {
        indicatorColorListeners += listener
        runCatching { listener(indicatorColor) }
    }

    private fun updateIndicatorColor(nextColor: Int) {
        if (indicatorColor == nextColor) return
        indicatorColor = nextColor
        indicatorColorListeners.forEach { listener ->
            runCatching { listener(nextColor) }
        }
        mainHandler.post {
            val active = synchronized(holders) { holders.values.toList() }
            active.forEach { holder ->
                holder.bars.updateIndicatorColor(nextColor)
            }
        }
    }

    fun install(module: XposedModule, param: XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != Package.SYSTEMUI) return
        if (!installed.compareAndSet(false, true)) return

        runCatching {
            with(module) { PreferenceProvider.loadPreference() }
                ?.systemUI?.statusBar?.signalRuntime
                ?.let { config = sanitize(it) }
        }

        runCatching {
            val clazz = param.classLoader.loadClass(PHONE_STATUS_BAR_VIEW)
            clazz.declaredConstructors.forEach { constructor ->
                module.hook(constructor).intercept { chain ->
                    val result = chain.proceed()
                    (chain.thisObject as? View)?.let { view ->
                        view.post {
                            registerStatusBar(module, view)
                            ensurePreferenceWatcher(module, view.context)
                            ensureTelephony(view.context)
                        }
                        view.postDelayed({ registerStatusBar(module, view) }, 250L)
                        view.postDelayed({ registerStatusBar(module, view) }, 1200L)
                    }
                    result
                }
            }
        }

        hookCollapsedStatusBarIconTint(module, param.classLoader)
    }

    private fun hookCollapsedStatusBarIconTint(module: XposedModule, classLoader: ClassLoader) {
        runCatching {
            val iconClass = classLoader.loadClass(STATUS_BAR_ICON_VIEW)
            statusBarIconViewClass = iconClass
            var current: Class<*>? = iconClass
            val methods = mutableListOf<java.lang.reflect.Method>()
            while (current != null) {
                methods += current.declaredMethods.filter { method ->
                    (method.name == "onDarkChanged" || method.name == "setIconColor") &&
                        method.parameterTypes.any { it == Int::class.javaPrimitiveType }
                }
                current = current.superclass
            }
            methods.distinctBy { it.name + it.parameterTypes.joinToString { type -> type.name } }
                .forEach { method ->
                    val tintIndex = method.parameterTypes.indices.firstOrNull {
                        method.parameterTypes[it] == Int::class.javaPrimitiveType
                    } ?: return@forEach
                    module.hook(method).intercept { chain ->
                        val result = chain.proceed()
                        val icon = chain.thisObject as? View ?: return@intercept result
                        val cached = collapsedTintViewCache[icon]
                        if (cached == false) return@intercept result
                        if (cached == null) {
                            val belongsToCollapsedBar = collapsedStatusBarRoot(icon) != null
                            collapsedTintViewCache[icon] = belongsToCollapsedBar
                            if (!belongsToCollapsedBar) return@intercept result
                        }
                        val color = runCatching { chain.getArg(tintIndex) as? Int }.getOrNull()
                        if (color != null && color != Color.TRANSPARENT) updateIndicatorColor(color)
                        result
                    }
                }
        }.onFailure { Log.w("OneuiXPStatusSignal", "Status bar tint hook unavailable", it) }
    }

    private fun collapsedStatusBarRoot(view: View): ViewGroup? {
        var node: View? = view
        while (node != null) {
            if (node is ViewGroup &&
                (node.javaClass.name == PHONE_STATUS_BAR_VIEW ||
                    node.javaClass.name == MIUI_PHONE_STATUS_BAR_VIEW)
            ) return node
            node = node.parent as? View
        }
        return null
    }

    private fun sanitize(source: Preference.SystemUI.StatusBarSignalRuntime) = source.copy(
        portraitSizeDp = source.portraitSizeDp.coerceIn(8f, 32f),
        landscapeSizeDp = source.landscapeSizeDp.coerceIn(8f, 32f),
        horizontalOffsetDp = source.horizontalOffsetDp.coerceIn(-24f, 24f),
        verticalOffsetDp = source.verticalOffsetDp.coerceIn(-15f, 15f),
        startMarginDp = source.startMarginDp.coerceIn(-6f, 12f),
        endMarginDp = source.endMarginDp.coerceIn(-6f, 12f),
    )

    private fun rememberCollapsedTintViews(statusBar: ViewGroup) {
        val iconClass = statusBarIconViewClass ?: return
        val matched = ArrayList<View>()
        fun visit(view: View) {
            if (iconClass.isInstance(view)) matched += view
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) visit(view.getChildAt(index))
            }
        }
        visit(statusBar)
        synchronized(collapsedTintViewCache) {
            matched.forEach { collapsedTintViewCache[it] = true }
        }
    }

    private fun registerStatusBar(module: XposedModule, raw: View) {
        val statusBar = raw as? ViewGroup ?: return
        if (!statusBar.isLaidOut) {
            statusBar.post { registerStatusBar(module, statusBar) }
            return
        }
        rememberCollapsedTintViews(statusBar)
        val existing = synchronized(holders) { holders[statusBar] }
        if (existing != null) {
            applyHolder(existing)
            return
        }
        val parent = findRightIndicatorArea(statusBar) ?: return
        val context = statusBar.context
        val bars = IndependentSignalView(context).apply {
            isClickable = false
            isFocusable = false
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            isClickable = false
            isFocusable = false
            addView(
                bars,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        parent.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val holder = Holder(statusBar, parent, root, bars)
        synchronized(holders) { holders[statusBar] = holder }
        // Mount once after inflation. Reordering the child from every animated
        // layout callback makes the shade/Quick Panel feel permanently sticky.
        mountBeforeWifi(holder)
        ensurePreferenceWatcher(module, context)
        applyHolder(holder)
    }

    private fun findRightIndicatorArea(statusBar: View): ViewGroup? {
        val resources = statusBar.resources
        listOf(
            "system_icon_area",
            "status_bar_right_side",
            "status_bar_end_side_content",
            "status_icon_area",
            "status_bar_end_side",
        ).forEach { name ->
            val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
            if (id != 0) {
                (statusBar.findViewById<View>(id) as? ViewGroup)?.let { return it }
            }
        }
        return null
    }

    private fun mountBeforeWifi(holder: Holder) {
        val parent = holder.parent
        val anchor = findDirectWifiAnchor(holder.statusBarView, parent)
        val currentIndex = parent.indexOfChild(holder.root)
        val targetIndex = anchor?.let(parent::indexOfChild)?.takeIf { it >= 0 } ?: parent.childCount
        if (currentIndex >= 0 && currentIndex == targetIndex - 1) return
        if (holder.root.parent === parent) parent.removeView(holder.root)
        val refreshed = findDirectWifiAnchor(holder.statusBarView, parent)
        val index = refreshed?.let(parent::indexOfChild)?.takeIf { it >= 0 } ?: parent.childCount
        parent.addView(
            holder.root,
            index,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    private fun findDirectWifiAnchor(statusBar: ViewGroup, parent: ViewGroup): View? {
        val resources = statusBar.resources
        listOf(
            "wifi_group", "wifi_combo", "status_bar_wifi",
            "status_bar_wifi_group", "wifi_container", "modern_status_bar_wifi",
        ).forEach { name ->
            val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
            if (id == 0) return@forEach
            var found: View = statusBar.findViewById<View>(id) ?: return@forEach
            var owner = found.parent as? ViewGroup
            while (owner != null && owner !== parent) {
                found = owner
                owner = found.parent as? ViewGroup
            }
            if (owner === parent) return found
        }
        return null
    }

    private fun ensureTelephony(context: Context) {
        if (subscriptionManager != null) return
        val appContext = context.applicationContext ?: context
        subscriptionManager = appContext.getSystemService(SubscriptionManager::class.java)
        val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
            override fun onSubscriptionsChanged() {
                refreshSubscriptions(appContext)
            }
        }
        subscriptionListener = listener
        runCatching { subscriptionManager?.addOnSubscriptionsChangedListener(listener) }
        refreshSubscriptions(appContext)
    }

    private fun refreshSubscriptions(context: Context) {
        val active: List<SubscriptionInfo> = runCatching {
            subscriptionManager?.activeSubscriptionInfoList.orEmpty()
        }.getOrDefault(emptyList())
            .sortedBy { it.simSlotIndex }
            .take(2)

        val wanted = active.map { it.subscriptionId }.toSet()
        val stale = callbacks.keys.filter { it !in wanted }
        stale.forEach { subId ->
            callbacks.remove(subId)?.let { (manager, callback) ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching { manager.unregisterTelephonyCallback(callback) }
                }
            }
            synchronized(states) { states.remove(subId) }
        }

        active.forEach { info ->
            synchronized(states) {
                val old = states[info.subscriptionId]
                states[info.subscriptionId] = (old ?: SignalState(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                )).copy(slotIndex = info.simSlotIndex)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !callbacks.containsKey(info.subscriptionId)
            ) {
                val manager = context.getSystemService(TelephonyManager::class.java)
                    .createForSubscriptionId(info.subscriptionId)
                val callback = MobileCallback(info.subscriptionId)
                runCatching {
                    manager.registerTelephonyCallback(context.mainExecutor, callback)
                    callbacks[info.subscriptionId] = manager to callback
                }
            }
        }
        mainHandler.post { applyAll() }
    }

    private fun updateSignal(
        subscriptionId: Int,
        level: Int? = null,
        inService: Boolean? = null,
    ) {
        synchronized(states) {
            val old = states[subscriptionId] ?: return
            states[subscriptionId] = old.copy(
                level = level ?: old.level,
                inService = inService ?: old.inService,
            )
        }
        mainHandler.post { applyAll() }
    }

    private fun ensurePreferenceWatcher(module: XposedModule, context: Context) {
        if (!watcherStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext ?: context
        val worker = HandlerThread("OneuiXPStatusSignalPrefs").apply { start() }
        preferenceThread = worker
        val background = Handler(worker.looper)
        var previous: Preference.SystemUI.StatusBarSignalRuntime? = null

        fun refreshOnce() {
            background.post {
                val next = runCatching {
                    with(module) { PreferenceProvider.loadPreference() }
                        ?.systemUI?.statusBar?.signalRuntime
                }.getOrNull() ?: return@post
                if (next == previous) return@post
                previous = next
                mainHandler.post {
                    config = sanitize(next)
                    applyAll()
                }
            }
        }

        val observer = object : ContentObserver(background) {
            override fun onChange(selfChange: Boolean) = refreshOnce()
            override fun onChange(selfChange: Boolean, uri: Uri?) = refreshOnce()
        }
        preferenceObserver = observer
        runCatching {
            appContext.contentResolver.registerContentObserver(
                Uri.parse(PREFERENCE_CHANGED_URI), false, observer,
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
                            Preference.SystemUI.StatusBar.serializer(), payload,
                        )
                    }.getOrNull() ?: return@post
                    mainHandler.post {
                        config = sanitize(statusBar.signalRuntime)
                        applyAll()
                    }
                }
            }
        }
        runtimeReceiver = receiver
        runCatching {
            val filter = IntentFilter(StatusBarRuntimeBridge.ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(receiver, filter)
            }
        }
        refreshOnce()
    }

    private fun applyAll() {
        val active = synchronized(holders) {
            holders.values.filter { it.statusBarView.isAttachedToWindow }
        }
        active.forEach(::applyHolder)
    }

    private fun applyHolder(holder: Holder) {
        val current = config
        val density = holder.root.resources.displayMetrics.density
        // Combo owns the cellular signal presentation while enabled. Do not let
        // the independent two-SIM renderer reappear during its own live refresh.
        if (StatusBarComboStyleOneRuntime.isEnabled()) {
            // Combo either owns the native signal slots or explicitly hands them
            // back. Do not resurrect an alpha-hidden native icon when Combo is
            // about to fold it; that was the source of transparent empty slots.
            if (!StatusBarComboStyleOneRuntime.isMobileNativeSuppressed()) {
                restoreNativeSignalViews(holder)
            }
            holder.root.visibility = View.GONE
            return
        }
        holder.root.visibility = if (current.enabled) View.VISIBLE else View.GONE
        holder.root.translationX = current.horizontalOffsetDp * density
        holder.root.translationY = current.verticalOffsetDp * density
        val spacingPx = (current.startMarginDp * density).roundToInt()
        val barParams = holder.bars.layoutParams
        if (barParams is LinearLayout.LayoutParams &&
            (barParams.marginStart != spacingPx || barParams.marginEnd != spacingPx)
        ) {
            barParams.marginStart = spacingPx
            barParams.marginEnd = spacingPx
            holder.bars.layoutParams = barParams
        }
        val snapshot = synchronized(states) { states.values.toList() }
        holder.bars.update(snapshot, current, indicatorColor)
        if (current.enabled) {
            mountBeforeWifi(holder)
            hideNativeSignalViews(holder)
        } else {
            restoreNativeSignalViews(holder)
        }
    }

    private fun hideNativeSignalViews(holder: Holder) {
        val density = holder.statusBarView.resources.displayMetrics.density.coerceAtLeast(1f)
        collectDescendants(holder.parent).forEach { view ->
            if (view === holder.root || view === holder.bars || isDescendantOf(view, holder.root)) return@forEach
            if (view.width > 0 && view.width / density > 80f) return@forEach
            if (view.height > 0 && view.height / density > 48f) return@forEach
            val descriptor = buildString {
                if (view.id != View.NO_ID) {
                    runCatching { view.resources.getResourceEntryName(view.id) }
                        .getOrNull()?.let { append(it).append(' ') }
                }
                append(view.javaClass.name).append(' ')
                append(view.contentDescription ?: "")
            }.lowercase()
            val positive = descriptor.contains("mobile") || descriptor.contains("signal") ||
                descriptor.contains("cellular") || descriptor.contains("cell signal") ||
                descriptor.contains("移动网络") || descriptor.contains("信号")
            val negative = descriptor.contains("wifi") || descriptor.contains("bluetooth") ||
                descriptor.contains("battery") || descriptor.contains("network_speed") ||
                descriptor.contains("netspeed") || descriptor.contains("clock")
            if (!positive || negative) return@forEach
            if (!holder.hiddenNative.containsKey(view)) {
                holder.hiddenNative[view] = NativeHiddenState(view.alpha, view.visibility)
            }
            view.alpha = 0f
        }
    }

    private fun restoreNativeSignalViews(holder: Holder) {
        holder.hiddenNative.forEach { (view, state) ->
            if (view.parent != null) {
                view.alpha = state.alpha
                view.visibility = state.visibility
            }
        }
        holder.hiddenNative.clear()
    }

    private fun collectDescendants(root: ViewGroup): List<View> {
        val out = ArrayList<View>()
        fun visit(group: ViewGroup) {
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                out += child
                if (child is ViewGroup) visit(child)
            }
        }
        visit(root)
        return out
    }

    private fun isDescendantOf(view: View, ancestor: View): Boolean {
        var parent = view.parent
        while (parent is View) {
            if (parent === ancestor) return true
            parent = parent.parent
        }
        return false
    }
}
