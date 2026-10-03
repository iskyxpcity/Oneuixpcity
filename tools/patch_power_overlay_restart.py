from pathlib import Path

runtime_path = Path('hook/src/main/java/io/github/soclear/oneuix/hook/systemui/StatusBarPowerRuntime.kt')
ui_path = Path('app/src/main/java/com/oneuixp/app/ui/screen/status/StatusPowerRuntimeControls.kt')

runtime = runtime_path.read_text()
ui = ui_path.read_text()

runtime = runtime.replace(
    '        var lastLayoutKey: String = "",\n        var awaitingOverlayLayout: Boolean = false,\n',
    '        var lastLayoutKey: String = "",\n        var awaitingOverlayLayout: Boolean = false,\n        var overlayMountRetries: Int = 0,\n',
    1,
)
runtime = runtime.replace(
    '        horizontalOffsetDp = source.horizontalOffsetDp.coerceIn(-300f, 300f),\n        verticalOffsetDp = source.verticalOffsetDp.coerceIn(-60f, 60f),\n',
    '        horizontalOffsetDp = source.horizontalOffsetDp.coerceIn(-150f, 150f),\n        verticalOffsetDp = source.verticalOffsetDp.coerceIn(-30f, 30f),\n',
    1,
)
old_wait = '''    private fun waitForOverlayInlineLayout(holder: Holder) {
        if (holder.awaitingOverlayLayout) return
        holder.awaitingOverlayLayout = true
        holder.root.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int,
            ) {
                if (right - left <= 0 || bottom - top <= 0) return
                v.removeOnLayoutChangeListener(this)
                holder.awaitingOverlayLayout = false
                v.post { applyConfig(holder, config) }
            }
        })
        holder.root.requestLayout()
        holder.inlineParent.requestLayout()
    }
'''
new_wait = '''    private fun scheduleOverlayMountRetry(holder: Holder) {
        if (holder.awaitingOverlayLayout || holder.overlayMode) return
        if (!config.enabled || !config.overlayOnTop) {
            holder.overlayMountRetries = 0
            return
        }
        if (holder.overlayMountRetries >= 12) return
        holder.awaitingOverlayLayout = true
        holder.overlayMountRetries += 1
        val delay = (holder.overlayMountRetries * 16L).coerceAtMost(120L)
        holder.root.postDelayed({
            holder.awaitingOverlayLayout = false
            if (!holder.statusBarView.isAttachedToWindow) return@postDelayed
            if (!config.enabled || !config.overlayOnTop || holder.overlayMode) {
                holder.overlayMountRetries = 0
                return@postDelayed
            }
            applyConfig(holder, config)
        }, delay)
    }
'''
if old_wait not in runtime:
    raise SystemExit('waitForOverlayInlineLayout block not found')
runtime = runtime.replace(old_wait, new_wait, 1)
old_mount = '''        if (current.overlayOnTop) {
            // True overlay uses only fixed host-edge coordinates plus the user X/Y values.
            // Do not resolve or query Wi-Fi/Bluetooth/location/network-speed views here.
            if (!holder.overlayMode) {
                if (
                    holder.root.width <= 0 || holder.root.height <= 0 ||
                    holder.statusBarView.width <= 0 || holder.statusBarView.height <= 0
                ) {
                    waitForOverlayInlineLayout(holder)
                    return false
                }
                holder.statusBarView.overlay.add(holder.root)
                holder.overlayMode = true
                holder.awaitingOverlayLayout = false
            }
            positionTrueOverlay(holder, current)
            return true
        }
'''
new_mount = '''        if (current.overlayOnTop) {
            // Restore overlay immediately after SystemUI restart. The power text may still
            // be empty at this point, so never wait for root.width/root.height. The first
            // sample will re-measure and lay out the fixed X/Y position again.
            if (!holder.overlayMode) {
                if (holder.statusBarView.width <= 0 || holder.statusBarView.height <= 0) {
                    scheduleOverlayMountRetry(holder)
                    return false
                }
                holder.statusBarView.overlay.add(holder.root)
                holder.overlayMode = true
                holder.awaitingOverlayLayout = false
                holder.overlayMountRetries = 0
            }
            positionTrueOverlay(holder, current)
            return true
        }
'''
if old_mount not in runtime:
    raise SystemExit('overlay mount block not found')
runtime = runtime.replace(old_mount, new_mount, 1)
runtime = runtime.replace(
    '        if (!ensureInlineAnchor(holder, current)) return false\n',
    '        holder.overlayMountRetries = 0\n        if (!ensureInlineAnchor(holder, current)) return false\n',
    1,
)

ui = ui.replace(
    '    var horizontalOffset by remember(powerRuntime.horizontalOffsetDp) {\n        mutableFloatStateOf(powerRuntime.horizontalOffsetDp)\n    }\n    var verticalOffset by remember(powerRuntime.verticalOffsetDp) {\n        mutableFloatStateOf(powerRuntime.verticalOffsetDp)\n    }\n',
    '    var horizontalOffset by remember(powerRuntime.horizontalOffsetDp) {\n        mutableFloatStateOf(powerRuntime.horizontalOffsetDp.coerceIn(-150f, 150f))\n    }\n    var verticalOffset by remember(powerRuntime.verticalOffsetDp) {\n        mutableFloatStateOf(powerRuntime.verticalOffsetDp.coerceIn(-30f, 30f))\n    }\n',
    1,
)
ui = ui.replace('                            valueRange = -300f..300f,\n', '                            valueRange = -150f..150f,\n', 1)
ui = ui.replace('                            valueRange = -60f..60f,\n', '                            valueRange = -30f..30f,\n', 1)

runtime_path.write_text(runtime)
ui_path.write_text(ui)
