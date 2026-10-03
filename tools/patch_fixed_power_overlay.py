from pathlib import Path

runtime_path = Path('hook/src/main/java/io/github/soclear/oneuix/hook/systemui/StatusBarPowerRuntime.kt')
ui_path = Path('app/src/main/java/com/oneuixp/app/ui/screen/status/StatusPowerRuntimeControls.kt')

runtime = runtime_path.read_text()

# Overlay mode no longer tracks Wi-Fi/Bluetooth/dynamic status-icon coordinates.
runtime = runtime.replace('import android.content.res.Configuration\n', '')
runtime = runtime.replace(
'''        var overlayMode: Boolean = false,\n        var overlayAnchorX: Float = 0f,\n        var overlayAnchorUsesRightEdge: Boolean = true,\n        var overlayHostWidth: Int = 0,\n        var overlayHostHeight: Int = 0,\n        var overlayOrientation: Int = Configuration.ORIENTATION_UNDEFINED,\n        var lastLayoutKey: String = "",\n''',
'''        var overlayMode: Boolean = false,\n        var lastLayoutKey: String = "",\n''')
runtime = runtime.replace(
'''            slots = slots,\n            mountedSide = initialSide,\n            overlayOrientation = host.resources.configuration.orientation,\n        )\n''',
'''            slots = slots,\n            mountedSide = initialSide,\n        )\n''')

layout_listener_start = '''        host.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->\n            if (!holder.overlayMode) return@addOnLayoutChangeListener\n            val orientation = host.resources.configuration.orientation\n            if (orientation == holder.overlayOrientation) return@addOnLayoutChangeListener\n            holder.overlayOrientation = orientation\n            host.postOnAnimation {\n                if (!holder.overlayMode || !host.isAttachedToWindow) return@postOnAnimation\n                refreshOverlayAnchor(holder, config, force = true)\n                positionTrueOverlay(holder, config)\n            }\n        }\n'''
layout_listener_fixed = '''        host.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->\n            if (!holder.overlayMode) return@addOnLayoutChangeListener\n            val sizeChanged = right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop\n            if (!sizeChanged) return@addOnLayoutChangeListener\n            host.postOnAnimation {\n                if (holder.overlayMode && host.isAttachedToWindow) {\n                    positionTrueOverlay(holder, config)\n                }\n            }\n        }\n'''
if layout_listener_start not in runtime:
    raise SystemExit('orientation listener block not found')
runtime = runtime.replace(layout_listener_start, layout_listener_fixed)

start = runtime.index('    private fun captureInlineOverlayAnchor(')
end = runtime.index('    private fun waitForOverlayInlineLayout(', start)
runtime = runtime[:start] + runtime[end:]

start = runtime.index('    private fun ensureMountMode(')
end = runtime.index('    private fun positionTrueOverlay(', start)
new_mount = '''    private fun ensureMountMode(\n        holder: Holder,\n        current: Preference.SystemUI.StatusBarPowerRuntime,\n    ): Boolean {\n        val density = holder.root.resources.displayMetrics.density\n        if (current.overlayOnTop) {\n            // True overlay uses only fixed host-edge coordinates plus the user X/Y values.\n            // Do not resolve or query Wi-Fi/Bluetooth/location/network-speed views here.\n            if (!holder.overlayMode) {\n                if (\n                    holder.root.width <= 0 || holder.root.height <= 0 ||\n                    holder.statusBarView.width <= 0 || holder.statusBarView.height <= 0\n                ) {\n                    waitForOverlayInlineLayout(holder)\n                    return false\n                }\n                holder.statusBarView.overlay.add(holder.root)\n                holder.overlayMode = true\n                holder.awaitingOverlayLayout = false\n            }\n            positionTrueOverlay(holder, current)\n            return true\n        }\n\n        if (!ensureInlineAnchor(holder, current)) return false\n        if (holder.overlayMode) {\n            holder.statusBarView.overlay.remove(holder.root)\n            holder.overlayMode = false\n            addInlineAtEdge(holder)\n        } else {\n            addInlineAtEdge(holder)\n        }\n        holder.root.translationX = current.horizontalOffsetDp * density\n        holder.root.translationY = current.verticalOffsetDp * density\n        return true\n    }\n\n'''
runtime = runtime[:start] + new_mount + runtime[end:]

start = runtime.index('    private fun positionTrueOverlay(')
end = runtime.index('    private fun applyConfig(', start)
new_position = '''    private fun positionTrueOverlay(\n        holder: Holder,\n        current: Preference.SystemUI.StatusBarPowerRuntime,\n    ) {\n        if (!holder.overlayMode) return\n        val host = holder.statusBarView\n        if (host.width <= 0 || host.height <= 0) return\n\n        val density = holder.root.resources.displayMetrics.density\n        val widthSpec = View.MeasureSpec.makeMeasureSpec(host.width, View.MeasureSpec.AT_MOST)\n        val heightSpec = View.MeasureSpec.makeMeasureSpec(host.height, View.MeasureSpec.AT_MOST)\n        holder.root.measure(widthSpec, heightSpec)\n\n        val width = holder.root.measuredWidth.coerceAtLeast(1)\n        val height = holder.root.measuredHeight.coerceAtLeast(1)\n        val xOffset = (current.horizontalOffsetDp * density).roundToInt()\n        val left = if (current.placementSide == "left") {\n            xOffset\n        } else {\n            host.width - width + xOffset\n        }\n        val top = (\n            (host.height - height) / 2f + current.verticalOffsetDp * density\n        ).roundToInt()\n        val right = left + width\n        val bottom = top + height\n\n        holder.root.translationX = 0f\n        holder.root.translationY = 0f\n        if (\n            holder.root.left != left || holder.root.top != top ||\n            holder.root.right != right || holder.root.bottom != bottom\n        ) {\n            holder.root.layout(left, top, right, bottom)\n        }\n    }\n\n'''
runtime = runtime[:start] + new_position + runtime[end:]

# Guard against any stale dynamic overlay-anchor code surviving.
for forbidden in ('refreshOverlayAnchor(', 'captureInlineOverlayAnchor(', 'overlayAnchorX', 'overlayAnchorUsesRightEdge', 'overlayOrientation', 'overlayHostWidth', 'overlayHostHeight'):
    if forbidden in runtime:
        raise SystemExit(f'stale dynamic overlay anchor code remains: {forbidden}')

runtime_path.write_text(runtime)

ui = ui_path.read_text()
if 'import android.view.HapticFeedbackConstants\n' not in ui:
    ui = ui.replace('package com.oneuixp.app.ui.screen.status\n\n', 'package com.oneuixp.app.ui.screen.status\n\nimport android.view.HapticFeedbackConstants\n')
if 'import androidx.compose.ui.platform.LocalView\n' not in ui:
    ui = ui.replace('import androidx.compose.ui.graphics.vector.ImageVector\n', 'import androidx.compose.ui.graphics.vector.ImageVector\nimport androidx.compose.ui.platform.LocalView\n')
ui = ui.replace(
'    val powerIcon = ImageVector.vectorResource(id = R.drawable.battery)\n',
'''    val powerIcon = ImageVector.vectorResource(id = R.drawable.battery)\n    val hapticView = LocalView.current\n\n    fun tickIfChanged(previous: Float, next: Float) {\n        if (previous != next) {\n            hapticView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)\n        }\n    }\n''',
1)

replacements = {
'''                                powerTextSize = roundPowerHalfStep(it)\n                                onPreview(powerRuntime.copy(textSizeSp = powerTextSize))\n''':
'''                                val next = roundPowerHalfStep(it)\n                                tickIfChanged(powerTextSize, next)\n                                powerTextSize = next\n                                onPreview(powerRuntime.copy(textSizeSp = powerTextSize))\n''',
'''                                refreshInterval = (it / 500f).roundToInt() * 500f\n                                onPreview(\n''':
'''                                val next = (it / 500f).roundToInt() * 500f\n                                tickIfChanged(refreshInterval, next)\n                                refreshInterval = next\n                                onPreview(\n''',
'''                                columnGap = roundPowerHalfStep(it)\n                                onPreview(powerRuntime.copy(columnGapDp = columnGap))\n''':
'''                                val next = roundPowerHalfStep(it)\n                                tickIfChanged(columnGap, next)\n                                columnGap = next\n                                onPreview(powerRuntime.copy(columnGapDp = columnGap))\n''',
'''                                    rowGap = roundPowerHalfStep(it)\n                                    onPreview(powerRuntime.copy(rowGapDp = rowGap))\n''':
'''                                    val next = roundPowerHalfStep(it)\n                                    tickIfChanged(rowGap, next)\n                                    rowGap = next\n                                    onPreview(powerRuntime.copy(rowGapDp = rowGap))\n''',
'''                                horizontalOffset = roundPowerHalfStep(it)\n                                onPreview(powerRuntime.copy(horizontalOffsetDp = horizontalOffset))\n''':
'''                                val next = roundPowerHalfStep(it)\n                                tickIfChanged(horizontalOffset, next)\n                                horizontalOffset = next\n                                onPreview(powerRuntime.copy(horizontalOffsetDp = horizontalOffset))\n''',
'''                                verticalOffset = roundPowerHalfStep(it)\n                                onPreview(powerRuntime.copy(verticalOffsetDp = verticalOffset))\n''':
'''                                val next = roundPowerHalfStep(it)\n                                tickIfChanged(verticalOffset, next)\n                                verticalOffset = next\n                                onPreview(powerRuntime.copy(verticalOffsetDp = verticalOffset))\n''',
'''                                startPadding = roundPowerHalfStep(it)\n                                onPreview(powerRuntime.copy(startPaddingDp = startPadding))\n''':
'''                                val next = roundPowerHalfStep(it)\n                                tickIfChanged(startPadding, next)\n                                startPadding = next\n                                onPreview(powerRuntime.copy(startPaddingDp = startPadding))\n''',
'''                                endPadding = roundPowerHalfStep(it)\n                                onPreview(powerRuntime.copy(endPaddingDp = endPadding))\n''':
'''                                val next = roundPowerHalfStep(it)\n                                tickIfChanged(endPadding, next)\n                                endPadding = next\n                                onPreview(powerRuntime.copy(endPaddingDp = endPadding))\n''',
}
for old, new in replacements.items():
    if old not in ui:
        raise SystemExit('power slider block not found: ' + old.splitlines()[0])
    ui = ui.replace(old, new, 1)

ui_path.write_text(ui)
