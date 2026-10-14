package io.github.soclear.oneuix.hook.systemui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.libxposed.api.XposedModule
import io.github.soclear.oneuix.common.ONE_UI_VERSION
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.hook.util.afterAttachTry
import io.github.soclear.oneuix.hook.util.StatusBarClockFormatter
import io.github.soclear.oneuix.hook.util.StatusBarClockRichFormatter
import io.github.soclear.oneuix.hook.util.reflect
import io.github.soclear.oneuix.hook.util.xlog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

@SuppressLint("PrivateApi")
object StatusBar {
    @Volatile
    private var statusBarClockSecondRuntimeEnabled = false

    @Volatile
    private var statusBarClockRuntime: Preference.SystemUI.StatusBar? = null

    private val statusBarClockViews = WeakHashMap<TextView, Unit>()
    private val statusBarClockBaseTextSizes = WeakHashMap<TextView, Float>()

    private fun rememberClockBaseTextSize(clockTextView: TextView): Float =
        synchronized(statusBarClockBaseTextSizes) {
            statusBarClockBaseTextSizes.getOrPut(clockTextView) {
                clockTextView.textSize
            }
        }

    private fun updateClockBaseTextSize(clockTextView: TextView, baseSizePx: Float) {
        synchronized(statusBarClockBaseTextSizes) {
            statusBarClockBaseTextSizes[clockTextView] = baseSizePx
        }
    }

    private fun applySingleLineClockScale(
        clockTextView: TextView,
        current: Preference.SystemUI.StatusBar,
    ) {
        val baseSize = rememberClockBaseTextSize(clockTextView)
        val isDoubleLine =
            current.setStatusBarClockFormat && current.statusBarClockFormat.contains('\n')
        val targetSize = if (current.setStatusBarClockTextScale && !isDoubleLine) {
            baseSize * current.statusBarClockTextScale.coerceIn(0.5f, 2.5f)
        } else {
            baseSize
        }
        if (kotlin.math.abs(clockTextView.textSize - targetSize) > 0.1f) {
            clockTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetSize)
        }
    }

    private fun renderStatusBarClock(
        clockTextView: TextView,
        current: Preference.SystemUI.StatusBar,
    ): CharSequence {
        val now = LocalDateTime.now()
        return if (current.enableStatusBarClockRichStyle) {
            runCatching {
                StatusBarClockRichFormatter.format(
                    pattern = current.statusBarClockFormat,
                    dateTime = now,
                    resources = clockTextView.resources,
                    styles = current.statusBarClockBlockStyles,
                )
            }.getOrElse {
                StatusBarClockFormatter.format(current.statusBarClockFormat, now)
            }
        } else {
            runCatching {
                StatusBarClockFormatter.format(current.statusBarClockFormat, now)
            }.getOrElse {
                DateTimeFormatter.ofPattern("HH:mm").format(now)
            }
        }
    }

    private fun shouldBurnInMoveClock(current: Preference.SystemUI.StatusBar): Boolean =
        current.setStatusBarClockFormat ||
            current.setStatusBarClockTextScale ||
            current.enableStatusBarClockRichStyle ||
            current.modifyStatusBarClockPosition ||
            current.updateStatusBarClockEverySecond ||
            current.modifyDoubleLineClockGap ||
            current.useFold7CustomDoubleLineClockScale

    private fun applyStatusBarClockRuntime(
        clockTextView: TextView,
        current: Preference.SystemUI.StatusBar,
    ) {
        if (shouldBurnInMoveClock(current)) {
            StatusBarBurnInProtectionRuntime.registerView(clockTextView)
        } else {
            StatusBarBurnInProtectionRuntime.unregisterOffsetTarget(clockTextView)
        }
        if (!current.setStatusBarClockFormat) {
            applySingleLineClockScale(clockTextView, current)
            OfficialMainStatusBarMod.applyStatusBarClockPosition(clockTextView)
            return
        }
        val dateTime = renderStatusBarClock(clockTextView, current)
        if (dateTime.contains('\n')) {
            applySingleLineClockScale(clockTextView, current)
            OfficialMainStatusBarMod.applyDoubleLineClockText(clockTextView, dateTime)
        } else {
            OfficialMainStatusBarMod.restoreSingleLineClock(clockTextView, dateTime)
            applySingleLineClockScale(clockTextView, current)
        }
        OfficialMainStatusBarMod.applyStatusBarClockPosition(clockTextView)
    }

    private fun clockContentSettingsChanged(
        previous: Preference.SystemUI.StatusBar?,
        current: Preference.SystemUI.StatusBar,
    ): Boolean {
        if (previous == null) return true
        return previous.setStatusBarClockFormat != current.setStatusBarClockFormat ||
            previous.statusBarClockFormat != current.statusBarClockFormat ||
            previous.enableStatusBarClockRichStyle != current.enableStatusBarClockRichStyle ||
            previous.statusBarClockBlockStyles != current.statusBarClockBlockStyles ||
            previous.setStatusBarClockTextScale != current.setStatusBarClockTextScale ||
            previous.statusBarClockTextScale != current.statusBarClockTextScale ||
            previous.statusBarDoubleLineClockSize != current.statusBarDoubleLineClockSize ||
            previous.modifyDoubleLineClockGap != current.modifyDoubleLineClockGap ||
            previous.doubleLineClockGapDp != current.doubleLineClockGapDp ||
            previous.useFold7CustomDoubleLineClockScale != current.useFold7CustomDoubleLineClockScale ||
            previous.fold7DoubleLineClockTimeScale != current.fold7DoubleLineClockTimeScale ||
            previous.fold7DoubleLineClockDateScale != current.fold7DoubleLineClockDateScale
    }

    fun updateStatusBarClockRuntime(current: Preference.SystemUI.StatusBar) {
        val previous = statusBarClockRuntime
        statusBarClockRuntime = current
        val activeViews = synchronized(statusBarClockViews) {
            statusBarClockViews.keys.toList()
        }
        activeViews.forEach { clock ->
            if (shouldBurnInMoveClock(current)) {
                StatusBarBurnInProtectionRuntime.registerView(clock)
            } else {
                StatusBarBurnInProtectionRuntime.unregisterOffsetTarget(clock)
            }
        }

        // Position, alignment, fixed width and margin sliders should only move the
        // native TextView. Replacing its Spannable text on each preview frame makes
        // Samsung's clock blink and forces a layout that can undo the translation.
        if (!clockContentSettingsChanged(previous, current)) return
        activeViews.forEach { clockTextView ->
            clockTextView.post {
                applyStatusBarClockRuntime(clockTextView, current)
            }
        }
    }
    private val statusBarClockSecondHelpers: MutableSet<Any> =
        Collections.synchronizedSet(
            Collections.newSetFromMap(WeakHashMap<Any, Boolean>())
        )

    private fun applyStatusBarClockSecondState(helper: Any, enabled: Boolean) {
        runCatching {
            val secondTick = helper.reflect["mSecondTick"] as? Runnable ?: return
            val currentHandler = helper.reflect["mSecondsHandler"] as? Handler
            if (enabled) {
                if (currentHandler == null) {
                    val handler = Handler(Looper.getMainLooper())
                    helper.reflect["mSecondsHandler"] = handler
                    handler.post(secondTick)
                }
            } else {
                currentHandler?.removeCallbacks(secondTick)
                helper.reflect["mSecondsHandler"] = null
            }
        }
    }

    fun setStatusBarClockSecondRuntimeEnabled(enabled: Boolean) {
        if (statusBarClockSecondRuntimeEnabled == enabled) return
        statusBarClockSecondRuntimeEnabled = enabled
        val helpers = synchronized(statusBarClockSecondHelpers) {
            statusBarClockSecondHelpers.toList()
        }
        helpers.forEach { helper ->
            applyStatusBarClockSecondState(helper, enabled)
        }
    }

    context(xposedModule: XposedModule)
    fun setStatusBarPaddingDp(left: Float?, right: Float?) = afterAttachTry(left != null || right != null) {
        val className = "com.android.systemui.statusbar.phone.IndicatorGardenAlgorithmCenterCutout"
        val clazz = classLoader.loadClass(className)
        xlog(
            "CHAIN_PROBE status-bar padding class found=$className leftDp=$left rightDp=$right",
            priority = Log.INFO,
        )

        if (left != null) {
            val method = clazz.getDeclaredMethod("calculateLeftPadding")
            val callbackLogged = AtomicBoolean(false)
            xlog(
                "CHAIN_PROBE installing left-padding hook method=${clazz.name}#${method.name}",
                priority = Log.INFO,
            )
            xposedModule.hook(method).intercept { chain ->
                val inputProperties = chain.thisObject.reflect["inputProperties"]
                val density = inputProperties?.reflect?.get("density") as? Float ?: 1f
                val result = (left * density).roundToInt()
                if (callbackLogged.compareAndSet(false, true)) {
                    xlog(
                        "CHAIN_PROBE left-padding callback hit dp=$left density=$density px=$result",
                        priority = Log.INFO,
                    )
                }
                result
            }
        }

        if (right != null) {
            val method = clazz.getDeclaredMethod("calculateRightPadding")
            val callbackLogged = AtomicBoolean(false)
            xlog(
                "CHAIN_PROBE installing right-padding hook method=${clazz.name}#${method.name}",
                priority = Log.INFO,
            )
            xposedModule.hook(method).intercept { chain ->
                val inputProperties = chain.thisObject.reflect["inputProperties"]
                val density = inputProperties?.reflect?.get("density") as? Float ?: 1f
                val result = (right * density).roundToInt()
                if (callbackLogged.compareAndSet(false, true)) {
                    xlog(
                        "CHAIN_PROBE right-padding callback hit dp=$right density=$density px=$result",
                        priority = Log.INFO,
                    )
                }
                result
            }
        }
    }

    context(xposedModule: XposedModule)
    fun setBatteryIconScale(
        widthScale: Float?,
        heightScale: Float?
    ) = afterAttachTry(widthScale != null || heightScale != null) {
        val clazz = classLoader.loadClass("com.android.systemui.battery.BatteryMeterView")
        val method = clazz.getDeclaredMethod("scaleBatteryMeterViewsLegacy")
        xposedModule.hook(method).intercept { chain ->
            val result = chain.proceed()
            try {
                val mBatteryIconView = chain.thisObject.reflect["mBatteryIconView"] as? ImageView
                if (mBatteryIconView != null) {
                    mBatteryIconView.layoutParams = mBatteryIconView.layoutParams.apply {
                        if (widthScale != null) {
                            width = (width * widthScale).roundToInt()
                        }
                        if (heightScale != null) {
                            height = (height * heightScale).roundToInt()
                        }
                    }
                }
            } catch (t: Throwable) {
                xlog(t)
            }
            result
        }
    }


    context(xposedModule: XposedModule)
    fun hideBatteryPercentageSign() = afterAttachTry(Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val batterMeterFormat = "status_bar_settings_${
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) "uniform_"
            else ""
        }battery_meter_format"

        @SuppressLint("DiscouragedApi")
        val targetId = resources.getIdentifier(batterMeterFormat, "string", Package.SYSTEMUI)
        if (targetId != 0) {
            val resourcesClass = classLoader.loadClass("android.content.res.Resources")
            resourcesClass.declaredMethods
                .filter { it.name == "getString" && it.parameterTypes.firstOrNull() == Int::class.javaPrimitiveType }
                .forEach { method ->
                    xposedModule.hook(method).intercept { chain ->
                        if (chain.args.firstOrNull() == targetId) "%d" else chain.proceed()
                    }
                }
        }
    }

    context(xposedModule: XposedModule)
    fun installStatusBarClockSecondControl(initialEnabled: Boolean) = afterAttachTry {
        statusBarClockSecondRuntimeEnabled = initialEnabled

        try {
            val helperClass = classLoader.loadClass(
                "com.android.systemui.statusbar.policy.QSClockQuickStarHelper"
            )
            helperClass.declaredConstructors.forEach { constructor ->
                xposedModule.hook(constructor).intercept { chain ->
                    val result = chain.proceed()
                    statusBarClockSecondHelpers.add(chain.thisObject)
                    applyStatusBarClockSecondState(
                        chain.thisObject,
                        statusBarClockSecondRuntimeEnabled,
                    )
                    result
                }
            }

            val method = helperClass.getDeclaredMethod("updateSecondsClockHandler")
            xposedModule.hook(method).intercept { chain ->
                val result = chain.proceed()
                statusBarClockSecondHelpers.add(chain.thisObject)
                applyStatusBarClockSecondState(
                    chain.thisObject,
                    statusBarClockSecondRuntimeEnabled,
                )
                result
            }
        } catch (t: Throwable) {
            xlog(t)
        }

        // Keep Samsung's clock digits tabular only while second-level updates are enabled.
        try {
            val controllerClass = classLoader.loadClass(
                "com.android.systemui.statusbar.policy.QSClockIndicatorViewController"
            )
            val onViewAttachedMethod = controllerClass.getDeclaredMethod("onViewAttached")
            xposedModule.hook(onViewAttachedMethod).intercept { chain ->
                val result = chain.proceed()
                if (statusBarClockSecondRuntimeEnabled) {
                    try {
                        val clockTextView = chain.thisObject.reflect["view"] as? TextView
                        clockTextView?.fontFeatureSettings = "tnum"
                    } catch (t: Throwable) {
                        xlog(t)
                    }
                }
                result
            }
        } catch (t: Throwable) {
            xlog(t)
        }
    }

    context(xposedModule: XposedModule)
    fun installStatusBarClockTextScaleRuntime() = afterAttachTry {
        val controllerClass = classLoader.loadClass(
            "com.android.systemui.statusbar.policy.QSClockIndicatorViewController"
        )
        val method = controllerClass.getDeclaredMethod("onDensityOrFontScaleChanged")
        xposedModule.hook(method).intercept { chain ->
            val result = chain.proceed()
            try {
                val clockView = chain.thisObject.reflect["view"] as? TextView
                if (clockView != null) {
                    updateClockBaseTextSize(clockView, clockView.textSize)
                    statusBarClockRuntime?.let { current ->
                        applySingleLineClockScale(clockView, current)
                    }
                    OfficialMainStatusBarMod.applyStatusBarClockPosition(clockView)
                }
            } catch (t: Throwable) {
                xlog(t)
            }
            result
        }
    }

    context(xposedModule: XposedModule)
    fun setStatusBarClockFormat(initialFormat: String) {
        setStatusBarClockText(initialFormat)
    }

    context(xposedModule: XposedModule)
    private fun setStatusBarClockText(initialFormat: String) = afterAttachTry {
        val clockClassName = "com.android.systemui.statusbar.policy.QSClockIndicatorView"
        val bellClassName = "com.android.systemui.statusbar.policy.QSClockBellSound"
        val clockClass = classLoader.loadClass(clockClassName)
        val qsClockBellSoundClass = classLoader.loadClass(bellClassName)
        // Samsung's QSClockIndicatorView can impose a one-line measure constraint.
        // In double-line mode give it the full PhoneStatusBarView height as the
        // exact native clock-space constraint before Samsung's own onMeasure runs.
        runCatching {
            generateSequence(clockClass as Class<*>?) { it.superclass }
                .mapNotNull {
                    runCatching {
                        it.getDeclaredMethod(
                            "onMeasure",
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                        )
                    }.getOrNull()
                }
                .first()
                .let { onMeasureMethod ->
                    xposedModule.hook(onMeasureMethod).intercept { measureChain ->
                        val view = measureChain.thisObject as? TextView
                            ?: return@intercept measureChain.proceed()

                        if (
                            view.text?.contains('\n') == true &&
                            view.maxLines == 2
                        ) {
                            val originalHeightSpec = measureChain.args[1] as? Int
                            if (originalHeightSpec != null) {
                                measureChain.args[1] =
                                    OfficialMainStatusBarMod.expandDoubleLineClockHeightMeasureSpec(
                                        view,
                                        originalHeightSpec,
                                    )
                            }
                        }

                        val result = measureChain.proceed()

                        if (
                            view.text?.contains('\n') == true &&
                            view.maxLines == 2
                        ) {
                            // Samsung's QSClockIndicatorView receives the relaxed
                            // full-status-bar ceiling above. Do not call private
                            // setMeasuredDimension() or invoke TextView.onMeasure()
                            // reflectively a second time; that path is unsafe on
                            // Samsung SystemUI and can trigger LSPosed safe mode.
                            runCatching {
                                val availableHeight =
                                    OfficialMainStatusBarMod.doubleLineClockSlotHeight(view)
                                if (availableHeight > 0) {
                                    measureChain.args[1] = View.MeasureSpec.makeMeasureSpec(
                                        availableHeight,
                                        View.MeasureSpec.AT_MOST,
                                    )
                                }
                            }.onFailure { xlog(it) }

                        }
                        result
                    }
                }
        }.onFailure {
            xlog(it)
        }

        // Samsung's actual clock slot is the correct native spatial container.
        // Expanding the first ancestor under PhoneStatusBarView also expands the
        // surrounding status-bar row, so the clock never truly receives the full
        // status-bar coordinate space. Hook only the slot that owns the Samsung
        // clock hierarchy and give that slot the real status-bar height.
        val doubleLineClockSlotHooks = Collections.synchronizedSet(
            Collections.newSetFromMap(WeakHashMap<Class<*>, Boolean>())
        )
        val doubleLineClockSlotInstances = Collections.synchronizedSet(
            Collections.newSetFromMap(WeakHashMap<ViewGroup, Boolean>())
        )
        val doubleLineClockHostHooks = Collections.synchronizedSet(
            Collections.newSetFromMap(WeakHashMap<Class<*>, Boolean>())
        )
        val doubleLineClockHostInstances = Collections.synchronizedSet(
            Collections.newSetFromMap(WeakHashMap<ViewGroup, Boolean>())
        )

        fun installDoubleLineClockHostMeasureHook(clockView: TextView) {
            val host = OfficialMainStatusBarMod.getDoubleLineClockHost(clockView) ?: return
            doubleLineClockHostInstances.add(host)

            val hostClass = host.javaClass
            if (!doubleLineClockHostHooks.add(hostClass)) return

            runCatching {
                val hostMeasure = generateSequence(hostClass as Class<*>?) { it.superclass }
                    .mapNotNull {
                        runCatching {
                            it.getDeclaredMethod(
                                "onMeasure",
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                            )
                        }.getOrNull()
                    }
                    .first()

                xposedModule.hook(hostMeasure).intercept { hostChain ->
                    val measuredHost = hostChain.thisObject as? ViewGroup
                        ?: return@intercept hostChain.proceed()

                    if (!doubleLineClockHostInstances.contains(measuredHost)) {
                        return@intercept hostChain.proceed()
                    }

                    val clock =
                        OfficialMainStatusBarMod.findDoubleLineClockDescendant(measuredHost)
                            ?: return@intercept hostChain.proceed()

                    if (clock.text?.contains('\n') != true || clock.maxLines != 2) {
                        return@intercept hostChain.proceed()
                    }

                    val fullHeight =
                        OfficialMainStatusBarMod.doubleLineClockSlotHeight(clock)
                    if (fullHeight <= 0) {
                        return@intercept hostChain.proceed()
                    }

                    measuredHost.clipChildren = false
                    measuredHost.clipToPadding = false
                    hostChain.args[1] = View.MeasureSpec.makeMeasureSpec(
                        fullHeight,
                        View.MeasureSpec.EXACTLY,
                    )
                    hostChain.proceed()
                }
            }.onFailure {
                doubleLineClockHostHooks.remove(hostClass)
                xlog(it)
            }
        }

        fun installDoubleLineClockSlotMeasureHook(clockView: TextView) {
            installDoubleLineClockHostMeasureHook(clockView)

            val slot =
                OfficialMainStatusBarMod.getDoubleLineClockSlot(clockView) ?: return
            doubleLineClockSlotInstances.add(slot)

            val slotClass = slot.javaClass
            if (!doubleLineClockSlotHooks.add(slotClass)) return

            runCatching {
                val slotMeasure = generateSequence(slotClass as Class<*>?) { it.superclass }
                    .mapNotNull {
                        runCatching {
                            it.getDeclaredMethod(
                                "onMeasure",
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                            )
                        }.getOrNull()
                    }
                    .first()

                xposedModule.hook(slotMeasure).intercept { slotChain ->
                    val host = slotChain.thisObject as? ViewGroup
                        ?: return@intercept slotChain.proceed()

                    // The class hook is shared, but only the concrete native clock
                    // slot instances registered above may enter the custom path.
                    if (!doubleLineClockSlotInstances.contains(host)) {
                        return@intercept slotChain.proceed()
                    }

                    val clock = OfficialMainStatusBarMod.findDoubleLineClockDescendant(host)
                        ?: return@intercept slotChain.proceed()

                    if (clock.text?.contains('\n') != true || clock.maxLines != 2) {
                        return@intercept slotChain.proceed()
                    }

                    val fullHeight =
                        OfficialMainStatusBarMod.doubleLineClockSlotHeight(clock)
                    if (fullHeight <= 0) {
                        return@intercept slotChain.proceed()
                    }

                    host.clipChildren = false
                    host.clipToPadding = false

                    // This slot is the full-height spatial box. Samsung's native
                    // hierarchy is preserved and no view is reparented.
                    slotChain.args[1] = View.MeasureSpec.makeMeasureSpec(
                        fullHeight,
                        View.MeasureSpec.EXACTLY,
                    )

                    slotChain.proceed()
                }

                xlog(
                    "CHAIN_PROBE double-line-clock-slot onMeasure hooked class=" +
                        slotClass.name,
                    priority = Log.INFO,
                )
            }.onFailure {
                doubleLineClockSlotHooks.remove(slotClass)
                xlog(it)
            }
        }

        val method = clockClass.getDeclaredMethod("notifyTimeChanged", qsClockBellSoundClass)
        val callbackLogged = AtomicBoolean(false)
        xlog(
            "CHAIN_PROBE installing clock-format hook " +
                "class=$clockClassName bell=$bellClassName method=${method.name}",
            priority = Log.INFO,
        )
        xposedModule.hook(method).intercept { chain ->
            val clockTextView = chain.thisObject as? TextView ?: return@intercept chain.proceed()
            val result = chain.proceed()
            installDoubleLineClockSlotMeasureHook(clockTextView)
            OfficialMainStatusBarMod.ensurePreferenceWatcher(
                xposedModule,
                clockTextView.context,
            )
            synchronized(statusBarClockViews) {
                statusBarClockViews[clockTextView] = Unit
            }

            val current = statusBarClockRuntime
            if (current != null && shouldBurnInMoveClock(current)) {
                StatusBarBurnInProtectionRuntime.registerView(clockTextView)
            } else {
                StatusBarBurnInProtectionRuntime.unregisterOffsetTarget(clockTextView)
            }
            if (current != null && !current.setStatusBarClockFormat) {
                rememberClockBaseTextSize(clockTextView)
                applySingleLineClockScale(clockTextView, current)
                return@intercept result
            }

            val effective = current ?: Preference.SystemUI.StatusBar(
                setStatusBarClockFormat = true,
                statusBarClockFormat = initialFormat,
            )
            val dateTime = renderStatusBarClock(clockTextView, effective)

            if (callbackLogged.compareAndSet(false, true)) {
                xlog(
                    "CHAIN_PROBE clock-format callback hit value=${
                        dateTime.toString().replace("\n", "\\n")
                    } rich=${effective.enableStatusBarClockRichStyle}",
                    priority = Log.INFO,
                )
            }

            rememberClockBaseTextSize(clockTextView)
            if (dateTime.contains('\n')) {
                applySingleLineClockScale(clockTextView, effective)
                installDoubleLineClockSlotMeasureHook(clockTextView)
                OfficialMainStatusBarMod.applyDoubleLineClockText(clockTextView, dateTime)
            } else {
                OfficialMainStatusBarMod.restoreSingleLineClock(clockTextView, dateTime)
                applySingleLineClockScale(clockTextView, effective)
            }
            OfficialMainStatusBarMod.applyStatusBarClockPosition(clockTextView)
            result
        }
    }

    context(xposedModule: XposedModule)
    fun hideSecureFolderStatusBarIcon() = afterAttachTry {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val controllerImplClass = classLoader.loadClass(
                "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl"
            )
            val holderClass = classLoader.loadClass(
                "com.android.systemui.statusbar.phone.StatusBarIconHolder"
            )
            val setIconMethod = controllerImplClass.getDeclaredMethod(
                "setIcon",
                String::class.java,
                holderClass
            )
            xposedModule.hook(setIconMethod).intercept { chain ->
                val slot = chain.args[0] as? String
                if (slot == "secure_folder") {
                    null
                } else {
                    chain.proceed()
                }
            }
        } else {
            val controllerImplClass = classLoader.loadClass(
                "com.android.systemui.statusbar.phone.StatusBarIconControllerImpl"
            )
            val setIconMethod = controllerImplClass.getDeclaredMethod(
                "setIcon",
                String::class.java,
                Int::class.javaPrimitiveType,
                CharSequence::class.java
            )
            xposedModule.hook(setIconMethod).intercept { chain ->
                val slot = chain.args[0] as? String
                if (slot == "secure_folder") {
                    null
                } else {
                    chain.proceed()
                }
            }
        }
    }

    context(xposedModule: XposedModule)
    fun restoreBluetoothStatusBarIcon() = afterAttachTry {
        val controllerImplClass = classLoader.loadClass(
            "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl"
        )
        val iconManagerClass = classLoader.loadClass(
            "com.android.systemui.statusbar.phone.ui.IconManager"
        )
        val method = controllerImplClass.getDeclaredMethod(
            "hideBySimplification",
            iconManagerClass,
            String::class.java
        )
        xposedModule.hook(method).intercept { chain ->
            val slot = chain.args.getOrNull(1) as? String
            if (slot == "bluetooth" || slot == "bluetooth_connected") {
                false
            } else {
                chain.proceed()
            }
        }
    }

    context(xposedModule: XposedModule)
    fun doubleTapStatusBarToSleep() {
        var lastTapTime = 0L

        fun lockScreen(context: Context) {
            val powerManager = context.getSystemService(PowerManager::class.java)
            powerManager?.reflect?.call("goToSleep", SystemClock.uptimeMillis())
        }

        afterAttachTry {
            val viewClass = classLoader.loadClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView"
            )
            val method = viewClass.getDeclaredMethod("onTouchEvent", MotionEvent::class.java)
            xposedModule.hook(method).intercept { chain ->
                val event = chain.args[0] as MotionEvent
                if (event.action != MotionEvent.ACTION_DOWN) {
                    chain.proceed()
                } else {
                    val currentTime = System.nanoTime()
                    val interval = currentTime - lastTapTime
                    if (interval in 40_000_000L..300_000_000L) {
                        lastTapTime = 0L
                        val view = chain.thisObject as View
                        lockScreen(view.context)
                        true
                    } else {
                        lastTapTime = currentTime
                        chain.proceed()
                    }
                }
            }
        }
    }

    context(xposedModule: XposedModule)
    fun hideLockscreenStatusBar() = afterAttachTry {
        val viewClass = classLoader.loadClass(
            "com.android.systemui.statusbar.phone.KeyguardStatusBarView"
        )
        val method = viewClass.getDeclaredMethod("setVisibility", Int::class.javaPrimitiveType)
        xposedModule.hook(method).intercept { chain ->
            val newArgs = chain.args.toTypedArray()
            newArgs[0] = View.GONE
            chain.proceed(newArgs)
        }
    }

    context(xposedModule: XposedModule)
    fun setCustomCarrierName(carrierName: String) = afterAttachTry {
        val managerClass = classLoader.loadClass(
            "com.android.keyguard.CarrierTextManager"
        )
        val callbackInfoClass = classLoader.loadClass(
            $$"com.android.keyguard.CarrierTextManager$CarrierTextCallbackInfo"
        )
        val method = managerClass.getDeclaredMethod("postToCallback", callbackInfoClass)
        xposedModule.hook(method).intercept { chain ->
            val carrierTextCallbackInfo = chain.args[0]
            if (carrierTextCallbackInfo != null) {
                runCatching { carrierTextCallbackInfo.reflect["carrierText"] = carrierName }
                runCatching { carrierTextCallbackInfo.reflect["carrierTextShort"] = carrierName }
            }
            chain.proceed()
        }
    }

    @SuppressLint("SetTextI18n")
    context(xposedModule: XposedModule)
    fun addBatteryLevelText(
        hidePercentSign: Boolean,
        hideChargingIcon: Boolean,
    ) = afterAttachTry(ONE_UI_VERSION >= 70000) {
        val batteryMeterViewClass = classLoader.loadClass("com.android.systemui.battery.BatteryMeterView")
        val viewId = View.generateViewId()

        val scaleMethod = batteryMeterViewClass.getDeclaredMethod("scaleBatteryMeterViewsLegacy")
        xposedModule.hook(scaleMethod).intercept { chain ->
            val result = chain.proceed()
            try {
                val batteryMeterView = chain.thisObject as ViewGroup
                var textView = batteryMeterView.findViewById<TextView>(viewId)
                if (textView == null) {
                    textView = TextView(batteryMeterView.context).apply {
                        id = viewId
                        gravity = Gravity.CENTER
                    }
                    batteryMeterView.addView(
                        textView, LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    )
                }
                val level = batteryMeterView.reflect["mLevel"] as? Int ?: 0
                val percent = if (hidePercentSign) "$level" else "$level%"
                val isCharging = batteryMeterView.reflect.call("isCharging") as? Boolean ?: false
                val suffix = if (isCharging && !hideChargingIcon) "\u26A1\uFE0E" else ""
                textView.text = "$percent$suffix"
                val textColor = batteryMeterView.reflect["mTextColor"] as? Int ?: 0
                textView.setTextColor(textColor)
            } catch (t: Throwable) {
                xlog(t)
            }
            result
        }

        batteryMeterViewClass.declaredMethods
            .filter { it.name == "updateColors" }
            .forEach { method ->
                xposedModule.hook(method).intercept { chain ->
                    val result = chain.proceed()
                    try {
                        val view = chain.thisObject as ViewGroup
                        val textView = view.findViewById<TextView>(viewId)
                        val textColor = view.reflect["mTextColor"] as? Int ?: 0
                        textView?.setTextColor(textColor)
                    } catch (t: Throwable) {
                        xlog(t)
                    }
                    result
                }
            }
    }
}
