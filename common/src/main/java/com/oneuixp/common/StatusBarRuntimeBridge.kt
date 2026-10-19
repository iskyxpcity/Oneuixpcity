package com.oneuixp.common

object StatusBarRuntimeBridge {
    const val ACTION = "com.oneuixp.app.action.STATUS_BAR_RUNTIME"
    const val EXTRA_STATUS_BAR_JSON = "status_bar_json"
    const val ACTION_POWER_OVERLAY = "com.oneuixp.app.action.STATUS_BAR_POWER_OVERLAY"
    const val EXTRA_POWER_OVERLAY_ON_TOP = "power_overlay_on_top"

    // Single on-demand export path for diagnostic events collected by SystemUI hooks.
    const val ONEUIXP_APP_PACKAGE = "com.oneuixp.app"
    const val ACTION_DIAGNOSTIC_LOG_REQUEST = "com.oneuixp.app.action.DIAGNOSTIC_LOG_REQUEST"
    const val ACTION_DIAGNOSTIC_LOG_RESPONSE = "com.oneuixp.app.action.DIAGNOSTIC_LOG_RESPONSE"
    const val EXTRA_DIAGNOSTIC_REQUEST_ID = "diagnostic_request_id"
    const val EXTRA_DIAGNOSTIC_LOG_TEXT = "diagnostic_log_text"
}
