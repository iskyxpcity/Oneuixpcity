package com.alohaoneui.app.ui.screen.status

import androidx.annotation.DrawableRes
import com.alohaoneui.app.R

enum class StatusIconStyleTarget(
    val key: String,
    val title: String,
    val description: String,
    @get:DrawableRes val iconRes: Int,
) {
    Battery(
        key = "battery",
        title = "电池图标样式",
        description = "使用 App 内置模块电池样式替换状态栏电池图标。",
        iconRes = R.drawable.battery,
    ),
    Wifi(
        key = "wifi",
        title = "WiFi 信号图标",
        description = "替换 WiFi 信号样式，支持 WiFi 5 / WiFi 6 / WiFi 7 图标和上下箭头隐藏。",
        iconRes = R.drawable.flux_status_wifi,
    ),
    Mobile(
        key = "mobile",
        title = "移动信号图标",
        description = "替换移动网络信号图标，支持正常状态栏、下拉面板和双卡样式。",
        iconRes = R.drawable.flux_status_signal,
    ),
    Bluetooth(
        key = "bluetooth",
        title = "蓝牙图标",
        description = "替换状态栏蓝牙未连接和已连接图标。",
        iconRes = R.drawable.bluetooth,
    ),
    Vpn(
        key = "vpn",
        title = "VPN 图标",
        description = "替换状态栏 VPN 图标。",
        iconRes = R.drawable.flux_status_lockscreen,
    );

    companion object {
        fun fromKey(key: String): StatusIconStyleTarget? = entries.firstOrNull { it.key == key }
    }
}
