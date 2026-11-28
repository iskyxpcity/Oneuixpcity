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
    private data class SignalIconRequest(
        val kind: String,
        val level: Int,
        val variant: String? = null,
        val fileName: String? = null,
    )

    private val installed = AtomicBoolean(false)
    private val watcherStarted = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    private val drawableOrigins = Collections.synchronizedMap(WeakHashMap<Drawable, ResourceOrigin>())
    private val originalDrawables = Collections.synchronizedMap(WeakHashMap<Resources, MutableMap<Int, Drawable>>())
    private val signalViews = Collections.synchronizedMap(WeakHashMap<ImageView, ResourceOrigin>())
    private val batteryMeters = Collections.synchronizedMap(WeakHashMap<Any, Boolean>())
    private val batteryMeterByImageView = Collections.synchronizedMap(WeakHashMap<ImageView, Any>())
    private val batteryOriginalStates = Collections.synchronizedMap(WeakHashMap<ImageView, BatteryOriginalState>())
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
                        val replacement = buildBatteryDrawable(imageView.context, meter)
                        if (replacement != null) {
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
        val patterns = listOf(
            Regex("^ic_mobile_(\\d)_[45]_bar(?:_error)?$"),
            Regex("^stat_sys_signal_5level_(\\d)(?:_error)?$"),
            Regex("^stat_sys_signal_sprint_(\\d)(?:_error)?$"),
            Regex("^stat_sys_signal(?:_5bar)?_dual[12]_(\\d)(?:_error)?$"),
            Regex("^stat_sys_signal_(\\d)(?:_error)?$"),
            Regex("^sec_stat_sys_signal(?:_5bar)?(?:_dual[12])?_(\\d)(?:_error)?$"),
        )
        patterns.forEach { pattern ->
            pattern.find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { level ->
                if (level in 0..5) return SignalIconRequest("mobile", level)
            }
        }
        return null
    }

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
        val candidates = buildList {
            if (!request.fileName.isNullOrBlank()) {
                add(request.fileName)
            } else {
                val available = runCatching { app.assets.list(assetDir + "/" + dir).orEmpty() }
                    .getOrDefault(emptyArray())
                val genericPrefix = request.kind
                val variantPrefix = request.variant
                val maxLevel = available.mapNotNull { file ->
                    val variantLevel = if (variantPrefix != null) {
                        Regex("^" + Regex.escape(variantPrefix) + "_(\\d)\\.png$").find(file)
                            ?.groupValues?.getOrNull(1)?.toIntOrNull()
                    } else null
                    variantLevel ?: Regex("^" + Regex.escape(genericPrefix) + "_(\\d)\\.png$").find(file)
                        ?.groupValues?.getOrNull(1)?.toIntOrNull()
                }.maxOrNull() ?: 4
                val requested = request.level.coerceIn(0, 5)
                val mapped = if (maxLevel == 4 && requested == 5) 4 else requested.coerceIn(0, maxLevel)
                listOf(requested, mapped, maxLevel).distinct().forEach { level ->
                    if (!variantPrefix.isNullOrBlank()) add(variantPrefix + "_" + level + ".png")
                    add(genericPrefix + "_" + level + ".png")
                }
            }
        }
        candidates.forEach { file ->
            runCatching {
                app.assets.open(assetDir + "/" + dir + "/" + file).use { BitmapFactory.decodeStream(it) }
            }.getOrNull()?.let { return it }
        }
        return null
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
        applyingBatteryViews.add(imageView)
        runCatching { imageView.setImageDrawable(drawable) }
        applyingBatteryViews.remove(imageView)
        (drawable as? AnimationDrawable)?.start()
        imageView.adjustViewBounds = false
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
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
                animation.addFrame(BitmapDrawable(app.resources, bitmap), BATTERY_CHARGE_FRAME_DURATION_MS)
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
            val field = runCatching { type.getDeclaredField(name) }.getOrNull()
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
