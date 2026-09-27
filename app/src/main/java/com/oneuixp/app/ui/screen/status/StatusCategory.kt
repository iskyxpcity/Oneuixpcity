package com.oneuixp.app.ui.screen.status

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.oneuixp.app.R

enum class StatusCategory(
    @get:StringRes val titleRes: Int,
    @get:DrawableRes val iconRes: Int,
    val backgroundArgb: Int,
) {
    Time(
        R.string.status_category_time,
        R.drawable.flux_status_time,
        0xFF4C8DFF.toInt(),
    ),
    Notification(
        R.string.status_category_notification,
        R.drawable.flux_status_notification,
        0xFF7B8CFF.toInt(),
    ),
    NetworkSpeed(
        R.string.status_category_network_speed,
        R.drawable.flux_status_network_speed,
        0xFF2BB7A8.toInt(),
    ),
    Power(
        R.string.status_category_power,
        R.drawable.flux_status_power,
        0xFFFF6B6B.toInt(),
    ),
    Signal(
        R.string.status_category_signal,
        R.drawable.flux_status_signal,
        0xFFFF9F43.toInt(),
    ),
    Wifi(
        R.string.status_category_wifi,
        R.drawable.flux_status_wifi,
        0xFF4C8DFF.toInt(),
    ),
    Lockscreen(
        R.string.status_category_lockscreen,
        R.drawable.flux_status_lockscreen,
        0xFF8B7CFF.toInt(),
    ),
    NotificationCenter(
        R.string.status_category_notification_center,
        R.drawable.flux_status_notification_center,
        0xFF3DDC84.toInt(),
    ),
    ControlCenter(
        R.string.status_category_control_center,
        R.drawable.flux_status_control_center,
        0xFF2BB7A8.toInt(),
    ),
}
