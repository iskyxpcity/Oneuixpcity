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
        0xFFC1D7FF.toInt(),
    ),
    Notification(
        R.string.status_category_notification,
        R.drawable.flux_status_notification,
        0xFFD2D7FF.toInt(),
    ),
    NetworkSpeed(
        R.string.status_category_network_speed,
        R.drawable.flux_status_network_speed,
        0xFFB5E6E2.toInt(),
    ),
    Power(
        R.string.status_category_power,
        R.drawable.flux_status_power,
        0xFFFFCCCB.toInt(),
    ),
    Signal(
        R.string.status_category_signal,
        R.drawable.flux_status_signal,
        0xFFFFDDBD.toInt(),
    ),
    Wifi(
        R.string.status_category_wifi,
        R.drawable.flux_status_wifi,
        0xFFC1D7FF.toInt(),
    ),
    Lockscreen(
        R.string.status_category_lockscreen,
        R.drawable.flux_status_lockscreen,
        0xFFD7D1FF.toInt(),
    ),
    NotificationCenter(
        R.string.status_category_notification_center,
        R.drawable.flux_status_notification_center,
        0xFFBCF3D4.toInt(),
    ),
    ControlCenter(
        R.string.status_category_control_center,
        R.drawable.flux_status_control_center,
        0xFFB5E6E2.toInt(),
    ),
}
