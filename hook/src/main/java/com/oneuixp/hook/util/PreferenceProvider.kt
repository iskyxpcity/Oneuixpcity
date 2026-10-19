package com.oneuixp.hook.util

import android.os.ParcelFileDescriptor
import io.github.libxposed.api.XposedModule
import com.oneuixp.common.IgnoreUnknownKeysJson
import com.oneuixp.common.Preference
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream

object PreferenceProvider {
    // Runtime preference reads are intentionally silent. User-triggered diagnostics live in
    // OneuiXP Settings instead of continuously writing LSPosed module logs.
    @OptIn(ExperimentalSerializationApi::class)
    context(xposedModule: XposedModule)
    fun loadPreference(): Preference? = try {
        val parcelFileDescriptor = xposedModule.openRemoteFile(Preference.FILE_NAME)
        ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor).use { inputStream ->
            if (inputStream.channel.size() == 0L) return null
            IgnoreUnknownKeysJson.decodeFromStream<Preference>(inputStream)
        }
    } catch (_: Throwable) {
        null
    }
}
