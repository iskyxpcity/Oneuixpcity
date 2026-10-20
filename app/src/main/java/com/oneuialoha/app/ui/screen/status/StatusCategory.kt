package com.oneuialoha.app.ui.screen.status

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.oneuialoha.app.R

enum class StatusCategory(
    @get:StringRes val titleRes: Int,
    @get:DrawableRes val iconRes: Int,
    val backgroundArgb: Int,
) {
    Padding(
        R.string.status_category_padding,
        R.drawable.padding,
        0xFF607D8B.toInt(),
    ),
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
        R.drawable.net_speed,
        0xFF2BB7A8.toInt(),
    ),
    Power(
        R.string.status_category_power,
        R.drawable.flux_status_power,
        0xFFFF6B6B.toInt(),
    ),
    ComboStyleOne(
        R.string.status_combo_style_one_category,
        R.drawable.status_combo_style_one,
        0xFF7C6CFF.toInt(),
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
    Bluetooth(
        R.string.status_category_bluetooth,
        R.drawable.bluetooth,
        0xFF536DFE.toInt(),
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
    Other(
        R.string.oneuialoha_other,
        R.drawable.power_settings_new,
        0xFF607D8B.toInt(),
    ),
}
