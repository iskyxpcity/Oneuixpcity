package com.alohaoneui.hook.systemui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import com.alohaoneui.common.IgnoreUnknownKeysJson
import com.alohaoneui.common.Package
import com.alohaoneui.common.Preference
import com.alohaoneui.common.StatusBarRuntimeBridge
import com.alohaoneui.hook.util.PreferenceProvider
import java.lang.reflect.Method
import java.util.Collections
import java.util.Locale
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import kotlinx.serialization.decodeFromString

/**
 * Runtime owner for the five upstream status-bar icon packs.
 * It is deliberately independent from the Material/Miuix UI implementations:
 * both themes persist the same theme-neutral icon configuration.
 */
internal object StatusBarIconStylesRuntime {
    private const val APP_PACKAGE = "com.alohaoneui.app"
    private const val PREFERENCE_CHANGED_URI = "content://com.alohaoneui.app.statusbridge/preferences"
    private const val BATTERY_PERCENT_TAG = "alohaoneui_battery_percent"
    private const val BATTERY_BASE_WIDTH_DP = 28
    private const val BATTERY_BASE_HEIGHT_DP = 18
    private const val BATTERY_CHARGE_FRAME_DURATION_MS = 55
    private const val STATUS_GLYPH_NORMALIZED_HEIGHT_PX = 55
    private const val SIGNAL_ICON_NORMALIZED_HEIGHT_PX = 55
    private const val STACKED_MOBILE_SIGNAL_WIDTH_PX = 52
    private const val STACKED_MOBILE_SIGNAL_HEIGHT_PX = 56
    private const val STACKED_MOBILE_SIGNAL_MAIN_HEIGHT_PX = 36
    private const val STACKED_MOBILE_SIGNAL_MAIN_TOP_PX = 0
    private const val STACKED_MOBILE_SIGNAL_VECTOR_SCALE = 4f
    private val STACKED_MOBILE_SIGNAL_4BAR_PILL_STARTS = floatArrayOf(1.05f, 4.15f, 7.25f, 10.35f)
    private val STACKED_MOBILE_SIGNAL_5BAR_PILL_STARTS = floatArrayOf(0.9f, 3.35f, 5.8f, 8.25f, 10.7f)
    private const val STACKED_MOBILE_SIGNAL_4BAR_PILL_WIDTH_VECTOR = 1.6f
    private const val STACKED_MOBILE_SIGNAL_5BAR_PILL_WIDTH_VECTOR = 1.4f
    private const val STACKED_MOBILE_SIGNAL_PILL_TOP_VECTOR = 10.44f
    private const val STACKED_MOBILE_SIGNAL_PILL_BOTTOM_VECTOR = 13f
    private const val STACKED_MOBILE_SIGNAL_PILL_RADIUS_VECTOR = 0.8f
    private const val STACKED_MOBILE_SIGNAL_PILL_INACTIVE_ALPHA = 102
    private const val BATTERY_STYLE_PREFIX = "laobai.systemui.battery."
    private const val WIFI_STYLE_PREFIX = "laobai.systemui.wifi_signal."
    private const val MOBILE_STYLE_PREFIX = "laobai.systemui.mobile_signal."
    private const val BLUETOOTH_STYLE_PREFIX = "laobai.systemui.bluetooth_icon."
    private const val VPN_STYLE_PREFIX = "laobai.systemui.vpn_icon."
    private const val WIFI_ASSET_DIR = "wifi_signal_icons"
    private const val MOBILE_ASSET_DIR = "mobile_signal_icons"
    private const val BLUETOOTH_ASSET_DIR = "bluetooth_status_icons"
    private const val VPN_ASSET_DIR = "vpn_status_icons"

    private data class ResourceOrigin(val resources: Resources, val resId: Int)
    private data class BatteryOriginalState(
        val drawable: Drawable?,
        val width: Int?,
        val height: Int?,
    )
    private data class BatteryColors(
        val foreground: Int,
        val background: Int,
        val singleTone: Int,
    )
    private data class SignalIconRequest(
        val kind: String,
        val level: Int,
        val variant: String? = null,
        val fileName: String? = null,
        val stackedSlot: Int? = null,
        val maxLevel: Int = 4,
    )

    private val installed = AtomicBoolean(false)
    private val watcherStarted = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    private val drawableOrigins = Collections.synchronizedMap(WeakHashMap<Drawable, ResourceOrigin>())
    private val originalDrawables = Collections.synchronizedMap(WeakHashMap<Resources, MutableMap<Int, Drawable>>())
    private val signalViews = Collections.synchronizedMap(WeakHashMap<ImageView, ResourceOrigin>())
    private val batteryMeters = Collections.synchronizedMap(WeakHashMap<Any, Boolean>())
    private val batteryColors = Collections.synchronizedMap(WeakHashMap<Any, BatteryColors>())
    private val batteryMeterByImageView = Collections.synchronizedMap(WeakHashMap<ImageView, Any>())
    private val batteryOriginalStates = Collections.synchronizedMap(WeakHashMap<ImageView, BatteryOriginalState>())
    private val batteryCustomDrawables = Collections.newSetFromMap(WeakHashMap<Drawable, Boolean>())
    private val applyingSignalViews = Collections.newSetFromMap(WeakHashMap<ImageView, Boolean>())
    private val applyingBatteryViews = Collections.newSetFromMap(WeakHashMap<ImageView, Boolean>())

    @Volatile private var moduleRef: XposedModule? = null
    @Volatile private var systemUiContext: Context? = null
    @Volatile private var appContext: Context? = null
    @Volatile private var config = Preference.SystemUI.StatusBarIconStyles()
    @Volatile private var charging = false
    @Volatile private var batteryLevel = 100
    private var preferenceThread: HandlerThread? = null
    private var preferenceObserver: ContentObserver? = null
    private var runtimeReceiver: BroadcastReceiver? = null
    private var batteryReceiver: BroadcastReceiver? = null

    fun install(module: XposedModule, param: XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != Package.SYSTEMUI || param.processName != Package.SYSTEMUI) return
        if (!installed.compareAndSet(false, true)) return
        moduleRef = module
        hookApplicationAttach(module)
        hookResourceLookups(module)
        hookImageViewDrawable(module)
        hookBatteryMeterViews(module, param.classLoader)
        hookSamsungBatteryPercentageIcon(module, param.classLoader)
    }

    private fun hookApplicationAttach(module: XposedModule) {
        runCatching {
            val method = Application::class.java.getDeclaredMethod("attach", Context::class.java)
            module.hook(method).intercept { chain ->
                val result = chain.proceed()
                val context = chain.args.firstOrNull() as? Context
                if (context != null) {
                    systemUiContext = context
                    ensurePreferenceWatcher(module, context)
                    ensureBatteryReceiver(context)
                }
                result
            }
        }
    }

    private fun hookResourceLookups(module: XposedModule) {
        Resources::class.java.declaredMethods
            .filter { method ->
                method.name == "getDrawable" || method.name == "getDrawableForDensity"
            }
            .distinctBy(Method::toGenericString)
            .forEach { method ->
                runCatching {
                    module.hook(method).intercept { chain ->
                        val resources = chain.thisObject as? Resources
                        val resId = chain.args.firstOrNull() as? Int
                        val original = chain.proceed() as? Drawable
                        if (resources == null || resId == null || original == null) {
                            original
                        } else {
                            synchronized(originalDrawables) {
                                originalDrawables.getOrPut(resources) { mutableMapOf() }[resId] = original
                            }
                            val origin = ResourceOrigin(resources, resId)
                            drawableOrigins[original] = origin
                            val replacement = replaceSignalDrawable(resources, resId)
                            if (replacement != null) {
                                drawableOrigins[replacement] = origin
                                replacement
                            } else original
                        }
                    }
                }
            }
    }

    private fun hookImageViewDrawable(module: XposedModule) {
        runCatching {
            val method = ImageView::class.java.getDeclaredMethod("setImageDrawable", Drawable::class.java)
            module.hook(method).intercept { chain ->
                val imageView = chain.thisObject as? ImageView
                val incoming = chain.args.firstOrNull() as? Drawable
                if (imageView != null && incoming != null && !applyingBatteryViews.contains(imageView)) {
                    val meter = batteryMeterByImageView[imageView]
                    if (meter != null && config.batteryEnabled) {
                        if (!batteryCustomDrawables.contains(incoming)) {
                            batteryOriginalStates[imageView]?.let { original ->
                                batteryOriginalStates[imageView] = original.copy(drawable = incoming)
                            }
                        }
                        val replacement = buildBatteryDrawable(imageView.context, meter)
                        if (replacement != null) {
                            batteryCustomDrawables.add(replacement)
                            applyingBatteryViews.add(imageView)
                            chain.args[0] = replacement
                            val result = chain.proceed()
                            applyingBatteryViews.remove(imageView)
                            applyBatteryIconSize(imageView)
                            applyBatteryPercent(meter, imageView)
                            return@intercept result
                        }
                    }
                }
                val result = chain.proceed()
                if (imageView != null && !applyingSignalViews.contains(imageView)) {
                    drawableOrigins[incoming]?.let { signalViews[imageView] = it }
                }
                result
            }
        }
    }



    private fun hookSamsungBatteryPercentageIcon(module: XposedModule, classLoader: ClassLoader) {
        runCatching {
            val clazz = classLoader.loadClass("com.android.systemui.battery.SamsungBatteryMeterDrawable")
            clazz.declaredMethods.filter { it.name == "shouldShowPercentageIcon" }.forEach { method ->
                module.hook(method).intercept { chain ->
                    if (config.batteryEnabled || !config.batteryPercentEnabled) {
                        false
                    } else chain.proceed()
                }
            }
        }
    }

    private fun ensurePreferenceWatcher(module: XposedModule, context: Context) {
        if (!watcherStarted.compareAndSet(false, true)) return
        val worker = HandlerThread("AlohaOneUIStatusIconPrefs").apply { start() }
        preferenceThread = worker
        val background = Handler(worker.looper)

        fun refreshOnce() {
            background.post {
                val next = runCatching {
                    with(module) { PreferenceProvider.loadPreference() }?.systemUI?.statusBar?.iconStyles
                }.getOrNull() ?: return@post
                main.post { updateConfig(next) }
            }
        }

        val observer = object : ContentObserver(background) {
            override fun onChange(selfChange: Boolean) = refreshOnce()
            override fun onChange(selfChange: Boolean, uri: Uri?) = refreshOnce()
        }
        preferenceObserver = observer
        runCatching {
            context.contentResolver.registerContentObserver(
                Uri.parse(PREFERENCE_CHANGED_URI),
                false,
                observer,
            )
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != StatusBarRuntimeBridge.ACTION) return
                val payload = intent.getStringExtra(StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON) ?: return
                background.post {
                    val statusBar = runCatching {
                        IgnoreUnknownKeysJson.decodeFromString(
                            Preference.SystemUI.StatusBar.serializer(),
                            payload,
                        )
                    }.getOrNull() ?: return@post
                    main.post { updateConfig(statusBar.iconStyles) }
                }
            }
        }
        runtimeReceiver = receiver
        runCatching {
            val filter = IntentFilter(StatusBarRuntimeBridge.ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
        }
        refreshOnce()
    }

    @Suppress("DEPRECATION")
    private fun ensureBatteryReceiver(context: Context) {
        if (batteryReceiver != null) return
        val sticky = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        updateBatteryState(sticky)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateBatteryState(intent)
                main.post { applyAllBatteryMeters() }
            }
        }
        batteryReceiver = receiver
        runCatching {
            context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
    }

    private fun updateBatteryState(intent: Intent?) {
        if (intent == null) return
        batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, batteryLevel).coerceIn(0, 100)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun updateConfig(next: Preference.SystemUI.StatusBarIconStyles) {
        if (config == next) return
        config = next
        reapplySignalViews()
        applyAllBatteryMeters()
    }

    private fun reapplySignalViews() {
        val snapshot = synchronized(signalViews) { signalViews.toMap() }
        snapshot.forEach { (view, origin) ->
            if (!view.isAttachedToWindow) return@forEach
            val original = synchronized(originalDrawables) {
                originalDrawables[origin.resources]?.get(origin.resId)
            } ?: return@forEach
            val replacement = replaceSignalDrawable(origin.resources, origin.resId) ?: original
            applyingSignalViews.add(view)
            runCatching { view.setImageDrawable(replacement) }
            applyingSignalViews.remove(view)
        }
    }

    private fun replaceSignalDrawable(resources: Resources, resId: Int): Drawable? {
        val context = systemUiContext ?: return null
        val name = runCatching { resources.getResourceEntryName(resId) }.getOrNull() ?: return null
        val lower = name.lowercase(Locale.US)
        if (config.hideWifiActivityArrows && isWifiActivityIconName(lower)) {
            return ColorDrawable(Color.TRANSPARENT)
        }
        if (config.hideMobileNetworkType && isMobileDataTypeIconName(lower)) {
            return ColorDrawable(Color.TRANSPARENT)
        }
        val request = parseSignalIcon(name) ?: return null
        val styleId = when (request.kind) {
            "wifi" -> if (config.wifiEnabled) config.wifiStyleId else ""
            "mobile" -> if (config.mobileEnabled) config.mobileStyleId else ""
            "bluetooth" -> if (config.bluetoothEnabled) config.bluetoothStyleId else ""
            "vpn" -> if (config.vpnEnabled) config.vpnStyleId else ""
            else -> ""
        }
        if (styleId.isBlank()) return null
        val prefix = when (request.kind) {
            "wifi" -> WIFI_STYLE_PREFIX
            "mobile" -> MOBILE_STYLE_PREFIX
            "bluetooth" -> BLUETOOTH_STYLE_PREFIX
            "vpn" -> VPN_STYLE_PREFIX
            else -> return null
        }
        if (!styleId.startsWith(prefix)) return null
        val dir = styleId.removePrefix(prefix).takeIf { it.isNotBlank() } ?: return null
        val bitmap = loadSignalBitmap(context, dir, request) ?: return null
        val app = appContext(context) ?: return null
        return BitmapDrawable(app.resources, bitmap).apply {
            setAntiAlias(true)
            setFilterBitmap(true)
            setDither(true)
        }
    }

    private fun parseSignalIcon(name: String): SignalIconRequest? {
        parseBluetoothIcon(name)?.let { return it }
        parseVpnIcon(name)?.let { return it }
        parseWifiIcon(name)?.let { return it }
        return parseMobileIcon(name)
    }

    private fun parseWifiIcon(name: String): SignalIconRequest? {
        if (!name.contains("wifi", true) || !name.contains("signal", true)) return null
        val level = Regex("_(\\d)(?:_error)?$").find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return null
        if (level !in 0..5) return null
        val variant = when {
            name.contains("wifi7", true) -> "wifi7"
            name.contains("wifi6e", true) || name.contains("6ewifi", true) -> "wifi6e"
            name.contains("wifi6", true) -> "wifi6"
            name.contains("wifi5", true) -> "wifi5"
            else -> null
        }
        return SignalIconRequest("wifi", level, variant)
    }

    private fun parseMobileIcon(name: String): SignalIconRequest? {
        Regex("^ic_mobile_(\\d)_[45]_bar(?:_error)?$").find(name)?.let { match ->
            normalizeMobileSignalLevel(match.groupValues[1].toIntOrNull() ?: return null)?.let { level ->
                return SignalIconRequest("mobile", level)
            }
        }
        Regex("^stat_sys_signal_5level_(\\d)(?:_error)?$").find(name)?.let { match ->
            normalizeMobileSignalLevel(match.groupValues[1].toIntOrNull() ?: return null)?.let { level ->
                return SignalIconRequest("mobile", level)
            }
        }
        Regex("^stat_sys_signal_sprint_(\\d)(?:_error)?$").find(name)?.let { match ->
            normalizeMobileSignalLevel(match.groupValues[1].toIntOrNull() ?: return null)?.let { level ->
                return SignalIconRequest("mobile", level)
            }
        }
        Regex("^stat_sys_signal(_5bar)?_dual([12])_(\\d)(?:_error)?$").find(name)?.let { match ->
            val maxLevel = if (match.groupValues[1].isNotBlank()) 5 else 4
            val slot = match.groupValues[2].toIntOrNull()
            normalizeMobileSignalLevel(match.groupValues[3].toIntOrNull() ?: return null)?.let { level ->
                return SignalIconRequest("mobile", level, stackedSlot = slot, maxLevel = maxLevel)
            }
        }
        Regex("^stat_sys_signal_(\\d)(?:_error)?$").find(name)?.let { match ->
            normalizeMobileSignalLevel(match.groupValues[1].toIntOrNull() ?: return null)?.let { level ->
                return SignalIconRequest("mobile", level)
            }
        }
        Regex("^sec_stat_sys_signal(_5bar)?(?:_dual([12]))?_(\\d)(?:_error)?$").find(name)?.let { match ->
            val maxLevel = if (match.groupValues[1].isNotBlank()) 5 else 4
            val slot = match.groupValues.getOrNull(2)?.toIntOrNull()
            normalizeMobileSignalLevel(match.groupValues[3].toIntOrNull() ?: return null)?.let { level ->
                return SignalIconRequest("mobile", level, stackedSlot = slot, maxLevel = maxLevel)
            }
        }
        return null
    }

    private fun normalizeMobileSignalLevel(level: Int): Int? =
        level.takeIf { it in 0..5 }


    private fun parseBluetoothIcon(name: String): SignalIconRequest? = when (name.lowercase(Locale.US)) {
        "stat_sys_data_bluetooth",
        "sec_stat_sys_data_bluetooth",
        "car_stat_sys_data_bluetooth_indicator" -> SignalIconRequest("bluetooth", 0, fileName = "bluetooth.png")
        "stat_sys_data_bluetooth_connected",
        "sec_stat_sys_data_bluetooth_connected" -> SignalIconRequest("bluetooth", 1, fileName = "bluetooth_connected.png")
        else -> null
    }

    private fun parseVpnIcon(name: String): SignalIconRequest? {
        val lower = name.lowercase(Locale.US)
        val matches = lower == "stat_sys_vpn" ||
            lower == "stat_sys_vpn_ic" ||
            lower == "stat_sys_branded_vpn" ||
            lower == "stat_sys_no_internet_vpn_ic" ||
            lower == "stat_sys_no_internet_branded_vpn" ||
            (lower.startsWith("stat_sys_") && (lower.endsWith("_vpn") || lower.endsWith("_vpn_ic")))
        return if (matches) SignalIconRequest("vpn", 0, fileName = "vpn.png") else null
    }

    private fun isWifiActivityIconName(name: String): Boolean =
        name == "stat_sys_wifi_signal_in" ||
            name == "stat_sys_wifi_signal_out" ||
            name == "stat_sys_wifi_signal_inout" ||
            name == "stat_sys_wifi_signal_no_inout" ||
            name.startsWith("stat_sys_wifi_signal_lgt_")

    private fun isMobileDataTypeIconName(name: String): Boolean {
        if (name.contains("_in") || name.contains("_out") || name.contains("inout")) return false
        if (name == "stat_sys_data_saver" ||
            name == "stat_sys_data_bluetooth" ||
            name == "stat_sys_data_bluetooth_connected" ||
            name == "stat_sys_data_disabled_forbidden"
        ) return false
        if (name.startsWith("stat_sys_data_") || name.startsWith("sec_stat_sys_data_")) {
            return name.contains("4g") || name.contains("5g") || name.contains("lte") ||
                name.contains("nr") || name.contains("5ga") || name.contains("5ge")
        }
        return Regex("(^|_)(4g|4g_plus|5g|5g_plus|lte|lte_plus|nr|5ga|5ge)(_|$)")
            .containsMatchIn(name)
    }

    private fun loadSignalBitmap(context: Context, dir: String, request: SignalIconRequest): Bitmap? {
        val app = appContext(context) ?: return null
        val assetDir = when (request.kind) {
            "wifi" -> WIFI_ASSET_DIR
            "bluetooth" -> BLUETOOTH_ASSET_DIR
            "vpn" -> VPN_ASSET_DIR
            else -> MOBILE_ASSET_DIR
        }
        val names = buildList {
            if (!request.fileName.isNullOrBlank()) {
                add(request.fileName)
            } else {
                signalAssetLevelCandidates(app, assetDir, dir, request).forEach { level ->
                    if (request.kind == "wifi" && !request.variant.isNullOrBlank()) {
                        add(request.variant + "_" + level + ".png")
                    }
                    add(request.kind + "_" + level + ".png")
                }
            }
        }
        return names.firstNotNullOfOrNull { name ->
            runCatching {
                app.assets.open(assetDir + "/" + dir + "/" + name).use { input ->
                    val bitmap = BitmapFactory.decodeStream(input) ?: return@use null
                    prepareSignalBitmap(bitmap, dir, request)
                }
            }.getOrElse {
                runCatching {
                    app.assets.open("signal_icons/" + dir + "/" + name).use { input ->
                        val bitmap = BitmapFactory.decodeStream(input) ?: return@use null
                        prepareSignalBitmap(bitmap, dir, request)
                    }
                }.getOrNull()
            }
        }
    }

    private fun signalAssetLevelCandidates(
        app: Context,
        assetDir: String,
        dir: String,
        request: SignalIconRequest,
    ): List<Int> {
        val requested = request.level.coerceIn(0, 5)
        val availableMax = signalAssetMaxLevel(app, assetDir, dir, request).coerceIn(0, 5)
        val mappedLevel = when {
            availableMax <= 0 -> 0
            requested == 5 -> availableMax
            availableMax == 5 -> ((requested.coerceIn(0, 4) * 5f) / 4f).roundToInt().coerceIn(0, 5)
            else -> ((requested.coerceIn(0, 4) * availableMax.toFloat()) / 4f).roundToInt().coerceIn(0, availableMax)
        }
        return listOf(requested, mappedLevel, requested.coerceIn(0, availableMax), availableMax).distinct()
    }

    private fun signalAssetMaxLevel(
        app: Context,
        assetDir: String,
        dir: String,
        request: SignalIconRequest,
    ): Int {
        return runCatching {
            val genericRegex = Regex("^" + Regex.escape(request.kind) + "_(\\d)\\.png$")
            val variantRegex = if (request.kind == "wifi" && !request.variant.isNullOrBlank()) {
                Regex("^" + Regex.escape(request.variant) + "_(\\d)\\.png$")
            } else null
            app.assets.list(assetDir + "/" + dir).orEmpty()
                .asSequence()
                .mapNotNull { name ->
                    variantRegex?.find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
                        ?: genericRegex.find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
                }
                .maxOrNull()
        }.getOrNull() ?: 4
    }

    private fun prepareSignalBitmap(bitmap: Bitmap, dir: String, request: SignalIconRequest): Bitmap {
        val cropped = cropTransparentBitmap(bitmap, dir)
        val normalized = if (request.kind == "bluetooth" || request.kind == "vpn") {
            cleanStatusGlyphBitmap(normalizeStatusGlyphBitmapSize(cropped))
        } else cropped
        return if (request.kind == "mobile" && request.stackedSlot != null) {
            buildOfficialStackedMobileSignalBitmap(normalized, request.stackedSlot, request.level, request.maxLevel)
        } else normalized
    }

    private fun cropTransparentBitmap(bitmap: Bitmap, dir: String): Bitmap {
        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (Color.alpha(bitmap.getPixel(x, y)) != 0) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        if (right < left || bottom < top) return bitmap
        if (left == 0 && top == 0 && right == bitmap.width - 1 && bottom == bitmap.height - 1) return bitmap
        val cropped = Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1)
        val styleNumber = Regex("^style(\\d+)$").find(dir)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return if (styleNumber != null && styleNumber in 12..61) normalizeNewSignalBitmapSize(cropped) else cropped
    }

    private fun normalizeNewSignalBitmapSize(bitmap: Bitmap): Bitmap {
        if (bitmap.height >= SIGNAL_ICON_NORMALIZED_HEIGHT_PX) return bitmap
        val targetHeight = SIGNAL_ICON_NORMALIZED_HEIGHT_PX
        val targetWidth = ((bitmap.width.toFloat() / bitmap.height.toFloat()) * targetHeight)
            .roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun normalizeStatusGlyphBitmapSize(bitmap: Bitmap): Bitmap {
        if (bitmap.height >= STATUS_GLYPH_NORMALIZED_HEIGHT_PX) return bitmap
        val targetHeight = STATUS_GLYPH_NORMALIZED_HEIGHT_PX
        val targetWidth = ((bitmap.width.toFloat() / bitmap.height.toFloat()) * targetHeight)
            .roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun cleanStatusGlyphBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                if (alpha == 0) {
                    output.setPixel(x, y, Color.TRANSPARENT)
                    continue
                }
                val brightness = maxOf(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
                val cleanAlpha = when {
                    brightness < 96 -> 0
                    brightness > 190 -> alpha
                    else -> (alpha * ((brightness - 96) / 94f)).roundToInt()
                }
                output.setPixel(x, y, Color.argb(cleanAlpha, 255, 255, 255))
            }
        }
        return output
    }

    private fun buildOfficialStackedMobileSignalBitmap(bitmap: Bitmap, slot: Int, level: Int, maxLevel: Int): Bitmap {
        val output = Bitmap.createBitmap(
            STACKED_MOBILE_SIGNAL_WIDTH_PX,
            STACKED_MOBILE_SIGNAL_HEIGHT_PX,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        if (slot == 1) {
            val targetHeight = STACKED_MOBILE_SIGNAL_MAIN_HEIGHT_PX.toFloat()
            val targetWidth = (bitmap.width.toFloat() / bitmap.height.toFloat()) * targetHeight
            val left = (STACKED_MOBILE_SIGNAL_WIDTH_PX - targetWidth) / 2f
            val top = STACKED_MOBILE_SIGNAL_MAIN_TOP_PX.toFloat()
            canvas.drawBitmap(bitmap, null, RectF(left, top, left + targetWidth, top + targetHeight), paint)
        } else {
            drawStackedMobileSignalPills(canvas, paint, level, maxLevel)
        }
        return output
    }

    private fun drawStackedMobileSignalPills(canvas: Canvas, paint: Paint, level: Int, maxLevel: Int) {
        val pillCount = maxLevel.coerceIn(4, 5)
        val activeCount = level.coerceIn(0, pillCount)
        val starts = if (pillCount == 5) STACKED_MOBILE_SIGNAL_5BAR_PILL_STARTS
        else STACKED_MOBILE_SIGNAL_4BAR_PILL_STARTS
        val scale = STACKED_MOBILE_SIGNAL_VECTOR_SCALE
        val width = (if (pillCount == 5) STACKED_MOBILE_SIGNAL_5BAR_PILL_WIDTH_VECTOR
        else STACKED_MOBILE_SIGNAL_4BAR_PILL_WIDTH_VECTOR) * scale
        val top = STACKED_MOBILE_SIGNAL_PILL_TOP_VECTOR * scale
        val bottom = STACKED_MOBILE_SIGNAL_PILL_BOTTOM_VECTOR * scale
        val radius = STACKED_MOBILE_SIGNAL_PILL_RADIUS_VECTOR * scale
        starts.forEachIndexed { index, start ->
            val alpha = if (index < activeCount) 255 else STACKED_MOBILE_SIGNAL_PILL_INACTIVE_ALPHA
            paint.color = Color.argb(alpha, 255, 255, 255)
            val x = start * scale
            canvas.drawRoundRect(RectF(x, top, x + width, bottom), radius, radius, paint)
        }
    }


    private fun appContext(context: Context): Context? {
        appContext?.let { return it }
        val created = runCatching {
            context.createPackageContext(APP_PACKAGE, Context.CONTEXT_IGNORE_SECURITY)
        }.getOrNull() ?: return null
        appContext = created
        return created
    }

    private fun hookBatteryMeterViews(module: XposedModule, classLoader: ClassLoader) {
        runCatching {
            val clazz = classLoader.loadClass("com.android.systemui.battery.BatteryMeterView")
            val names = setOf(
                "updateColors",
                "onDarkChanged",
                "scaleBatteryMeterViewsLegacy",
                "updatePercentText",
                "updateShowPercent",
                "onBatteryLevelChanged",
            )
            clazz.declaredMethods.filter { it.name in names }.forEach { method ->
                runCatching {
                    module.hook(method).intercept { chain ->
                        val result = chain.proceed()
                        main.post { applyBatteryIcon(chain.thisObject) }
                        result
                    }
                }
            }
            clazz.declaredConstructors.forEach { constructor ->
                runCatching {
                    module.hook(constructor).intercept { chain ->
                        val result = chain.proceed()
                        main.post { applyBatteryIcon(chain.thisObject) }
                        result
                    }
                }
            }
        }
    }

    private fun applyAllBatteryMeters() {
        val snapshot = synchronized(batteryMeters) { batteryMeters.keys.toList() }
        snapshot.forEach(::applyBatteryIcon)
    }

    private fun applyBatteryIcon(meter: Any?) {
        if (meter == null) return
        val imageView = getFieldValue(meter, "mBatteryIconView") as? ImageView ?: return
        batteryMeters[meter] = true
        batteryMeterByImageView[imageView] = meter
        if (!config.batteryEnabled || !config.batteryStyleId.startsWith(BATTERY_STYLE_PREFIX)) {
            restoreBatteryIcon(imageView)
            applyBatteryPercent(meter, null)
            return
        }
        if (!batteryOriginalStates.containsKey(imageView)) {
            val params = imageView.layoutParams
            batteryOriginalStates[imageView] = BatteryOriginalState(
                drawable = imageView.drawable,
                width = params?.width,
                height = params?.height,
            )
        }
        val drawable = buildBatteryDrawable(imageView.context, meter) ?: return
        batteryCustomDrawables.add(drawable)
        applyingBatteryViews.add(imageView)
        runCatching { imageView.setImageDrawable(drawable) }
        applyingBatteryViews.remove(imageView)
        (drawable as? AnimationDrawable)?.start()
        imageView.adjustViewBounds = false
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.clearColorFilter()
        imageView.imageTintList = null
        imageView.setPadding(0, 0, 0, 0)
        imageView.visibility = View.VISIBLE
        applyBatteryIconSize(imageView)
        applyBatteryPercent(meter, imageView)
    }

    private fun restoreBatteryIcon(imageView: ImageView) {
        val original = batteryOriginalStates[imageView] ?: return
        applyingBatteryViews.add(imageView)
        runCatching { imageView.setImageDrawable(original.drawable) }
        applyingBatteryViews.remove(imageView)
        val params = imageView.layoutParams
        if (params != null && original.width != null && original.height != null) {
            params.width = original.width
            params.height = original.height
            imageView.layoutParams = params
        }
        batteryMeterByImageView.remove(imageView)
    }

    private fun buildBatteryDrawable(context: Context, meter: Any): Drawable? {
        val app = appContext(context) ?: return null
        val dir = config.batteryStyleId.removePrefix(BATTERY_STYLE_PREFIX).takeIf { it.isNotBlank() } ?: return null
        val level = (getFieldValue(meter, "mLevel") as? Int ?: batteryLevel).coerceIn(0, 100)
        val isCharging = charging
        if (isCharging && config.batteryChargingAnimationEnabled) {
            val animation = AnimationDrawable().apply { isOneShot = false }
            for (frameLevel in level..100) {
                val bitmap = loadBatteryBitmap(app, dir, "battery_charge_" + frameLevel + ".png") ?: continue
                val frame = BitmapDrawable(app.resources, bitmap)
                if (shouldTintBatteryBitmap(bitmap)) frame.setTint(currentBatteryTint(meter))
                animation.addFrame(frame, BATTERY_CHARGE_FRAME_DURATION_MS)
            }
            if (animation.numberOfFrames >= 2) {
                animation.setBounds(0, 0, animation.intrinsicWidth, animation.intrinsicHeight)
                return animation
            }
        }
        val bitmap = if (isCharging) {
            loadBatteryBitmap(app, dir, "battery_charge_" + level + ".png")
                ?: loadBatteryBitmap(app, dir, "battery_charge_full.png")
                ?: loadBatteryBitmap(app, dir, "battery_" + level + ".png")
                ?: loadBatteryBitmap(app, dir, "battery_100.png")
        } else {
            loadBatteryBitmap(app, dir, "battery_" + level + ".png")
                ?: loadBatteryBitmap(app, dir, "battery_100.png")
        } ?: return null
        val drawable = BitmapDrawable(app.resources, bitmap)
        if (shouldTintBatteryBitmap(bitmap)) {
            drawable.setTint(currentBatteryTint(meter))
        }
        return drawable
    }

    private fun loadBatteryBitmap(app: Context, dir: String, fileName: String): Bitmap? =
        runCatching {
            app.assets.open("battery_icons/" + dir + "/" + fileName).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()

    private fun shouldTintBatteryBitmap(bitmap: Bitmap): Boolean {
        var colored = 0
        var lightNeutral = 0
        var visible = 0
        val stepX = maxOf(1, bitmap.width / 16)
        val stepY = maxOf(1, bitmap.height / 16)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.alpha(pixel) > 48) {
                    visible++
                    val red = Color.red(pixel)
                    val green = Color.green(pixel)
                    val blue = Color.blue(pixel)
                    val max = maxOf(red, green, blue)
                    val min = minOf(red, green, blue)
                    if (max - min > 42) colored++
                    if (min >= 165 && max - min <= 30) lightNeutral++
                }
                x += stepX
            }
            y += stepY
        }
        return visible > 0 && colored <= maxOf(2, visible / 12) && lightNeutral >= visible / 3
    }

    private fun currentBatteryTint(meter: Any): Int {
        batteryColors[meter]?.foreground?.let { return Color.rgb(Color.red(it), Color.green(it), Color.blue(it)) }
        listOf("mIconTint", "mNonAdaptedForegroundColor", "mTextColor", "mDarkTint").forEach { field ->
            val color = getFieldValue(meter, field) as? Int ?: 0
            if (Color.alpha(color) != 0) return Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
        }
        return Color.WHITE
    }

    private fun applyBatteryIconSize(imageView: ImageView) {
        val percent = config.batterySizePercent.coerceIn(80, 180)
        val density = imageView.resources.displayMetrics.density
        val width = (BATTERY_BASE_WIDTH_DP * percent / 100f * density).roundToInt()
        val height = (BATTERY_BASE_HEIGHT_DP * percent / 100f * density).roundToInt()
        val params = imageView.layoutParams ?: ViewGroup.LayoutParams(width, height)
        params.width = width
        params.height = height
        imageView.layoutParams = params
    }

    private fun applyBatteryPercent(meter: Any, imageView: ImageView?) {
        val group = meter as? ViewGroup ?: return
        val enabled = config.batteryEnabled && config.batteryPercentEnabled
        if (!enabled) {
            (group.findViewWithTag<View>(BATTERY_PERCENT_TAG) as? TextView)?.visibility = View.GONE
            return
        }
        val level = (getFieldValue(meter, "mLevel") as? Int ?: batteryLevel).coerceIn(0, 100)
        val textView = (group.findViewWithTag<View>(BATTERY_PERCENT_TAG) as? TextView)
            ?: TextView(group.context).apply {
                tag = BATTERY_PERCENT_TAG
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                includeFontPadding = false
            }
        val right = config.batteryPercentPosition == 1
        val params = (textView.layoutParams as? LinearLayout.LayoutParams)
            ?: LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        params.marginStart = if (right) dp(group.context, 2) else 0
        params.marginEnd = if (right) 0 else dp(group.context, 2)
        val iconIndex = (0 until group.childCount).firstOrNull { index ->
            group.getChildAt(index) === imageView
        } ?: 0
        val targetIndex = if (right) group.childCount else iconIndex
        if (textView.parent !== group) {
            group.addView(textView, targetIndex.coerceIn(0, group.childCount), params)
        } else {
            textView.layoutParams = params
            val currentIndex = group.indexOfChild(textView)
            val adjustedTarget = if (targetIndex > currentIndex) targetIndex - 1 else targetIndex
            if (currentIndex != adjustedTarget.coerceIn(0, group.childCount - 1)) {
                group.removeView(textView)
                group.addView(textView, adjustedTarget.coerceIn(0, group.childCount), params)
            }
        }
        textView.visibility = View.VISIBLE
        textView.text = level.toString() + "%"
        textView.setTextColor(imageView?.let { currentBatteryTint(meter) } ?: Color.WHITE)
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    private fun getFieldValue(target: Any, name: String): Any? {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            val currentType = type
            val field = runCatching { currentType.getDeclaredField(name) }.getOrNull()
            if (field != null) {
                return runCatching {
                    field.isAccessible = true
                    field.get(target)
                }.getOrNull()
            }
            type = type.superclass
        }
        return null
    }
}
