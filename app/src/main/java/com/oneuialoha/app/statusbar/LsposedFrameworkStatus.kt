package com.oneuialoha.app.statusbar

import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LsposedFrameworkSnapshot(
    val connected: Boolean = false,
    val scopeKnown: Boolean = false,
    val scope: Set<String> = emptySet(),
    val pingAlive: Boolean = false,
    val active: Boolean = false,
) {
    val systemUiScopeActive: Boolean
        get() = scopeKnown && "com.android.systemui" in scope
}

/**
 * Flux 3.8.0 style connection state:
 * - register one modern Xposed service listener
 * - when a service is available, getScope() is authoritative
 * - service presence is the module-enabled gate
 * - getScope describes configured scope only
 * - PING describes live SystemUI hook only and never enables the module by itself
 * - no periodic polling loop
 */
object LsposedFrameworkStatus {
    private const val TAG = "OneUIAloha-LSPosed"
    private val initialized = AtomicBoolean(false)

    @Volatile private var service: XposedService? = null
    private val serviceObservers = CopyOnWriteArraySet<(XposedService?) -> Unit>()
    private val _state = MutableStateFlow(LsposedFrameworkSnapshot())
    val state: StateFlow<LsposedFrameworkSnapshot> = _state.asStateFlow()

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        Log.i(TAG, "Registering Flux-style modern Xposed service listener")
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(boundService: XposedService) {
                service = boundService
                notifyServiceObservers(boundService)
                Thread(
                    { refreshScope() },
                    "OneUIAloha-LSPosed-bind",
                ).apply {
                    isDaemon = true
                    start()
                }
            }

            override fun onServiceDied(deadService: XposedService) {
                if (service === deadService) {
                    service = null
                    notifyServiceObservers(null)
                    publishWithoutService()
                }
            }
        })
    }

    fun observeService(observer: (XposedService?) -> Unit) {
        serviceObservers.add(observer)
        observer(service)
    }

    private fun notifyServiceObservers(value: XposedService?) {
        serviceObservers.forEach { observer ->
            runCatching { observer(value) }
                .onFailure { Log.w(TAG, "Xposed service observer failed", it) }
        }
    }

    /**
     * Binder work must not run on the main thread.
     * A scope result is meaningful only while the module service is connected.
     */
    fun refreshScope() {
        val current = service
        if (current == null) {
            publishWithoutService()
            return
        }

        val result = runCatching {
            current.scope
                .asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()
        }

        val previous = _state.value
        if (result.isSuccess) {
            val scope = result.getOrThrow()
            val next = LsposedFrameworkSnapshot(
                connected = true,
                scopeKnown = true,
                scope = scope,
                pingAlive = previous.pingAlive,
                active = "com.android.systemui" in scope,
            )
            if (next != previous) {
                Log.i(
                    TAG,
                    "Flux-style scope count=${scope.size}, SystemUI=${next.active}",
                )
                _state.value = next
            }
        } else {
            Log.w(TAG, "getScope failed; falling back to PING", result.exceptionOrNull())
            _state.value = LsposedFrameworkSnapshot(
                connected = true,
                scopeKnown = false,
                scope = emptySet(),
                pingAlive = previous.pingAlive,
                active = false,
            )
        }
    }

    fun updatePingAlive(alive: Boolean) {
        val previous = _state.value
        val active = previous.connected &&
            previous.scopeKnown &&
            previous.systemUiScopeActive
        val next = previous.copy(
            pingAlive = alive,
            active = active,
        )
        if (next != previous) _state.value = next
    }

    private fun publishWithoutService() {
        val previous = _state.value
        val next = LsposedFrameworkSnapshot(
            connected = false,
            scopeKnown = false,
            scope = emptySet(),
            pingAlive = previous.pingAlive,
            active = false,
        )
        if (next != previous) _state.value = next
    }
}
