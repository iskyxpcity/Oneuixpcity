package com.alohaoneui.hook.systemui

import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.SystemClock
import android.os.UserHandle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.InputEvent
import android.view.MotionEvent
import com.alohaoneui.hook.util.AlohaOneUIDiagnosticLog

internal fun GlobalQuickActions.performGlobalGestureAction(context: Context, action: String) {
    if (action == ACTION_NONE || action == ACTION_NOTIFICATIONS) {
        recordGlobalGestureActionResult(action, "skipped")
        return
    }
    runCatching {
        if (action.startsWith(ACTION_CUSTOM_APP_PREFIX)) {
            startCustomApp(context, action)
            recordGlobalGestureActionResult(action, "success")
            log("three-finger action performed: $action")
            return
        }
        when (action) {
            ACTION_BACK -> injectKey(KeyEvent.KEYCODE_BACK)
            ACTION_HOME -> injectKey(KeyEvent.KEYCODE_HOME)
            ACTION_RECENTS -> injectKey(KeyEvent.KEYCODE_APP_SWITCH)
            ACTION_SCREENSHOT -> takeScreenshot(context)
            ACTION_SMART_SCREENSHOT -> takeSmartScreenshot(context)
            ACTION_LOCK_SCREEN -> GlobalQuickActions.callCompatible(getPowerManager(context), "goToSleep", SystemClock.uptimeMillis())
            ACTION_POWER_MENU -> showGlobalActions()
            ACTION_PREVIOUS_TRACK -> injectKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            ACTION_NEXT_TRACK -> injectKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            ACTION_PLAY_PAUSE -> injectKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            ACTION_MEDIA_REWIND -> injectKey(KeyEvent.KEYCODE_MEDIA_REWIND)
            ACTION_MEDIA_FAST_FORWARD -> injectKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
            ACTION_FLASHLIGHT -> toggleFlashlight(context)
            ACTION_KILL_FOREGROUND_APP -> killForegroundApp(context)
            ACTION_MEMORY_CLEAN -> cleanMemory(context)
            ACTION_SCREEN_RECORD -> toggleScreenRecord(context)
            ACTION_TAKE_PHOTO -> openCameraCapture(context, video = false)
            ACTION_QUICK_VIDEO -> openCameraCapture(context, video = true)
            ACTION_AIRPLANE_MODE -> toggleAirplaneMode(context)
            ACTION_BLUETOOTH -> toggleBluetooth(context)
            ACTION_SILENT_MODE -> setRingerMode(context, AudioManager.RINGER_MODE_SILENT)
            ACTION_VIBRATE_MODE -> setRingerMode(context, AudioManager.RINGER_MODE_VIBRATE)
            ACTION_WIFI -> toggleWifi(context)
            ACTION_MOBILE_DATA -> toggleMobileData(context)
            ACTION_SCROLL_TOP -> scrollToTop()
            ACTION_SCROLL_BOTTOM -> scrollToBottom()
            ACTION_WECHAT_SCAN -> startActivityComponent(context, WECHAT_PACKAGE, WECHAT_SCAN_ACTIVITY)
            ACTION_WECHAT_PAY -> startActivityComponent(context, WECHAT_PACKAGE, WECHAT_PAY_ACTIVITY)
            ACTION_WECHAT_COLLECT -> startActivityComponent(context, WECHAT_PACKAGE, WECHAT_COLLECT_ACTIVITY)
            ACTION_ALIPAY_SCAN -> startActivityUri(context, ALIPAY_SCAN_URI)
            ACTION_ALIPAY_PAY -> startActivityUri(context, ALIPAY_PAY_URI)
            ACTION_ALIPAY_COLLECT -> startActivityUri(context, ALIPAY_COLLECT_URI)
            else -> throw IllegalArgumentException("Unsupported global shortcut action ID: $action")
        }
        recordGlobalGestureActionResult(action, "success")
        log("three-finger action performed: $action")
    }.onFailure {
        recordGlobalGestureActionResult(action, "failure", it)
        log("three-finger action failed: $action")
        Log.w(TAG, "Source global shortcut action failed", it)
    }
}

internal fun GlobalQuickActions.recordGlobalGestureActionResult(
    action: String,
    status: String,
    failure: Throwable? = null,
) {
    val safeAction = action.replace('\n', ' ').replace('\r', ' ').take(160)
    val failureSummary = failure?.let {
        " error=${it.javaClass.simpleName}:${it.message.orEmpty().replace('\n', ' ').replace('\r', ' ').take(240)}"
    }.orEmpty()
    lastGlobalActionDiagnostic =
        "atMs=${System.currentTimeMillis()} action=$safeAction status=$status$failureSummary"
    AlohaOneUIDiagnosticLog.record(TAG, lastGlobalActionDiagnostic)
}

internal fun GlobalQuickActions.performHapticFeedback(context: Context, enabled: Boolean, strength: Int) {
    if (!enabled) return
    runCatching {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        val amplitude = (strength.coerceIn(MIN_HAPTIC_STRENGTH, MAX_HAPTIC_STRENGTH) * 255 / MAX_HAPTIC_STRENGTH)
            .coerceIn(1, 255)
        vibrator.vibrate(VibrationEffect.createOneShot(HAPTIC_DURATION_MS, amplitude))
    }.recoverCatching {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(HAPTIC_DURATION_MS)
    }
}

internal fun GlobalQuickActions.withAlpha(color: Int, alpha: Int): Int {
    return (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
}

internal fun GlobalQuickActions.injectKey(keyCode: Int) {
    val now = SystemClock.uptimeMillis()
    val mode = 0
    val down = KeyEvent(
        now,
        now,
        KeyEvent.ACTION_DOWN,
        keyCode,
        0,
        0,
        KeyCharacterMap.VIRTUAL_KEYBOARD,
        0,
        KeyEvent.FLAG_FROM_SYSTEM,
        InputDevice.SOURCE_KEYBOARD
    )
    val up = KeyEvent.changeAction(down, KeyEvent.ACTION_UP)
    injectInputEventCompat(down, mode)
    injectInputEventCompat(up, mode)
}

/**
 * Prefer the InputManagerService instance captured in system_server, where the
 * original global gestures are dispatched. Fall back to InputManager's client
 * facade for triggers running in SystemUI.
 */
internal fun GlobalQuickActions.injectInputEventCompat(event: InputEvent, mode: Int) {
    var systemServerFailure: Throwable? = null
    val service = inputManagerService
    if (service != null) {
        val attempt = runCatching {
            GlobalQuickActions.callCompatible(service, "injectInputEvent", event, mode)
        }
        if (attempt.isSuccess && attempt.getOrNull() != false) return
        systemServerFailure = attempt.exceptionOrNull()
            ?: IllegalStateException("InputManagerService.injectInputEvent returned false")
    }

    val inputManager = try {
        getInputManager()
    } catch (failure: Throwable) {
        systemServerFailure?.let { if (it !== failure) failure.addSuppressed(it) }
        throw failure
    }
    try {
        val result = GlobalQuickActions.callCompatible(inputManager, "injectInputEvent", event, mode)
        if (result == false) throw IllegalStateException("InputManager.injectInputEvent returned false")
    } catch (failure: Throwable) {
        systemServerFailure?.let { if (it !== failure) failure.addSuppressed(it) }
        throw failure
    }
}

internal fun GlobalQuickActions.takeScreenshot(context: Context? = null) {
    runCatching {
        startSamsungCaptureSideButtonAction(context, "take_screenshot")
    }.recoverCatching {
        sendSamsungScreenshotExecutor(context)
    }.recoverCatching {
        injectKey(KeyEvent.KEYCODE_SYSRQ)
    }.recoverCatching {
        runShellCommand("cmd statusbar screenshot")
    }.recoverCatching {
        runShellCommand("input keyevent KEYCODE_SYSRQ")
    }.getOrThrow()
}

internal fun GlobalQuickActions.takeSmartScreenshot(context: Context) {
    runCatching {
        startSamsungCaptureSideButtonAction(context, "smart_select")
    }.recoverCatching {
        takeScreenshot(context)
    }.getOrThrow()
}

internal fun GlobalQuickActions.sendSamsungScreenshotExecutor(context: Context?) {
    val intent = Intent("com.samsung.android.capture.ScreenshotExecutor").apply {
        setPackage("android")
        putExtra("capturedOrigin", 100)
        putExtra("callingPackageName", "com.samsung.android.app.smartcapture")
        putExtra("displayId", "0")
        putExtra("type", "Fullscreen")
    }
    if (context != null) {
        context.sendBroadcast(intent, "com.samsung.permission.CAPTURE")
    } else {
        runShellCommand("am broadcast -a com.samsung.android.capture.ScreenshotExecutor --ei capturedOrigin 100 --es callingPackageName com.samsung.android.app.smartcapture --es displayId 0 --es type Fullscreen")
    }
}

internal fun GlobalQuickActions.startSamsungCaptureSideButtonAction(context: Context?, selectedAction: String) {
    val intent = Intent("smartcapture.intent.action.side_button_action").apply {
        component = ComponentName(
            "com.samsung.android.app.smartcapture",
            "com.samsung.android.app.settings.SideButtonStarterService"
        )
        putExtra("selected_action", selectedAction)
    }
    if (context != null) {
        val currentUser = UserHandle::class.java.getDeclaredField("CURRENT").apply { isAccessible = true }.get(null) as UserHandle
        runCatching {
            GlobalQuickActions.callCompatible(context, "startServiceAsUser", intent, currentUser)
        }.recoverCatching {
            context.startService(intent)
        }.getOrThrow()
    } else {
        runShellCommand(
            "am startservice -n com.samsung.android.app.smartcapture/com.samsung.android.app.settings.SideButtonStarterService " +
                "-a smartcapture.intent.action.side_button_action --es selected_action ${shellQuote(selectedAction)}"
        )
    }
}

internal fun GlobalQuickActions.showGlobalActions() {
    runCatching {
        runShellCommand("input keyevent --longpress KEYCODE_POWER")
    }.recoverCatching {
        injectLongPressKey(KeyEvent.KEYCODE_POWER, 700L)
    }.getOrThrow()
}

internal fun GlobalQuickActions.injectLongPressKey(keyCode: Int, durationMs: Long) {
    val downTime = SystemClock.uptimeMillis()
    val mode = 0
    val down = KeyEvent(
        downTime,
        downTime,
        KeyEvent.ACTION_DOWN,
        keyCode,
        0,
        0,
        KeyCharacterMap.VIRTUAL_KEYBOARD,
        0,
        KeyEvent.FLAG_FROM_SYSTEM or KeyEvent.FLAG_LONG_PRESS,
        InputDevice.SOURCE_KEYBOARD
    )
    injectInputEventCompat(down, mode)
    Thread {
        runCatching {
            Thread.sleep(durationMs)
            val upTime = SystemClock.uptimeMillis()
            val up = KeyEvent(
                downTime,
                upTime,
                KeyEvent.ACTION_UP,
                keyCode,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_FROM_SYSTEM,
                InputDevice.SOURCE_KEYBOARD
            )
            injectInputEventCompat(up, mode)
        }
    }.start()
}

internal fun GlobalQuickActions.runShellCommand(command: String) {
    Runtime.getRuntime().exec(arrayOf("sh", "-c", command)).waitFor()
}

internal fun GlobalQuickActions.runShellCommandForSuccess(command: String): Boolean {
    return runCatching {
        Runtime.getRuntime().exec(arrayOf("sh", "-c", command)).waitFor() == 0
    }.getOrDefault(false)
}

internal fun GlobalQuickActions.toggleFlashlight(context: Context) {
    val cameraManager = context.getSystemService(CameraManager::class.java) ?: return
    val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
        runCatching {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }.getOrDefault(false)
    } ?: return
    flashlightEnabled = !flashlightEnabled
    cameraManager.setTorchMode(cameraId, flashlightEnabled)
}

internal fun GlobalQuickActions.toggleAirplaneMode(context: Context) {
    val resolver = context.contentResolver
    val enabled = Settings.Global.getInt(resolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
    val next = if (enabled) 0 else 1
    Settings.Global.putInt(resolver, Settings.Global.AIRPLANE_MODE_ON, next)
    context.sendBroadcast(Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply {
        putExtra("state", next == 1)
    })
}

internal fun GlobalQuickActions.toggleBluetooth(context: Context) {
    val adapter = BluetoothAdapter.getDefaultAdapter()
    val enabled = adapter?.isEnabled == true
    val handled = if (enabled) {
        runShellCommandForSuccess("svc bluetooth disable")
    } else {
        runShellCommandForSuccess("svc bluetooth enable")
    }
    if (handled) return
    @Suppress("DEPRECATION")
    if (enabled) adapter?.disable() else adapter?.enable()
}

internal fun GlobalQuickActions.setRingerMode(context: Context, mode: Int) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    audioManager.ringerMode = mode
}

internal fun GlobalQuickActions.toggleWifi(context: Context) {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE)
        ?: context.getSystemService(Context.WIFI_SERVICE)
    val enabled = runCatching {
        GlobalQuickActions.callCompatible(wifiManager, "isWifiEnabled") as? Boolean
    }.getOrNull() ?: runCatching {
        Settings.Global.getInt(context.contentResolver, "wifi_on", 0) == 1
    }.getOrDefault(false)
    val next = !enabled
    val handledByManager = runCatching {
        val result = GlobalQuickActions.callCompatible(wifiManager, "setWifiEnabled", next)
        result !is Boolean || result
    }.getOrDefault(false)
    if (handledByManager) return
    val state = if (next) "enabled" else "disabled"
    if (runShellCommandForSuccess("cmd wifi set-wifi-enabled $state")) return
    if (runShellCommandForSuccess(if (next) "svc wifi enable" else "svc wifi disable")) return
    Settings.Global.putInt(context.contentResolver, "wifi_on", if (next) 1 else 0)
}

internal fun GlobalQuickActions.toggleMobileData(context: Context) {
    val telephonyManager = defaultDataTelephonyManager(context)
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val mobileEnabled = runCatching {
        GlobalQuickActions.callCompatible(telephonyManager, "isDataEnabled") as? Boolean
    }.getOrNull() ?: runCatching {
        GlobalQuickActions.callCompatible(telephonyManager, "getDataEnabled") as? Boolean
    }.getOrNull() ?: runCatching {
        GlobalQuickActions.callCompatible(connectivityManager, "getMobileDataEnabled") as? Boolean
    }.getOrNull() ?: runCatching {
        Settings.Global.getInt(context.contentResolver, "mobile_data", 1) == 1
    }.getOrDefault(true)
    val next = !mobileEnabled
    val handledByTelephony = runCatching {
        GlobalQuickActions.callCompatible(telephonyManager, "setDataEnabled", next)
        true
    }.recoverCatching {
        GlobalQuickActions.callCompatible(telephonyManager, "setDataEnabledForReason", 0, next)
        true
    }.getOrDefault(false)
    if (handledByTelephony) return
    runCatching {
        GlobalQuickActions.callCompatible(connectivityManager, "setMobileDataEnabled", next)
        true
    }.getOrDefault(false).let { if (it) return }
    if (runShellCommandForSuccess(if (next) "svc data enable" else "svc data disable")) return
    if (runShellCommandForSuccess("cmd phone data ${if (next) "enable" else "disable"}")) return
    Settings.Global.putInt(context.contentResolver, "mobile_data", if (next) 1 else 0)
}

internal fun GlobalQuickActions.defaultDataTelephonyManager(context: Context): Any? {
    val base = context.getSystemService(Context.TELEPHONY_SERVICE) ?: return null
    val subId = runCatching {
        Class.forName("android.telephony.SubscriptionManager")
            .getMethod("getDefaultDataSubscriptionId")
            .invoke(null) as? Int
    }.getOrNull()
    if (subId == null || subId < 0) return base
    return runCatching {
        GlobalQuickActions.callCompatible(base, "createForSubscriptionId", subId)
    }.getOrDefault(base)
}

internal fun GlobalQuickActions.injectSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 180L) {
    val downTime = SystemClock.uptimeMillis()
    fun send(action: Int, eventTime: Long, x: Float, y: Float) {
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        runCatching {
            injectInputEventCompat(event, 0)
        }
        event.recycle()
    }
    send(MotionEvent.ACTION_DOWN, downTime, startX, startY)
    val steps = 12
    for (step in 1 until steps) {
        val progress = step / steps.toFloat()
        send(
            MotionEvent.ACTION_MOVE,
            downTime + (durationMs * progress).toLong(),
            startX + (endX - startX) * progress,
            startY + (endY - startY) * progress
        )
    }
    send(MotionEvent.ACTION_UP, downTime + durationMs, endX, endY)
}

internal fun GlobalQuickActions.scrollToTop() {
    runCatching {
        val metrics = Resources.getSystem().displayMetrics
        val x = metrics.widthPixels * 0.5f
        val startY = metrics.heightPixels * 0.30f
        val endY = metrics.heightPixels * 0.84f
        repeat(7) {
            injectSwipe(x, startY, x, endY)
            SystemClock.sleep(35)
        }
    }.recoverCatching {
        runShellCommand("input keyevent KEYCODE_MOVE_HOME")
    }.getOrThrow()
}

internal fun GlobalQuickActions.scrollToBottom() {
    runCatching {
        val metrics = Resources.getSystem().displayMetrics
        val x = metrics.widthPixels * 0.5f
        val startY = metrics.heightPixels * 0.84f
        val endY = metrics.heightPixels * 0.30f
        repeat(7) {
            injectSwipe(x, startY, x, endY)
            SystemClock.sleep(35)
        }
    }.recoverCatching {
        runShellCommand("input keyevent KEYCODE_MOVE_END")
    }.getOrThrow()
}
