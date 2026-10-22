package com.alohaoneui.hook.systemui

import android.content.Context
import android.util.Log
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

internal fun GlobalQuickActions.currentApplicationContext(): Context? = runCatching {
    GlobalQuickActions.callCompatible(Class.forName("android.app.ActivityThread"), "currentApplication") as? Context
}.getOrNull()

internal fun GlobalQuickActions.findSystemContext(): Context? = runCatching {
    val activityThread = Class.forName("android.app.ActivityThread")
    val instance = GlobalQuickActions.callCompatible(activityThread, "currentActivityThread") ?: return@runCatching null
    GlobalQuickActions.readField(instance, "mSystemContext") as? Context
}.getOrNull()

internal fun GlobalQuickActions.currentSystemContext(): Context? =
    systemServerContext ?: GlobalQuickActions.findSystemContext() ?: GlobalQuickActions.currentApplicationContext()

internal fun GlobalQuickActions.postOnMain(action: () -> Unit) {
    mainHandler.post(action)
}

internal fun GlobalQuickActions.postOnMainDelayed(action: Runnable, delayMillis: Long) {
    mainHandler.postDelayed(action, delayMillis)
}

internal fun GlobalQuickActions.statusBarHeightPx(context: Context): Int {
    val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (id > 0) context.resources.getDimensionPixelSize(id) else dp(context, 28)
}

internal fun GlobalQuickActions.dp(context: Context, value: Int): Int =
    (value * context.resources.displayMetrics.density + 0.5f).toInt()

internal fun GlobalQuickActions.withAlpha(color: Int, alpha: Int): Int =
    (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

internal fun GlobalQuickActions.readField(instance: Any?, name: String): Any? {
    var type: Class<*> = instance?.javaClass ?: return null
    while (true) {
        try {
            return type.getDeclaredField(name).apply { isAccessible = true }.get(instance)
        } catch (_: NoSuchFieldException) {
            type = type.superclass ?: return null
        }
    }
}

internal fun GlobalQuickActions.callCompatible(target: Any?, name: String, vararg args: Any?): Any? {
    val actualTarget = target ?: throw NullPointerException("callCompatible target is null: $name")
    val type = if (actualTarget is Class<*>) actualTarget else actualTarget.javaClass
    val method = findCompatibleMethod(type, name, args)
        ?: throw NoSuchMethodException("${type.name}#${name}(${args.joinToString { it?.javaClass?.name ?: "null" }})")
    return try {
        method.invoke(if (actualTarget is Class<*>) null else actualTarget, *args)
    } catch (failure: InvocationTargetException) {
        throw (failure.targetException ?: failure)
    }
}

private fun findCompatibleMethod(type: Class<*>, name: String, args: Array<out Any?>): Method? {
    var current: Class<*>? = type
    while (current != null) {
        val currentType = current
        val candidates = runCatching { currentType.declaredMethods.toList() }.getOrDefault(emptyList())
            .filter { it.name == name && it.parameterTypes.size == args.size }
            .sortedBy { method ->
                method.parameterTypes.indices.count { i ->
                    val arg = args[i]
                    arg != null && !boxed(method.parameterTypes[i]).isAssignableFrom(arg.javaClass)
                }
            }
        val compatible = candidates.firstOrNull { method ->
            method.parameterTypes.indices.all { i ->
                val arg = args[i]
                if (arg == null) !method.parameterTypes[i].isPrimitive
                else boxed(method.parameterTypes[i]).isAssignableFrom(arg.javaClass)
            }
        }
        if (compatible != null) return compatible.apply { isAccessible = true }
        current = currentType.superclass
    }
    return null
}

private fun boxed(type: Class<*>): Class<*> = when (type) {
    java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
    java.lang.Byte.TYPE -> java.lang.Byte::class.java
    java.lang.Short.TYPE -> java.lang.Short::class.java
    java.lang.Integer.TYPE -> java.lang.Integer::class.java
    java.lang.Long.TYPE -> java.lang.Long::class.java
    java.lang.Float.TYPE -> java.lang.Float::class.java
    java.lang.Double.TYPE -> java.lang.Double::class.java
    java.lang.Character.TYPE -> java.lang.Character::class.java
    else -> type
}

internal fun GlobalQuickActions.log(message: String) {
    if (DEBUG_LOG) Log.i(TAG, message)
}
