package com.oneuixp.app.ui.screen.home

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Immutable
data class OneuiXPDeviceInfo(
    val model: String,
    val androidApi: String,
    val oneUi: String,
    val ramRom: String,
    val deviceCode: String,
    val chipset: String,
    val abiArchitecture: String,
    val resolutionDpi: String,
    val securityPatch: String,
)

@Composable
fun rememberOneuiXPDeviceInfo(): OneuiXPDeviceInfo {
    val context = LocalContext.current.applicationContext
    val configuration = LocalContext.current.resources.configuration
    return remember(
        context,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        configuration.densityDpi,
    ) {
        val metrics = context.resources.displayMetrics
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }
        val storage = StatFs(Environment.getDataDirectory().path)

        fun gigabytes(value: Long): String = String.format("%.1f GB", value / 1073741824.0)

        val oneUi = runCatching {
            val cls = Class.forName("android.os.SystemProperties")
            val get = cls.getMethod("get", String::class.java, String::class.java)
            (get.invoke(null, "ro.build.version.oneui", "") as String).ifBlank { "-" }
        }.getOrDefault("-")

        val chipset = if (Build.VERSION.SDK_INT >= 31) {
            Build.SOC_MODEL.ifBlank { Build.HARDWARE }
        } else {
            Build.HARDWARE
        }
        val architecture = System.getProperty("os.arch") ?: "-"

        OneuiXPDeviceInfo(
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidApi = "Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}",
            oneUi = oneUi,
            ramRom = "${gigabytes(memory.totalMem)} | ${gigabytes(storage.totalBytes)}",
            deviceCode = Build.DEVICE,
            chipset = chipset,
            abiArchitecture = "${Build.SUPPORTED_ABIS.firstOrNull() ?: "-"} / $architecture",
            resolutionDpi = "${metrics.widthPixels}×${metrics.heightPixels} / ${metrics.densityDpi}dpi",
            securityPatch = Build.VERSION.SECURITY_PATCH.ifBlank { "-" },
        )
    }
}
