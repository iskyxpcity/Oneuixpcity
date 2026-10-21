package com.alohaoneui.hook.systemui

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import com.alohaoneui.common.Preference
import com.alohaoneui.hook.util.PreferenceProvider
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Runtime adapter for the global shortcut controls migrated from App20261018Aloha.
 *
 * Unlike the source project, Aloha OneUI uses libxposed API 101 and a single serialized
 * Preference object. Every callback refreshes its cached preference from preference.json,
 * so changing an option doesn't require reinstalling or rewriting legacy SharedPreferences.
 * The host-specific hook points are deliberately isolated here to avoid touching unrelated
 * status-bar behavior (including Samsung QuickStar's native seconds scheduler).
 */
object GlobalQuickActions {
    private const val TAG = "AlohaOneUI-GlobalQuickActions"
    private const val ACTION_NONE = "none"
    private const val ACTION_CUSTOM_APP_PREFIX = "custom_app:"
    private const val STATUS_BAR_DOUBLE_CLICK_MS = 320L
    private const val DEFAULT_LONG_PRESS_MS = 500L
    private const val DOUBLE_TAP_MS = 300L
    private const val FLOATING_BALL_LONG_PRESS_MS = 420L
    private const val FLOATING_BALL_SWIPE_THRESHOLD_DP = 48f
    private const val FLOATING_BALL_TAP_SLOP_DP = 8f

    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var cachedSettings: Preference.GlobalShortcutActions? = null
    @Volatile private var cachedSettingsAt = 0L
    @Volatile private var systemServerContext: Context? = null
    @Volatile private var inputManagerService: Any? = null
    @Volatile private var phoneWindowManager: Any? = null

    private val gesturePointerIds = IntArray(3) { -1 }
    private val gestureStartX = FloatArray(3)
    private val gestureStartY = FloatArray(3)
    private var threeFingerActive = false
    private var threeFingerConsumed = false
    private var threeFingerStartSpan = 0f

    private var lastPointerSignature = ""
    private var lastPointerSignatureAt = 0L

    private var statusGestureActive = false
    private var statusGestureMoved = false
    private var statusLongPressTriggered = false
    private var statusDownX = 0f
    private var statusDownY = 0f
    private var statusDownTime = 0L
    private var statusSide = 0
    private var statusLastTapTime = 0L
    private var statusPendingSingle: Runnable? = null
    private var statusLongPressRunnable: Runnable? = null

    private var volumeDownKeyCode = 0
    private var volumeLongPressTriggered = false
    private var volumeLongPressRunnable: Runnable? = null
    private var volumeDoubleClickRunnable: Runnable? = null
    private var volumeDoubleClickPending = false
    private var volumeDoubleClickKeyCode = 0

    private class NavigationTouchState {
        var active = false
        var downX = 0f
        var downY = 0f
        var lastTapUpTime = 0L
        var longPressTriggered = false
        var cancelled = false
        var pendingSingle: Runnable? = null
        var longPress: Runnable? = null
    }
    private val navigationStates = WeakHashMap<View, NavigationTouchState>()

    private var floatingBallView: View? = null
    private var floatingBallParams: WindowManager.LayoutParams? = null
    private var floatingBallWindowManager: WindowManager? = null
    private var floatingBallSettingsHash = 0
    private var floatingBallX = 0
    private var floatingBallY = 0
    private var floatingBallDownX = 0f
    private var floatingBallDownY = 0f
    private var floatingBallStartX = 0
    private var floatingBallStartY = 0
    private var floatingBallDownTime = 0L
    private var floatingBallLongPressTriggered = false
    private var floatingBallMoved = false
    private var floatingBallLastTapTime = 0L
    private var floatingBallPendingClick: Runnable? = null
    private var floatingBallLongPress: Runnable? = null
    private var floatingBallAutoEdge: Runnable? = null
    private val torchStates = mutableMapOf<String, Boolean>()

    context(xposedModule: XposedModule)
    private fun currentSettings(): Preference.GlobalShortcutActions {
        val now = SystemClock.uptimeMillis()
        val cached = cachedSettings
        if (cached == null || now - cachedSettingsAt > 450L) {
            runCatching {
                PreferenceProvider.loadPreference()?.other?.globalShortcutActions
            }.getOrNull()?.let { cachedSettings = it }
            cachedSettingsAt = now
        }
        return cachedSettings ?: Preference.GlobalShortcutActions()
    }

    context(xposedModule: XposedModule)
    fun installSystemUi(param: XposedModuleInterface.PackageReadyParam) {
        // Main.onPackageReady already gates this to the primary SystemUI process.
        if (param.packageName != "com.android.systemui") return
        runCatching {
            val keyButton = param.classLoader.loadClass(
                "com.android.systemui.navigationbar.views.buttons.KeyButtonView"
            )
            val method = keyButton.getDeclaredMethod("onTouchEvent", MotionEvent::class.java)
            xposedModule.hook(method).intercept { chain ->
                val view = chain.thisObject as? View
                val event = chain.args.firstOrNull() as? MotionEvent
                if (view == null || event == null) {
                    chain.proceed()
                } else {
                    val handled = handleNavigationTouch(view, event, currentSettings())
                    if (handled == null) chain.proceed() else handled
                }
            }
            Log.i(TAG, "installed SystemUI virtual-navigation shortcut hook")
        }.onFailure {
            Log.w(TAG, "could not install virtual-navigation shortcut hook", it)
        }
    }

    context(xposedModule: XposedModule)
    fun installSystemServer(loader: ClassLoader) {
        systemServerContext = findSystemContext() ?: systemServerContext
        runCatching { hookInputManagerService(loader) }
            .onFailure { Log.w(TAG, "InputManagerService hook unavailable", it) }
        runCatching { hookGlobalPointerEvents(loader) }
            .onFailure { Log.w(TAG, "global pointer-event hook unavailable", it) }
        runCatching { hookPhoneWindowManager(loader) }
            .onFailure { Log.w(TAG, "PhoneWindowManager hook unavailable", it) }

        systemServerContext?.let { context ->
            runCatching { ensureFloatingBall(context, currentSettings()) }
                .onFailure { Log.w(TAG, "initial floating-ball setup failed", it) }
        }
        Log.i(TAG, "installed system-server global shortcut dispatch")
    }

    context(xposedModule: XposedModule)
    private fun hookInputManagerService(loader: ClassLoader) {
        val clazz = loader.loadClass("com.android.server.input.InputManagerService")
        clazz.declaredConstructors.forEach { constructor ->
            xposedModule.hook(constructor).intercept { chain ->
                val result = chain.proceed()
                inputManagerService = chain.thisObject
                result
            }
        }
        clazz.declaredMethods.filter { it.name == "start" }.forEach { method ->
            xposedModule.hook(method).intercept { chain ->
                val result = chain.proceed()
                inputManagerService = chain.thisObject
                result
            }
        }
    }

    context(xposedModule: XposedModule)
    private fun hookGlobalPointerEvents(loader: ClassLoader) {
        try {
            val dispatcher = loader.loadClass("com.android.server.wm.PointerEventDispatcher")
            val method = dispatcher.getDeclaredMethod("onInputEvent", InputEvent::class.java)
            xposedModule.hook(method).intercept { chain ->
                val event = chain.args.firstOrNull() as? MotionEvent
                if (event != null) handleGlobalMotion(event, currentSettings())
                chain.proceed()
            }
            Log.i(TAG, "hooked PointerEventDispatcher.onInputEvent")
            return
        } catch (first: Throwable) {
            Log.w(TAG, "PointerEventDispatcher.onInputEvent not available; trying pointer listener", first)
        }

        val listener = loader.loadClass("android.view.WindowManagerPolicyConstants\$PointerEventListener")
        listener.declaredMethods.filter {
            it.name == "onPointerEvent" &&
                it.parameterTypes.size == 1 &&
                MotionEvent::class.java.isAssignableFrom(it.parameterTypes[0])
        }.forEach { method ->
            xposedModule.hook(method).intercept { chain ->
                val event = chain.args.firstOrNull() as? MotionEvent
                if (event != null) {
                    val signature = "${event.eventTime}:${event.actionMasked}:${event.pointerCount}"
                    val now = SystemClock.uptimeMillis()
                    if (signature != lastPointerSignature || now - lastPointerSignatureAt > 20L) {
                        lastPointerSignature = signature
                        lastPointerSignatureAt = now
                        handleGlobalMotion(event, currentSettings())
                    }
                }
                chain.proceed()
            }
        }
    }

    context(xposedModule: XposedModule)
    private fun hookPhoneWindowManager(loader: ClassLoader) {
        val clazz = loader.loadClass("com.android.server.policy.PhoneWindowManager")
        clazz.declaredConstructors.forEach { constructor ->
            xposedModule.hook(constructor).intercept { chain ->
                val result = chain.proceed()
                phoneWindowManager = chain.thisObject
                result
            }
        }
        clazz.declaredMethods.filter { method ->
            method.name == "interceptKeyBeforeQueueing" &&
                method.parameterTypes.any { KeyEvent::class.java.isAssignableFrom(it) }
        }.forEach { method ->
            xposedModule.hook(method).intercept { chain ->
                phoneWindowManager = chain.thisObject
                val event = chain.args.firstOrNull { it is KeyEvent } as? KeyEvent
                val context = currentSystemContext()
                if (event != null && context != null &&
                    handleScreenOffVolumeKey(context, event, currentSettings())
                ) {
                    0
                } else {
                    chain.proceed()
                }
            }
        }
    }

    context(xposedModule: XposedModule)
    private fun handleGlobalMotion(event: MotionEvent, settings: Preference.GlobalShortcutActions) {
        val context = systemServerContext ?: findSystemContext()
        if (context != null) {
            systemServerContext = context
            ensureFloatingBall(context, settings)
        }

        handleStatusBarMotion(event, context, settings)
        if (context == null) return

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (event.actionMasked != MotionEvent.ACTION_MOVE) {
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) resetThreeFingerState()
                    if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                        resetThreeFingerState()
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 3) startThreeFingerGesture(event)
                else if (event.pointerCount > 3) resetThreeFingerState()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 3) resetThreeFingerState()
            }
            MotionEvent.ACTION_MOVE -> handleThreeFingerMove(context, event, settings)
        }
    }

    private fun startThreeFingerGesture(event: MotionEvent) {
        threeFingerActive = true
        threeFingerConsumed = false
        threeFingerStartSpan = averageThreeFingerSpan(event)
        for (i in 0 until 3) {
            gesturePointerIds[i] = event.getPointerId(i)
            gestureStartX[i] = event.getX(i)
            gestureStartY[i] = event.getY(i)
        }
    }

    private fun handleThreeFingerMove(
        context: Context,
        event: MotionEvent,
        settings: Preference.GlobalShortcutActions,
    ) {
        if (!threeFingerActive || threeFingerConsumed || event.pointerCount != 3 ||
            (event.source and InputDevice.SOURCE_TOUCHSCREEN) != InputDevice.SOURCE_TOUCHSCREEN
        ) return

        val density = context.resources.displayMetrics.density.takeIf { it > 0 } ?: 1f
        val span = averageThreeFingerSpan(event)
        val spanChangeDp = (span - threeFingerStartSpan) / density

        if (settings.threeFingerPinchEnabled &&
            spanChangeDp <= -settings.threeFingerPinchThresholdDp.toFloat() &&
            settings.threeFingerPinchAction != ACTION_NONE
        ) {
            consumeThreeFingerGesture(context, settings.threeFingerPinchAction,
                settings.threeFingerPinchHapticEnabled, settings.threeFingerPinchHapticStrength)
            return
        }
        if (settings.threeFingerSpreadEnabled &&
            spanChangeDp >= settings.threeFingerSpreadThresholdDp.toFloat() &&
            settings.threeFingerSpreadAction != ACTION_NONE
        ) {
            consumeThreeFingerGesture(context, settings.threeFingerSpreadAction,
                settings.threeFingerSpreadHapticEnabled, settings.threeFingerSpreadHapticStrength)
            return
        }

        if (!settings.threeFingerGestureEnabled) return
        val threshold = settings.threeFingerTouchCancelThresholdDp * density
        var direction: String? = null
        for (i in 0 until 3) {
            val index = event.findPointerIndex(gesturePointerIds[i])
            if (index < 0) return
            val dx = event.getX(index) - gestureStartX[i]
            val dy = event.getY(index) - gestureStartY[i]
            if (max(abs(dx), abs(dy)) < threshold) return
            val fingerDirection = if (abs(dy) > abs(dx)) {
                if (dy < 0f) "up" else "down"
            } else {
                if (dx < 0f) "left" else "right"
            }
            if (direction == null) direction = fingerDirection
            else if (direction != fingerDirection) return
        }

        val action = when (direction) {
            "up" -> settings.threeFingerSwipeUpAction
            "down" -> settings.threeFingerSwipeDownAction
            "left" -> settings.threeFingerSwipeLeftAction
            "right" -> settings.threeFingerSwipeRightAction
            else -> ACTION_NONE
        }
        if (action != ACTION_NONE) {
            consumeThreeFingerGesture(context, action, settings.threeFingerHapticEnabled, settings.threeFingerHapticStrength)
        }
    }

    private fun consumeThreeFingerGesture(
        context: Context,
        action: String,
        haptic: Boolean,
        strength: Int,
    ) {
        threeFingerConsumed = true
        runCatching { callCompatible(inputManagerService, "cancelCurrentTouch") }
        performHaptic(context, haptic, strength)
        performGlobalAction(context, action)
    }

    private fun averageThreeFingerSpan(event: MotionEvent): Float {
        if (event.pointerCount < 3) return 0f
        var total = 0f
        var pairs = 0
        for (i in 0..2) for (j in i + 1..2) {
            total += hypot(event.getX(i) - event.getX(j), event.getY(i) - event.getY(j))
            pairs++
        }
        return if (pairs == 0) 0f else total / pairs
    }

    private fun resetThreeFingerState() {
        threeFingerActive = false
        threeFingerConsumed = false
        threeFingerStartSpan = 0f
        for (i in 0 until 3) {
            gesturePointerIds[i] = -1
            gestureStartX[i] = 0f
            gestureStartY[i] = 0f
        }
    }

    context(xposedModule: XposedModule)
    private fun handleStatusBarMotion(
        event: MotionEvent,
        context: Context?,
        settings: Preference.GlobalShortcutActions,
    ) {
        if (!settings.statusBarShortcutEnabled || context == null) {
            if (event.actionMasked == MotionEvent.ACTION_CANCEL) resetStatusBarState()
            return
        }
        val anyConfigured = settings.statusBarDoubleClickAction != ACTION_NONE ||
            settings.statusBarLeftClickAction != ACTION_NONE ||
            settings.statusBarLeftLongPressAction != ACTION_NONE ||
            settings.statusBarRightClickAction != ACTION_NONE ||
            settings.statusBarRightLongPressAction != ACTION_NONE
        if (!anyConfigured) return

        val barHeight = statusBarHeightPx(context)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.rawY > barHeight) {
                    statusGestureActive = false
                    return
                }
                statusGestureActive = true
                statusGestureMoved = false
                statusLongPressTriggered = false
                statusDownX = event.rawX
                statusDownY = event.rawY
                statusDownTime = event.eventTime
                val width = context.resources.displayMetrics.widthPixels.toFloat()
                statusSide = when {
                    statusDownX < width * 0.33f -> -1
                    statusDownX > width * 0.67f -> 1
                    else -> 0
                }
                val longAction = if (statusSide < 0) settings.statusBarLeftLongPressAction
                    else settings.statusBarRightLongPressAction
                statusLongPressRunnable?.let { mainHandler.removeCallbacks(it) }
                statusLongPressRunnable = if (longAction != ACTION_NONE && statusSide != 0) {
                    Runnable {
                        if (statusGestureActive && !statusGestureMoved) {
                            statusLongPressTriggered = true
                            callCompatible(inputManagerService, "cancelCurrentTouch")
                            performHaptic(context, settings.statusBarShortcutHapticEnabled, settings.statusBarShortcutHapticStrength)
                            performGlobalAction(context, longAction)
                        }
                    }.also { mainHandler.postDelayed(it, DEFAULT_LONG_PRESS_MS) }
                } else null
            }

            MotionEvent.ACTION_MOVE -> {
                if (!statusGestureActive) return
                if (max(abs(event.rawX - statusDownX), abs(event.rawY - statusDownY)) >
                    ViewConfiguration.get(context).scaledTouchSlop
                ) {
                    statusGestureMoved = true
                    statusLongPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    statusLongPressRunnable = null
                }
            }

            MotionEvent.ACTION_UP -> {
                if (!statusGestureActive) return
                statusLongPressRunnable?.let { mainHandler.removeCallbacks(it) }
                statusLongPressRunnable = null
                if (!statusGestureMoved && !statusLongPressTriggered &&
                    event.eventTime - statusDownTime <= 800L
                ) {
                    val clickAction = when (statusSide) {
                        -1 -> settings.statusBarLeftClickAction
                        1 -> settings.statusBarRightClickAction
                        else -> ACTION_NONE
                    }
                    val secondTap = statusLastTapTime > 0L &&
                        event.eventTime - statusLastTapTime <= STATUS_BAR_DOUBLE_CLICK_MS
                    if (secondTap && settings.statusBarDoubleClickAction != ACTION_NONE) {
                        statusPendingSingle?.let { mainHandler.removeCallbacks(it) }
                        statusPendingSingle = null
                        statusLastTapTime = 0L
                        callCompatible(inputManagerService, "cancelCurrentTouch")
                        performHaptic(context, settings.statusBarShortcutHapticEnabled, settings.statusBarShortcutHapticStrength)
                        performGlobalAction(context, settings.statusBarDoubleClickAction)
                    } else {
                        statusLastTapTime = event.eventTime
                        if (clickAction != ACTION_NONE) {
                            statusPendingSingle?.let { mainHandler.removeCallbacks(it) }
                            val pending = Runnable {
                                performHaptic(context, settings.statusBarShortcutHapticEnabled, settings.statusBarShortcutHapticStrength)
                                performGlobalAction(context, clickAction)
                                statusPendingSingle = null
                            }
                            statusPendingSingle = pending
                            if (settings.statusBarDoubleClickAction != ACTION_NONE) {
                                mainHandler.postDelayed(pending, STATUS_BAR_DOUBLE_CLICK_MS)
                            } else {
                                mainHandler.post(pending)
                            }
                        }
                    }
                }
                statusGestureActive = false
            }

            MotionEvent.ACTION_CANCEL -> resetStatusBarState()
        }
    }

    private fun resetStatusBarState() {
        statusGestureActive = false
        statusGestureMoved = false
        statusLongPressTriggered = false
        statusLongPressRunnable?.let { mainHandler.removeCallbacks(it) }
        statusLongPressRunnable = null
    }

    private fun statusBarHeightPx(context: Context): Int {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id != 0) runCatching { context.resources.getDimensionPixelSize(id) }.getOrDefault(dp(context, 24f))
            else dp(context, 24f)
    }

    private fun handleNavigationTouch(
        view: View,
        event: MotionEvent,
        settings: Preference.GlobalShortcutActions,
    ): Boolean? {
        if (!settings.navigationKeyShortcutEnabled) return null
        val role = navigationRole(view) ?: return null
        val doubleAction = navigationDoubleAction(settings, role)
        val longAction = navigationLongAction(settings, role)
        if (doubleAction == ACTION_NONE && longAction == ACTION_NONE) return null

        val state = synchronized(navigationStates) {
            navigationStates.getOrPut(view) { NavigationTouchState() }
        }
        val context = view.context
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                state.active = true
                state.downX = event.rawX
                state.downY = event.rawY
                state.longPressTriggered = false
                state.cancelled = false
                state.longPress?.let { mainHandler.removeCallbacks(it) }
                state.longPress = if (longAction != ACTION_NONE) {
                    Runnable {
                        if (state.active && !state.cancelled) {
                            state.longPressTriggered = true
                            state.pendingSingle?.let { mainHandler.removeCallbacks(it) }
                            state.pendingSingle = null
                            performHaptic(context, settings.navigationKeyHapticEnabled, settings.navigationKeyHapticStrength)
                            performGlobalAction(context, longAction)
                        }
                    }.also { mainHandler.postDelayed(it, DEFAULT_LONG_PRESS_MS) }
                } else null
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (state.active && max(abs(event.rawX - state.downX), abs(event.rawY - state.downY)) >
                    ViewConfiguration.get(context).scaledTouchSlop
                ) {
                    state.cancelled = true
                    state.longPress?.let { mainHandler.removeCallbacks(it) }
                    state.longPress = null
                }
                return if (state.active) true else null
            }

            MotionEvent.ACTION_UP -> {
                if (!state.active) return null
                state.active = false
                state.longPress?.let { mainHandler.removeCallbacks(it) }
                state.longPress = null
                if (state.longPressTriggered || state.cancelled) return true

                val normalKey = navigationDefaultKey(role)
                if (doubleAction != ACTION_NONE) {
                    val isDouble = state.lastTapUpTime > 0L &&
                        event.eventTime - state.lastTapUpTime <= ViewConfiguration.getDoubleTapTimeout()
                    if (isDouble) {
                        state.pendingSingle?.let { mainHandler.removeCallbacks(it) }
                        state.pendingSingle = null
                        state.lastTapUpTime = 0L
                        performHaptic(context, settings.navigationKeyHapticEnabled, settings.navigationKeyHapticStrength)
                        performGlobalAction(context, doubleAction)
                    } else {
                        state.lastTapUpTime = event.eventTime
                        state.pendingSingle?.let { mainHandler.removeCallbacks(it) }
                        val pending = Runnable {
                            injectKey(normalKey)
                            state.pendingSingle = null
                        }
                        state.pendingSingle = pending
                        mainHandler.postDelayed(pending, ViewConfiguration.getDoubleTapTimeout().toLong())
                    }
                } else {
                    injectKey(normalKey)
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                state.active = false
                state.cancelled = true
                state.longPress?.let { mainHandler.removeCallbacks(it) }
                state.longPress = null
                return true
            }
        }
        return if (state.active) true else null
    }

    private fun navigationRole(view: View): String? {
        val idName = runCatching { view.resources.getResourceEntryName(view.id) }.getOrDefault("")
            .lowercase()
        val hint = (idName + " " + view.javaClass.simpleName).lowercase()
        return when {
            hint.contains("back") -> "back"
            hint.contains("home") -> "home"
            hint.contains("recent") || hint.contains("recents") || hint.contains("task") -> "recents"
            else -> null
        }
    }

    private fun navigationDoubleAction(settings: Preference.GlobalShortcutActions, role: String): String =
        when (role) {
            "back" -> settings.navigationBackDoubleTapAction
            "home" -> settings.navigationHomeDoubleTapAction
            else -> settings.navigationRecentsDoubleTapAction
        }.ifBlank { ACTION_NONE }

    private fun navigationLongAction(settings: Preference.GlobalShortcutActions, role: String): String =
        when (role) {
            "back" -> settings.navigationBackLongPressAction
            "home" -> settings.navigationHomeLongPressAction
            else -> settings.navigationRecentsLongPressAction
        }.ifBlank { ACTION_NONE }

    private fun navigationDefaultKey(role: String): Int = when (role) {
        "back" -> KeyEvent.KEYCODE_BACK
        "home" -> KeyEvent.KEYCODE_HOME
        else -> KeyEvent.KEYCODE_APP_SWITCH
    }

    private fun handleScreenOffVolumeKey(
        context: Context,
        event: KeyEvent,
        settings: Preference.GlobalShortcutActions,
    ): Boolean {
        val code = event.keyCode
        if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        val activeSequence = volumeDownKeyCode == code || volumeLongPressTriggered
        if (!settings.screenOffVolumeKeyEnabled && !activeSequence) return false
        val interactive = runCatching {
            context.getSystemService(PowerManager::class.java)?.isInteractive ?: true
        }.getOrDefault(true)
        if (interactive && !activeSequence) return false
        if (!settings.screenOffVolumeLongPressEnabled && !settings.screenOffVolumeDoubleClickEnabled) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount > 0) return activeSequence
                volumeLongPressRunnable?.let { mainHandler.removeCallbacks(it) }
                volumeDownKeyCode = code
                volumeLongPressTriggered = false
                if (settings.screenOffVolumeLongPressEnabled) {
                    volumeLongPressRunnable = Runnable {
                        if (volumeDownKeyCode == code) {
                            volumeLongPressTriggered = true
                            volumeDownKeyCode = 0
                            cancelVolumeDoubleClick()
                            performHaptic(context, settings.screenOffVolumeHapticEnabled, settings.screenOffVolumeHapticStrength)
                            injectKey(if (code == KeyEvent.KEYCODE_VOLUME_UP) {
                                KeyEvent.KEYCODE_MEDIA_NEXT
                            } else {
                                KeyEvent.KEYCODE_MEDIA_PREVIOUS
                            })
                        }
                    }.also { mainHandler.postDelayed(it, settings.screenOffVolumeLongPressTimeoutMs.toLong()) }
                }
                return true
            }

            KeyEvent.ACTION_UP -> {
                volumeLongPressRunnable?.let { mainHandler.removeCallbacks(it) }
                volumeLongPressRunnable = null
                if (volumeLongPressTriggered) {
                    volumeLongPressTriggered = false
                    volumeDownKeyCode = 0
                    return true
                }
                volumeDownKeyCode = 0
                if (settings.screenOffVolumeDoubleClickEnabled) {
                    if (volumeDoubleClickPending && volumeDoubleClickKeyCode == code) {
                        cancelVolumeDoubleClick()
                        performHaptic(context, settings.screenOffVolumeHapticEnabled, settings.screenOffVolumeHapticStrength)
                        injectKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    } else {
                        cancelVolumeDoubleClick()
                        volumeDoubleClickPending = true
                        volumeDoubleClickKeyCode = code
                        volumeDoubleClickRunnable = Runnable {
                            if (volumeDoubleClickPending && volumeDoubleClickKeyCode == code) {
                                volumeDoubleClickPending = false
                                volumeDoubleClickKeyCode = 0
                                adjustMusicVolume(context, code)
                            }
                            volumeDoubleClickRunnable = null
                        }.also { mainHandler.postDelayed(it, settings.screenOffVolumeDoubleClickTimeoutMs.toLong()) }
                    }
                } else {
                    adjustMusicVolume(context, code)
                }
                return true
            }

            KeyEvent.ACTION_MULTIPLE -> return true
            else -> return activeSequence
        }
    }

    private fun cancelVolumeDoubleClick() {
        volumeDoubleClickRunnable?.let { mainHandler.removeCallbacks(it) }
        volumeDoubleClickRunnable = null
        volumeDoubleClickPending = false
        volumeDoubleClickKeyCode = 0
    }

    private fun adjustMusicVolume(context: Context, keyCode: Int) {
        runCatching {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                0,
            )
        }.onFailure { Log.w(TAG, "volume fallback failed", it) }
    }

    private fun ensureFloatingBall(context: Context, settings: Preference.GlobalShortcutActions) {
        if (!settings.floatingBallEnabled) {
            removeFloatingBall()
            return
        }
        if (floatingBallView != null && floatingBallSettingsHash == settings.hashCode()) return
        removeFloatingBall()

        val manager = context.getSystemService(WindowManager::class.java) ?: return
        val size = dp(context, settings.floatingBallSizeDp.coerceIn(40, 88).toFloat())
        val screenWidth = context.resources.displayMetrics.widthPixels
        val initialX = if (floatingBallX > 0) floatingBallX else screenWidth - size - dp(context, 18f)
        val initialY = if (floatingBallY > 0) floatingBallY else statusBarHeightPx(context) + dp(context, 160f)
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_SECURE
        val params = WindowManager.LayoutParams(
            size,
            size,
            2019, // TYPE_NAVIGATION_BAR_PANEL: available to system_server without app overlay permission.
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            x = initialX.coerceAtLeast(0)
            y = initialY.coerceAtLeast(0)
            title = "Aloha OneUI Global Shortcut"
        }
        val view = object : View(context) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val radius = min(width, height) / 2f - dp(context, 1f)
                val color = settings.floatingBallColorArgb
                paint.style = Paint.Style.FILL
                paint.color = android.graphics.Color.argb(
                    (android.graphics.Color.alpha(color) * 0.42f).roundToInt().coerceIn(24, 190),
                    android.graphics.Color.red(color),
                    android.graphics.Color.green(color),
                    android.graphics.Color.blue(color),
                )
                canvas.drawCircle(width / 2f, height / 2f, radius, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(context, 2f).toFloat()
                paint.color = android.graphics.Color.argb(190, 255, 255, 255)
                canvas.drawCircle(width / 2f, height / 2f, radius, paint)
                paint.style = Paint.Style.FILL
                paint.color = color
                canvas.drawCircle(width / 2f, height / 2f, radius * 0.72f, paint)
            }
        }
        view.setOnTouchListener { touchedView, event ->
            handleFloatingBallTouch(context, manager, touchedView, params, event, settings)
            true
        }

        runCatching {
            manager.addView(view, params)
            floatingBallView = view
            floatingBallParams = params
            floatingBallWindowManager = manager
            floatingBallSettingsHash = settings.hashCode()
            floatingBallX = params.x
            floatingBallY = params.y
            Log.i(TAG, "floating ball added")
        }.onFailure {
            // A Samsung build may not allow TYPE_NAVIGATION_BAR_PANEL for the SystemServer context.
            runCatching {
                params.type = 2038 // TYPE_APPLICATION_OVERLAY fallback.
                manager.addView(view, params)
                floatingBallView = view
                floatingBallParams = params
                floatingBallWindowManager = manager
                floatingBallSettingsHash = settings.hashCode()
                floatingBallX = params.x
                floatingBallY = params.y
            }.onFailure { fallback ->
                Log.w(TAG, "floating ball could not be attached", fallback)
                floatingBallView = null
                floatingBallParams = null
                floatingBallWindowManager = null
                floatingBallSettingsHash = 0
            }
        }
    }

    private fun removeFloatingBall() {
        floatingBallAutoEdge?.let { mainHandler.removeCallbacks(it) }
        floatingBallAutoEdge = null
        floatingBallPendingClick?.let { mainHandler.removeCallbacks(it) }
        floatingBallPendingClick = null
        floatingBallLongPress?.let { mainHandler.removeCallbacks(it) }
        floatingBallLongPress = null
        val view = floatingBallView
        if (view != null) {
            runCatching { floatingBallWindowManager?.removeView(view) }
        }
        floatingBallView = null
        floatingBallParams = null
        floatingBallWindowManager = null
        floatingBallSettingsHash = 0
    }

    private fun handleFloatingBallTouch(
        context: Context,
        manager: WindowManager,
        view: View,
        params: WindowManager.LayoutParams,
        event: MotionEvent,
        settings: Preference.GlobalShortcutActions,
    ) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                floatingBallAutoEdge?.let { mainHandler.removeCallbacks(it) }
                floatingBallAutoEdge = null
                floatingBallDownX = event.rawX
                floatingBallDownY = event.rawY
                floatingBallStartX = params.x
                floatingBallStartY = params.y
                floatingBallDownTime = event.eventTime
                floatingBallLongPressTriggered = false
                floatingBallMoved = false
                floatingBallLongPress?.let { mainHandler.removeCallbacks(it) }
                floatingBallLongPress = Runnable {
                    floatingBallLongPressTriggered = true
                }.also { mainHandler.postDelayed(it, FLOATING_BALL_LONG_PRESS_MS) }
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - floatingBallDownX
                val dy = event.rawY - floatingBallDownY
                if (max(abs(dx), abs(dy)) > dp(context, FLOATING_BALL_TAP_SLOP_DP)) {
                    floatingBallMoved = true
                }
                if (floatingBallLongPressTriggered) {
                    params.x = (floatingBallStartX + dx).roundToInt().coerceIn(0, context.resources.displayMetrics.widthPixels - view.width)
                    params.y = (floatingBallStartY + dy).roundToInt().coerceAtLeast(0)
                    floatingBallX = params.x
                    floatingBallY = params.y
                    runCatching { manager.updateViewLayout(view, params) }
                }
            }

            MotionEvent.ACTION_UP -> {
                floatingBallLongPress?.let { mainHandler.removeCallbacks(it) }
                floatingBallLongPress = null
                if (floatingBallLongPressTriggered) {
                    floatingBallLongPressTriggered = false
                    scheduleFloatingBallAutoEdge(context, manager, view, params, settings)
                    return
                }
                val dx = event.rawX - floatingBallDownX
                val dy = event.rawY - floatingBallDownY
                val threshold = dp(context, FLOATING_BALL_SWIPE_THRESHOLD_DP)
                val action = if (max(abs(dx), abs(dy)) >= threshold) {
                    when {
                        abs(dy) >= abs(dx) && dy < 0 -> settings.floatingBallSwipeUpAction
                        abs(dy) >= abs(dx) -> settings.floatingBallSwipeDownAction
                        dx < 0 -> settings.floatingBallSwipeLeftAction
                        else -> settings.floatingBallSwipeRightAction
                    }
                } else if (event.eventTime - floatingBallLastTapTime <= DOUBLE_TAP_MS &&
                    floatingBallLastTapTime > 0L && settings.floatingBallDoubleClickAction != ACTION_NONE
                ) {
                    floatingBallPendingClick?.let { mainHandler.removeCallbacks(it) }
                    floatingBallPendingClick = null
                    floatingBallLastTapTime = 0L
                    settings.floatingBallDoubleClickAction
                } else {
                    floatingBallLastTapTime = event.eventTime
                    val single = settings.floatingBallClickAction
                    if (single != ACTION_NONE) {
                        floatingBallPendingClick?.let { mainHandler.removeCallbacks(it) }
                        val pending = Runnable {
                            performHaptic(context, settings.floatingBallHapticEnabled, settings.floatingBallHapticStrength)
                            performGlobalAction(context, single)
                            floatingBallPendingClick = null
                        }
                        floatingBallPendingClick = pending
                        if (settings.floatingBallDoubleClickAction != ACTION_NONE) {
                            mainHandler.postDelayed(pending, DOUBLE_TAP_MS)
                        } else {
                            mainHandler.post(pending)
                        }
                    }
                    ACTION_NONE
                }
                if (action != ACTION_NONE) {
                    performHaptic(context, settings.floatingBallHapticEnabled, settings.floatingBallHapticStrength)
                    performGlobalAction(context, action)
                }
                scheduleFloatingBallAutoEdge(context, manager, view, params, settings)
            }

            MotionEvent.ACTION_CANCEL -> {
                floatingBallLongPress?.let { mainHandler.removeCallbacks(it) }
                floatingBallLongPress = null
                floatingBallLongPressTriggered = false
            }
        }
    }

    private fun scheduleFloatingBallAutoEdge(
        context: Context,
        manager: WindowManager,
        view: View,
        params: WindowManager.LayoutParams,
        settings: Preference.GlobalShortcutActions,
    ) {
        floatingBallAutoEdge?.let { mainHandler.removeCallbacks(it) }
        floatingBallAutoEdge = null
        if (settings.floatingBallAutoEdgeDelaySeconds <= 0) return
        val runnable = Runnable {
            val displayWidth = context.resources.displayMetrics.widthPixels
            params.x = if (params.x + view.width / 2 < displayWidth / 2) 0
                else (displayWidth - view.width).coerceAtLeast(0)
            floatingBallX = params.x
            runCatching { manager.updateViewLayout(view, params) }
            floatingBallAutoEdge = null
        }
        floatingBallAutoEdge = runnable
        mainHandler.postDelayed(runnable, settings.floatingBallAutoEdgeDelaySeconds.coerceIn(1, 10) * 1000L)
    }

    private fun performHaptic(context: Context, enabled: Boolean, strength: Int) {
        if (!enabled) return
        runCatching {
            val vibrator = context.getSystemService(Vibrator::class.java) ?: return
            val amplitude = (strength.coerceIn(1, 100) * 255 / 100).coerceIn(1, 255)
            vibrator.vibrate(VibrationEffect.createOneShot(18L, amplitude))
        }.onFailure {
            runCatching {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(18L)
            }
        }
    }

    private fun performGlobalAction(context: Context, action: String) {
        if (action.isBlank() || action == ACTION_NONE) return
        runCatching {
            if (action.startsWith(ACTION_CUSTOM_APP_PREFIX)) {
                val component = ComponentName.unflattenFromString(action.removePrefix(ACTION_CUSTOM_APP_PREFIX)) ?: return
                context.startActivity(Intent(Intent.ACTION_MAIN).setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            }
            when (action) {
                "back" -> injectKey(KeyEvent.KEYCODE_BACK)
                "home" -> injectKey(KeyEvent.KEYCODE_HOME)
                "recents" -> injectKey(KeyEvent.KEYCODE_APP_SWITCH)
                "screenshot" -> {
                    if (!invokePolicy("takeScreenshot")) injectKey(KeyEvent.KEYCODE_SYSRQ)
                }
                "smart_screenshot" -> {
                    if (!startComponent(context, "com.samsung.android.smartcapture", "com.samsung.android.smartcapture.SmartCaptureActivity")) {
                        if (!invokePolicy("takeScreenshot")) injectKey(KeyEvent.KEYCODE_SYSRQ)
                    }
                }
                "lock_screen" -> runCatching {
                    val power = context.getSystemService(PowerManager::class.java)
                    callCompatible(power, "goToSleep", SystemClock.uptimeMillis())
                }
                "power_menu" -> invokePolicy("showGlobalActions")
                "notifications" -> runCatching {
                    callCompatible(context.getSystemService("statusbar"), "expandNotificationsPanel")
                }
                "previous_track" -> injectKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                "next_track" -> injectKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                "play_pause" -> injectKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                "media_rewind" -> injectKey(KeyEvent.KEYCODE_MEDIA_REWIND)
                "media_fast_forward" -> injectKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
                "flashlight" -> toggleFlashlight(context)
                "kill_foreground_app" -> cleanForegroundApp(context)
                "memory_clean" -> cleanBackgroundApps(context)
                "screen_record" -> {
                    if (!startComponent(context, "com.android.systemui", "com.android.systemui.screenrecord.ScreenRecordPermissionActivity")) {
                        context.sendBroadcast(Intent("com.android.systemui.screenrecord.START").setPackage("com.android.systemui"))
                    }
                }
                "take_photo" -> launchCamera(context, video = false)
                "quick_video" -> launchCamera(context, video = true)
                "airplane_mode" -> toggleAirplaneMode(context)
                "bluetooth" -> toggleBluetooth()
                "silent_mode" -> setRingerMode(context, AudioManager.RINGER_MODE_SILENT)
                "vibrate_mode" -> setRingerMode(context, AudioManager.RINGER_MODE_VIBRATE)
                "wifi" -> toggleWifi(context)
                "mobile_data" -> toggleMobileData(context)
                "wechat_scan" -> startActionUri(context, "weixin://dl/scan", "com.tencent.mm")
                "wechat_pay" -> startActionUri(context, "weixin://dl/moments", "com.tencent.mm")
                "wechat_collect" -> startActionUri(context, "weixin://dl/scan", "com.tencent.mm")
                "alipay_scan" -> startActionUri(context, "alipays://platformapi/startapp?saId=10000007", "com.eg.android.AlipayGphone")
                "alipay_pay" -> startActionUri(context, "alipays://platformapi/startapp?saId=20000056", "com.eg.android.AlipayGphone")
                "alipay_collect" -> startActionUri(context, "alipays://platformapi/startapp?saId=20000123", "com.eg.android.AlipayGphone")
            }
        }.onFailure { Log.w(TAG, "shortcut action failed: $action", it) }
    }

    private fun injectKey(keyCode: Int) {
        runCatching {
            val now = SystemClock.uptimeMillis()
            val down = KeyEvent(
                now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD,
            )
            val up = KeyEvent.changeAction(down, KeyEvent.ACTION_UP)
            val manager = inputManagerService
            if (manager != null && callCompatible(manager, "injectInputEvent", down, 0) != null) {
                callCompatible(manager, "injectInputEvent", up, 0)
                return
            }
            val clazz = Class.forName("android.hardware.input.InputManager")
            val instance = callCompatible(clazz, "getInstance")
            if (instance != null) {
                callCompatible(instance, "injectInputEvent", down, 0)
                callCompatible(instance, "injectInputEvent", up, 0)
            }
        }.onFailure { Log.w(TAG, "key injection failed: $keyCode", it) }
    }

    private fun invokePolicy(method: String): Boolean {
        val policy = phoneWindowManager ?: return false
        return runCatching {
            callCompatible(policy, method)
            true
        }.getOrDefault(false)
    }

    private fun startComponent(context: Context, packageName: String, className: String): Boolean {
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_MAIN).setComponent(ComponentName(packageName, className))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }

    private fun startActionUri(context: Context, uri: String, packageName: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }.onFailure { Log.w(TAG, "could not launch shortcut URI $uri", it) }
    }

    private fun launchCamera(context: Context, video: Boolean) {
        runCatching {
            val intent = if (video) Intent(MediaStore.ACTION_VIDEO_CAPTURE) else Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching {
                context.packageManager.getLaunchIntentForPackage("com.sec.android.app.camera")
            }.getOrNull()?.let { launch ->
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return
            }
            context.startActivity(intent)
        }.onFailure { Log.w(TAG, "camera shortcut launch failed", it) }
    }

    private fun toggleFlashlight(context: Context) {
        val camera = context.getSystemService(CameraManager::class.java) ?: return
        val cameraId = camera.cameraIdList.firstOrNull { id ->
            runCatching { camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }.getOrDefault(false)
        } ?: return
        val next = !(torchStates[cameraId] ?: false)
        camera.setTorchMode(cameraId, next)
        torchStates[cameraId] = next
    }

    private fun toggleAirplaneMode(context: Context) {
        val resolver = context.contentResolver
        val enabled = Settings.Global.getInt(resolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        val next = if (enabled) 0 else 1
        Settings.Global.putInt(resolver, Settings.Global.AIRPLANE_MODE_ON, next)
        context.sendBroadcast(Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).putExtra("state", next == 1))
    }

    @Suppress("DEPRECATION")
    private fun toggleBluetooth() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        if (adapter.isEnabled) adapter.disable() else adapter.enable()
    }

    @Suppress("DEPRECATION")
    private fun toggleWifi(context: Context) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        wifi.isWifiEnabled.let { wifi.setWifiEnabled(!it) }
    }

    private fun toggleMobileData(context: Context) {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        val enabled = runCatching { callCompatible(telephony, "isDataEnabled") as? Boolean }.getOrNull() ?: false
        callCompatible(telephony, "setDataEnabled", !enabled)
    }

    private fun setRingerMode(context: Context, mode: Int) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audio.ringerMode = if (audio.ringerMode == mode) AudioManager.RINGER_MODE_NORMAL else mode
    }

    private fun cleanForegroundApp(context: Context) {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return
        val topPackage = runCatching { manager.getRunningTasks(1).firstOrNull()?.topActivity?.packageName }.getOrNull()
            ?: return
        if (topPackage == "android" || topPackage == "com.android.systemui" || topPackage == context.packageName) return
        runCatching { manager.killBackgroundProcesses(topPackage) }
        runCatching { callCompatible(manager, "forceStopPackage", topPackage) }
    }

    private fun cleanBackgroundApps(context: Context) {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return
        runCatching {
            manager.runningAppProcesses.orEmpty()
                .filter { it.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED }
                .mapNotNull { it.processName.substringBefore(':').takeIf(String::isNotBlank) }
                .distinct()
                .filterNot { it == "android" || it == "com.android.systemui" || it == context.packageName }
                .forEach { packageName -> runCatching { manager.killBackgroundProcesses(packageName) } }
        }.onFailure { Log.w(TAG, "memory clean failed", it) }
    }

    private fun currentSystemContext(): Context? =
        systemServerContext ?: findSystemContext()?.also { systemServerContext = it }

    private fun findSystemContext(): Context? = runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        val thread = activityThread.getDeclaredMethod("currentActivityThread").apply { isAccessible = true }.invoke(null)
        val value = readField(thread, "mSystemContext")
        value as? Context
    }.getOrNull()

    private fun dp(context: Context, value: Float): Int =
        (value * (context.resources.displayMetrics.density.takeIf { it > 0f } ?: 1f)).roundToInt()

    private fun readField(instance: Any?, name: String): Any? {
        if (instance == null) return null
        var type: Class<*>? = instance.javaClass
        while (type != null) {
            val currentType = type
            runCatching {
                val field = currentType.getDeclaredField(name)
                field.isAccessible = true
                return field.get(instance)
            }
            type = currentType.superclass
        }
        return null
    }

    private fun callCompatible(target: Any?, name: String, vararg args: Any?): Any? {
        if (target == null) return null
        val clazz = if (target is Class<*>) target else target.javaClass
        var type: Class<*>? = clazz
        while (type != null) {
            val currentType = type
            val methods = runCatching { currentType.declaredMethods.toList() }.getOrDefault(emptyList())
            val candidate = methods.firstOrNull { method ->
                method.name == name &&
                    method.parameterTypes.size == args.size &&
                    method.parameterTypes.indices.all { index ->
                        val argument = args[index]
                        argument == null && !method.parameterTypes[index].isPrimitive ||
                            argument != null && (
                                method.parameterTypes[index].isInstance(argument) ||
                                    method.parameterTypes[index].isPrimitive && primitiveBox(method.parameterTypes[index]).isInstance(argument)
                                )
                    }
            }
            if (candidate != null) {
                return runCatching {
                    candidate.isAccessible = true
                    candidate.invoke(if (target is Class<*>) null else target, *args)
                }.getOrNull()
            }
            type = currentType.superclass
        }
        return null
    }

    private fun primitiveBox(type: Class<*>): Class<*> = when (type) {
        Boolean::class.javaPrimitiveType -> Boolean::class.javaObjectType
        Int::class.javaPrimitiveType -> Int::class.javaObjectType
        Long::class.javaPrimitiveType -> Long::class.javaObjectType
        Float::class.javaPrimitiveType -> Float::class.javaObjectType
        Double::class.javaPrimitiveType -> Double::class.javaObjectType
        Short::class.javaPrimitiveType -> Short::class.javaObjectType
        Byte::class.javaPrimitiveType -> Byte::class.javaObjectType
        Char::class.javaPrimitiveType -> Char::class.javaObjectType
        else -> type
    }
}
