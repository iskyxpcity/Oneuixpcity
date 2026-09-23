package me.weishu.kernelsu.ui.screen.home

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.core.content.pm.PackageInfoCompat

@Immutable
data class ManagerVersion(val versionName: String, val versionCode: Long)

fun getManagerVersion(context: Context): ManagerVersion {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)!!
    return ManagerVersion(
        versionName = info.versionName.orEmpty(),
        versionCode = PackageInfoCompat.getLongVersionCode(info),
    )
}
