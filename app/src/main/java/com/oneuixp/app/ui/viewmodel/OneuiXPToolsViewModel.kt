package com.oneuixp.app.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneuixp.app.oneuiXPApp
import com.oneuixp.app.oneuixp.OneuiXPPreferenceBridge
import com.oneuixp.app.oneuixp.OneuiXPSettingSpec
import com.oneuixp.app.oneuixp.OneuiXPSettingType
import com.oneuixp.app.oneuixp.OneuiXPVisibility
import com.oneuixp.app.oneuixp.oneuiXPDataStore
import com.oneuixp.common.IgnoreUnknownKeysJson
import com.oneuixp.common.ONE_UI_VERSION
import com.oneuixp.common.PowerMenuAction
import com.oneuixp.common.Preference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.oneuixp.common.ProductionPreferenceMigration
import java.io.InputStream
import java.io.OutputStream

class OneuiXPToolsViewModel : ViewModel() {
    private val dataStore = oneuiXPApp.oneuiXPDataStore

    /**
     * Material pages read generic settings by serialized property path. The
     * default Json encoder omits values equal to their schema defaults, which
     * made those fields look missing and forced sliders to their minimum.
     * Keep a path-reader encoder that always includes defaults.
     */
    private val pathValueJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val preference: StateFlow<Preference> = dataStore.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = Preference(),
    )

    fun updateData(nextPreference: (Preference) -> Preference) {
        viewModelScope.launch {
            // Serializer.writeTo already performs the remote LSPosed write and
            // publishes the runtime status-bar state. Do not immediately queue a
            // second full remote-file synchronization for the same user action.
            dataStore.updateData(nextPreference)
        }
    }

    /**
     * Runtime-only preview for high-frequency controls such as sliders.
     * This deliberately bypasses DataStore so dragging never queues disk writes.
     * The final value is persisted by the normal event on drag release.
     */
    fun previewData(nextPreference: (Preference) -> Preference) {
        OneuiXPPreferenceBridge.preview(
            oneuiXPApp,
            nextPreference(preference.value),
        )
    }

    /**
     * OneUIXP-Mod compatible full preference backup.
     * The exported JSON deliberately contains only the Preference payload, not
     * OneuiXP's local synchronization marker.
     */
    suspend fun backupTo(output: OutputStream) = withContext(Dispatchers.IO) {
        output.write(
            IgnoreUnknownKeysJson.encodeToString(
                Preference.serializer(),
                dataStore.data.first(),
            ).encodeToByteArray()
        )
    }

    /**
     * Restore through the same production migration decoder used by OneUIXP-Mod
     * so backups from older schemas remain importable.
     */
    suspend fun restoreFrom(input: InputStream) = withContext(Dispatchers.IO) {
        val restored = ProductionPreferenceMigration.decode(
            input.readBytes().decodeToString()
        )
        dataStore.updateData { restored }
        OneuiXPPreferenceBridge.synchronizeNow(oneuiXPApp)
    }


    fun checked(preference: Preference, spec: OneuiXPSettingSpec): Boolean {
        if (spec.type == OneuiXPSettingType.PowerAction) {
            val name = spec.powerActionName ?: return false
            return PowerMenuAction.normalize(preference.systemUI.other.powerMenuActions)
                .firstOrNull { it.name == name }
                ?.visible
                ?: false
        }
        return value(preference, spec).toBooleanStrictOrNull() ?: false
    }

    fun value(preference: Preference, spec: OneuiXPSettingSpec): String {
        if (spec.type == OneuiXPSettingType.PowerAction) {
            return checked(preference, spec).toString()
        }
        return valueAt(preference, spec.path)
    }

    fun isVisible(
        preference: Preference,
        spec: OneuiXPSettingSpec,
    ): Boolean = evaluateVisibility(preference, spec.visibility)

    private fun evaluateVisibility(
        preference: Preference,
        rule: OneuiXPVisibility,
    ): Boolean = when (rule) {
        OneuiXPVisibility.Always -> true
        is OneuiXPVisibility.Bool ->
            (valueAt(preference, rule.path).toBooleanStrictOrNull() ?: false) == rule.expected
        is OneuiXPVisibility.IntEquals ->
            valueAt(preference, rule.path).toIntOrNull() == rule.value
        is OneuiXPVisibility.StringContains ->
            valueAt(preference, rule.path).contains(rule.token)
        is OneuiXPVisibility.All ->
            rule.rules.all { evaluateVisibility(preference, it) }
        is OneuiXPVisibility.Not ->
            !evaluateVisibility(preference, rule.rule)
        is OneuiXPVisibility.AndroidAtLeast -> Build.VERSION.SDK_INT >= rule.sdk
        is OneuiXPVisibility.AndroidAtMost -> Build.VERSION.SDK_INT <= rule.sdk
        is OneuiXPVisibility.AndroidEquals -> Build.VERSION.SDK_INT == rule.sdk
        is OneuiXPVisibility.OneUiAtLeast -> ONE_UI_VERSION >= rule.version
        is OneuiXPVisibility.OneUiBelow -> ONE_UI_VERSION < rule.version
        is OneuiXPVisibility.ModelPrefix ->
            Build.MODEL.startsWith(rule.prefix, ignoreCase = true)
    }

    private fun valueAt(
        preference: Preference,
        path: String,
    ): String {
        val root = pathValueJson.encodeToJsonElement(
            Preference.serializer(),
            preference,
        )
        var current: JsonElement = root
        for (key in path.split('.')) {
            current = (current as? JsonObject)?.get(key) ?: return ""
        }
        return current.jsonPrimitive.content
    }

    fun setChecked(spec: OneuiXPSettingSpec, checked: Boolean) {
        viewModelScope.launch {
            dataStore.updateData { current ->
                if (spec.type == OneuiXPSettingType.PowerAction) {
                    val name = spec.powerActionName ?: return@updateData current
                    val actions = PowerMenuAction.normalize(
                        current.systemUI.other.powerMenuActions
                    ).map { action ->
                        if (action.name == name) action.copy(visible = checked) else action
                    }
                    current.copy(
                        systemUI = current.systemUI.copy(
                            other = current.systemUI.other.copy(
                                powerMenuActions = actions,
                            ),
                        ),
                    )
                } else {
                    current.withJsonValue(spec.path, JsonPrimitive(checked))
                }
            }
            OneuiXPPreferenceBridge.ensureSynchronized(oneuiXPApp)
        }
    }

    fun setRawValue(spec: OneuiXPSettingSpec, raw: String) {
        val next: JsonPrimitive = when (spec.type) {
            OneuiXPSettingType.IntValue,
            OneuiXPSettingType.ChoiceInt -> raw.toIntOrNull()?.let(::JsonPrimitive) ?: return

            OneuiXPSettingType.FloatValue ->
                raw.toFloatOrNull()?.let(::JsonPrimitive) ?: return

            OneuiXPSettingType.TextValue,
            OneuiXPSettingType.ChoiceText -> JsonPrimitive(raw)

            OneuiXPSettingType.Toggle,
            OneuiXPSettingType.PowerAction -> return
        }

        viewModelScope.launch {
            dataStore.updateData { current ->
                var updated = current.withJsonValue(spec.path, next)
                if (
                    spec.path == "other.watchPairingConnectionMode" &&
                    raw.toIntOrNull() != 1
                ) {
                    updated = updated.withJsonValue(
                        "other.supplementChinaWearOsGms",
                        JsonPrimitive(false),
                    )
                }
                updated
            }
            OneuiXPPreferenceBridge.ensureSynchronized(oneuiXPApp)
        }
    }

    private fun Preference.withJsonValue(
        path: String,
        value: JsonElement,
    ): Preference {
        val root = IgnoreUnknownKeysJson.encodeToJsonElement(
            Preference.serializer(),
            this,
        ).jsonObject
        val patched = replaceNested(root, path.split('.'), value)
        return IgnoreUnknownKeysJson.decodeFromJsonElement(
            Preference.serializer(),
            patched,
        )
    }

    private fun replaceNested(
        source: JsonObject,
        keys: List<String>,
        value: JsonElement,
    ): JsonObject {
        val key = keys.firstOrNull() ?: return source
        val updated = source.toMutableMap()
        if (keys.size == 1) {
            updated[key] = value
        } else {
            val child = source[key] as? JsonObject ?: JsonObject(emptyMap())
            updated[key] = replaceNested(child, keys.drop(1), value)
        }
        return JsonObject(updated)
    }
}
