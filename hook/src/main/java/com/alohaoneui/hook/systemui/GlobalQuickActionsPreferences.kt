package com.alohaoneui.hook.systemui

import android.content.Context
import com.alohaoneui.common.Preference

internal val GlobalQuickActions.threeFingerSettings: ThreeFingerSettings
    get() {
        val s = cachedSettings ?: Preference.GlobalShortcutActions()
        return ThreeFingerSettings(
            enabled = s.threeFingerGestureEnabled,
            touchCancelThresholdDp = s.threeFingerTouchCancelThresholdDp
                .coerceIn(MIN_THREE_FINGER_TOUCH_CANCEL_THRESHOLD_DP, MAX_THREE_FINGER_TOUCH_CANCEL_THRESHOLD_DP),
            hapticEnabled = s.threeFingerHapticEnabled,
            hapticStrength = s.threeFingerHapticStrength.coerceIn(MIN_HAPTIC_STRENGTH, MAX_HAPTIC_STRENGTH),
            upAction = s.threeFingerSwipeUpAction.ifBlank { ACTION_NONE },
            downAction = s.threeFingerSwipeDownAction.ifBlank { ACTION_NONE },
            leftAction = s.threeFingerSwipeLeftAction.ifBlank { ACTION_NONE },
            rightAction = s.threeFingerSwipeRightAction.ifBlank { ACTION_NONE },
        )
    }

internal val GlobalQuickActions.threeFingerPinchSettings: ThreeFingerPinchSettings
    get() {
        val s = cachedSettings ?: Preference.GlobalShortcutActions()
        return ThreeFingerPinchSettings(
            enabled = s.threeFingerPinchEnabled,
            thresholdDp = s.threeFingerPinchThresholdDp
                .coerceIn(MIN_THREE_FINGER_PINCH_THRESHOLD_DP, MAX_THREE_FINGER_PINCH_THRESHOLD_DP),
            hapticEnabled = s.threeFingerPinchHapticEnabled,
            hapticStrength = s.threeFingerPinchHapticStrength.coerceIn(MIN_HAPTIC_STRENGTH, MAX_HAPTIC_STRENGTH),
            action = s.threeFingerPinchAction.ifBlank { ACTION_NONE },
        )
    }

internal val GlobalQuickActions.threeFingerSpreadSettings: ThreeFingerSpreadSettings
    get() {
        val s = cachedSettings ?: Preference.GlobalShortcutActions()
        return ThreeFingerSpreadSettings(
            enabled = s.threeFingerSpreadEnabled,
            thresholdDp = s.threeFingerSpreadThresholdDp
                .coerceIn(MIN_THREE_FINGER_SPREAD_THRESHOLD_DP, MAX_THREE_FINGER_SPREAD_THRESHOLD_DP),
            hapticEnabled = s.threeFingerSpreadHapticEnabled,
            hapticStrength = s.threeFingerSpreadHapticStrength.coerceIn(MIN_HAPTIC_STRENGTH, MAX_HAPTIC_STRENGTH),
            action = s.threeFingerSpreadAction.ifBlank { ACTION_NONE },
        )
    }

internal fun GlobalQuickActions.floatingBallSettings(context: Context): FloatingBallSettings {
    val s = cachedSettings ?: Preference.GlobalShortcutActions()
    return FloatingBallSettings(
        enabled = s.floatingBallEnabled,
        color = s.floatingBallColorArgb,
        sizeDp = s.floatingBallSizeDp.coerceIn(MIN_FLOATING_BALL_SIZE_DP, MAX_FLOATING_BALL_SIZE_DP),
        autoEdgeDelaySeconds = s.floatingBallAutoEdgeDelaySeconds
            .coerceIn(MIN_FLOATING_BALL_AUTO_EDGE_DELAY_SECONDS, MAX_FLOATING_BALL_AUTO_EDGE_DELAY_SECONDS),
        hapticEnabled = s.floatingBallHapticEnabled,
        hapticStrength = s.floatingBallHapticStrength.coerceIn(MIN_HAPTIC_STRENGTH, MAX_HAPTIC_STRENGTH),
        clickAction = s.floatingBallClickAction.ifBlank { ACTION_NONE },
        doubleClickAction = s.floatingBallDoubleClickAction.ifBlank { ACTION_NONE },
        swipeUpAction = s.floatingBallSwipeUpAction.ifBlank { ACTION_NONE },
        swipeDownAction = s.floatingBallSwipeDownAction.ifBlank { ACTION_NONE },
        swipeLeftAction = s.floatingBallSwipeLeftAction.ifBlank { ACTION_NONE },
        swipeRightAction = s.floatingBallSwipeRightAction.ifBlank { ACTION_NONE },
    )
}

internal fun GlobalQuickActions.statusBarShortcutSettings(context: Context): StatusBarShortcutSettings {
    val s = cachedSettings ?: Preference.GlobalShortcutActions()
    return StatusBarShortcutSettings(
        enabled = s.statusBarShortcutEnabled,
        hapticEnabled = s.statusBarShortcutHapticEnabled,
        hapticStrength = s.statusBarShortcutHapticStrength.coerceIn(MIN_HAPTIC_STRENGTH, MAX_HAPTIC_STRENGTH),
        doubleClickAction = s.statusBarDoubleClickAction.ifBlank { ACTION_NONE },
        leftClickAction = s.statusBarLeftClickAction.ifBlank { ACTION_NONE },
        leftLongPressAction = s.statusBarLeftLongPressAction.ifBlank { ACTION_NONE },
        rightClickAction = s.statusBarRightClickAction.ifBlank { ACTION_NONE },
        rightLongPressAction = s.statusBarRightLongPressAction.ifBlank { ACTION_NONE },
    )
}

internal fun GlobalQuickActions.navigationKeyShortcutSettings(context: Context): NavigationKeyShortcutSettings {
    val s = cachedSettings ?: Preference.GlobalShortcutActions()
    return NavigationKeyShortcutSettings(
        enabled = s.navigationKeyShortcutEnabled,
        hapticEnabled = s.navigationKeyHapticEnabled,
        hapticStrength = s.navigationKeyHapticStrength.coerceIn(MIN_HAPTIC_STRENGTH, MAX_HAPTIC_STRENGTH),
        backDoubleTapAction = s.navigationBackDoubleTapAction.ifBlank { ACTION_NONE },
        backLongPressAction = s.navigationBackLongPressAction.ifBlank { ACTION_NONE },
        homeDoubleTapAction = s.navigationHomeDoubleTapAction.ifBlank { ACTION_NONE },
        homeLongPressAction = s.navigationHomeLongPressAction.ifBlank { ACTION_NONE },
        recentsDoubleTapAction = s.navigationRecentsDoubleTapAction.ifBlank { ACTION_NONE },
        recentsLongPressAction = s.navigationRecentsLongPressAction.ifBlank { ACTION_NONE },
    )
}

internal fun GlobalQuickActions.screenOffVolumeKeySettings(context: Context): ScreenOffVolumeKeySettings {
    val s = cachedSettings ?: Preference.GlobalShortcutActions()
    return ScreenOffVolumeKeySettings(
        enabled = s.screenOffVolumeKeyEnabled,
        longPressEnabled = s.screenOffVolumeLongPressEnabled,
        longPressTimeoutMs = s.screenOffVolumeLongPressTimeoutMs
            .coerceIn(MIN_SCREEN_OFF_VOLUME_LONG_PRESS_TIMEOUT_MS, MAX_SCREEN_OFF_VOLUME_LONG_PRESS_TIMEOUT_MS),
        doubleClickEnabled = s.screenOffVolumeDoubleClickEnabled,
        doubleClickTimeoutMs = s.screenOffVolumeDoubleClickTimeoutMs
            .coerceIn(MIN_SCREEN_OFF_VOLUME_DOUBLE_CLICK_TIMEOUT_MS, MAX_SCREEN_OFF_VOLUME_DOUBLE_CLICK_TIMEOUT_MS),
        hapticEnabled = s.screenOffVolumeHapticEnabled,
        hapticStrength = s.screenOffVolumeHapticStrength.coerceIn(MIN_HAPTIC_STRENGTH, MAX_HAPTIC_STRENGTH),
    )
}
