package me.weishu.kernelsu.statusbar

import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LsposedFrameworkSnapshot(
    val connected: Boolean = false,
    val scope: Set<String> = emptySet(),
) {
    val systemUiScopeActive: Boolean
        get() = "com.android.systemui" in scope
}

/**
 * Direct status channel from LSPosed itself.
 *
 * Unlike [StatusBridge], this does not wait for SystemUI to restart and inject.
 * When LSPosed binds its module service to the app, [XposedService.getScope]
 * exposes the manager's actually configured scope immediately.
 */
object LsposedFrameworkStatus {
    private val initialized = AtomicBoolean(false)

    @Volatile
    private var service: XposedService? = null

    private val _state = MutableStateFlow(LsposedFrameworkSnapshot())
    val state: StateFlow<LsposedFrameworkSnapshot> = _state.asStateFlow()

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return

        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(boundService: XposedService) {
                service = boundService
                refresh()
            }

            override fun onServiceDied(deadService: XposedService) {
                if (service === deadService) {
                    service = null
                    _state.value = LsposedFrameworkSnapshot()
                }
            }
        })
    }

    fun refresh() {
        val current = service
        if (current == null) {
            _state.value = LsposedFrameworkSnapshot()
            return
        }

        val scope = runCatching {
            current.scope
                .asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()
        }.getOrElse {
            _state.value.scope
        }

        _state.value = LsposedFrameworkSnapshot(
            connected = true,
            scope = scope,
        )
    }
}
