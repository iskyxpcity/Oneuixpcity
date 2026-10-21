package com.oneuialoha.app.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneuialoha.app.oneUIAlohaApp
import com.oneuialoha.app.oneuialoha.OneUIAlohaPreferenceBridge
import com.oneuialoha.app.oneuialoha.OneUIAlohaSettingSpec
import com.oneuialoha.app.oneuialoha.OneUIAlohaSettingType
import com.oneuialoha.app.oneuialoha.OneUIAlohaVisibility
import com.oneuialoha.app.oneuialoha.oneUIAlohaDataStore
import com.oneuialoha.common.IgnoreUnknownKeysJson
import com.oneuialoha.common.ONE_UI_VERSION
import com.oneuialoha.common.PowerMenuAction
import com.oneuialoha.common.Preference
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
import com.oneuialoha.common.ProductionPreferenceMigration
import java.io.InputStream
import java.io.OutputStream

class OneUIAlohaToolsViewModel : ViewModel() {
    private val dataStore = oneUIAlohaApp.oneUIAlohaDataStore

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
        OneUIAlohaPreferenceBridge.preview(
            oneUIAlohaApp,
            nextPreference(preference.value),
        )
    }

    /**
     * Compatible full preference backup.
     * The exported JSON deliberately contains only the Preference payload, not
     * OneUIAloha's local synchronization marker.
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
     * Restore through the same production migration decoder used by the app
     * so backups from older schemas remain importable.
     */
    suspend fun restoreFrom(input: InputStream) = withContext(Dispatchers.IO) {
        val restored = ProductionPreferenceMigration.decode(
            input.readBytes().decodeToString()
        )
        dataStore.updateData { restored }
        OneUIAlohaPreferenceBridge.synchronizeNow(oneUIAlohaApp)
    }


    fun checked(preference: Preference, spec: OneUIAlohaSettingSpec): Boolean {
        if (spec.type == OneUIAlohaSettingType.PowerAction) {
            val name = spec.powerActionName ?: return false
            return PowerMenuAction.normalize(preference.systemUI.other.powerMenuActions)
                .firstOrNull { it.name == name }
                ?.visible
                ?: false
        }
        return value(preference, spec).toBooleanStrictOrNull() ?: false
    }

    fun value(preference: Preference, spec: OneUIAlohaSettingSpec): String {
        if (spec.type == OneUIAlohaSettingType.PowerAction) {
            return checked(preference, spec).toString()
        }
        return valueAt(preference, spec.path)
    }

    fun isVisible(
        preference: Preference,
        spec: OneUIAlohaSettingSpec,
    ): Boolean = evaluateVisibility(preference, spec.visibility)

    private fun evaluateVisibility(
        preference: Preference,
        rule: OneUIAlohaVisibility,
    ): Boolean = when (rule) {
        OneUIAlohaVisibility.Always -> true
        is OneUIAlohaVisibility.Bool ->
            (valueAt(preference, rule.path).toBooleanStrictOrNull() ?: false) == rule.expected
        is OneUIAlohaVisibility.IntEquals ->
            valueAt(preference, rule.path).toIntOrNull() == rule.value
        is OneUIAlohaVisibility.StringContains ->
            valueAt(preference, rule.path).contains(rule.token)
        is OneUIAlohaVisibility.All ->
            rule.rules.all { evaluateVisibility(preference, it) }
        is OneUIAlohaVisibility.Not ->
            !evaluateVisibility(preference, rule.rule)
        is OneUIAlohaVisibility.AndroidAtLeast -> Build.VERSION.SDK_INT >= rule.sdk
        is OneUIAlohaVisibility.AndroidAtMost -> Build.VERSION.SDK_INT <= rule.sdk
        is OneUIAlohaVisibility.AndroidEquals -> Build.VERSION.SDK_INT == rule.sdk
        is OneUIAlohaVisibility.OneUiAtLeast -> ONE_UI_VERSION >= rule.version
        is OneUIAlohaVisibility.OneUiBelow -> ONE_UI_VERSION < rule.version
        is OneUIAlohaVisibility.ModelPrefix ->
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

    fun setChecked(spec: OneUIAlohaSettingSpec, checked: Boolean) {
        viewModelScope.launch {
            dataStore.updateData { current ->
                if (spec.type == OneUIAlohaSettingType.PowerAction) {
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
            OneUIAlohaPreferenceBridge.ensureSynchronized(oneUIAlohaApp)
        }
    }

    fun setRawValue(spec: OneUIAlohaSettingSpec, raw: String) {
        val next: JsonPrimitive = when (spec.type) {
            OneUIAlohaSettingType.IntValue,
            OneUIAlohaSettingType.ChoiceInt -> raw.toIntOrNull()?.let(::JsonPrimitive) ?: return

            OneUIAlohaSettingType.FloatValue ->
                raw.toFloatOrNull()?.let(::JsonPrimitive) ?: return

            OneUIAlohaSettingType.TextValue,
            OneUIAlohaSettingType.ChoiceText -> JsonPrimitive(raw)

            OneUIAlohaSettingType.Toggle,
            OneUIAlohaSettingType.PowerAction -> return
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
            OneUIAlohaPreferenceBridge.ensureSynchronized(oneUIAlohaApp)
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
