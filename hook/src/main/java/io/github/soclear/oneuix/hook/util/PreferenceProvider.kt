package io.github.soclear.oneuix.hook.util

import android.os.ParcelFileDescriptor
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Preference
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream

object PreferenceProvider {
    // libxposed 102 通过框架的 remote file 机制读取模块配置
    // （宿主进程对 remote file 只读，由模块 App 通过 libxposed service 推送到共享目录）
    @OptIn(ExperimentalSerializationApi::class)
    context(xposedModule: XposedModule)
    fun loadPreference(): Preference? = try {
        val remoteFiles = runCatching {
            xposedModule.listRemoteFiles().joinToString(prefix = "[", postfix = "]")
        }.getOrElse { "<list failed: ${it.javaClass.simpleName}>" }

        val parcelFileDescriptor = xposedModule.openRemoteFile(Preference.FILE_NAME)
        ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor).use { inputStream ->
            val size = inputStream.channel.size()
            if (size == 0L) {
                xlog(
                    "CHAIN_PROBE preference.json is empty; remoteFiles=$remoteFiles",
                    priority = Log.WARN,
                )
                return null
            }

            val preference = IgnoreUnknownKeysJson.decodeFromStream<Preference>(inputStream)
            xlog(
                "CHAIN_PROBE preference.json loaded size=$size " +
                    "leftEnabled=${preference.systemUI.statusBar.modifyStatusBarLeftPadding}, " +
                    "leftDp=${preference.systemUI.statusBar.statusBarLeftPaddingDp}, " +
                    "clockEnabled=${preference.systemUI.statusBar.setStatusBarClockFormat}, " +
                    "clockFormat=${preference.systemUI.statusBar.statusBarClockFormat.replace("\n", "\\n")}, " +
                    "maxIconsEnabled=${preference.systemUI.statusBar.modifyStatusBarMaxNotificationIcons}, " +
                    "maxIcons=${preference.systemUI.statusBar.statusBarMaxNotificationIcons}, " +
                    "launcherMemory=${preference.other.showMemoryUsageInRecents}; " +
                    "remoteFiles=$remoteFiles",
                priority = Log.INFO,
            )
            preference
        }
    } catch (t: java.io.FileNotFoundException) {
        xlog(
            "CHAIN_PROBE preference.json missing in remote storage",
            throwable = t,
            priority = Log.WARN,
        )
        null
    } catch (t: Throwable) {
        xlog("CHAIN_PROBE preference.json load failed", t, Log.ERROR)
        null
    }
}
