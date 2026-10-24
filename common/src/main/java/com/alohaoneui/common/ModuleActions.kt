package com.alohaoneui.common

/**
 * Intra-module broadcasts. Used when a setting can only become effective after the hooked
 * process ends: the app broadcasts, and the hooked process acts on itself. This keeps the
 * whole flow inside the framework, so no root shell is ever needed.
 */
object ModuleActions {
    /**
     * Sent to Theme Store once the region fields are written. The store process holds the
     * resolved region for its whole lifetime, so it ends itself and the next launch resolves
     * the region again through the patched getters.
     */
    const val THEME_STORE_REGION_CHANGED = "com.alohaoneui.action.THEME_STORE_REGION_CHANGED"
}
