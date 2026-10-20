package com.oneuixp.app.oneuixp

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.UserManager
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import io.github.libxposed.service.XposedService
import com.oneuixp.app.statusbar.LsposedFrameworkStatus
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarRuntimeBridge
import io.github.soclear.oneuix.common.ProductionPreferenceMigration
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonPrimitive

private fun Preference.probeSummary(): String =
    "leftEnabled=${systemUI.statusBar.modifyStatusBarLeftPadding}, " +
        "leftDp=${systemUI.statusBar.statusBarLeftPaddingDp}, " +
        "clockEnabled=${systemUI.statusBar.setStatusBarClockFormat}, " +
        "clockFormat=${systemUI.statusBar.statusBarClockFormat.replace("\n", "\\n")}, " +
        "maxIconsEnabled=${systemUI.statusBar.modifyStatusBarMaxNotificationIcons}, " +
        "maxIcons=${systemUI.statusBar.statusBarMaxNotificationIcons}, " +
        "launcherMemory=${other.showMemoryUsageInRecents}"

private object OneuiXXposedService {
    private const val TAG = "OneuiXP-OneUIXBridge"
    private const val PREFERENCE_CHANGED_PATH = "preferences"
    private val initialized = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val RUNTIME_PREVIEW_INTERVAL_MS = 32L
    private val runtimePreviewLock = Any()
    private var pendingRuntimeStatusBar: Preference.SystemUI.StatusBar? = null
    private var runtimePreviewJob: Job? = null
    @Volatile
    private var application: Application? = null

    @Volatile
    var service: XposedService? = null
        private set

    fun initialize(application: Application) {
        this.application = application
        if (!initialized.compareAndSet(false, true)) return

        // libxposed-service has one global service-listener slot. Register it
        // only in LsposedFrameworkStatus and observe that shared Binder here.
        LsposedFrameworkStatus.initialize()
        LsposedFrameworkStatus.observeService { boundService ->
            service = boundService
            if (boundService != null) {
                Log.i(TAG, "CHAIN_PROBE shared XposedService received; scheduling preference sync")
                synchronize(application)
            } else {
                Log.w(TAG, "CHAIN_PROBE shared XposedService unavailable")
            }
        }
    }

    private fun sendRuntimeStatusBar(current: Preference.SystemUI.StatusBar) {
        val app = application ?: return
        val payload = runCatching {
            IgnoreUnknownKeysJson.encodeToString(
                Preference.SystemUI.StatusBar.serializer(),
                current,
            )
        }.getOrElse {
            Log.w(TAG, "Runtime status-bar encode failed", it)
            return
        }

        runCatching {
            app.sendBroadcast(
                Intent(StatusBarRuntimeBridge.ACTION)
                    .setPackage(Package.SYSTEMUI)
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    .putExtra(StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON, payload)
            )
        }.onFailure {
            Log.w(TAG, "Runtime status-bar broadcast failed", it)
        }
    }

    fun scheduleRuntimeStatusBar(current: Preference.SystemUI.StatusBar) {
        synchronized(runtimePreviewLock) {
            pendingRuntimeStatusBar = current
            if (runtimePreviewJob?.isActive == true) return

            runtimePreviewJob = scope.launch {
                while (true) {
                    delay(RUNTIME_PREVIEW_INTERVAL_MS)
                    val next = synchronized(runtimePreviewLock) {
                        pendingRuntimeStatusBar.also {
                            pendingRuntimeStatusBar = null
                        }
                    }
                    if (next == null) {
                        synchronized(runtimePreviewLock) {
                            runtimePreviewJob = null
                        }
                        return@launch
                    }
                    sendRuntimeStatusBar(next)

                    val finished = synchronized(runtimePreviewLock) {
                        if (pendingRuntimeStatusBar == null) {
                            runtimePreviewJob = null
                            true
                        } else {
                            false
                        }
                    }
                    if (finished) return@launch
                }
            }
        }
    }

    fun publishPersistedRuntimeStatusBar(current: Preference.SystemUI.StatusBar) {
        synchronized(runtimePreviewLock) {
            pendingRuntimeStatusBar = null
            runtimePreviewJob?.cancel()
            runtimePreviewJob = null
        }
        sendRuntimeStatusBar(current)
    }

    fun synchronize(application: Application) {
        scope.launch {
            val unlocked =
                application.getSystemService(UserManager::class.java)?.isUserUnlocked == true
            if (!unlocked) return@launch

            runCatching {
                val current = application.oneuiXDataStore.data.first()
                Log.i(TAG, "CHAIN_PROBE local DataStore " + current.probeSummary())
                if (!OneuiXPreferenceSerializer.synchronizeRemote(current)) {
                    error("libxposed remote preference service unavailable")
                }
            }.onFailure {
                Log.w(TAG, "Remote preference synchronization retry failed", it)
            }
        }
    }

    fun notifyPreferenceChanged() {
        val app = application ?: return
        runCatching {
            app.contentResolver.notifyChange(
                Uri.parse("content://${app.packageName}.statusbridge/$PREFERENCE_CHANGED_PATH"),
                null,
            )
        }
    }

    suspend fun synchronizeNow(application: Application): Boolean {
        val unlocked =
            application.getSystemService(UserManager::class.java)?.isUserUnlocked == true
        if (!unlocked) return false
        return runCatching {
            // updateData is serialized by DataStore. An identity transaction here waits
            // behind every slider/switch mutation already queued by the UI, so a reboot
            // can never flush an older snapshot just because the last UI write is pending.
            val current = application.oneuiXDataStore.updateData { it }
            Log.i(TAG, "CHAIN_PROBE synchronous local DataStore " + current.probeSummary())
            OneuiXPreferenceSerializer.synchronizeRemote(current)
        }.onFailure {
            Log.w(TAG, "Synchronous remote preference flush failed", it)
        }.getOrDefault(false)
    }
}

object OneuiXPreferenceSerializer : Serializer<Preference> {
    private const val TAG = "OneuiXP-OneUIXPrefs"
    private const val SYNC_MARKER = "_oneuiXpRemoteSynced"

    override val defaultValue: Preference = Preference()

    @OptIn(ExperimentalSerializationApi::class)
    private fun readRemote(): Preference? = try {
        val service = OneuiXXposedService.service ?: return null
        val descriptor = service.openRemoteFile(Preference.FILE_NAME)
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { source ->
            if (source.channel.size() == 0L) null
            else ProductionPreferenceMigration.decode(source.readBytes().decodeToString())
        }
    } catch (_: java.io.FileNotFoundException) {
        null
    } catch (t: Throwable) {
        Log.w(TAG, "Remote preferences unavailable; preserving local state", t)
        null
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun writeRemote(value: Preference): Boolean = try {
        val service = OneuiXXposedService.service ?: return false
        val descriptor = service.openRemoteFile(Preference.FILE_NAME)
        ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { destination ->
            destination.channel.truncate(0)
            IgnoreUnknownKeysJson.encodeToStream(Preference.serializer(), value, destination)
            destination.channel.force(true)
        }
        Log.i(TAG, "CHAIN_PROBE remote preference.json write OK " + value.probeSummary())
        OneuiXXposedService.notifyPreferenceChanged()
        true
    } catch (t: Throwable) {
        Log.w(TAG, "CHAIN_PROBE remote preference.json write FAILED", t)
        false
    }

    fun synchronizeRemote(value: Preference): Boolean = writeRemote(value)

    override suspend fun readFrom(input: InputStream): Preference {
        val localBytes = runCatching { input.readBytes() }.getOrNull()
        val localRaw = localBytes?.takeIf { it.isNotEmpty() }?.decodeToString()
        val local = localRaw?.let { raw ->
            runCatching { ProductionPreferenceMigration.decode(raw) }.getOrNull()
        }
        val syncMarker = localRaw?.let { raw ->
            runCatching {
                (IgnoreUnknownKeysJson.parseToJsonElement(raw) as? JsonObject)
                    ?.get(SYNC_MARKER)?.jsonPrimitive?.booleanOrNull
            }.getOrNull()
        }
        val unsynced = syncMarker == false
        val productionLocal = local != null && syncMarker == null
        val remote = readRemote()
        val selected = ProductionPreferenceMigration.select(
            local = local,
            remote = remote,
            localUnsynced = unsynced,
            localFromProduction = productionLocal,
        )
        if (local != null && selected == local && local != remote &&
            (unsynced || productionLocal || remote == null || remote == defaultValue)
        ) {
            writeRemote(local)
        } else if (localRaw == null && remote == null) {
            writeRemote(defaultValue)
        }
        return selected
    }

    override suspend fun writeTo(t: Preference, output: OutputStream) {
        val synchronized = writeRemote(t)
        OneuiXXposedService.publishPersistedRuntimeStatusBar(t.systemUI.statusBar)
        val encoded = IgnoreUnknownKeysJson.encodeToString(Preference.serializer(), t)
        val root = IgnoreUnknownKeysJson.parseToJsonElement(encoded) as JsonObject
        val local = JsonObject(root + (SYNC_MARKER to JsonPrimitive(synchronized)))
        output.write(local.toString().encodeToByteArray())
    }
}

val Context.oneuiXDataStore: DataStore<Preference> by dataStore(
    fileName = "oneuix_features.json",
    serializer = OneuiXPreferenceSerializer,
)

object OneuiXPPreferenceBridge {
    fun initialize(application: Application) {
        OneuiXXposedService.initialize(application)
    }

    fun ensureSynchronized(application: Application) {
        OneuiXXposedService.initialize(application)
        OneuiXXposedService.synchronize(application)
    }

    fun preview(application: Application, preference: Preference) {
        OneuiXXposedService.initialize(application)
        OneuiXXposedService.scheduleRuntimeStatusBar(preference.systemUI.statusBar)
    }

    suspend fun synchronizeNow(application: Application): Boolean {
        OneuiXXposedService.initialize(application)
        return OneuiXXposedService.synchronizeNow(application)
    }
}
