package me.weishu.kernelsu.ui.component

import androidx.compose.runtime.Composable

/**
 * Legacy KernelSU-only settings must not be shown in OneuiXP.
 * Do not query the removed native library to decide whether to render them.
 * Kept as a compatibility wrapper until the obsolete settings UI is deleted.
 */
@Composable
fun KsuIsValid(content: @Composable () -> Unit) {
    // Intentionally hidden: OneuiXP has no KernelSU manager or native backend.
}
