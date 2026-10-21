package com.alohaoneui.app.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alohaoneui.app.alohaOneUIApp
import com.alohaoneui.app.alohaoneui.AlohaOneUIPreferenceBridge
import com.alohaoneui.app.alohaoneui.AlohaOneUISettingSpec
import com.alohaoneui.app.alohaoneui.AlohaOneUISettingType
import com.alohaoneui.app.alohaoneui.AlohaOneUIVisibility
import com.alohaoneui.app.alohaoneui.alohaOneUIDataStore
import com.alohaoneui.common.IgnoreUnknownKeysJson
import com.alohaoneui.common.ONE_UI_VERSION
import com.alohaoneui.common.PowerMenuAction
import com.alohaoneui.common.Preference
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
import com.alohaoneui.common.ProductionPreferenceMigration
import java.io.InputStream
import java.io.OutputStream

class AlohaOneUIToolsViewModel : ViewModel() {
    private val dataStore = alohaOneUIApp.alohaOneUIDataStore

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
        AlohaOneUIPreferenceBridge.preview(
            alohaOneUIApp,
            nextPreference(preference.value),
        )
    }

    /**
     * Compatible full preference backup.
     * The exported JSON deliberately contains only the Preference payload, not
     * AlohaOneUI's local synchronization marker.
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
        AlohaOneUIPreferenceBridge.synchronizeNow(alohaOneUIApp)
    }


    fun checked(preference: Preference, spec: AlohaOneUISettingSpec): Boolean {
        if (spec.type == AlohaOneUISettingType.PowerAction) {
            val name = spec.powerActionName ?: return false
            return PowerMenuAction.normalize(preference.systemUI.other.powerMenuActions)
                .firstOrNull { it.name == name }
                ?.visible
                ?: false
        }
        return value(preference, spec).toBooleanStrictOrNull() ?: false
    }

    fun value(preference: Preference, spec: AlohaOneUISettingSpec): String {
        if (spec.type == AlohaOneUISettingType.PowerAction) {
            return checked(preference, spec).toString()
        }
        return valueAt(preference, spec.path)
    }

    fun isVisible(
        preference: Preference,
        spec: AlohaOneUISettingSpec,
    ): Boolean = evaluateVisibility(preference, spec.visibility)

    private fun evaluateVisibility(
        preference: Preference,
        rule: AlohaOneUIVisibility,
    ): Boolean = when (rule) {
        AlohaOneUIVisibility.Always -> true
        is AlohaOneUIVisibility.Bool ->
            (valueAt(preference, rule.path).toBooleanStrictOrNull() ?: false) == rule.expected
        is AlohaOneUIVisibility.IntEquals ->
            valueAt(preference, rule.path).toIntOrNull() == rule.value
        is AlohaOneUIVisibility.StringContains ->
            valueAt(preference, rule.path).contains(rule.token)
        is AlohaOneUIVisibility.All ->
            rule.rules.all { evaluateVisibility(preference, it) }
        is AlohaOneUIVisibility.Not ->
            !evaluateVisibility(preference, rule.rule)
        is AlohaOneUIVisibility.AndroidAtLeast -> Build.VERSION.SDK_INT >= rule.sdk
        is AlohaOneUIVisibility.AndroidAtMost -> Build.VERSION.SDK_INT <= rule.sdk
        is AlohaOneUIVisibility.AndroidEquals -> Build.VERSION.SDK_INT == rule.sdk
        is AlohaOneUIVisibility.OneUiAtLeast -> ONE_UI_VERSION >= rule.version
        is AlohaOneUIVisibility.OneUiBelow -> ONE_UI_VERSION < rule.version
        is AlohaOneUIVisibility.ModelPrefix ->
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

    fun setChecked(spec: AlohaOneUISettingSpec, checked: Boolean) {
        viewModelScope.launch {
            dataStore.updateData { current ->
                if (spec.type == AlohaOneUISettingType.PowerAction) {
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
            AlohaOneUIPreferenceBridge.ensureSynchronized(alohaOneUIApp)
        }
    }

    fun setRawValue(spec: AlohaOneUISettingSpec, raw: String) {
        val next: JsonPrimitive = when (spec.type) {
            AlohaOneUISettingType.IntValue,
            AlohaOneUISettingType.ChoiceInt -> raw.toIntOrNull()?.let(::JsonPrimitive) ?: return

            AlohaOneUISettingType.FloatValue ->
                raw.toFloatOrNull()?.let(::JsonPrimitive) ?: return

            AlohaOneUISettingType.TextValue,
            AlohaOneUISettingType.ChoiceText -> JsonPrimitive(raw)

            AlohaOneUISettingType.Toggle,
            AlohaOneUISettingType.PowerAction -> return
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
            AlohaOneUIPreferenceBridge.ensureSynchronized(alohaOneUIApp)
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
