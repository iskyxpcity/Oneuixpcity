from pathlib import Path

runtime_path = Path('hook/src/main/java/io/github/soclear/oneuix/hook/systemui/StatusBarPowerRuntime.kt')
ui_path = Path('app/src/main/java/com/oneuixp/app/ui/screen/status/StatusPowerRuntimeControls.kt')
bridge_path = Path('common/src/main/java/io/github/soclear/oneuix/common/StatusBarRuntimeBridge.kt')

runtime = runtime_path.read_text()
ui = ui_path.read_text()
bridge = bridge_path.read_text()

# Bridge: dedicated explicit overlay command, separate from generic status preview JSON.
bridge = bridge.replace(
    '    const val EXTRA_STATUS_BAR_JSON = "status_bar_json"\n',
    '    const val EXTRA_STATUS_BAR_JSON = "status_bar_json"\n'
    '    const val ACTION_POWER_OVERLAY = "com.oneuixp.app.action.STATUS_BAR_POWER_OVERLAY"\n'
    '    const val EXTRA_POWER_OVERLAY_ON_TOP = "power_overlay_on_top"\n',
    1,
)

# Runtime: persisted preference owns restart restoration; previews can never alter overlay mode.
runtime = runtime.replace(
    '        var previous: Preference.SystemUI.StatusBarPowerRuntime? = null\n',
    '        var previousPersisted: Preference.SystemUI.StatusBarPowerRuntime? = null\n',
    1,
)
runtime = runtime.replace(
    '                if (current == previous) return@post\n                previous = current\n                mainHandler.post { updateRuntime(current) }\n',
    '                if (current == previousPersisted) return@post\n'
    '                previousPersisted = current\n'
    '                mainHandler.post { updateRuntime(current, preserveOverlay = false) }\n',
    1,
)
old_receiver = '''        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != StatusBarRuntimeBridge.ACTION) return
                val payload = intent.getStringExtra(
                    StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON
                ) ?: return
                background.post {
                    val statusBar = runCatching {
                        IgnoreUnknownKeysJson.decodeFromString(
                            Preference.SystemUI.StatusBar.serializer(),
                            payload,
                        )
                    }.getOrNull() ?: return@post
                    val current = statusBar.powerRuntime
                    previous = current
                    mainHandler.post { updateRuntime(current) }
                }
            }
        }
'''
new_receiver = '''        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                when (intent?.action) {
                    StatusBarRuntimeBridge.ACTION_POWER_OVERLAY -> {
                        val enabled = intent.getBooleanExtra(
                            StatusBarRuntimeBridge.EXTRA_POWER_OVERLAY_ON_TOP,
                            config.overlayOnTop,
                        )
                        mainHandler.post { updateOverlayOnTop(enabled) }
                    }
                    StatusBarRuntimeBridge.ACTION -> {
                        val payload = intent.getStringExtra(
                            StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON
                        ) ?: return
                        background.post {
                            val statusBar = runCatching {
                                IgnoreUnknownKeysJson.decodeFromString(
                                    Preference.SystemUI.StatusBar.serializer(),
                                    payload,
                                )
                            }.getOrNull() ?: return@post
                            // Generic preview payloads can be stale. They may update visual
                            // values, but never the mount layer. The overlay switch has its
                            // own explicit command above.
                            mainHandler.post {
                                updateRuntime(statusBar.powerRuntime, preserveOverlay = true)
                            }
                        }
                    }
                }
            }
        }
'''
if old_receiver not in runtime:
    raise SystemExit('receiver block not found')
runtime = runtime.replace(old_receiver, new_receiver, 1)
runtime = runtime.replace(
    '            val filter = IntentFilter(StatusBarRuntimeBridge.ACTION)\n',
    '            val filter = IntentFilter().apply {\n'
    '                addAction(StatusBarRuntimeBridge.ACTION)\n'
    '                addAction(StatusBarRuntimeBridge.ACTION_POWER_OVERLAY)\n'
    '            }\n',
    1,
)
old_update = '''    private fun updateRuntime(newConfig: Preference.SystemUI.StatusBarPowerRuntime) {
        config = sanitize(newConfig)
        val active = synchronized(holders) {
            holders.values.filter { it.statusBarView.isAttachedToWindow }
        }
        active.forEach { holder -> holder.root.post { applyConfig(holder, config) } }
        lastSample?.let { renderSample(it, config) }
        refreshSampling()
    }
'''
new_update = '''    private fun updateRuntime(
        newConfig: Preference.SystemUI.StatusBarPowerRuntime,
        preserveOverlay: Boolean,
    ) {
        val sanitized = sanitize(newConfig)
        config = if (preserveOverlay) {
            sanitized.copy(overlayOnTop = config.overlayOnTop)
        } else {
            sanitized
        }
        applyCurrentConfig()
    }

    private fun updateOverlayOnTop(enabled: Boolean) {
        if (config.overlayOnTop == enabled) {
            applyCurrentConfig()
            return
        }
        config = config.copy(overlayOnTop = enabled)
        applyCurrentConfig()
    }

    private fun applyCurrentConfig() {
        val active = synchronized(holders) {
            holders.values.filter { it.statusBarView.isAttachedToWindow }
        }
        active.forEach { holder -> holder.root.post { applyConfig(holder, config) } }
        lastSample?.let { renderSample(it, config) }
        refreshSampling()
    }
'''
if old_update not in runtime:
    raise SystemExit('updateRuntime block not found')
runtime = runtime.replace(old_update, new_update, 1)

runtime = runtime.replace(
    '        horizontalOffsetDp = source.horizontalOffsetDp.coerceIn(-150f, 150f),\n',
    '        horizontalOffsetDp = if (source.placementSide == "left") {\n'
    '            source.horizontalOffsetDp.coerceIn(0f, 180f)\n'
    '        } else {\n'
    '            source.horizontalOffsetDp.coerceIn(-180f, 0f)\n'
    '        },\n',
    1,
)

# UI: explicit overlay command; side-specific 180dp horizontal range.
ui = ui.replace('import android.view.HapticFeedbackConstants\n', 'import android.content.Intent\nimport android.view.HapticFeedbackConstants\n', 1)
ui = ui.replace('import androidx.compose.ui.platform.LocalView\n', 'import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.platform.LocalView\n', 1)
ui = ui.replace('import io.github.soclear.oneuix.common.Preference\n', 'import io.github.soclear.oneuix.common.Preference\nimport io.github.soclear.oneuix.common.StatusBarRuntimeBridge\n', 1)
ui = ui.replace(
    '    val powerIcon = ImageVector.vectorResource(id = R.drawable.battery)\n    val hapticView = LocalView.current\n',
    '    val powerIcon = ImageVector.vectorResource(id = R.drawable.battery)\n'
    '    val hapticView = LocalView.current\n'
    '    val context = LocalContext.current\n',
    1,
)
old_state = '''    var horizontalOffset by remember(powerRuntime.horizontalOffsetDp) {
        mutableFloatStateOf(powerRuntime.horizontalOffsetDp.coerceIn(-150f, 150f))
    }
'''
new_state = '''    var horizontalOffset by remember(powerRuntime.horizontalOffsetDp, powerRuntime.placementSide) {
        mutableFloatStateOf(
            if (powerRuntime.placementSide == "left") {
                powerRuntime.horizontalOffsetDp.coerceIn(0f, 180f)
            } else {
                powerRuntime.horizontalOffsetDp.coerceIn(-180f, 0f)
            }
        )
    }
'''
if old_state not in ui:
    raise SystemExit('horizontal state block not found')
ui = ui.replace(old_state, new_state, 1)
ui = ui.replace(
    '                        onSelectedIndexChange = {\n                            onChange(powerRuntime.copy(placementSide = placementKey(it)))\n                        },\n',
    '                        onSelectedIndexChange = {\n'
    '                            onChange(\n'
    '                                powerRuntime.copy(\n'
    '                                    placementSide = placementKey(it),\n'
    '                                    horizontalOffsetDp = 0f,\n'
    '                                )\n'
    '                            )\n'
    '                        },\n',
    1,
)
old_overlay = '''                        checked = powerRuntime.overlayOnTop,
                        onCheckedChange = {
                            onChange(powerRuntime.copy(overlayOnTop = it))
                        },
'''
new_overlay = '''                        checked = powerRuntime.overlayOnTop,
                        onCheckedChange = { enabled ->
                            context.sendBroadcast(
                                Intent(StatusBarRuntimeBridge.ACTION_POWER_OVERLAY).apply {
                                    setPackage("com.android.systemui")
                                    putExtra(
                                        StatusBarRuntimeBridge.EXTRA_POWER_OVERLAY_ON_TOP,
                                        enabled,
                                    )
                                }
                            )
                            onChange(powerRuntime.copy(overlayOnTop = enabled))
                        },
'''
if old_overlay not in ui:
    raise SystemExit('overlay switch block not found')
ui = ui.replace(old_overlay, new_overlay, 1)
ui = ui.replace(
    '                            valueRange = -150f..150f,\n',
    '                            valueRange = if (powerRuntime.placementSide == "left") {\n'
    '                                0f..180f\n'
    '                            } else {\n'
    '                                -180f..0f\n'
    '                            },\n',
    1,
)

runtime_path.write_text(runtime)
ui_path.write_text(ui)
bridge_path.write_text(bridge)
