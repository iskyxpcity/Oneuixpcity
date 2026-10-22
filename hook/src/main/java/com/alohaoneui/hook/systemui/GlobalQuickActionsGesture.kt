package com.alohaoneui.hook.systemui

import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.util.Locale
import kotlin.math.abs

internal fun GlobalQuickActions.handleGlobalMotion(event: MotionEvent) {
    handleThreeFingerPinchMotion(event)
    handleThreeFingerSpreadMotion(event)
    handleThreeFingerMotion(event)
    handleStatusBarShortcutMotion(event)
    val context = systemServerContext ?: currentApplicationContext() ?: return
    if (SystemClock.uptimeMillis() - lastFloatingBallEnsureTime > SETTINGS_CACHE_MS) {
        lastFloatingBallEnsureTime = SystemClock.uptimeMillis()
        postOnMain { ensureFloatingBall(context) }
    }
}

internal fun GlobalQuickActions.handleThreeFingerMotion(event: MotionEvent) {
    if ((event.source and InputDevice.SOURCE_TOUCHSCREEN) != InputDevice.SOURCE_TOUCHSCREEN) return
    if (!threeFingerSettings.enabled) return
    val pointerCount = event.pointerCount
    val actionMasked = event.actionMasked
    when (actionMasked) {
        MotionEvent.ACTION_DOWN,
        MotionEvent.ACTION_CANCEL,
        MotionEvent.ACTION_UP -> {
            resetThreeFingerState()
            return
        }
        MotionEvent.ACTION_POINTER_DOWN -> {
            if (pointerCount == THREE_FINGER_COUNT) {
                threeFingerActive = true
                threeFingerConsumed = false
                threeFingerTouchCancelled = false
                threeFingerStartTime = event.eventTime
                recordThreeFingerStart(event)
            } else if (pointerCount > THREE_FINGER_COUNT) {
                resetThreeFingerState()
            }
            return
        }
        MotionEvent.ACTION_POINTER_UP -> {
            if (pointerCount <= THREE_FINGER_COUNT) {
                resetThreeFingerState()
            }
            return
        }
        MotionEvent.ACTION_MOVE -> Unit
        else -> return
    }
    if (!threeFingerActive || threeFingerConsumed || pointerCount != THREE_FINGER_COUNT) return

    val context = currentApplicationContext() ?: return
    val threshold = dp(context, threeFingerSettings.touchCancelThresholdDp).toFloat()
    val direction = consistentThreeFingerDirection(event, threshold) ?: return
    val action = threeFingerSettings.actionFor(direction)
    if (action == ACTION_NONE) return

    threeFingerTouchCancelled = true
    cancelCurrentTouchGesture()
    threeFingerConsumed = true
    performHapticFeedback(context, threeFingerSettings.hapticEnabled, threeFingerSettings.hapticStrength)
    performGlobalGestureAction(context, action)
}

internal fun GlobalQuickActions.recordThreeFingerStart(event: MotionEvent) {
    for (index in 0 until THREE_FINGER_COUNT) {
        threeFingerPointerIds[index] = event.getPointerId(index)
        threeFingerStartXs[index] = event.getX(index)
        threeFingerStartYs[index] = event.getY(index)
    }
}

internal fun GlobalQuickActions.consistentThreeFingerDirection(event: MotionEvent, threshold: Float): SwipeDirection? {
    var direction: SwipeDirection? = null
    for (index in 0 until THREE_FINGER_COUNT) {
        val pointerIndex = event.findPointerIndex(threeFingerPointerIds[index])
        if (pointerIndex < 0) return null
        val dx = event.getX(pointerIndex) - threeFingerStartXs[index]
        val dy = event.getY(pointerIndex) - threeFingerStartYs[index]
        val absX = kotlin.math.abs(dx)
        val absY = kotlin.math.abs(dy)
        if (maxOf(absX, absY) < threshold) return null
        val fingerDirection = swipeDirection(dx, dy, absX, absY)
        val previous = direction
        if (previous == null) {
            direction = fingerDirection
        } else if (previous != fingerDirection) {
            return null
        }
    }
    return direction
}

internal fun GlobalQuickActions.swipeDirection(dx: Float, dy: Float, absX: Float, absY: Float): SwipeDirection {
    return if (absY > absX) {
        if (dy < 0) SwipeDirection.UP else SwipeDirection.DOWN
    } else {
        if (dx < 0) SwipeDirection.LEFT else SwipeDirection.RIGHT
    }
}

internal fun GlobalQuickActions.cancelCurrentTouchGesture() {
    val service = inputManagerService ?: return
    runCatching {
        GlobalQuickActions.callCompatible(service, "cancelCurrentTouch")
        log("three-finger current touch cancelled")
    }.onFailure {
        log("three-finger cancelCurrentTouch failed")
        Log.w(TAG, "Source gesture helper failed", it)
    }
}

internal fun GlobalQuickActions.averageX(event: MotionEvent): Float {
    var total = 0f
    val count = minOf(THREE_FINGER_COUNT, event.pointerCount)
    for (index in 0 until count) total += event.getX(index)
    return total / count
}

internal fun GlobalQuickActions.averageY(event: MotionEvent): Float {
    var total = 0f
    val count = minOf(THREE_FINGER_COUNT, event.pointerCount)
    for (index in 0 until count) total += event.getY(index)
    return total / count
}

internal fun GlobalQuickActions.resetThreeFingerState() {
    threeFingerActive = false
    threeFingerConsumed = false
    threeFingerTouchCancelled = false
    threeFingerStartTime = 0L
    for (index in 0 until THREE_FINGER_COUNT) {
        threeFingerPointerIds[index] = -1
        threeFingerStartXs[index] = 0f
        threeFingerStartYs[index] = 0f
    }
}

internal fun GlobalQuickActions.handleThreeFingerPinchMotion(event: MotionEvent) {
    if ((event.source and InputDevice.SOURCE_TOUCHSCREEN) != InputDevice.SOURCE_TOUCHSCREEN) return
    val settings = threeFingerPinchSettings
    if (!settings.enabled) return
    val pointerCount = event.pointerCount
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN,
        MotionEvent.ACTION_CANCEL,
        MotionEvent.ACTION_UP -> {
            resetThreeFingerPinchState()
            return
        }
        MotionEvent.ACTION_POINTER_DOWN -> {
            if (pointerCount == THREE_FINGER_COUNT) {
                recordThreeFingerPinchStart(event)
            } else if (pointerCount > THREE_FINGER_COUNT) {
                resetThreeFingerPinchState()
            }
            return
        }
        MotionEvent.ACTION_POINTER_UP -> {
            if (pointerCount <= THREE_FINGER_COUNT) {
                resetThreeFingerPinchState()
            }
            return
        }
        MotionEvent.ACTION_MOVE -> Unit
        else -> return
    }
    if (!threeFingerPinchActive || threeFingerPinchConsumed || pointerCount != THREE_FINGER_COUNT) return

    val context = currentApplicationContext() ?: return
    val currentSpan = threeFingerPinchAverageSpan(event)
    if (currentSpan <= 0f) return
    val shrink = threeFingerPinchStartSpan - currentSpan
    if (shrink < dp(context, settings.thresholdDp).toFloat()) return
    val action = settings.normalizedAction()
    if (action == ACTION_NONE) return

    cancelCurrentTouchGesture()
    threeFingerPinchConsumed = true
    resetThreeFingerState()
    resetThreeFingerSpreadState()
    performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
    performGlobalGestureAction(context, action)
}

internal fun GlobalQuickActions.recordThreeFingerPinchStart(event: MotionEvent) {
    threeFingerPinchActive = true
    threeFingerPinchConsumed = false
    for (index in 0 until THREE_FINGER_COUNT) {
        threeFingerPinchPointerIds[index] = event.getPointerId(index)
    }
    threeFingerPinchStartSpan = threeFingerPinchAverageSpan(event)
    if (threeFingerPinchStartSpan <= 0f) resetThreeFingerPinchState()
}

internal fun GlobalQuickActions.threeFingerPinchAverageSpan(event: MotionEvent): Float {
    val points = ArrayList<Pair<Float, Float>>(THREE_FINGER_COUNT)
    for (index in 0 until THREE_FINGER_COUNT) {
        val pointerIndex = event.findPointerIndex(threeFingerPinchPointerIds[index])
        if (pointerIndex < 0) return 0f
        points.add(event.getX(pointerIndex) to event.getY(pointerIndex))
    }
    var total = 0f
    for (first in 0 until points.size - 1) {
        for (second in first + 1 until points.size) {
            val dx = points[first].first - points[second].first
            val dy = points[first].second - points[second].second
            total += kotlin.math.sqrt(dx * dx + dy * dy)
        }
    }
    return total / 3f
}

internal fun GlobalQuickActions.resetThreeFingerPinchState() {
    threeFingerPinchActive = false
    threeFingerPinchConsumed = false
    threeFingerPinchStartSpan = 0f
    for (index in 0 until THREE_FINGER_COUNT) {
        threeFingerPinchPointerIds[index] = -1
    }
}

internal fun GlobalQuickActions.handleThreeFingerSpreadMotion(event: MotionEvent) {
    if ((event.source and InputDevice.SOURCE_TOUCHSCREEN) != InputDevice.SOURCE_TOUCHSCREEN) return
    val settings = threeFingerSpreadSettings
    if (!settings.enabled) return
    val pointerCount = event.pointerCount
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN,
        MotionEvent.ACTION_CANCEL,
        MotionEvent.ACTION_UP -> {
            resetThreeFingerSpreadState()
            return
        }
        MotionEvent.ACTION_POINTER_DOWN -> {
            if (pointerCount == THREE_FINGER_COUNT) {
                recordThreeFingerSpreadStart(event)
            } else if (pointerCount > THREE_FINGER_COUNT) {
                resetThreeFingerSpreadState()
            }
            return
        }
        MotionEvent.ACTION_POINTER_UP -> {
            if (pointerCount <= THREE_FINGER_COUNT) {
                resetThreeFingerSpreadState()
            }
            return
        }
        MotionEvent.ACTION_MOVE -> Unit
        else -> return
    }
    if (!threeFingerSpreadActive || threeFingerSpreadConsumed || pointerCount != THREE_FINGER_COUNT) return

    val context = currentApplicationContext() ?: return
    val currentSpan = threeFingerSpreadAverageSpan(event)
    if (currentSpan <= 0f) return
    val expansion = currentSpan - threeFingerSpreadStartSpan
    if (expansion < dp(context, settings.thresholdDp).toFloat()) return
    val action = settings.normalizedAction()
    if (action == ACTION_NONE) return

    cancelCurrentTouchGesture()
    threeFingerSpreadConsumed = true
    resetThreeFingerState()
    resetThreeFingerPinchState()
    performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
    performGlobalGestureAction(context, action)
}

internal fun GlobalQuickActions.recordThreeFingerSpreadStart(event: MotionEvent) {
    threeFingerSpreadActive = true
    threeFingerSpreadConsumed = false
    for (index in 0 until THREE_FINGER_COUNT) {
        threeFingerSpreadPointerIds[index] = event.getPointerId(index)
    }
    threeFingerSpreadStartSpan = threeFingerSpreadAverageSpan(event)
    if (threeFingerSpreadStartSpan <= 0f) resetThreeFingerSpreadState()
}

internal fun GlobalQuickActions.threeFingerSpreadAverageSpan(event: MotionEvent): Float {
    val points = ArrayList<Pair<Float, Float>>(THREE_FINGER_COUNT)
    for (index in 0 until THREE_FINGER_COUNT) {
        val pointerIndex = event.findPointerIndex(threeFingerSpreadPointerIds[index])
        if (pointerIndex < 0) return 0f
        points.add(event.getX(pointerIndex) to event.getY(pointerIndex))
    }
    var total = 0f
    for (first in 0 until points.size - 1) {
        for (second in first + 1 until points.size) {
            val dx = points[first].first - points[second].first
            val dy = points[first].second - points[second].second
            total += kotlin.math.sqrt(dx * dx + dy * dy)
        }
    }
    return total / 3f
}

internal fun GlobalQuickActions.resetThreeFingerSpreadState() {
    threeFingerSpreadActive = false
    threeFingerSpreadConsumed = false
    threeFingerSpreadStartSpan = 0f
    for (index in 0 until THREE_FINGER_COUNT) {
        threeFingerSpreadPointerIds[index] = -1
    }
}

internal fun GlobalQuickActions.handleStatusBarShortcutMotion(event: MotionEvent) {
    if ((event.source and InputDevice.SOURCE_TOUCHSCREEN) != InputDevice.SOURCE_TOUCHSCREEN) return
    val context = systemServerContext ?: currentApplicationContext() ?: return
    val settings = statusBarShortcutSettings(context)
    if (!settings.enabled) return
    val statusBarHeight = statusBarHeightPx(context)
    if (statusBarHeight <= 0) return

    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            if (event.y > statusBarHeight) return
            statusBarShortcutDownX = event.x
            statusBarShortcutDownY = event.y
            statusBarShortcutDownTime = event.eventTime
            statusBarShortcutLongPressTriggered = false
            statusBarShortcutDownInBar = true
        }
        MotionEvent.ACTION_MOVE -> {
            if (!statusBarShortcutDownInBar || statusBarShortcutLongPressTriggered) return
            val moved = maxOf(
                kotlin.math.abs(event.x - statusBarShortcutDownX),
                kotlin.math.abs(event.y - statusBarShortcutDownY)
            )
            if (moved > dp(context, STATUS_BAR_TAP_SLOP_DP)) {
                statusBarShortcutDownInBar = false
                return
            }
            if (event.eventTime - statusBarShortcutDownTime >= STATUS_BAR_LONG_PRESS_MS) {
                val action = if (statusBarShortcutDownX < context.resources.displayMetrics.widthPixels / 2f) {
                    settings.leftLongPressAction
                } else {
                    settings.rightLongPressAction
                }
                if (action != ACTION_NONE) {
                    statusBarShortcutLongPressTriggered = true
                    statusBarShortcutDownInBar = false
                    performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
                    performGlobalGestureAction(context, action)
                }
            }
        }
        MotionEvent.ACTION_UP -> {
            if (!statusBarShortcutDownInBar || statusBarShortcutLongPressTriggered) {
                resetStatusBarShortcutState()
                return
            }
            val duration = event.eventTime - statusBarShortcutDownTime
            val moved = maxOf(
                kotlin.math.abs(event.x - statusBarShortcutDownX),
                kotlin.math.abs(event.y - statusBarShortcutDownY)
            )
            if (duration <= STATUS_BAR_TAP_MAX_MS && moved <= dp(context, STATUS_BAR_TAP_SLOP_DP)) {
                val now = event.eventTime
                if (now - statusBarShortcutLastTapTime <= STATUS_BAR_DOUBLE_TAP_MS) {
                    statusBarPendingSingleTap?.let { mainHandler?.removeCallbacks(it) }
                    statusBarPendingSingleTap = null
                    statusBarShortcutLastTapTime = 0L
                    if (settings.doubleClickAction != ACTION_NONE) {
                        performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
                        performGlobalGestureAction(context, settings.doubleClickAction)
                    }
                } else {
                    val singleAction = if (statusBarShortcutDownX < context.resources.displayMetrics.widthPixels / 2f) {
                        settings.leftClickAction
                    } else {
                        settings.rightClickAction
                    }
                    statusBarShortcutLastTapTime = now
                    statusBarPendingSingleTap = Runnable {
                        if (singleAction != ACTION_NONE) {
                            performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
                            performGlobalGestureAction(context, singleAction)
                        }
                        statusBarPendingSingleTap = null
                    }.also { runnable ->
                        postOnMainDelayed(runnable, STATUS_BAR_DOUBLE_TAP_MS)
                    }
                }
            }
            resetStatusBarShortcutState()
        }
        MotionEvent.ACTION_CANCEL -> resetStatusBarShortcutState()
    }
}

internal fun GlobalQuickActions.resetStatusBarShortcutState() {
    statusBarShortcutDownInBar = false
    statusBarShortcutLongPressTriggered = false
    statusBarShortcutDownX = 0f
    statusBarShortcutDownY = 0f
    statusBarShortcutDownTime = 0L
}


internal fun GlobalQuickActions.handleNavigationKeyShortcutTouch(view: View, event: MotionEvent): Boolean {
    if ((event.source and InputDevice.SOURCE_TOUCHSCREEN) != InputDevice.SOURCE_TOUCHSCREEN) return false
    val context = view.context ?: currentApplicationContext() ?: return false
    val settings = navigationKeyShortcutSettings(context)
    if (!settings.enabled) return false
    val role = detectNavigationKeyRole(view) ?: return false
    val doubleAction = settings.doubleActionFor(role)
    val longAction = settings.longActionFor(role)
    if (doubleAction == ACTION_NONE && longAction == ACTION_NONE) return false

    val state = navigationKeyTouchStates.getOrPut(view) { NavigationKeyTouchState() }
    val handler = navigationKeyHandler
    val slop = ViewConfiguration.get(context).scaledTouchSlop
    val doubleTimeout = ViewConfiguration.getDoubleTapTimeout().toLong()
    val longTimeout = ViewConfiguration.getLongPressTimeout().toLong()

    fun cancelLongPress() {
        state.longPressRunnable?.let { handler.removeCallbacks(it) }
        state.longPressRunnable = null
    }

    fun cancelPendingSingleTap() {
        state.pendingSingleTapRunnable?.let { handler.removeCallbacks(it) }
        state.pendingSingleTapRunnable = null
    }

    fun performDefaultSingleTap() {
        injectKey(role.keyCode)
    }

    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            if (doubleAction != ACTION_NONE && event.eventTime - state.lastTapUpTime <= doubleTimeout) {
                cancelPendingSingleTap()
            }
            state.downX = event.rawX
            state.downY = event.rawY
            state.downTime = event.eventTime
            state.longPressTriggered = false
            cancelLongPress()
            if (longAction != ACTION_NONE) {
                state.longPressRunnable = Runnable {
                    state.longPressTriggered = true
                    cancelPendingSingleTap()
                    performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
                    performGlobalGestureAction(context, longAction)
                }.also { handler.postDelayed(it, longTimeout) }
            }
            view.isPressed = true
            return true
        }

        MotionEvent.ACTION_MOVE -> {
            val moved = maxOf(
                kotlin.math.abs(event.rawX - state.downX),
                kotlin.math.abs(event.rawY - state.downY)
            )
            if (moved > slop) {
                cancelLongPress()
            }
            return true
        }

        MotionEvent.ACTION_UP -> {
            view.isPressed = false
            cancelLongPress()
            if (state.longPressTriggered) {
                state.longPressTriggered = false
                state.lastTapUpTime = 0L
                return true
            }

            val moved = maxOf(
                kotlin.math.abs(event.rawX - state.downX),
                kotlin.math.abs(event.rawY - state.downY)
            )
            if (moved > slop) {
                state.lastTapUpTime = 0L
                return true
            }

            val now = event.eventTime
            if (doubleAction != ACTION_NONE && now - state.lastTapUpTime <= doubleTimeout) {
                cancelPendingSingleTap()
                state.lastTapUpTime = 0L
                performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
                performGlobalGestureAction(context, doubleAction)
                return true
            }

            state.lastTapUpTime = now
            if (doubleAction != ACTION_NONE) {
                state.pendingSingleTapRunnable = Runnable {
                    performDefaultSingleTap()
                    state.pendingSingleTapRunnable = null
                }.also { handler.postDelayed(it, doubleTimeout) }
            } else {
                performDefaultSingleTap()
            }
            return true
        }

        MotionEvent.ACTION_CANCEL -> {
            view.isPressed = false
            cancelLongPress()
            state.longPressTriggered = false
            return true
        }
    }
    return true
}

internal fun GlobalQuickActions.detectNavigationKeyRole(view: View): NavigationKeyRole? {
    val code = runCatching { (GlobalQuickActions.readField(view, "mCode") as? Number)?.toInt() ?: 0 }.getOrDefault(0)
    when (code) {
        KeyEvent.KEYCODE_BACK -> return NavigationKeyRole.BACK
        KeyEvent.KEYCODE_HOME -> return NavigationKeyRole.HOME
        KeyEvent.KEYCODE_APP_SWITCH -> return NavigationKeyRole.RECENTS
    }
    val idName = viewIdName(view).orEmpty().lowercase(Locale.US)
    return when {
        idName.contains("back") -> NavigationKeyRole.BACK
        idName.contains("home") -> NavigationKeyRole.HOME
        idName.contains("recent") || idName.contains("recents") || idName.contains("task") -> NavigationKeyRole.RECENTS
        else -> null
    }
}

internal fun GlobalQuickActions.handleScreenOffVolumeKeyShortcut(context: Context, event: KeyEvent): Boolean {
    val keyCode = event.keyCode
    if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false

    val activeSequence = screenOffVolumeDownKeyCode == keyCode || screenOffVolumeLongPressTriggered
    val settings = screenOffVolumeKeySettings(context)
    if (!settings.enabled && !activeSequence) return false
    if (!activeSequence && isScreenInteractive(context)) return false
    if (!settings.longPressEnabled && !settings.doubleClickEnabled) return false

    return when (event.action) {
        KeyEvent.ACTION_DOWN -> {
            if (event.repeatCount > 0) return true
            cancelScreenOffVolumeLongPress()
            screenOffVolumeDownKeyCode = keyCode
            screenOffVolumeLongPressTriggered = false
            if (settings.longPressEnabled) {
                screenOffVolumeLongPressRunnable = Runnable {
                    if (screenOffVolumeDownKeyCode == keyCode) {
                        screenOffVolumeLongPressTriggered = true
                        screenOffVolumeDownKeyCode = 0
                        cancelScreenOffVolumeDoubleClick()
                        performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
                        injectKey(
                            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                                KeyEvent.KEYCODE_MEDIA_NEXT
                            } else {
                                KeyEvent.KEYCODE_MEDIA_PREVIOUS
                            }
                        )
                        log("screen-off volume long press handled: $keyCode")
                    }
                }.also { runnable ->
                    screenOffVolumeHandler.postDelayed(runnable, settings.longPressTimeoutMs.toLong())
                }
            }
            true
        }
        KeyEvent.ACTION_UP -> {
            cancelScreenOffVolumeLongPress()
            if (screenOffVolumeLongPressTriggered) {
                screenOffVolumeLongPressTriggered = false
                screenOffVolumeDownKeyCode = 0
                return true
            }
            screenOffVolumeDownKeyCode = 0
            if (settings.doubleClickEnabled) {
                if (screenOffVolumeDoubleClickPending && screenOffVolumeDoubleClickKeyCode == keyCode) {
                    cancelScreenOffVolumeDoubleClick()
                    performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
                    injectKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    log("screen-off volume double click handled: $keyCode")
                } else {
                    scheduleScreenOffVolumeSingleTap(context, settings, keyCode)
                }
            } else {
                adjustMusicVolume(context, keyCode)
            }
            true
        }
        else -> false
    }
}

internal fun GlobalQuickActions.scheduleScreenOffVolumeSingleTap(context: Context, settings: ScreenOffVolumeKeySettings, keyCode: Int) {
    cancelScreenOffVolumeDoubleClick()
    screenOffVolumeDoubleClickPending = true
    screenOffVolumeDoubleClickKeyCode = keyCode
    screenOffVolumeDoubleClickRunnable = Runnable {
        if (screenOffVolumeDoubleClickPending && screenOffVolumeDoubleClickKeyCode == keyCode) {
            screenOffVolumeDoubleClickPending = false
            screenOffVolumeDoubleClickKeyCode = 0
            adjustMusicVolume(context, keyCode)
        }
        screenOffVolumeDoubleClickRunnable = null
    }.also { runnable ->
        screenOffVolumeHandler.postDelayed(runnable, settings.doubleClickTimeoutMs.toLong())
    }
}

internal fun GlobalQuickActions.adjustMusicVolume(context: Context, keyCode: Int) {
    runCatching {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            AudioManager.ADJUST_RAISE
        } else {
            AudioManager.ADJUST_LOWER
        }
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
    }.onFailure {
        Log.w(TAG, "Source gesture helper failed", it)
    }
}

internal fun GlobalQuickActions.isScreenInteractive(context: Context): Boolean {
    return runCatching {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive == true
    }.getOrDefault(true)
}

internal fun GlobalQuickActions.cancelScreenOffVolumeLongPress() {
    screenOffVolumeLongPressRunnable?.let { screenOffVolumeHandler.removeCallbacks(it) }
    screenOffVolumeLongPressRunnable = null
}

internal fun GlobalQuickActions.cancelScreenOffVolumeDoubleClick() {
    screenOffVolumeDoubleClickRunnable?.let { screenOffVolumeHandler.removeCallbacks(it) }
    screenOffVolumeDoubleClickRunnable = null
    screenOffVolumeDoubleClickPending = false
    screenOffVolumeDoubleClickKeyCode = 0
}

internal fun GlobalQuickActions.resetScreenOffVolumeState() {
    cancelScreenOffVolumeLongPress()
    cancelScreenOffVolumeDoubleClick()
    screenOffVolumeDownKeyCode = 0
    screenOffVolumeLongPressTriggered = false
}
