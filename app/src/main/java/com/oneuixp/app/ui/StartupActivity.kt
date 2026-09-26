package com.oneuixp.app.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import com.oneuixp.app.R
import com.oneuixp.app.data.repository.SettingsRepositoryImpl
import com.oneuixp.app.ui.component.miuix.effect.BgEffectConfig
import com.oneuixp.app.ui.component.miuix.effect.DeviceType
import com.oneuixp.app.ui.component.miuix.effect.OS3_BG_FRAG
import com.oneuixp.app.ui.theme.ColorMode
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class StartupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val colorMode = ColorMode.fromValue(SettingsRepositoryImpl().themeMode)
        val systemDark =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
        val darkMode = colorMode.isDark || (colorMode.isSystem && systemDark)

        configureStartupWindow(darkMode)

        setContentView(
            NativeOneuiXPStartupView(
                context = this,
                darkMode = darkMode,
                onFinished = ::openMainActivity,
            ),
        )
    }

    @Suppress("DEPRECATION")
    private fun configureStartupWindow(darkMode: Boolean) {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        var flags =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        if (!darkMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        if (!darkMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window.decorView.systemUiVisibility = flags
    }

    @SuppressLint("ObsoleteSdkInt")
    @Suppress("DEPRECATION")
    private fun openMainActivity() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            },
        )
        overridePendingTransition(0, 0)
        finish()
    }
}

private class NativeOneuiXPStartupView(
    context: Context,
    private val darkMode: Boolean,
    private val onFinished: () -> Unit,
) : View(context) {

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private val logo: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
    }

    private val surfaceColor =
        if (darkMode) Color.rgb(12, 12, 14) else Color.rgb(247, 248, 250)

    private val backgroundRenderer: NativeBackgroundRenderer =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeMiuixBackgroundRenderer(darkMode, surfaceColor)
        } else {
            CanvasMiuixBackgroundRenderer(darkMode, surfaceColor)
        }

    private var timeline = 0f
    private var startTimeMs = 0L
    private var started = false

    private val rainbow = intArrayOf(
        Color.rgb(255, 77, 77),
        Color.rgb(255, 149, 0),
        Color.rgb(255, 204, 0),
        Color.rgb(52, 199, 89),
        Color.rgb(0, 199, 190),
        Color.rgb(10, 132, 255),
        Color.rgb(94, 92, 230),
        Color.rgb(175, 82, 222),
    )

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3600L
        interpolator = LinearInterpolator()
        addUpdateListener {
            timeline = it.animatedValue as Float
            invalidate()
        }
        addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    postDelayed(onFinished, 120L)
                }
            },
        )
    }

    init {
        setBackgroundColor(surfaceColor)
        isClickable = false
        isFocusable = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!started) {
            started = true
            startTimeMs = SystemClock.uptimeMillis()
            animator.start()
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val elapsedSeconds =
            if (startTimeMs == 0L) 0f
            else (SystemClock.uptimeMillis() - startTimeMs) / 1000f

        backgroundRenderer.draw(
            canvas = canvas,
            width = width,
            height = height,
            timeSeconds = elapsedSeconds,
        )

        drawLogo(canvas)
        drawSamsungTitle(canvas)
        drawRainbowTitle(canvas)
    }

    private fun drawLogo(canvas: Canvas) {
        // Keep the logo present from the very first native frame so the Android
        // starting surface can hand off without a visible icon disappearance.
        val settle = easeOutBack(segment(timeline, 0f, 0.18f))
        val scale = 0.92f + 0.08f * settle
        val size = dp(108f)
        val cx = width / 2f
        val cy = height * 0.37f

        val half = size / 2f
        val dst = RectF(cx - half, cy - half, cx + half, cy + half)

        canvas.save()
        canvas.scale(scale, scale, cx, cy)
        imagePaint.alpha = 255
        canvas.drawBitmap(logo, null, dst, imagePaint)
        canvas.restore()
    }

    private fun drawSamsungTitle(canvas: Canvas) {
        val progress = segment(timeline, 0.12f, 0.42f)
        if (progress <= 0f) return

        val title = "Samsung Galaxy One UI"
        textPaint.typeface =
            android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
        textPaint.textSize = sp(26f)
        textPaint.color = if (darkMode) Color.WHITE else Color.rgb(30, 30, 34)
        textPaint.alpha = (255f * progress).roundToInt().coerceIn(0, 255)

        val x = (width - textPaint.measureText(title)) / 2f
        val y = height * 0.37f + dp(108f) / 2f + dp(61f) + dp(10f) * (1f - progress)
        canvas.drawText(title, x, y, textPaint)
    }

    private fun drawRainbowTitle(canvas: Canvas) {
        val title = "Oneui XP"
        val firstLetterStart = 0.34f
        val letterDuration = 0.14f
        val visibleLetters = title.count { !it.isWhitespace() }.coerceAtLeast(1)
        val stagger =
            if (visibleLetters > 1) {
                (1f - firstLetterStart - letterDuration) / (visibleLetters - 1)
            } else {
                0f
            }

        textPaint.typeface =
            android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
        textPaint.textSize = sp(30f)

        val spaceWidth = dp(8f)
        val widths = FloatArray(title.length)
        var totalWidth = 0f
        title.forEachIndexed { index, c ->
            widths[index] =
                if (c.isWhitespace()) spaceWidth else textPaint.measureText(c.toString())
            totalWidth += widths[index]
        }

        var x = (width - totalWidth) / 2f
        val baseline = height * 0.37f + dp(108f) / 2f + dp(113f)
        var visibleIndex = 0

        title.forEachIndexed { index, c ->
            if (c.isWhitespace()) {
                x += widths[index]
                return@forEachIndexed
            }

            val start = firstLetterStart + stagger * visibleIndex
            val progress =
                segment(
                    timeline,
                    start,
                    (start + letterDuration).coerceAtMost(1f),
                )
            val settle = easeOutBack(progress)
            val color = rainbow[visibleIndex % rainbow.size]
            visibleIndex += 1

            if (progress > 0f) {
                val charWidth = widths[index]
                val cx = x + charWidth / 2f
                val y = baseline + dp(18f) * (1f - progress)
                val scale = 0.70f + 0.30f * settle

                textPaint.color = color
                textPaint.alpha = (255f * progress).roundToInt().coerceIn(0, 255)

                canvas.save()
                canvas.scale(scale, scale, cx, y)
                canvas.drawText(c.toString(), x, y, textPaint)
                canvas.restore()
            }

            x += widths[index]
        }
    }

    private fun dp(value: Float): Float = value * density
    private fun sp(value: Float): Float = value * scaledDensity
}

private interface NativeBackgroundRenderer {
    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeSeconds: Float,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class RuntimeMiuixBackgroundRenderer(
    darkMode: Boolean,
    private val surfaceColor: Int,
) : NativeBackgroundRenderer {

    private val preset = BgEffectConfig.get(DeviceType.PHONE, darkMode)
    private val shader = RuntimeShader(OS3_BG_FRAG)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.shader = this@RuntimeMiuixBackgroundRenderer.shader
    }
    private val pointsAnim = FloatArray(8)
    private val colors = FloatArray(16)

    init {
        shader.setFloatUniform("uTranslateY", 0f)
        shader.setFloatUniform("uNoiseScale", 1.5f)
        shader.setFloatUniform("uPointRadiusMulti", 1f)
        shader.setFloatUniform("uAlphaMulti", 1f)
        shader.setFloatUniform("uPoints", preset.points)
        shader.setFloatUniform("uLightOffset", preset.lightOffset)
        shader.setFloatUniform("uSaturateOffset", preset.saturateOffset)
    }

    override fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeSeconds: Float,
    ) {
        canvas.drawColor(surfaceColor)
        if (width <= 0 || height <= 0) return

        shader.setFloatUniform("uResolution", width.toFloat(), height.toFloat())
        shader.setFloatUniform("uAnimTime", timeSeconds)

        val heightRatio = 0.8f
        if (width <= height) {
            shader.setFloatUniform("uBound", 0f, 1f - heightRatio, 1f, heightRatio)
        } else {
            val aspectRatio = width.toFloat() / height.toFloat()
            val contentCenterY = 1f - heightRatio / 2f
            shader.setFloatUniform(
                "uBound",
                0f,
                contentCenterY - aspectRatio / 2f,
                1f,
                aspectRatio,
            )
        }

        val offset = preset.pointOffset
        for (i in 0 until 4) {
            val srcX = preset.points[i * 3]
            val srcY = preset.points[i * 3 + 1]
            val animX = srcX + sin(timeSeconds + srcY) * offset
            val animY = srcY + cos(timeSeconds + animX) * offset
            pointsAnim[i * 2] = animX
            pointsAnim[i * 2 + 1] = animY
        }
        shader.setFloatUniform("uPointsAnim", pointsAnim)

        val delay = preset.colorInterpPeriod * 0.5f
        val stage = ((timeSeconds - delay) / 1.2f).coerceIn(0f, 1f)
        for (i in 0 until 16) {
            colors[i] = preset.colors2[i] + (preset.colors1[i] - preset.colors2[i]) * stage
        }
        shader.setFloatUniform("uColors", colors)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}

private class CanvasMiuixBackgroundRenderer(
    darkMode: Boolean,
    private val surfaceColor: Int,
) : NativeBackgroundRenderer {

    private val preset = BgEffectConfig.get(DeviceType.PHONE, darkMode)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeSeconds: Float,
    ) {
        canvas.drawColor(surfaceColor)
        if (width <= 0 || height <= 0) return

        val colors = preset.colors2
        val offset = preset.pointOffset
        val baseRadius = min(width.toFloat(), height * 0.8f)

        for (i in 0 until 4) {
            val srcX = preset.points[i * 3]
            val srcY = preset.points[i * 3 + 1]
            val radius = preset.points[i * 3 + 2] * baseRadius
            val animX = srcX + sin(timeSeconds + srcY) * offset
            val animY = srcY + cos(timeSeconds + animX) * offset

            val x = animX * width
            val y = height * (0.8f - 0.8f * animY)

            val r = (colors[i * 4] * 255f).roundToInt().coerceIn(0, 255)
            val g = (colors[i * 4 + 1] * 255f).roundToInt().coerceIn(0, 255)
            val b = (colors[i * 4 + 2] * 255f).roundToInt().coerceIn(0, 255)
            val a = (colors[i * 4 + 3] * 220f).roundToInt().coerceIn(0, 255)

            paint.shader =
                RadialGradient(
                    x,
                    y,
                    radius,
                    Color.argb(a, r, g, b),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP,
                )
            canvas.drawCircle(x, y, radius, paint)
        }
        paint.shader = null
    }
}

private fun segment(
    progress: Float,
    start: Float,
    end: Float,
): Float {
    if (end <= start) return if (progress >= end) 1f else 0f
    return ((progress - start) / (end - start)).coerceIn(0f, 1f)
}

private fun easeOutBack(
    value: Float,
    overshoot: Float = 1.70158f,
): Float {
    val x = value - 1f
    val c3 = overshoot + 1f
    return 1f + c3 * x * x * x + overshoot * x * x
}
