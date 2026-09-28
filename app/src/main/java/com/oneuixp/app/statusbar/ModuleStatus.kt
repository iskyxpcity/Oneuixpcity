package com.oneuixp.app.statusbar

/**
 * Flux-style self-process activation probe.
 *
 * The normal app implementation is always false. When LSPosed actually injects
 * OneuiXP's own process, the modern Xposed entry replaces this method with true.
 */
object ModuleStatus {
    @JvmStatic
    fun isActive(): Boolean = false
}
