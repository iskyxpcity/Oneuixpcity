package com.oneuixp.app.statusbar

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
) {
    val systemUiScopeActive: Boolean
        get() = scopeKnown && "com.android.systemui" in scope
}

/**
 * API 102 modern module service: configuration/scope state, not injection state.
 * The API102 META-INF module entry is essential for the framework to bind.
 */
object LsposedFrameworkStatus {
    private const val TAG = "OneuiXP-LSPosed"
    private val initialized = AtomicBoolean(false)

    @Volatile private var service: XposedService? = null
    private val serviceObservers = CopyOnWriteArraySet<(XposedService?) -> Unit>()
    private val _state = MutableStateFlow(LsposedFrameworkSnapshot())
    val state: StateFlow<LsposedFrameworkSnapshot> = _state.asStateFlow()

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        Log.i(TAG, "Registering modern Xposed service listener")
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(boundService: XposedService) {
                Log.i(TAG, "Modern Xposed service binder received")
                service = boundService
                _state.value = LsposedFrameworkSnapshot(connected = true)
                notifyServiceObservers(boundService)
                Thread({ refresh() }, "OneuiXP-LSPosed-initial-scope").apply {
                    isDaemon = true
                    start()
                }
            }

            override fun onServiceDied(deadService: XposedService) {
                if (service === deadService) {
                    service = null
                    Log.w(TAG, "Modern Xposed service binder died")
                    _state.value = LsposedFrameworkSnapshot()
                    notifyServiceObservers(null)
                }
            }
        })
    }

    /**
     * libxposed-service exposes a single XposedServiceHelper listener slot.
     * Keep framework status as the only registrar, then fan the same Binder out
     * to preference sync and any future in-app consumers.
     */
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

    /** Must not run on the main thread: getScope makes a Binder round-trip. */
    fun refresh() {
        val current = service
        if (current == null) {
            if (_state.value.connected) _state.value = LsposedFrameworkSnapshot()
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
            val next = LsposedFrameworkSnapshot(
                connected = true,
                scopeKnown = true,
                scope = result.getOrThrow(),
            )
            if (next != previous) {
                Log.i(TAG, "Live manager scope count=" + next.scope.size +
                    ", SystemUI scope=" + next.systemUiScopeActive)
                _state.value = next
            }
        } else {
            if (previous.scopeKnown || !previous.connected) {
                Log.w(TAG, "Connected to LSPosed but getScope failed", result.exceptionOrNull())
                _state.value = LsposedFrameworkSnapshot(connected = true)
            }
        }
    }
}
