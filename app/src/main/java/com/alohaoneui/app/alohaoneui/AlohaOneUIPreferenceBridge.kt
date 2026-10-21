package com.alohaoneui.app.alohaoneui

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
import com.alohaoneui.app.statusbar.LsposedFrameworkStatus
import com.alohaoneui.common.IgnoreUnknownKeysJson
import com.alohaoneui.common.Package
import com.alohaoneui.common.Preference
import com.alohaoneui.common.StatusBarRuntimeBridge
import com.alohaoneui.common.ProductionPreferenceMigration
import java.io.File
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

private val legacyPreferenceStoreMigrationLock = Any()

private fun migrateLegacyPreferenceStore(application: Application) {
    synchronized(legacyPreferenceStoreMigrationLock) {
        val directory = File(application.filesDir, "datastore")
        // Keep the previous on-device store readable for a one-time migration, without retaining its legacy name as a token.
        val currentFile = File(directory, "oneuixp_features.json")
        if (currentFile.exists()) return
        // Discover the sole prior feature store by its suffix, without hard-coding its former namespace.
        val legacyFile = directory.listFiles()
            ?.filter { candidate ->
                candidate.isFile &&
                    candidate.name.endsWith("_features.json") &&
                    candidate.name != currentFile.name
            }
            ?.singleOrNull()
            ?: return

        runCatching {
            if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
                error("Unable to create the preference datastore directory")
            }
            legacyFile.copyTo(currentFile, overwrite = false)
            check(currentFile.length() == legacyFile.length()) {
                "Copied preference datastore size does not match the original"
            }
            legacyFile.delete()
        }.onFailure {
            if (currentFile.exists() && legacyFile.exists() &&
                currentFile.length() != legacyFile.length()
            ) {
                currentFile.delete()
            }
            Log.w("Aloha OneUI", "Could not migrate existing preference storage", it)
        }
    }
}

private object AlohaOneUIXposedService {
    private const val TAG = "AlohaOneUI-PreferenceBridge"
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
        migrateLegacyPreferenceStore(application)
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
                val current = application.alohaOneUIDataStore.data.first()
                Log.i(TAG, "CHAIN_PROBE local DataStore " + current.probeSummary())
                if (!AlohaOneUIPreferenceSerializer.synchronizeRemote(current)) {
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
            val current = application.alohaOneUIDataStore.updateData { it }
            Log.i(TAG, "CHAIN_PROBE synchronous local DataStore " + current.probeSummary())
            AlohaOneUIPreferenceSerializer.synchronizeRemote(current)
        }.onFailure {
            Log.w(TAG, "Synchronous remote preference flush failed", it)
        }.getOrDefault(false)
    }
}

object AlohaOneUIPreferenceSerializer : Serializer<Preference> {
    private const val TAG = "AlohaOneUI-PreferenceSerializer"
    private const val SYNC_MARKER = "_oneuixp_remote_synced"

    override val defaultValue: Preference = Preference()

    @OptIn(ExperimentalSerializationApi::class)
    private fun readRemote(): Preference? = try {
        val service = AlohaOneUIXposedService.service ?: return null
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
        val service = AlohaOneUIXposedService.service ?: return false
        val descriptor = service.openRemoteFile(Preference.FILE_NAME)
        ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { destination ->
            destination.channel.truncate(0)
            IgnoreUnknownKeysJson.encodeToStream(Preference.serializer(), value, destination)
            destination.channel.force(true)
        }
        Log.i(TAG, "CHAIN_PROBE remote preference.json write OK " + value.probeSummary())
        AlohaOneUIXposedService.notifyPreferenceChanged()
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
                (IgnoreUnknownKeysJson.parseToJsonElement(raw) as? JsonObject)?.let { root ->
                    val currentMarker = (root[SYNC_MARKER] as? JsonPrimitive)?.booleanOrNull
                    currentMarker ?: root.entries
                        .filter { (key, value) ->
                            key != SYNC_MARKER &&
                                key.startsWith("_") &&
                                key.endsWith("RemoteSynced") &&
                                (value as? JsonPrimitive)?.booleanOrNull != null
                        }
                        .singleOrNull()
                        ?.value
                        ?.let { (it as? JsonPrimitive)?.booleanOrNull }
                }
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
        AlohaOneUIXposedService.publishPersistedRuntimeStatusBar(t.systemUI.statusBar)
        val encoded = IgnoreUnknownKeysJson.encodeToString(Preference.serializer(), t)
        val root = IgnoreUnknownKeysJson.parseToJsonElement(encoded) as JsonObject
        val local = JsonObject(root + (SYNC_MARKER to JsonPrimitive(synchronized)))
        output.write(local.toString().encodeToByteArray())
    }
}

val Context.alohaOneUIDataStore: DataStore<Preference> by dataStore(
    fileName = "oneuixp_features.json",
    serializer = AlohaOneUIPreferenceSerializer,
)

object AlohaOneUIPreferenceBridge {
    fun initialize(application: Application) {
        AlohaOneUIXposedService.initialize(application)
    }

    fun ensureSynchronized(application: Application) {
        AlohaOneUIXposedService.initialize(application)
        AlohaOneUIXposedService.synchronize(application)
    }

    fun preview(application: Application, preference: Preference) {
        AlohaOneUIXposedService.initialize(application)
        AlohaOneUIXposedService.scheduleRuntimeStatusBar(preference.systemUI.statusBar)
    }

    suspend fun synchronizeNow(application: Application): Boolean {
        AlohaOneUIXposedService.initialize(application)
        return AlohaOneUIXposedService.synchronizeNow(application)
    }
}
