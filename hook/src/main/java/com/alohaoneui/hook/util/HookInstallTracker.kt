package com.alohaoneui.hook.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Process-local count of successful AlohaOneUI hook installer blocks.
 *
 * A key identifies one installer call site. Repeated attach callbacks or
 * duplicate invocations do not inflate the count.
 */
object HookInstallTracker {
    private val successful = ConcurrentHashMap.newKeySet<String>()

    fun markSuccessful(key: String) {
        successful.add(key)
    }

    fun successfulCount(): Int = successful.size
}
