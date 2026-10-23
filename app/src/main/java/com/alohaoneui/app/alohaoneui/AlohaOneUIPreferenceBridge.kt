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
    private const val GLOBAL_SHORTCUT_SYNC_STATE_PREFS = "aloha_oneui_global_shortcut_sync"
    private const val KEY_GLOBAL_SHORTCUT_SYNC_PENDING = "pending"
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

    fun deferGlobalShortcutSync(application: Application): Boolean {
        // Commit before queuing the DataStore mutation so its Serializer cannot
        // publish the newly edited shortcuts before the user confirms sync.
        val committed = application.getSharedPreferences(
            GLOBAL_SHORTCUT_SYNC_STATE_PREFS,
            Context.MODE_PRIVATE,
        ).edit().putBoolean(KEY_GLOBAL_SHORTCUT_SYNC_PENDING, true).commit()
        if (!committed) {
            Log.e(TAG, "Could not persist deferred global-shortcut sync marker")
            return false
        }
        initialize(application)
        return true
    }

    fun isGlobalShortcutSyncPending(): Boolean =
        application?.getSharedPreferences(GLOBAL_SHORTCUT_SYNC_STATE_PREFS, Context.MODE_PRIVATE)
            ?.getBoolean(KEY_GLOBAL_SHORTCUT_SYNC_PENDING, false) == true

    private fun setGlobalShortcutSyncPending(application: Application, pending: Boolean): Boolean =
        application.getSharedPreferences(GLOBAL_SHORTCUT_SYNC_STATE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_GLOBAL_SHORTCUT_SYNC_PENDING, pending)
            .commit()

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
            // behind every user mutation already queued by the UI.
            val current = application.alohaOneUIDataStore.updateData { it }
            Log.i(TAG, "CHAIN_PROBE synchronous local DataStore " + current.probeSummary())
            // Ordinary sync honours a pending global-shortcut confirmation.
            AlohaOneUIPreferenceSerializer.synchronizeRemote(current)
        }.onFailure {
            Log.w(TAG, "Synchronous remote preference flush failed", it)
        }.getOrDefault(false)
    }

    suspend fun synchronizeGlobalShortcutActionsNow(application: Application): Boolean {
        val unlocked =
            application.getSystemService(UserManager::class.java)?.isUserUnlocked == true
        if (!unlocked) return false
        return try {
            // Wait behind the toggle/action update which opened the dialog.
            val current = application.alohaOneUIDataStore.updateData { it }
            Log.i(TAG, "Explicit global-shortcut sync requested " + current.probeSummary())
            if (!AlohaOneUIPreferenceSerializer.synchronizeRemoteIncludingPending(current)) {
                Log.w(TAG, "Explicit global-shortcut sync failed; pending state retained")
                return false
            }
            if (!setGlobalShortcutSyncPending(application, false)) {
                Log.w(TAG, "Remote shortcut sync succeeded but pending marker could not be cleared")
                return false
            }
            // Refresh DataStore's local sync marker after clearing the deferred state.
            // The remote write above already succeeded; this second write only keeps the
            // local marker consistent and may safely be retried if the service disconnects.
            runCatching { application.alohaOneUIDataStore.updateData { it } }
                .onFailure { Log.w(TAG, "Local sync marker refresh failed after successful shortcut sync", it) }
            true
        } catch (failure: Throwable) {
            Log.w(TAG, "Explicit global-shortcut synchronization failed", failure)
            false
        }
    }
    /**
     * A configuration restore replaces the previous local configuration. It must not
     * inherit an older "wait for shortcut sync" marker, or restored shortcut settings
     * can be silently kept off the remote LSPosed preference file.
     */
    suspend fun synchronizeRestoredConfiguration(application: Application): Boolean {
        val unlocked =
            application.getSystemService(UserManager::class.java)?.isUserUnlocked == true
        if (!unlocked) return false

        return try {
            // Wait for the DataStore restore transaction before publishing the complete payload.
            val current = application.alohaOneUIDataStore.updateData { it }
            val clearedBeforeWrite = setGlobalShortcutSyncPending(application, false)
            if (!clearedBeforeWrite) {
                Log.w(TAG, "Could not clear previous deferred shortcut-sync marker before config restore")
            }

            val synchronized =
                AlohaOneUIPreferenceSerializer.synchronizeRemoteIncludingPending(current)
            if (!synchronized) {
                Log.w(TAG, "Configuration is restored locally; remote sync can retry when service reconnects")
                return false
            }

            if (!setGlobalShortcutSyncPending(application, false)) {
                Log.w(TAG, "Could not clear deferred shortcut-sync marker after config restore")
                return false
            }
            runCatching { application.alohaOneUIDataStore.updateData { it } }
                .onFailure { Log.w(TAG, "DataStore marker refresh failed after config restore", it) }
            true
        } catch (failure: Throwable) {
            Log.w(TAG, "Full configuration synchronization after restore failed", failure)
            false
        }
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

    private fun preserveActiveGlobalShortcuts(value: Preference): Preference {
        if (!AlohaOneUIXposedService.isGlobalShortcutSyncPending()) return value
        val activeShortcuts = readRemote()?.other?.globalShortcutActions
            ?: defaultValue.other.globalShortcutActions
        return value.copy(
            other = value.other.copy(globalShortcutActions = activeShortcuts),
        )
    }

    /** Normal preference writes keep pending global shortcuts local until explicit confirmation. */
    fun synchronizeRemote(value: Preference): Boolean =
        writeRemote(preserveActiveGlobalShortcuts(value))

    /** Explicit confirmation path: publish the complete current preference, including pending shortcuts. */
    fun synchronizeRemoteIncludingPending(value: Preference): Boolean = writeRemote(value)

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
            // If global shortcut changes are waiting for confirmation, recover/push all
            // other local preferences but preserve the last active global-shortcut payload.
            synchronizeRemote(local)
        } else if (localRaw == null && remote == null) {
            synchronizeRemote(defaultValue)
        }
        return selected
    }

    override suspend fun writeTo(t: Preference, output: OutputStream) {
        val pendingGlobalShortcutSync = AlohaOneUIXposedService.isGlobalShortcutSyncPending()
        val synchronized = synchronizeRemote(t) && !pendingGlobalShortcutSync
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

    fun deferGlobalShortcutSync(application: Application): Boolean =
        AlohaOneUIXposedService.deferGlobalShortcutSync(application)

    suspend fun synchronizeGlobalShortcutActionsNow(application: Application): Boolean {
        AlohaOneUIXposedService.initialize(application)
        return AlohaOneUIXposedService.synchronizeGlobalShortcutActionsNow(application)
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

    suspend fun synchronizeRestoredConfiguration(application: Application): Boolean {
        AlohaOneUIXposedService.initialize(application)
        return AlohaOneUIXposedService.synchronizeRestoredConfiguration(application)
    }
}
