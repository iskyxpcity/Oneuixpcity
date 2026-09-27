package com.oneuixp.app.oneuix

import android.app.Application
import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.UserManager
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.ProductionPreferenceMigration
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonPrimitive

private object OneuiXXposedService {
    private const val TAG = "OneuiXP-OneUIXBridge"
    private val initialized = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var service: XposedService? = null
        private set

    fun initialize(application: Application) {
        if (!initialized.compareAndSet(false, true)) return
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(boundService: XposedService) {
                service = boundService
                synchronize(application)
            }

            override fun onServiceDied(deadService: XposedService) {
                if (service === deadService) service = null
            }
        })
    }

    fun synchronize(application: Application) {
        scope.launch {
            val unlocked =
                application.getSystemService(UserManager::class.java)?.isUserUnlocked == true
            if (!unlocked) return@launch

            runCatching {
                val current = application.oneuiXDataStore.data.first()
                if (!OneuiXPreferenceSerializer.synchronizeRemote(current)) {
                    error("libxposed remote preference service unavailable")
                }
            }.onFailure {
                Log.w(TAG, "Remote preference synchronization retry failed", it)
            }
        }
    }

    suspend fun synchronizeNow(application: Application): Boolean {
        val unlocked =
            application.getSystemService(UserManager::class.java)?.isUserUnlocked == true
        if (!unlocked) return false
        return runCatching {
            val current = application.oneuiXDataStore.data.first()
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
        true
    } catch (t: Throwable) {
        Log.w(TAG, "Remote preference synchronization failed", t)
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

object OneuiXPreferenceBridge {
    fun initialize(application: Application) {
        OneuiXXposedService.initialize(application)
    }

    fun ensureSynchronized(application: Application) {
        OneuiXXposedService.initialize(application)
        OneuiXXposedService.synchronize(application)
    }

    suspend fun synchronizeNow(application: Application): Boolean {
        OneuiXXposedService.initialize(application)
        return OneuiXXposedService.synchronizeNow(application)
    }
}
