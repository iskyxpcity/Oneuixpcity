package com.alohaoneui.hook.systemui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.MetricAffectingSpan
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import com.alohaoneui.common.ONE_UI_VERSION
import com.alohaoneui.common.Package
import com.alohaoneui.common.Preference
import com.alohaoneui.hook.util.TraditionalChineseCalendar
import com.alohaoneui.hook.util.ShadeClockFormatter
import com.alohaoneui.hook.util.afterAttachTry
import com.alohaoneui.hook.util.reflect
import com.alohaoneui.hook.util.xlog
import kotlin.math.roundToInt
import java.util.Collections
import java.util.Locale
import java.util.WeakHashMap
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SuppressLint("PrivateApi")
object QS {
    private data class ShadeClockOriginalStyle(
        val maxLines: Int,
        val singleLine: Boolean,
        val textSizePx: Float,
        val typeface: Typeface?,
        val textColor: Int,
        val gravity: Int,
        val includeFontPadding: Boolean,
        val textAlignment: Int,
        val lineSpacingExtra: Float,
        val lineSpacingMultiplier: Float,
        val minHeight: Int,
        val minimumHeight: Int,
    )

    @Volatile
    private var shadeClockRuntime = Preference.SystemUI.StatusBar()

    private val shadeClockClockViews = WeakHashMap<TextView, Unit>()
    private val shadeClockDateViews = WeakHashMap<TextView, Unit>()
    private val shadeClockNativeTexts = WeakHashMap<TextView, CharSequence>()
    private val shadeClockOriginalStyles = WeakHashMap<TextView, ShadeClockOriginalStyle>()
    private val shadeClockReplaceGuard = Collections.newSetFromMap(WeakHashMap<TextView, Boolean>())

    fun updateShadeClockRuntime(current: Preference.SystemUI.StatusBar) {
        shadeClockRuntime = current
        val clockViews = synchronized(shadeClockClockViews) { shadeClockClockViews.keys.toList() }
        val dateViews = synchronized(shadeClockDateViews) { shadeClockDateViews.keys.toList() }
        clockViews.forEach { view -> view.post { applyShadeClockClock(view) } }
        dateViews.forEach { view -> view.post { applyShadeClockDate(view) } }
    }

    private fun isShadeClockTimeView(view: View): Boolean {
        return view.javaClass.name in setOf(
            "com.android.systemui.statusbar.policy.QSClock",
            "com.android.systemui.statusbar.policy.QSClockHeaderView",
            "com.android.systemui.statusbar.policy.QSClockPanelView",
        )
    }

    private fun isShadeClockDateView(view: View): Boolean {
        return view.javaClass.name in setOf(
            "com.android.systemui.statusbar.policy.QSDate",
            "com.android.systemui.statusbar.policy.QSShortenDate",
        )
    }

    private fun rememberShadeClockView(view: TextView) {
        synchronized(shadeClockNativeTexts) {
            shadeClockNativeTexts.putIfAbsent(view, view.text ?: "")
        }
        synchronized(shadeClockOriginalStyles) {
            shadeClockOriginalStyles.getOrPut(view) {
                ShadeClockOriginalStyle(
                    maxLines = view.maxLines,
                    singleLine = view.isSingleLine,
                    textSizePx = view.textSize,
                    typeface = view.typeface,
                    textColor = view.currentTextColor,
                    gravity = view.gravity,
                    includeFontPadding = view.includeFontPadding,
                    textAlignment = view.textAlignment,
                    lineSpacingExtra = view.lineSpacingExtra,
                    lineSpacingMultiplier = view.lineSpacingMultiplier,
                    minHeight = view.minHeight,
                    minimumHeight = view.minimumHeight,
                )
            }
        }
    }

    private fun registerShadeClockClockView(view: TextView) {
        synchronized(shadeClockClockViews) { shadeClockClockViews[view] = Unit }
        rememberShadeClockView(view)
    }

    private fun registerShadeClockDateView(view: TextView) {
        synchronized(shadeClockDateViews) { shadeClockDateViews[view] = Unit }
        rememberShadeClockView(view)
    }

    private fun isShadeClockGuarded(view: TextView): Boolean =
        synchronized(shadeClockReplaceGuard) { shadeClockReplaceGuard.contains(view) }

    private fun setShadeClockText(view: TextView, text: CharSequence) {
        synchronized(shadeClockReplaceGuard) { shadeClockReplaceGuard.add(view) }
        try {
            view.text = text
        } finally {
            synchronized(shadeClockReplaceGuard) { shadeClockReplaceGuard.remove(view) }
        }
    }

    private fun restoreShadeClockView(view: TextView) {
        val nativeText = synchronized(shadeClockNativeTexts) { shadeClockNativeTexts[view] }
        if (nativeText != null) setShadeClockText(view, nativeText)
        synchronized(shadeClockOriginalStyles) { shadeClockOriginalStyles[view] }?.let { original ->
            view.maxLines = if (original.maxLines > 0) original.maxLines else Int.MAX_VALUE
            view.isSingleLine = original.singleLine
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, original.textSizePx)
            view.typeface = original.typeface
            view.setTextColor(original.textColor)
            view.gravity = original.gravity
            view.includeFontPadding = original.includeFontPadding
            view.textAlignment = original.textAlignment
            view.setLineSpacing(original.lineSpacingExtra, original.lineSpacingMultiplier)
            view.minHeight = original.minHeight
            view.minimumHeight = original.minimumHeight
        }
        view.requestLayout()
    }

    private fun shadeClockShowSeconds(view: TextView): Boolean {
        return Settings.Secure.getString(view.context.contentResolver, "icon_blacklist")
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .contains("slimindicator_show_seconds")
    }

    private fun shadeClockTimeText(view: TextView): CharSequence {
        val config = shadeClockRuntime
        val now = LocalDateTime.now()
        val showSeconds = shadeClockShowSeconds(view)
        val pattern = if (showSeconds) "HH:mm:ss" else "HH:mm"
        val timePart = if (config.shadeClockTimeEnabled) {
            DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(now)
        } else {
            ""
        }
        val text = runCatching {
            ShadeClockFormatter.format(config, now, showSeconds)
        }.getOrElse {
            DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(now)
        }
        return shadeClockStyledText(text, timePart)
    }

    private fun shadeClockStyledText(text: String, timePart: String): CharSequence {
        val spannable = SpannableString(text)
        val timeEnd = if (timePart.isNotBlank()) {
            text.indexOf(timePart).takeIf { it >= 0 }?.plus(timePart.length) ?: 0
        } else {
            0
        }
        if (timeEnd > 0) {
            spannable.setSpan(
                AbsoluteSizeSpan(23, true),
                0,
                timeEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            spannable.setSpan(
                ShadeClockFontWeightSpan(600),
                0,
                timeEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        if (timeEnd < text.length) {
            val dateStart = when {
                timeEnd == 0 -> 0
                text.getOrNull(timeEnd) == '\n' || text.getOrNull(timeEnd) == ' ' -> timeEnd + 1
                else -> timeEnd
            }.coerceAtMost(text.length)
            if (dateStart < text.length) {
                spannable.setSpan(
                    AbsoluteSizeSpan(15, true),
                    dateStart,
                    text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                spannable.setSpan(
                    ShadeClockFontWeightSpan(400),
                    dateStart,
                    text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
        return spannable
    }

    private class ShadeClockFontWeightSpan(
        private val weight: Int,
    ) : MetricAffectingSpan() {
        private fun apply(paint: TextPaint) {
            val base = Typeface.create("sec", Typeface.NORMAL)
            paint.typeface = Typeface.create(base, weight, false)
        }

        override fun updateDrawState(textPaint: TextPaint) = apply(textPaint)
        override fun updateMeasureState(textPaint: TextPaint) = apply(textPaint)
    }

    private fun applyShadeClockLayout(textView: TextView) {
        val doubleLine = shadeClockRuntime.shadeClockLayoutStyle == 1
        runCatching {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f)
            textView.typeface = Typeface.create(
                Typeface.create("sec", Typeface.NORMAL),
                600,
                false,
            )
            textView.isSingleLine = !doubleLine
            textView.maxLines = if (doubleLine) 2 else 1
            textView.includeFontPadding = false
            textView.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            textView.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            if (doubleLine) {
                textView.setLineSpacing(
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        2f,
                        textView.resources.displayMetrics,
                    ),
                    0.82f,
                )
                textView.minHeight = 0
                textView.minimumHeight = 0
                textView.layoutParams = textView.layoutParams?.apply {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                var parentView = textView.parent as? View
                repeat(8) {
                    val group = parentView as? ViewGroup ?: return@repeat
                    group.clipChildren = false
                    group.clipToPadding = false
                    group.minimumHeight = 0
                    group.layoutParams = group.layoutParams?.apply {
                        if (height > 0) height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    parentView = group.parent as? View
                }
            } else {
                textView.setLines(1)
                textView.setLineSpacing(0f, 1f)
            }
        }
    }

    private fun applyShadeClockClock(view: TextView, nativeText: CharSequence? = null) {
        if (nativeText != null) {
            synchronized(shadeClockNativeTexts) { shadeClockNativeTexts[view] = nativeText }
        }
        rememberShadeClockView(view)
        if (!shadeClockRuntime.shadeClockCustomEnabled) {
            restoreShadeClockView(view)
            return
        }
        setShadeClockText(view, shadeClockTimeText(view))
        applyShadeClockLayout(view)
    }

    private fun applyShadeClockDate(view: TextView, nativeText: CharSequence? = null) {
        if (nativeText != null) {
            synchronized(shadeClockNativeTexts) { shadeClockNativeTexts[view] = nativeText }
        }
        rememberShadeClockView(view)
        if (!shadeClockRuntime.shadeClockCustomEnabled) {
            restoreShadeClockView(view)
            return
        }
        setShadeClockText(view, "")
        view.requestLayout()
    }

    private fun findShadeClockMethod(
        clazz: Class<*>,
        name: String,
        vararg parameterTypes: Class<*>,
    ): java.lang.reflect.Method? {
        var current: Class<*>? = clazz
        while (current != null) {
            val owner = current
            val method = runCatching {
                owner.getDeclaredMethod(name, *parameterTypes)
            }.getOrNull()
            if (method != null) return method
            current = owner.superclass
        }
        return null
    }

    context(xposedModule: XposedModule)
    fun installShadeClockCustomization(current: Preference.SystemUI.StatusBar) =
        afterAttachTry(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            shadeClockRuntime = current
            val bellClass = classLoader.loadClass(
                "com.android.systemui.statusbar.policy.QSClockBellSound",
            )

            fun installTextHooks(className: String, datePart: Boolean) {
                val clazz = runCatching { classLoader.loadClass(className) }.getOrNull() ?: return
                val notifyMethod = findShadeClockMethod(clazz, "notifyTimeChanged", bellClass)
                if (notifyMethod != null) {
                    runCatching {
                        xposedModule.hook(notifyMethod).intercept { chain ->
                            val result = chain.proceed()
                            val view = chain.thisObject as? TextView
                            if (
                                view != null &&
                                (if (datePart) isShadeClockDateView(view) else isShadeClockTimeView(view))
                            ) {
                                if (datePart) {
                                    registerShadeClockDateView(view)
                                    if (shadeClockRuntime.shadeClockCustomEnabled) {
                                        applyShadeClockDate(view)
                                    } else {
                                        synchronized(shadeClockNativeTexts) {
                                            shadeClockNativeTexts[view] = view.text ?: ""
                                        }
                                    }
                                } else {
                                    registerShadeClockClockView(view)
                                    if (shadeClockRuntime.shadeClockCustomEnabled) {
                                        applyShadeClockClock(view)
                                    } else {
                                        synchronized(shadeClockNativeTexts) {
                                            shadeClockNativeTexts[view] = view.text ?: ""
                                        }
                                    }
                                }
                            }
                            result
                        }
                    }.onFailure { xlog(it) }
                }

                // Only hook an override declared by this clock/date class. Falling
                // back to View.onAttachedToWindow would affect every TextView in SystemUI.
                val attachedMethod = runCatching {
                    clazz.getDeclaredMethod("onAttachedToWindow")
                }.getOrNull()
                if (attachedMethod != null) {
                    runCatching {
                        xposedModule.hook(attachedMethod).intercept { chain ->
                            val result = chain.proceed()
                            val view = chain.thisObject as? TextView
                            if (
                                view != null &&
                                (if (datePart) isShadeClockDateView(view) else isShadeClockTimeView(view))
                            ) {
                                if (datePart) {
                                    registerShadeClockDateView(view)
                                    if (shadeClockRuntime.shadeClockCustomEnabled) {
                                        applyShadeClockDate(view)
                                    } else {
                                        synchronized(shadeClockNativeTexts) {
                                            shadeClockNativeTexts[view] = view.text ?: ""
                                        }
                                    }
                                } else {
                                    registerShadeClockClockView(view)
                                    if (shadeClockRuntime.shadeClockCustomEnabled) {
                                        applyShadeClockClock(view)
                                    } else {
                                        synchronized(shadeClockNativeTexts) {
                                            shadeClockNativeTexts[view] = view.text ?: ""
                                        }
                                    }
                                }
                            }
                            result
                        }
                    }.onFailure { xlog(it) }
                }
            }

            listOf(
                "com.android.systemui.statusbar.policy.QSClockHeaderView",
                "com.android.systemui.statusbar.policy.QSClockPanelView",
                "com.android.systemui.statusbar.policy.QSClock",
            ).forEach { installTextHooks(it, datePart = false) }

            listOf(
                "com.android.systemui.statusbar.policy.QSDate",
                "com.android.systemui.statusbar.policy.QSShortenDate",
            ).forEach { installTextHooks(it, datePart = true) }

            runCatching {
                val setTextMethod = TextView::class.java.getDeclaredMethod(
                    "setText",
                    CharSequence::class.java,
                )
                xposedModule.hook(setTextMethod).intercept { chain ->
                    val view = chain.thisObject as? TextView
                    if (
                        view != null &&
                        shadeClockRuntime.shadeClockCustomEnabled &&
                        !isShadeClockGuarded(view)
                    ) {
                        // Save Samsung's incoming native text before replacing it,
                        // so disabling customization can restore the original view.
                        synchronized(shadeClockNativeTexts) {
                            shadeClockNativeTexts[view] = chain.args.getOrNull(0) as? CharSequence ?: ""
                        }
                        when {
                            isShadeClockTimeView(view) -> {
                                registerShadeClockClockView(view)
                                chain.args[0] = shadeClockTimeText(view)
                                applyShadeClockLayout(view)
                            }
                            isShadeClockDateView(view) -> {
                                registerShadeClockDateView(view)
                                chain.args[0] = ""
                            }
                        }
                    }
                    chain.proceed()
                }
            }.onFailure { xlog(it) }
        }

    enum class QsBar {
        MediaPlayer,
        NearbyDevicesAndDeviceControl,
        SecurityFooter,
        DataUsage,
        SmartViewAndModes,
    }


    context(xposedModule: XposedModule)
    fun hideDeviceControlQsTile() = afterAttachTry(Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val qsTileHostClass = classLoader.loadClass("com.android.systemui.qs.QSTileHost")
        val createTileMethod = qsTileHostClass.getDeclaredMethod("createTile", String::class.java)
        xposedModule.hook(createTileMethod).intercept { chain ->
            if (chain.args.firstOrNull() == "DeviceControl") {
                null
            } else {
                chain.proceed()
            }
        }
    }

    context(xposedModule: XposedModule)
    fun hideSmartViewQsTile() = afterAttachTry(Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val qsTileHostClass = classLoader.loadClass("com.android.systemui.qs.QSTileHost")
        val createTileMethod = qsTileHostClass.getDeclaredMethod("createTile", String::class.java)
        xposedModule.hook(createTileMethod).intercept { chain ->
            if (chain.args.firstOrNull() == "custom(com.samsung.android.smartmirroring/.tile.SmartMirroringTile)") {
                null
            } else {
                chain.proceed()
            }
        }
    }

    // related classes: BarFactory BarController  BarOrderInteractor
    context(xposedModule: XposedModule)
    fun hideQsBar(qsBarSet: Set<QsBar>) =
        afterAttachTry(qsBarSet.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val hideBarCallback = XposedInterface.Hooker { chain ->
                val result = chain.proceed()
                val view = chain.thisObject.reflect["mBarRootView"] as? View
                view?.visibility = View.GONE
                result
            }

            if (QsBar.NearbyDevicesAndDeviceControl in qsBarSet && ONE_UI_VERSION < 80500) {
                try {
                    if (applicationInfo.targetSdkVersion >= Build.VERSION_CODES.BAKLAVA) {
                        val bottomLargeTileBarClass = classLoader.loadClass(
                            "com.android.systemui.qs.bar.BottomLargeTileBar"
                        )
                        val showBarMethod = bottomLargeTileBarClass.getDeclaredMethod(
                            "showBar",
                            Boolean::class.javaPrimitiveType
                        )
                        xposedModule.hook(showBarMethod).intercept(hideBarCallback)
                    } else {
                        val largeTileBarClass = classLoader.loadClass(
                            "com.android.systemui.qs.bar.LargeTileBar"
                        )
                        val updateLayoutMethod = largeTileBarClass.getDeclaredMethod(
                            "updateLayout",
                            LinearLayout::class.java
                        )
                        xposedModule.hook(updateLayoutMethod).intercept { chain ->
                            val result = chain.proceed()
                            val string = chain.thisObject.reflect["TAG"] as? String
                            if (string == "BottomLargeTileBar") {
                                val view = chain.thisObject.reflect["mBarRootView"] as? View
                                view?.visibility = View.GONE
                            }
                            result
                        }
                    }
                } catch (t: Throwable) {
                    xlog(t)
                }
            }

            if (QsBar.MediaPlayer in qsBarSet && ONE_UI_VERSION < 80500) {
                try {
                    val qsMediaPlayerBarClass = classLoader.loadClass(
                        "com.android.systemui.qs.bar.QSMediaPlayerBar"
                    )
                    val inflateViewsMethod = qsMediaPlayerBarClass.getDeclaredMethod(
                        "inflateViews",
                        ViewGroup::class.java
                    )
                    xposedModule.hook(inflateViewsMethod).intercept(hideBarCallback)
                } catch (t: Throwable) {
                    xlog(t)
                }
            }

            if (QsBar.SecurityFooter in qsBarSet) {
                try {
                    if (ONE_UI_VERSION >= 80500) {
                        val bottomBannerTransformClass = runCatching {
                            classLoader.loadClass(
                                $$"com.android.systemui.samsung.quicksetting.ui.banner.BottomBannerViewModel$1$1"
                            )
                        }.getOrNull()
                        bottomBannerTransformClass?.declaredMethods
                            ?.filter { it.name == "invoke" }
                            ?.forEach { method ->
                                xposedModule.hook(method).intercept { chain ->
                                    if (chain.args.getOrNull(1) is Boolean) {
                                        val newArgs = chain.args.toTypedArray()
                                        newArgs[1] = false
                                        chain.proceed(newArgs)
                                    } else {
                                        chain.proceed()
                                    }
                                }
                            }
                    } else if (applicationInfo.targetSdkVersion >= Build.VERSION_CODES.BAKLAVA) {
                        val barItemImplClass = classLoader.loadClass(
                            "com.android.systemui.qs.bar.BarItemImpl"
                        )
                        val showBarMethod = barItemImplClass.getDeclaredMethod(
                            "showBar",
                            Boolean::class.javaPrimitiveType
                        )
                        xposedModule.hook(showBarMethod).intercept { chain ->
                            val tag = chain.thisObject.reflect["TAG"]
                            if (tag == "SecurityFooterBar") {
                                val newArgs = chain.args.toTypedArray()
                                newArgs[0] = false
                                chain.proceed(newArgs)
                            } else {
                                chain.proceed()
                            }
                        }
                    } else {
                        val securityFooterBarClass = classLoader.loadClass(
                            "com.android.systemui.qs.bar.SecurityFooterBar"
                        )
                        val onVisibilityChangedMethod = securityFooterBarClass.getDeclaredMethod(
                            "onVisibilityChanged",
                            Int::class.javaPrimitiveType
                        )
                        xposedModule.hook(onVisibilityChangedMethod).intercept(hideBarCallback)
                    }
                } catch (t: Throwable) {
                    xlog(t)
                }
            }

            if (QsBar.DataUsage in qsBarSet) {
                try {
                    if (ONE_UI_VERSION >= 80500) {
                        val bottomBannerTransformClass = runCatching {
                            classLoader.loadClass(
                                $$"com.android.systemui.samsung.quicksetting.ui.banner.BottomBannerViewModel$1$1"
                            )
                        }.getOrNull()
                        bottomBannerTransformClass?.declaredMethods
                            ?.filter { it.name == "invoke" }
                            ?.forEach { method ->
                                xposedModule.hook(method).intercept { chain ->
                                    if (chain.args.getOrNull(2) is Boolean) {
                                        val newArgs = chain.args.toTypedArray()
                                        newArgs[2] = false
                                        chain.proceed(newArgs)
                                    } else {
                                        chain.proceed()
                                    }
                                }
                            }
                    } else {
                        val dataUsageBarClass = classLoader.loadClass(
                            "com.android.systemui.qs.bar.DataUsageBar"
                        )
                        val isAvailableMethod = dataUsageBarClass.getDeclaredMethod("isAvailable")
                        xposedModule.hook(isAvailableMethod).intercept { false }
                    }
                } catch (t: Throwable) {
                    xlog(t)
                }
            }

            if (QsBar.SmartViewAndModes in qsBarSet && ONE_UI_VERSION < 80500) {
                try {
                    val barItemImplClass = classLoader.loadClass(
                        "com.android.systemui.qs.bar.BarItemImpl"
                    )
                    val showBarMethod = barItemImplClass.getDeclaredMethod(
                        "showBar",
                        Boolean::class.javaPrimitiveType
                    )
                    xposedModule.hook(showBarMethod).intercept { chain ->
                        val tag = chain.thisObject.reflect["TAG"]
                        if (tag == "SmartViewLargeTileBar") {
                            val newArgs = chain.args.toTypedArray()
                            newArgs[0] = false
                            chain.proceed(newArgs)
                        } else {
                            chain.proceed()
                        }
                    }
                } catch (t: Throwable) {
                    xlog(t)
                }
            }

            // 横屏
            try {
                if (ONE_UI_VERSION >= 80500) return@afterAttachTry
                val nearbyDevicesAndDeviceControl = QsBar.NearbyDevicesAndDeviceControl in qsBarSet
                val smartViewAndModes = QsBar.SmartViewAndModes in qsBarSet
                if (!nearbyDevicesAndDeviceControl && !smartViewAndModes) {
                    return@afterAttachTry
                }
                val topLargeTileBarClass = classLoader.loadClass(
                    "com.android.systemui.qs.bar.TopLargeTileBar"
                )
                val tileRecordClass = classLoader.loadClass(
                    $$"com.android.systemui.qs.SecQSPanelControllerBase$TileRecord"
                )
                val addTileMethod = topLargeTileBarClass.getDeclaredMethod("addTile", tileRecordClass)
                xposedModule.hook(addTileMethod).intercept { chain ->
                    val tile = chain.args[0]?.reflect?.get("tile")
                    val tileSpec = tile?.reflect?.call("getTileSpec")
                    val flag = when (tileSpec) {
                        "DeviceControl" if nearbyDevicesAndDeviceControl -> true
                        "custom(com.samsung.android.mydevice/.quicksettings.MyDeviceTileService)" if nearbyDevicesAndDeviceControl -> true
                        "custom(com.samsung.android.smartmirroring/.tile.SmartMirroringTile)" if smartViewAndModes -> true
                        "custom(com.samsung.android.app.routines/.LifestyleModeTile)" if smartViewAndModes -> true
                        else -> false
                    }
                    if (flag) {
                        null
                    } else {
                        chain.proceed()
                    }
                }
            } catch (t: Throwable) {
                xlog(t)
            }
        }

    context(xposedModule: XposedModule)
    fun alwaysExpandQsTileChunk() =
        afterAttachTry(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && ONE_UI_VERSION < 80500) {
            try {
                val tileChunkLayoutBarClass = classLoader.loadClass(
                    "com.android.systemui.qs.bar.TileChunkLayoutBar"
                )
                val setContainerHeightMethod = tileChunkLayoutBarClass.getDeclaredMethod(
                    "setContainerHeight",
                    Int::class.javaPrimitiveType
                )
                xposedModule.hook(setContainerHeightMethod).intercept { chain ->
                    val expandedHeight = chain.thisObject.reflect["mContainerExpandedHeight"] as? Int
                    if (expandedHeight != null) {
                        val newArgs = chain.args.toTypedArray()
                        newArgs[0] = expandedHeight
                        chain.proceed(newArgs)
                    } else {
                        chain.proceed()
                    }
                }

                val inflateViewsMethod = tileChunkLayoutBarClass.getDeclaredMethod(
                    "inflateViews",
                    ViewGroup::class.java
                )
                xposedModule.hook(inflateViewsMethod).intercept { chain ->
                    val result = chain.proceed()
                    val scrollIndicator = chain.thisObject.reflect["mScrollIndicatorClickContainer"] as? View
                    scrollIndicator?.visibility = View.GONE
                    result
                }
            } catch (t: Throwable) {
                xlog(t)
            }
        }

    context(xposedModule: XposedModule)
    fun alwaysShowTimeDateOnQs() =
        afterAttachTry(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && ONE_UI_VERSION < 80500) {
            try {
                val animatorClass = classLoader.loadClass(
                    "com.android.systemui.qs.animator.SecQSFragmentAnimatorBase"
                )
                val qsClass = classLoader.loadClass("com.android.systemui.plugins.qs.QS")
                val setQsMethod = animatorClass.getDeclaredMethod("setQs", qsClass)
                xposedModule.hook(setQsMethod).intercept { chain ->
                    val result = chain.proceed()
                    if (applicationInfo.targetSdkVersion >= Build.VERSION_CODES.BAKLAVA) {
                        chain.thisObject.reflect["clockDateContainer"] = null
                    } else {
                        val context = chain.thisObject.reflect["context"] as? Context
                        if (context != null) {
                            chain.thisObject.reflect["clockDateContainer"] = View(context)
                        }
                    }
                    result
                }
            } catch (t: Throwable) {
                xlog(t)
            }

            try {
                val callback = XposedInterface.Hooker { chain ->
                    val result = chain.proceed()
                    val mContext = chain.thisObject.reflect["mContext"] as? Context
                    if (mContext != null) {
                        chain.thisObject.reflect["mClockDateContainer"] = View(mContext)
                    }
                    result
                }
                if (applicationInfo.targetSdkVersion >= Build.VERSION_CODES.BAKLAVA) {
                    val legacyAnimatorClass = classLoader.loadClass(
                        "com.android.systemui.qs.animator.LegacyQsExpandAnimator"
                    )
                    val method = legacyAnimatorClass.getDeclaredMethod($$"updateViews$2")
                    xposedModule.hook(method).intercept(callback)
                } else {
                    val expandAnimatorClass = classLoader.loadClass(
                        "com.android.systemui.qs.animator.QsExpandAnimator"
                    )
                    val method = expandAnimatorClass.getDeclaredMethod("updateViews")
                    xposedModule.hook(method).intercept(callback)
                }
            } catch (t: Throwable) {
                xlog(t)
            }
        }

    context(xposedModule: XposedModule)
    fun setQsClockStyle(
        monospaced: Boolean,
        modifyTextSize: Boolean,
        textSize: Float
    ) = afterAttachTry(monospaced || modifyTextSize) {
        // 布局在 res/layout/sec_qqs_date_buttons.xml
        val headerClass = classLoader.loadClass(
            "com.android.systemui.qs.SecQuickStatusBarHeader"
        )
        val onFinishInflateMethod = headerClass.getDeclaredMethod("onFinishInflate")
        xposedModule.hook(onFinishInflateMethod).intercept { chain ->
            val result = chain.proceed()
            try {
                val clockView = chain.thisObject.reflect["mClockView"] as? TextView
                if (clockView != null) {
                    // 启用 tabular (等宽) 数字: 'tnum' 1
                    // 禁用 proportional (不等宽) 数字: 'pnum' 0
                    if (monospaced) {
                        clockView.fontFeatureSettings = "'tnum' 1, 'pnum' 0"
                    }
                    if (modifyTextSize) {
                        clockView.textSize = textSize

                        val density = clockView.context.resources.displayMetrics.density
                        // 15sp 到 70sp
                        val ratio = 0.00218181f * textSize * textSize + 0.16727272f * textSize
                        val padding = -(density * ratio).roundToInt()
                        clockView.setPadding(
                            clockView.paddingLeft,
                            padding,
                            clockView.paddingRight,
                            padding
                        )
                    }
                }
            } catch (t: Throwable) {
                xlog(t)
            }
            result
        }
    }

    context(xposedModule: XposedModule)
    fun supportOutdoorMode() {
        val outdoorModeRowTag = "com.alohaoneui.hook.outdoor_mode_row"

        fun isOutdoorModeEnabled(context: Context): Boolean {
            return (android.provider.Settings.System::class.java.reflect.call(
                "getIntForUser",
                context.contentResolver,
                "display_outdoor_mode",
                0,
                -2
            ) as Int) != 0
        }

        fun setOutdoorModeEnabled(context: Context, enabled: Boolean) {
            android.provider.Settings.System::class.java.reflect.call(
                "putIntForUser",
                context.contentResolver,
                "display_outdoor_mode",
                if (enabled) 1 else 0,
                -2
            )
        }

        @SuppressLint("DiscouragedApi")
        fun addOutdoorModeRow(
            context: Context,
            detailView: ViewGroup,
            switchPreferenceClass: Class<*>
        ) {
            try {
                val outdoorContainer = switchPreferenceClass.reflect.call(
                    "inflateSwitch",
                    context,
                    detailView
                ) as View
                outdoorContainer.tag = outdoorModeRowTag

                val res = context.resources
                val titleId = res.getIdentifier(
                    "sec_brightness_outdoor_mode_title",
                    "string",
                    Package.SYSTEMUI
                )
                val summaryId = res.getIdentifier(
                    "sec_brightness_outdoor_mode_summary",
                    "string",
                    Package.SYSTEMUI
                )
                val titleViewId = res.getIdentifier("title", "id", Package.SYSTEMUI)
                val summaryViewId = res.getIdentifier("title_summary", "id", Package.SYSTEMUI)
                val switchViewId = res.getIdentifier("title_switch", "id", Package.SYSTEMUI)
                if (titleId == 0 || titleViewId == 0 || switchViewId == 0) return

                outdoorContainer.findViewById<TextView>(titleViewId)?.text =
                    res.getString(titleId)

                outdoorContainer.findViewById<TextView>(summaryViewId)?.apply {
                    text = if (summaryId != 0) res.getString(summaryId) else ""
                    visibility = if (summaryId != 0) View.VISIBLE else View.GONE
                }

                val outdoorSwitch: CompoundButton? = outdoorContainer.findViewById(switchViewId)
                outdoorSwitch?.isChecked = isOutdoorModeEnabled(context)
                outdoorSwitch?.setOnCheckedChangeListener { _, isChecked ->
                    setOutdoorModeEnabled(context, isChecked)
                }
                outdoorContainer.setOnClickListener {
                    val switch = outdoorSwitch ?: return@setOnClickListener
                    switch.isChecked = !switch.isChecked
                }

                // Keep the row directly below Samsung's Adaptive brightness row.
                val index = minOf(2, detailView.childCount)
                detailView.addView(outdoorContainer, index)
            } catch (t: Throwable) {
                xlog(t)
            }
        }

        afterAttachTry {
            val switchPreferenceClass = classLoader.loadClass(
                "com.android.systemui.qs.SecQSSwitchPreference"
            )
            val brightnessDetailClass = runCatching {
                classLoader.loadClass("com.android.systemui.settings.brightness.BrightnessDetailAdapter")
            }.getOrNull() ?: runCatching {
                classLoader.loadClass($$"com.android.systemui.settings.brightness.BrightnessDetail$1")
            }.getOrNull()

            if (brightnessDetailClass != null) {
                val createDetailViewMethod = brightnessDetailClass.getDeclaredMethod(
                    "createDetailView",
                    Context::class.java,
                    View::class.java,
                    ViewGroup::class.java
                )
                xposedModule.hook(createDetailViewMethod).intercept { chain ->
                    val result = chain.proceed()
                    val context = chain.args[0] as Context
                    val detailView = result as? ViewGroup
                    if (detailView != null) {
                        addOutdoorModeRow(context, detailView, switchPreferenceClass)
                    }
                    result
                }
            }
        }
    }

    context(xposedModule: XposedModule)
    fun showTraditionalChineseDateOnQS() = afterAttachTry(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    ) {
        val qsShortenDateClass = classLoader.loadClass(
            "com.android.systemui.statusbar.policy.QSShortenDate"
        )
        qsShortenDateClass.declaredConstructors.forEach { constructor ->
            xposedModule.hook(constructor).intercept { chain ->
                val result = chain.proceed()
                val textView = chain.thisObject as? TextView
                textView?.apply {
                    isSingleLine = false
                    setLines(2)
                    ellipsize = null
                    setPadding(paddingLeft, -10, paddingRight, -10)
                    setLineSpacing(0f, 0.8f)
                    val density = context.resources.displayMetrics.density
                    translationY = -10 * density
                }
                result
            }
        }

        val qsClockBellSoundClass = classLoader.loadClass(
            "com.android.systemui.statusbar.policy.QSClockBellSound"
        )
        val notifyTimeChangedMethod = qsShortenDateClass.getDeclaredMethod(
            "notifyTimeChanged",
            qsClockBellSoundClass
        )
        var previousDate = ""
        var result = ""
        xposedModule.hook(notifyTimeChangedMethod).intercept { chain ->
            val nativeResult = chain.proceed()
            val dateTextView = chain.thisObject as? TextView
            if (shadeClockRuntime.shadeClockCustomEnabled) {
                // The dedicated shade-clock hook now owns the combined clock view
                // and clears this native date view; do not apply the legacy layout here.
                return@intercept nativeResult
            }
            val shortDateText = chain.args[0]?.reflect?.get("ShortDateText") as? String ?: ""
            if (shortDateText != previousDate) {
                previousDate = shortDateText
                result = "$shortDateText\n${TraditionalChineseCalendar.getMonthAndDay()}"
            }
            if (dateTextView?.text != result) {
                dateTextView?.text = result
            }
            null
        }
    }

    context(xposedModule: XposedModule)
    fun addVolumeProgressToQsBar() = afterAttachTry(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && ONE_UI_VERSION < 80500
    ) {
        var textView: TextView? = null

        val volumeBarClass = classLoader.loadClass("com.android.systemui.qs.bar.VolumeBar")
        val inflateViewsMethod = volumeBarClass.getDeclaredMethod("inflateViews", ViewGroup::class.java)
        xposedModule.hook(inflateViewsMethod).intercept { chain ->
            val result = chain.proceed()
            try {
                val slider = chain.thisObject.reflect["mSlider"] as? View
                val sliderParent = slider?.parent as? FrameLayout
                if (sliderParent != null) {
                    val volumeSeekBar = chain.thisObject.reflect["mVolumeSeekBar"]
                    val progress = volumeSeekBar?.reflect?.get("progress") as? Int ?: 0
                    textView = TextView(sliderParent.context).apply {
                        setTextColor(Color.WHITE)
                        text = progress.toString()
                    }
                    val layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.END or Gravity.CENTER_VERTICAL
                        marginEnd = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            8.0f,
                            sliderParent.context.resources.displayMetrics
                        ).roundToInt()
                    }
                    sliderParent.addView(textView, layoutParams)
                }
            } catch (t: Throwable) {
                xlog(t)
            }
            result
        }

        val listenerClass = classLoader.loadClass(
            $$"com.android.systemui.qs.bar.VolumeToggleSeekBar$VolumeSeekbarChangeListener"
        )
        val onProgressChangedMethod = listenerClass.getDeclaredMethod(
            "onProgressChanged",
            SeekBar::class.java,
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        )
        xposedModule.hook(onProgressChangedMethod).intercept { chain ->
            val result = chain.proceed()
            textView?.text = chain.args[1].toString()
            result
        }
    }

    context(xposedModule: XposedModule)
    fun addBrightnessProgressToQsBar() = afterAttachTry(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && ONE_UI_VERSION < 80500
    ) {
        val textViewList = mutableListOf<TextView>()

        val controllerClass = classLoader.loadClass(
            "com.android.systemui.settings.brightness.BrightnessSliderController"
        )
        val onViewAttachedMethod = controllerClass.getDeclaredMethod("onViewAttached")
        xposedModule.hook(onViewAttachedMethod).intercept { chain ->
            val result = chain.proceed()
            try {
                val view = chain.thisObject.reflect["mView"]
                val slider = view?.reflect?.get("mSlider") as? View
                val frameLayout = slider?.parent as? FrameLayout
                if (frameLayout != null) {
                    val textView = TextView(frameLayout.context).apply {
                        setTextColor(Color.WHITE)
                    }
                    val layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.END or Gravity.CENTER_VERTICAL
                        marginEnd = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            8.0f,
                            frameLayout.context.resources.displayMetrics
                        ).roundToInt()
                    }
                    textViewList.add(textView)
                    frameLayout.addView(textView, layoutParams)
                }
            } catch (t: Throwable) {
                xlog(t)
            }
            result
        }

        val listenerClass = classLoader.loadClass(
            $$"com.android.systemui.settings.brightness.BrightnessSliderController$2"
        )
        val onProgressChangedMethod = listenerClass.getDeclaredMethod(
            "onProgressChanged",
            SeekBar::class.java,
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        )
        xposedModule.hook(onProgressChangedMethod).intercept { chain ->
            val result = chain.proceed()
            val progress = chain.args[1].toString()
            textViewList.forEach {
                it.text = progress
            }
            result
        }
    }
}
