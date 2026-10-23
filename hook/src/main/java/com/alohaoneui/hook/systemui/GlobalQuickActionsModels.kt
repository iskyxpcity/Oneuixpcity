package com.alohaoneui.hook.systemui

import android.view.KeyEvent

internal const val SETTINGS_CACHE_MS = 1000L
internal const val DEBUG_LOG = false
internal const val THREE_FINGER_COUNT = 3
internal const val MIN_THREE_FINGER_TOUCH_CANCEL_THRESHOLD_DP = 1
internal const val MAX_THREE_FINGER_TOUCH_CANCEL_THRESHOLD_DP = 20
internal const val DEFAULT_THREE_FINGER_TOUCH_CANCEL_THRESHOLD_DP = 8
internal const val MIN_THREE_FINGER_PINCH_THRESHOLD_DP = 8
internal const val MAX_THREE_FINGER_PINCH_THRESHOLD_DP = 160
internal const val DEFAULT_THREE_FINGER_PINCH_THRESHOLD_DP = 48
internal const val MIN_THREE_FINGER_SPREAD_THRESHOLD_DP = 8
internal const val MAX_THREE_FINGER_SPREAD_THRESHOLD_DP = 160
internal const val DEFAULT_THREE_FINGER_SPREAD_THRESHOLD_DP = 48
internal const val MIN_FLOATING_BALL_SIZE_DP = 40
internal const val MAX_FLOATING_BALL_SIZE_DP = 88
internal const val DEFAULT_FLOATING_BALL_SIZE_DP = 56
internal const val FLOATING_BALL_DOCK_INDICATOR_WIDTH_DP = 20
internal const val FLOATING_BALL_DOCK_INDICATOR_HEIGHT_DP = 36
internal const val FLOATING_BALL_DOCK_BREATH_PERIOD_MS = 1800L
internal const val FLOATING_BALL_DOCK_BREATH_FRAME_MS = 40L
internal const val FLOATING_BALL_LONG_PRESS_SLOP_DP = 28
internal const val FLOATING_BALL_TOUCH_STALE_MS = 8000L
internal const val FLOATING_BALL_IDLE_RETRY_MS = 250L
internal const val FLOATING_BALL_INNER_RADIUS_RATIO = 0.61f
internal const val FLOATING_BALL_INNER_OFFSET_LIMIT_DP = 11
internal const val FLOATING_BALL_TAP_SLOP_DP = 8
internal const val FLOATING_BALL_SWIPE_THRESHOLD_DP = 38
internal const val FLOATING_BALL_LONG_PRESS_MS = 520L
internal const val FLOATING_BALL_DOUBLE_TAP_MS = 320L
internal const val DEFAULT_FLOATING_BALL_COLOR = 0xAA5E7CFF.toInt()
internal const val MIN_FLOATING_BALL_AUTO_EDGE_DELAY_SECONDS = 0
internal const val MAX_FLOATING_BALL_AUTO_EDGE_DELAY_SECONDS = 10
internal const val DEFAULT_FLOATING_BALL_AUTO_EDGE_DELAY_SECONDS = 3
internal const val FLOATING_BALL_UPDATE_BROADCAST = "com.mods.grx.SERVICES"
internal const val FLOATING_BALL_UPDATE_EXTRA = "UPDATE"
internal const val FLOATING_BALL_UPDATE_CONFIG = "FBV_CONFIG"
internal const val FLOATING_BALL_UPDATE_ACTIONS = "FBV_ACTIONS"
internal const val MIN_HAPTIC_STRENGTH = 1
internal const val MAX_HAPTIC_STRENGTH = 100
internal const val DEFAULT_HAPTIC_STRENGTH = 60
internal const val HAPTIC_DURATION_MS = 24L
internal const val MIN_SCREEN_OFF_VOLUME_LONG_PRESS_TIMEOUT_MS = 350
internal const val MAX_SCREEN_OFF_VOLUME_LONG_PRESS_TIMEOUT_MS = 2000
internal const val DEFAULT_SCREEN_OFF_VOLUME_LONG_PRESS_TIMEOUT_MS = 500
internal const val MIN_SCREEN_OFF_VOLUME_DOUBLE_CLICK_TIMEOUT_MS = 100
internal const val MAX_SCREEN_OFF_VOLUME_DOUBLE_CLICK_TIMEOUT_MS = 1000
internal const val DEFAULT_SCREEN_OFF_VOLUME_DOUBLE_CLICK_TIMEOUT_MS = 500
internal const val TYPE_NAVIGATION_BAR_PANEL = 2024
internal const val STATUS_BAR_TAP_SLOP_DP = 10
internal const val STATUS_BAR_TAP_MAX_MS = 260L
internal const val STATUS_BAR_DOUBLE_TAP_MS = 320L
internal const val STATUS_BAR_LONG_PRESS_MS = 520L
internal const val NAVIGATION_KEY_DOUBLE_TAP_MS = 320L

internal const val ACTION_NONE = "none"
internal const val ACTION_BACK = "back"
internal const val ACTION_HOME = "home"
internal const val ACTION_RECENTS = "recents"
internal const val ACTION_NOTIFICATIONS = "notifications"
internal const val ACTION_SCREENSHOT = "screenshot"
internal const val ACTION_SMART_SCREENSHOT = "smart_screenshot"
internal const val ACTION_LOCK_SCREEN = "lock_screen"
internal const val ACTION_POWER_MENU = "power_menu"
internal const val ACTION_PREVIOUS_TRACK = "previous_track"
internal const val ACTION_NEXT_TRACK = "next_track"
internal const val ACTION_PLAY_PAUSE = "play_pause"
internal const val ACTION_MEDIA_REWIND = "media_rewind"
internal const val ACTION_MEDIA_FAST_FORWARD = "media_fast_forward"
internal const val ACTION_FLASHLIGHT = "flashlight"
internal const val ACTION_KILL_FOREGROUND_APP = "kill_foreground_app"
internal const val ACTION_MEMORY_CLEAN = "memory_clean"
internal const val ACTION_SCREEN_RECORD = "screen_record"
internal const val ACTION_QUICK_CAMERA = "quick_camera"
internal const val ACTION_TAKE_PHOTO = "take_photo"
internal const val ACTION_QUICK_VIDEO = "quick_video"
internal const val ACTION_AIRPLANE_MODE = "airplane_mode"
internal const val ACTION_BLUETOOTH = "bluetooth"
internal const val ACTION_SILENT_MODE = "silent_mode"
internal const val ACTION_VIBRATE_MODE = "vibrate_mode"
internal const val ACTION_WIFI = "wifi"
internal const val ACTION_MOBILE_DATA = "mobile_data"
internal const val ACTION_SCROLL_TOP = "scroll_top"
internal const val ACTION_SCROLL_BOTTOM = "scroll_bottom"
internal const val ACTION_CUSTOM_APP_PREFIX = "custom_app:"
internal const val ACTION_WECHAT_SCAN = "wechat_scan"
internal const val ACTION_WECHAT_PAY = "wechat_pay"
internal const val ACTION_WECHAT_COLLECT = "wechat_collect"
internal const val ACTION_ALIPAY_SCAN = "alipay_scan"
internal const val ACTION_ALIPAY_PAY = "alipay_pay"
internal const val ACTION_ALIPAY_COLLECT = "alipay_collect"

internal const val WECHAT_PACKAGE = "com.tencent.mm"
internal const val WECHAT_SCAN_ACTIVITY = "com.tencent.mm.plugin.scanner.ui.BaseScanUI"
internal const val WECHAT_PAY_ACTIVITY = "com.tencent.mm.plugin.offline.ui.WalletOfflineCoinPurseUI"
internal const val WECHAT_COLLECT_ACTIVITY = "com.tencent.mm.plugin.collect.ui.CollectMainUI"
internal const val ALIPAY_SCAN_URI = "alipayqr://platformapi/startapp?saId=10000007"
internal const val ALIPAY_PAY_URI = "alipayqr://platformapi/startapp?saId=20000056"
internal const val ALIPAY_COLLECT_URI = "alipays://platformapi/startapp?appId=20000123"

internal const val PACKAGE_ANDROID = "android"
internal const val PACKAGE_SYSTEMUI = "com.android.systemui"
internal const val PACKAGE_APP = "com.alohaoneui.app"
internal const val PACKAGE_MODULE_APP = "com.alohaoneui.hook"

internal enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

internal data class ThreeFingerSettings(
    val enabled: Boolean,
    val touchCancelThresholdDp: Int,
    val hapticEnabled: Boolean,
    val hapticStrength: Int,
    val upAction: String,
    val downAction: String,
    val leftAction: String,
    val rightAction: String,
) {
    fun actionFor(direction: SwipeDirection): String = when (direction) {
        SwipeDirection.UP -> upAction
        SwipeDirection.DOWN -> downAction
        SwipeDirection.LEFT -> leftAction
        SwipeDirection.RIGHT -> rightAction
    }.ifBlank { ACTION_NONE }

    companion object {
        fun disabled() = ThreeFingerSettings(
            enabled = false,
            touchCancelThresholdDp = DEFAULT_THREE_FINGER_TOUCH_CANCEL_THRESHOLD_DP,
            hapticEnabled = false,
            hapticStrength = DEFAULT_HAPTIC_STRENGTH,
            upAction = ACTION_NONE,
            downAction = ACTION_NONE,
            leftAction = ACTION_NONE,
            rightAction = ACTION_NONE,
        )
    }
}

internal data class ThreeFingerPinchSettings(
    val enabled: Boolean,
    val thresholdDp: Int,
    val hapticEnabled: Boolean,
    val hapticStrength: Int,
    val action: String,
) {
    fun normalizedAction(): String = action.ifBlank { ACTION_NONE }
    companion object {
        fun disabled() = ThreeFingerPinchSettings(
            enabled = false,
            thresholdDp = DEFAULT_THREE_FINGER_PINCH_THRESHOLD_DP,
            hapticEnabled = false,
            hapticStrength = DEFAULT_HAPTIC_STRENGTH,
            action = ACTION_NONE,
        )
    }
}

internal data class ThreeFingerSpreadSettings(
    val enabled: Boolean,
    val thresholdDp: Int,
    val hapticEnabled: Boolean,
    val hapticStrength: Int,
    val action: String,
) {
    fun normalizedAction(): String = action.ifBlank { ACTION_NONE }
    companion object {
        fun disabled() = ThreeFingerSpreadSettings(
            enabled = false,
            thresholdDp = DEFAULT_THREE_FINGER_SPREAD_THRESHOLD_DP,
            hapticEnabled = false,
            hapticStrength = DEFAULT_HAPTIC_STRENGTH,
            action = ACTION_NONE,
        )
    }
}

internal data class FloatingBallSettings(
    val enabled: Boolean,
    val color: Int,
    val sizeDp: Int,
    val autoEdgeDelaySeconds: Int,
    val hapticEnabled: Boolean,
    val hapticStrength: Int,
    val clickAction: String,
    val doubleClickAction: String,
    val swipeUpAction: String,
    val swipeDownAction: String,
    val swipeLeftAction: String,
    val swipeRightAction: String,
)

internal data class StatusBarShortcutSettings(
    val enabled: Boolean,
    val hapticEnabled: Boolean,
    val hapticStrength: Int,
    val doubleClickAction: String,
    val leftClickAction: String,
    val leftLongPressAction: String,
    val rightClickAction: String,
    val rightLongPressAction: String,
)

internal enum class NavigationKeyRole(val keyCode: Int) {
    BACK(KeyEvent.KEYCODE_BACK),
    HOME(KeyEvent.KEYCODE_HOME),
    RECENTS(KeyEvent.KEYCODE_APP_SWITCH),
}

internal data class NavigationKeyShortcutSettings(
    val enabled: Boolean,
    val hapticEnabled: Boolean,
    val hapticStrength: Int,
    val backDoubleTapAction: String,
    val backLongPressAction: String,
    val homeDoubleTapAction: String,
    val homeLongPressAction: String,
    val recentsDoubleTapAction: String,
    val recentsLongPressAction: String,
) {
    fun doubleActionFor(role: NavigationKeyRole): String = when (role) {
        NavigationKeyRole.BACK -> backDoubleTapAction
        NavigationKeyRole.HOME -> homeDoubleTapAction
        NavigationKeyRole.RECENTS -> recentsDoubleTapAction
    }.ifBlank { ACTION_NONE }

    fun longActionFor(role: NavigationKeyRole): String = when (role) {
        NavigationKeyRole.BACK -> backLongPressAction
        NavigationKeyRole.HOME -> homeLongPressAction
        NavigationKeyRole.RECENTS -> recentsLongPressAction
    }.ifBlank { ACTION_NONE }
}

internal data class NavigationKeyTouchState(
    var downX: Float = 0f,
    var downY: Float = 0f,
    var downTime: Long = 0L,
    var lastTapUpTime: Long = 0L,
    var longPressTriggered: Boolean = false,
    var longPressRunnable: Runnable? = null,
    var pendingSingleTapRunnable: Runnable? = null,
)

internal data class ScreenOffVolumeKeySettings(
    val enabled: Boolean,
    val longPressEnabled: Boolean,
    val longPressTimeoutMs: Int,
    val doubleClickEnabled: Boolean,
    val doubleClickTimeoutMs: Int,
    val hapticEnabled: Boolean,
    val hapticStrength: Int,
)
