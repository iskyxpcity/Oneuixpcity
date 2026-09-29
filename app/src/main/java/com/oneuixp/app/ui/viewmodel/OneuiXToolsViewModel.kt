package com.oneuixp.app.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneuixp.app.oneuiXPApp
import com.oneuixp.app.oneuix.OneuiXPreferenceBridge
import com.oneuixp.app.oneuix.OneuiXSettingSpec
import com.oneuixp.app.oneuix.OneuiXSettingType
import com.oneuixp.app.oneuix.OneuiXVisibility
import com.oneuixp.app.oneuix.oneuiXDataStore
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.ONE_UI_VERSION
import io.github.soclear.oneuix.common.PowerMenuAction
import io.github.soclear.oneuix.common.Preference
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
import io.github.soclear.oneuix.common.ProductionPreferenceMigration
import java.io.InputStream
import java.io.OutputStream

class OneuiXToolsViewModel : ViewModel() {
    private val dataStore = oneuiXPApp.oneuiXDataStore

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
            dataStore.updateData(nextPreference)
            OneuiXPreferenceBridge.ensureSynchronized(oneuiXPApp)
        }
    }

    /**
     * OneUIX-Mod compatible full preference backup.
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
     * Restore through the same production migration decoder used by OneUIX-Mod
     * so backups from older schemas remain importable.
     */
    suspend fun restoreFrom(input: InputStream) = withContext(Dispatchers.IO) {
        val restored = ProductionPreferenceMigration.decode(
            input.readBytes().decodeToString()
        )
        dataStore.updateData { restored }
        OneuiXPreferenceBridge.synchronizeNow(oneuiXPApp)
    }


    fun checked(preference: Preference, spec: OneuiXSettingSpec): Boolean {
        if (spec.type == OneuiXSettingType.PowerAction) {
            val name = spec.powerActionName ?: return false
            return PowerMenuAction.normalize(preference.systemUI.other.powerMenuActions)
                .firstOrNull { it.name == name }
                ?.visible
                ?: false
        }
        return value(preference, spec).toBooleanStrictOrNull() ?: false
    }

    fun value(preference: Preference, spec: OneuiXSettingSpec): String {
        if (spec.type == OneuiXSettingType.PowerAction) {
            return checked(preference, spec).toString()
        }
        return valueAt(preference, spec.path)
    }

    fun isVisible(
        preference: Preference,
        spec: OneuiXSettingSpec,
    ): Boolean = evaluateVisibility(preference, spec.visibility)

    private fun evaluateVisibility(
        preference: Preference,
        rule: OneuiXVisibility,
    ): Boolean = when (rule) {
        OneuiXVisibility.Always -> true
        is OneuiXVisibility.Bool ->
            (valueAt(preference, rule.path).toBooleanStrictOrNull() ?: false) == rule.expected
        is OneuiXVisibility.IntEquals ->
            valueAt(preference, rule.path).toIntOrNull() == rule.value
        is OneuiXVisibility.StringContains ->
            valueAt(preference, rule.path).contains(rule.token)
        is OneuiXVisibility.All ->
            rule.rules.all { evaluateVisibility(preference, it) }
        is OneuiXVisibility.Not ->
            !evaluateVisibility(preference, rule.rule)
        is OneuiXVisibility.AndroidAtLeast -> Build.VERSION.SDK_INT >= rule.sdk
        is OneuiXVisibility.AndroidAtMost -> Build.VERSION.SDK_INT <= rule.sdk
        is OneuiXVisibility.AndroidEquals -> Build.VERSION.SDK_INT == rule.sdk
        is OneuiXVisibility.OneUiAtLeast -> ONE_UI_VERSION >= rule.version
        is OneuiXVisibility.OneUiBelow -> ONE_UI_VERSION < rule.version
        is OneuiXVisibility.ModelPrefix ->
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

    fun setChecked(spec: OneuiXSettingSpec, checked: Boolean) {
        viewModelScope.launch {
            dataStore.updateData { current ->
                if (spec.type == OneuiXSettingType.PowerAction) {
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
            OneuiXPreferenceBridge.ensureSynchronized(oneuiXPApp)
        }
    }

    fun setRawValue(spec: OneuiXSettingSpec, raw: String) {
        val next: JsonPrimitive = when (spec.type) {
            OneuiXSettingType.IntValue,
            OneuiXSettingType.ChoiceInt -> raw.toIntOrNull()?.let(::JsonPrimitive) ?: return

            OneuiXSettingType.FloatValue ->
                raw.toFloatOrNull()?.let(::JsonPrimitive) ?: return

            OneuiXSettingType.TextValue,
            OneuiXSettingType.ChoiceText -> JsonPrimitive(raw)

            OneuiXSettingType.Toggle,
            OneuiXSettingType.PowerAction -> return
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
            OneuiXPreferenceBridge.ensureSynchronized(oneuiXPApp)
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
