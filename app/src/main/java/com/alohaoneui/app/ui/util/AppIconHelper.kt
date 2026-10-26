package com.alohaoneui.app.ui.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconHelper {
    fun applyThemeIcon(context: Context, followsTheme: Boolean) {
        val packageName = context.packageName
        val themedAlias = ComponentName(packageName, "$packageName.ui.StartupActivityThemed")
        val staticAlias = ComponentName(packageName, "$packageName.ui.StartupActivityStatic")
        val packageManager = context.packageManager
        val themedState = if (followsTheme) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val staticState = if (followsTheme) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        packageManager.setComponentEnabledSetting(staticAlias, staticState, PackageManager.DONT_KILL_APP)
        packageManager.setComponentEnabledSetting(themedAlias, themedState, PackageManager.DONT_KILL_APP)
    }
}
