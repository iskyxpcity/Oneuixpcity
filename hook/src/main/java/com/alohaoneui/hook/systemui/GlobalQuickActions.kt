package com.alohaoneui.hook.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import com.alohaoneui.common.Preference
import com.alohaoneui.common.StatusBarRuntimeBridge
import com.alohaoneui.hook.util.PreferenceProvider
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * libxposed entry adapter for the upstream global-shortcut runtime.
 * Trigger recognition, per-feature state machines, floating-ball touch handling, and action
 * implementations are kept in their source-derived responsibility files.
 */
object GlobalQuickActions {
    internal const val TAG = "AlohaOneUI-GlobalQuickActions"
    internal val mainHandler = Handler(Looper.getMainLooper())
    internal val navigationKeyHandler = Handler(Looper.getMainLooper())
    internal val screenOffVolumeHandler = Handler(Looper.getMainLooper())

    @Volatile internal var cachedSettings: Preference.GlobalShortcutActions? = null
    @Volatile internal var cachedSettingsAt = 0L
    @Volatile internal var systemServerContext: Context? = null
    @Volatile internal var inputManagerService: Any? = null
    @Volatile internal var phoneWindowManager: Any? = null
    private var preferenceObserver: ContentObserver? = null
    private val diagnosticQueryReceiverInstalled = AtomicBoolean(false)
    @Volatile internal var lastGlobalActionDiagnostic: String = "not-run"

    internal var threeFingerActive = false
    internal var threeFingerConsumed = false
    internal var threeFingerTouchCancelled = false
    internal val threeFingerPointerIds = IntArray(THREE_FINGER_COUNT) { -1 }
    internal val threeFingerStartXs = FloatArray(THREE_FINGER_COUNT)
    internal val threeFingerStartYs = FloatArray(THREE_FINGER_COUNT)
    internal var threeFingerStartTime = 0L
    internal var threeFingerPinchActive = false
    internal var threeFingerPinchConsumed = false
    internal val threeFingerPinchPointerIds = IntArray(THREE_FINGER_COUNT) { -1 }
    internal var threeFingerPinchStartSpan = 0f
    internal var threeFingerSpreadActive = false
    internal var threeFingerSpreadConsumed = false
    internal val threeFingerSpreadPointerIds = IntArray(THREE_FINGER_COUNT) { -1 }
    internal var threeFingerSpreadStartSpan = 0f

    @Volatile internal var floatingBallView: View? = null
    @Volatile internal var floatingBallLayoutParams: WindowManager.LayoutParams? = null
    @Volatile internal var floatingBallWindowManager: WindowManager? = null
    internal var floatingBallTouchStartX = 0f
    internal var floatingBallTouchStartY = 0f
    internal var floatingBallStartX = 0
    internal var floatingBallStartY = 0
    internal var floatingBallTouchDownTime = 0L
    internal var floatingBallLastTapTime = 0L
    internal var floatingBallMoved = false
    internal var floatingBallDragging = false
    @Volatile internal var floatingBallTouchActive = false
    internal var floatingBallLongPressRunnable: Runnable? = null
    internal var floatingBallPendingClickRunnable: Runnable? = null
    @Volatile internal var floatingBallAutoEdgeRunnable: Runnable? = null
    @Volatile internal var floatingBallAutoEdgeHost: View? = null
    @Volatile internal var floatingBallAutoEdgeGeneration = 0L
    internal var floatingBallInnerOffsetX = 0f
    internal var floatingBallInnerOffsetY = 0f
    internal var floatingBallX = 0
    internal var floatingBallY = 0
    internal var lastFloatingBallEnsureTime = 0L
    internal var lastFloatingBallSettings: FloatingBallSettings? = null

    internal var screenOffVolumeDownKeyCode = 0
    internal var screenOffVolumeLongPressTriggered = false
    internal var screenOffVolumeLongPressRunnable: Runnable? = null
    internal var screenOffVolumeDoubleClickPending = false
    internal var screenOffVolumeDoubleClickKeyCode = 0
    internal var screenOffVolumeDoubleClickRunnable: Runnable? = null

    internal var statusBarShortcutDownInBar = false
    internal var statusBarShortcutLongPressTriggered = false
    internal var statusBarShortcutDownX = 0f
    internal var statusBarShortcutDownY = 0f
    internal var statusBarShortcutDownTime = 0L
    internal var statusBarShortcutLastTapTime = 0L
    internal var statusBarPendingSingleTap: Runnable? = null
    internal val navigationKeyTouchStates: MutableMap<View, NavigationKeyTouchState> =
        Collections.synchronizedMap(WeakHashMap<View, NavigationKeyTouchState>())
    internal var flashlightEnabled = false

    context(xposedModule: XposedModule)
    private fun currentSettings(): Preference.GlobalShortcutActions {
        val now = SystemClock.uptimeMillis()
        val cached = cachedSettings
        if (cached == null || now - cachedSettingsAt > SETTINGS_CACHE_MS) {
            runCatching { PreferenceProvider.loadPreference()?.other?.globalShortcutActions }
                .getOrNull()?.let { cachedSettings = it }
            cachedSettingsAt = now
        }
        return cachedSettings ?: Preference.GlobalShortcutActions()
    }

    internal fun recordSystemServerDiagnosticEvent(context: Context, message: String) {
        val event = Intent(StatusBarRuntimeBridge.ACTION_SYSTEM_SERVER_DIAGNOSTIC_EVENT)
            .setPackage("com.android.systemui")
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            .putExtra(StatusBarRuntimeBridge.EXTRA_SYSTEM_SERVER_DIAGNOSTIC_CATEGORY, TAG)
            .putExtra(StatusBarRuntimeBridge.EXTRA_SYSTEM_SERVER_DIAGNOSTIC_MESSAGE, message.take(1800))
        runCatching { context.sendBroadcast(event) }
            .onFailure { Log.w(TAG, "could not bridge SystemServer runtime diagnostic event", it) }
    }

    private fun logGuardedHookFailure(label: String, event: MotionEvent, failure: Throwable) {
        if (failure is VirtualMachineError || failure is ThreadDeath) throw failure
        Log.e(TAG, "$label failed; action=${event.actionMasked}, pointers=${event.pointerCount}", failure)
    }

    context(xposedModule: XposedModule)
    private fun dispatchGlobalMotionSafely(event: MotionEvent) {
        try {
            currentSettings()
            GlobalQuickActions.handleGlobalMotion(event)
        } catch (failure: Throwable) {
            logGuardedHookFailure("global gesture/status-bar shortcut handler", event, failure)
        }
    }

    context(xposedModule: XposedModule)
    fun installSystemUi(param: XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != "com.android.systemui") return
        runCatching {
            val keyButton = param.classLoader.loadClass("com.android.systemui.navigationbar.views.buttons.KeyButtonView")
            val method = keyButton.getDeclaredMethod("onTouchEvent", MotionEvent::class.java)
            xposedModule.hook(method).intercept { chain ->
                val view = chain.thisObject as? View
                val event = chain.args.firstOrNull() as? MotionEvent
                if (view == null || event == null) {
                    chain.proceed()
                } else {
                    val handled = try {
                        currentSettings()
                        GlobalQuickActions.handleNavigationKeyShortcutTouch(view, event)
                    } catch (failure: Throwable) {
                        logGuardedHookFailure("SystemUI navigation shortcut handler", event, failure)
                        false
                    }
                    if (handled) true else chain.proceed()
                }
            }
            Log.i(TAG, "installed SystemUI virtual-navigation shortcut hook")
        }.onFailure {
            Log.w(TAG, "could not install virtual-navigation shortcut hook", it)
        }
    }

    context(xposedModule: XposedModule)
    fun installSystemServer(loader: ClassLoader) {
        systemServerContext = GlobalQuickActions.findSystemContext() ?: systemServerContext
        runCatching { hookInputManagerService(loader) }.onFailure { Log.w(TAG, "InputManagerService hook unavailable", it) }
        runCatching { hookGlobalPointerEvents(loader) }.onFailure { Log.w(TAG, "global pointer-event hook unavailable", it) }
        runCatching { hookPhoneWindowManager(loader) }.onFailure { Log.w(TAG, "PhoneWindowManager hook unavailable", it) }

        systemServerContext?.let { context ->
            currentSettings()
            lastFloatingBallSettings = GlobalQuickActions.floatingBallSettings(context)
            registerPreferenceObserver(context)
            registerSystemServerDiagnosticQueryReceiver(context)
            mainHandler.post {
                val activeContext = systemServerContext ?: context
                runCatching { GlobalQuickActions.ensureFloatingBall(activeContext) }
                    .onFailure { Log.w(TAG, "initial floating-ball setup failed", it) }
            }
        }
        Log.i(TAG, "installed SystemServer global shortcut dispatch")
    }

    private fun registerSystemServerDiagnosticQueryReceiver(context: Context) {
        if (!diagnosticQueryReceiverInstalled.compareAndSet(false, true)) return
        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action != StatusBarRuntimeBridge.ACTION_SYSTEM_SERVER_DIAGNOSTIC_QUERY) return
                    val requestId = intent.getStringExtra(StatusBarRuntimeBridge.EXTRA_DIAGNOSTIC_REQUEST_ID) ?: return
                    val snapshot = runCatching { buildSystemServerDiagnosticSnapshot(context) }
                        .getOrElse { "snapshotFailed=${it.javaClass.simpleName}:${it.message.orEmpty()}" }
                    val response = Intent(StatusBarRuntimeBridge.ACTION_SYSTEM_SERVER_DIAGNOSTIC_RESPONSE)
                        .setPackage("com.android.systemui")
                        .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        .putExtra(StatusBarRuntimeBridge.EXTRA_DIAGNOSTIC_REQUEST_ID, requestId)
                        .putExtra(StatusBarRuntimeBridge.EXTRA_SYSTEM_SERVER_DIAGNOSTIC_SNAPSHOT, snapshot)
                    runCatching { context.sendBroadcast(response) }
                        .onFailure { Log.w(TAG, "could not return on-demand SystemServer snapshot", it) }
                }
            }
            val filter = IntentFilter(StatusBarRuntimeBridge.ACTION_SYSTEM_SERVER_DIAGNOSTIC_QUERY)
            if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, null, null, Context.RECEIVER_EXPORTED)
            else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter, null, null)
            }
            Log.i(TAG, "registered on-demand SystemServer diagnostics receiver")
        } catch (failure: Exception) {
            diagnosticQueryReceiverInstalled.set(false)
            Log.w(TAG, "could not register on-demand SystemServer diagnostics receiver", failure)
        }
    }

    private fun buildSystemServerDiagnosticSnapshot(context: Context): String {
        val settings = cachedSettings ?: Preference.GlobalShortcutActions()
        val view = floatingBallView
        val params = floatingBallLayoutParams
        return buildString {
            appendLine("capturedAtMs=${System.currentTimeMillis()} uid=${Process.myUid()} contextPackage=${context.packageName}")
            appendLine("enabled=${settings.floatingBallEnabled} autoEdgeDelaySeconds=${settings.floatingBallAutoEdgeDelaySeconds}")
            appendLine("viewPresent=${view != null} attached=${view?.isAttachedToWindow} viewSize=${view?.width}x${view?.height} visibility=${view?.visibility}")
            appendLine("paramsPresent=${params != null} x=${params?.x} y=${params?.y} width=${params?.width} height=${params?.height} windowManagerPresent=${floatingBallWindowManager != null}")
            appendLine("timerArmed=${floatingBallAutoEdgeRunnable != null} timerHostMatchesView=${floatingBallAutoEdgeHost === view} generation=${floatingBallAutoEdgeGeneration} dragging=${floatingBallDragging} touchActive=${floatingBallTouchActive}")
            appendLine("savedX=${floatingBallX} savedY=${floatingBallY}")
            appendLine("lastGlobalAction=$lastGlobalActionDiagnostic")
        }
    }

    context(xposedModule: XposedModule)
    private fun registerPreferenceObserver(context: Context) {
        if (preferenceObserver != null) return
        runCatching {
            val observer = object : ContentObserver(mainHandler) {
                override fun onChange(selfChange: Boolean) {
                    cachedSettings = null
                    cachedSettingsAt = 0L
                    currentSettings()
                    val freshFloatingBall = GlobalQuickActions.floatingBallSettings(context)
                    val previous = lastFloatingBallSettings
                    lastFloatingBallSettings = freshFloatingBall
                    if (previous == null || previous != freshFloatingBall) {
                        mainHandler.post {
                            runCatching {
                                GlobalQuickActions.removeFloatingBall()
                                GlobalQuickActions.ensureFloatingBall(context)
                            }.onFailure { Log.w(TAG, "floating-ball preference refresh failed", it) }
                        }
                    }
                }
            }
            context.contentResolver.registerContentObserver(
                Uri.parse("content://com.alohaoneui.app.statusbridge/preferences"), false, observer
            )
            preferenceObserver = observer
        }.onFailure { Log.w(TAG, "global-shortcut preference observer registration failed", it) }
    }

    context(xposedModule: XposedModule)
    private fun hookInputManagerService(loader: ClassLoader) {
        val clazz = loader.loadClass("com.android.server.input.InputManagerService")
        clazz.declaredConstructors.forEach { constructor ->
            xposedModule.hook(constructor).intercept { chain ->
                val result = chain.proceed()
                inputManagerService = chain.thisObject
                result
            }
        }
        clazz.declaredMethods.filter { it.name == "start" }.forEach { method ->
            xposedModule.hook(method).intercept { chain ->
                val result = chain.proceed()
                inputManagerService = chain.thisObject
                result
            }
        }
    }

    context(xposedModule: XposedModule)
    private fun hookGlobalPointerEvents(loader: ClassLoader) {
        try {
            val dispatcher = loader.loadClass("com.android.server.wm.PointerEventDispatcher")
            val method = dispatcher.getDeclaredMethod("onInputEvent", InputEvent::class.java)
            xposedModule.hook(method).intercept { chain ->
                val event = chain.args.firstOrNull() as? MotionEvent
                if (event != null) dispatchGlobalMotionSafely(event)
                chain.proceed()
            }
            Log.i(TAG, "hooked PointerEventDispatcher.onInputEvent")
            return
        } catch (failure: Throwable) {
            Log.w(TAG, "PointerEventDispatcher.onInputEvent unavailable; using pointer-listener hook", failure)
        }
        val listener = loader.loadClass("android.view.WindowManagerPolicyConstants\$PointerEventListener")
        listener.declaredMethods.filter {
            it.name == "onPointerEvent" && it.parameterTypes.size == 1 && MotionEvent::class.java.isAssignableFrom(it.parameterTypes[0])
        }.forEach { method ->
            xposedModule.hook(method).intercept { chain ->
                val event = chain.args.firstOrNull() as? MotionEvent
                if (event != null) dispatchGlobalMotionSafely(event)
                chain.proceed()
            }
        }
    }

    context(xposedModule: XposedModule)
    private fun hookPhoneWindowManager(loader: ClassLoader) {
        val clazz = loader.loadClass("com.android.server.policy.PhoneWindowManager")
        clazz.declaredConstructors.forEach { constructor ->
            xposedModule.hook(constructor).intercept { chain ->
                val result = chain.proceed()
                phoneWindowManager = chain.thisObject
                result
            }
        }
        clazz.declaredMethods.filter { method ->
            method.name == "interceptKeyBeforeQueueing" && method.parameterTypes.any { KeyEvent::class.java.isAssignableFrom(it) }
        }.forEach { method ->
            xposedModule.hook(method).intercept { chain ->
                phoneWindowManager = chain.thisObject
                val event = chain.args.firstOrNull { it is KeyEvent } as? KeyEvent
                val context = GlobalQuickActions.currentSystemContext()
                if (event != null && context != null) {
                    currentSettings()
                    if (GlobalQuickActions.handleScreenOffVolumeKeyShortcut(context, event)) 0 else chain.proceed()
                } else chain.proceed()
            }
        }
    }
}
