from pathlib import Path

p = Path('hook/src/main/java/io/github/soclear/oneuix/hook/systemui/StatusBarPowerRuntime.kt')
s = p.read_text()

old = '''        var mountedSide: String = "right",
        var overlayMode: Boolean = false,
        var lastLayoutKey: String = "",
'''
new = '''        var mountedSide: String = "right",
        var overlayMode: Boolean = false,
        var overlayParent: android.view.ViewParent? = null,
        var lastLayoutKey: String = "",
'''
if old not in s:
    raise SystemExit('holder block not found')
s = s.replace(old, new, 1)

old = '''        if (holder.overlayMode) {
            holder.statusBarView.overlay.remove(holder.root)
            holder.overlayMode = false
        }
'''
new = '''        if (holder.overlayMode) {
            holder.statusBarView.overlay.remove(holder.root)
            holder.overlayMode = false
            holder.overlayParent = null
        }
'''
if old not in s:
    raise SystemExit('ensureInlineAnchor overlay block not found')
s = s.replace(old, new, 1)

old = '''        if (current.overlayOnTop) {
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

        holder.overlayMountRetries = 0
        if (!ensureInlineAnchor(holder, current)) return false
        if (holder.overlayMode) {
            holder.statusBarView.overlay.remove(holder.root)
            holder.overlayMode = false
            addInlineAtEdge(holder)
        } else {
            addInlineAtEdge(holder)
        }
'''
new = '''        if (current.overlayOnTop) {
            // Samsung may rebuild/reparent status-bar children while the keyguard is coming
            // up. Never trust overlayMode alone: verify that the power root is still attached
            // to the exact overlay parent captured after ViewGroupOverlay.add().
            val actuallyInOverlay = holder.overlayMode &&
                holder.overlayParent != null &&
                holder.root.parent === holder.overlayParent
            if (!actuallyInOverlay) {
                if (holder.statusBarView.width <= 0 || holder.statusBarView.height <= 0) {
                    scheduleOverlayMountRetry(holder)
                    return false
                }
                (holder.root.parent as? ViewGroup)?.removeView(holder.root)
                val mounted = runCatching {
                    holder.statusBarView.overlay.add(holder.root)
                    holder.overlayParent = holder.root.parent
                    holder.overlayParent != null
                }.getOrDefault(false)
                if (!mounted) {
                    holder.overlayMode = false
                    holder.overlayParent = null
                    scheduleOverlayMountRetry(holder)
                    return false
                }
                holder.overlayMode = true
                holder.awaitingOverlayLayout = false
                holder.overlayMountRetries = 0
            }
            positionTrueOverlay(holder, current)
            return true
        }

        holder.overlayMountRetries = 0
        if (!ensureInlineAnchor(holder, current)) return false
        if (holder.overlayMode || holder.overlayParent != null) {
            holder.statusBarView.overlay.remove(holder.root)
            holder.overlayMode = false
            holder.overlayParent = null
            addInlineAtEdge(holder)
        } else {
            addInlineAtEdge(holder)
        }
'''
if old not in s:
    raise SystemExit('ensureMountMode block not found')
s = s.replace(old, new, 1)

old = '''            if (desiredVisibility == View.VISIBLE) {
                if (holder.overlayMode) {
                    positionTrueOverlay(holder, current)
                } else if (current.overlayOnTop) {
                    holder.root.post { applyConfig(holder, current) }
                }
            }
'''
new = '''            if (desiredVisibility == View.VISIBLE) {
                if (current.overlayOnTop) {
                    val actuallyInOverlay = holder.overlayMode &&
                        holder.overlayParent != null &&
                        holder.root.parent === holder.overlayParent
                    if (actuallyInOverlay) {
                        positionTrueOverlay(holder, current)
                    } else {
                        // Keyguard/status-bar transitions can silently reparent overlay
                        // children. Repair the real mount state without requiring a manual
                        // off/on toggle from the user.
                        holder.root.post { applyConfig(holder, config) }
                    }
                }
            }
'''
if old not in s:
    raise SystemExit('renderSample overlay block not found')
s = s.replace(old, new, 1)

p.write_text(s)
