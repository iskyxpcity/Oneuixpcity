package io.github.soclear.oneuix.hook

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.hook.util.xlog
import java.lang.reflect.Method

/** App20261018Aloha's Samsung InCallUI return-delay hook, adapted to libxposed. */
object CallReturnDelay {
    private val inReturnDelayMethod = ThreadLocal.withInitial { false }

    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun install(enabled: Boolean, seconds: Int) {
        if (!enabled || param.packageName != Package.INCALLUI) return
        val delayMs = seconds.coerceIn(0, 5) * 1000L
        val loader = param.classLoader
        runCatching {
            loader.loadClass("Ab.e").declaredMethods
                .filter { (it.name == "y" || it.name == "e") && it.parameterCount == 0 }
                .forEach { method ->
                    xposedModule.hook(method).intercept { chain ->
                        val previous = inReturnDelayMethod.get()
                        inReturnDelayMethod.set(true)
                        try { chain.proceed() } finally { inReturnDelayMethod.set(previous) }
                    }
                }
        }.onFailure { xlog(it) }
        runCatching {
            loader.loadClass("Hd.a").declaredMethods
                .filter { it.name == "T" && it.parameterTypes.size == 1 && it.parameterTypes[0] == Long::class.javaPrimitiveType }
                .forEach { method: Method ->
                    xposedModule.hook(method).intercept { chain ->
                        val original = chain.args.getOrNull(0) as? Long
                        if ((original == 3500L || original == 10000L) && inReturnDelayMethod.get()) chain.args[0] = delayMs
                        chain.proceed()
                    }
                }
        }.onFailure { xlog(it) }
    }
}
