package io.github.soclear.oneuix.hook.util

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ReplacementSpan
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarClockBlockParser
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.roundToInt

object StatusBarClockRichFormatter {
    fun format(
        pattern: String,
        dateTime: LocalDateTime,
        resources: Resources,
        styles: List<Preference.SystemUI.StatusBarClockBlockStyle>,
    ): CharSequence {
        val parsed = StatusBarClockBlockParser.parse(pattern)
        val byKey = styles.associateBy { it.blockIndex to it.source }
        val output = SpannableStringBuilder()

        parsed.forEach { block ->
            if (block.isLineBreak) {
                output.append('\n')
                return@forEach
            }

            val rendered = block.literalText ?: runCatching {
                StatusBarClockFormatter.format(block.source, dateTime)
            }.getOrElse {
                block.source
            }
            val start = output.length
            output.append(rendered)
            val end = output.length
            val styleIndex = block.styleIndex ?: return@forEach
            val style = byKey[styleIndex to block.source] ?: return@forEach
            if (!style.enabled || start == end) return@forEach

            output.setSpan(
                StatusBarClockBlockSpan(resources, style),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

        return output
    }
}

private class StatusBarClockBlockSpan(
    resources: Resources,
    private val style: Preference.SystemUI.StatusBarClockBlockStyle,
) : ReplacementSpan() {
    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private fun styledPaint(base: Paint): Paint {
        val config = style
        return Paint(base).apply {
            if (config.overrideTextSize) {
                textSize = config.textSizeSp.coerceIn(6f, 32f) * scaledDensity
            }
            if (config.overrideFontWeight) {
                typeface = Typeface.create(
                    typeface ?: Typeface.DEFAULT,
                    config.fontWeight.coerceIn(100, 900),
                    false,
                )
            }
            if (config.overrideTextColor) {
                color = config.textColorArgb
            }
        }
    }

    private val horizontalPaddingPx: Float
        get() = if (style.backgroundEnabled) {
            style.horizontalPaddingDp.coerceIn(0f, 12f) * density
        } else {
            0f
        }

    private val verticalPaddingPx: Float
        get() = if (style.backgroundEnabled) {
            style.verticalPaddingDp.coerceIn(0f, 6f) * density
        } else {
            0f
        }

    private val borderWidthPx: Float
        get() = if (style.backgroundEnabled && style.backgroundBorderEnabled) {
            style.backgroundBorderWidthDp.coerceIn(0f, 6f) * density
        } else {
            0f
        }

    private fun baselineOffset(base: Paint, styled: Paint): Float {
        val baseMetrics = base.fontMetrics
        val styledMetrics = styled.fontMetrics
        return when (style.verticalAlignment) {
            "top" -> baseMetrics.ascent - styledMetrics.ascent
            "bottom" -> baseMetrics.descent - styledMetrics.descent
            else -> (
                (baseMetrics.ascent + baseMetrics.descent) -
                    (styledMetrics.ascent + styledMetrics.descent)
                ) / 2f
        }
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        val styled = styledPaint(paint)
        val width = styled.measureText(text, start, end) + horizontalPaddingPx * 2f

        fm?.let {
            val baseMetrics = paint.fontMetricsInt
            val styledMetrics = styled.fontMetricsInt
            val shift = baselineOffset(paint, styled).roundToInt()
            val extra = ceil(verticalPaddingPx + borderWidthPx / 2f).toInt()
            it.top = minOf(baseMetrics.top, styledMetrics.top + shift - extra)
            it.ascent = minOf(baseMetrics.ascent, styledMetrics.ascent + shift - extra)
            it.descent = maxOf(baseMetrics.descent, styledMetrics.descent + shift + extra)
            it.bottom = maxOf(baseMetrics.bottom, styledMetrics.bottom + shift + extra)
            it.leading = maxOf(baseMetrics.leading, styledMetrics.leading)
        }

        return ceil(width).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val styled = styledPaint(paint)
        val textWidth = styled.measureText(text, start, end)
        val horizontalPadding = horizontalPaddingPx
        val verticalPadding = verticalPaddingPx
        val baseline = y + baselineOffset(paint, styled)

        if (style.backgroundEnabled) {
            val value = text.subSequence(start, end).toString()
            val glyphBounds = Rect()
            styled.getTextBounds(value, 0, value.length, glyphBounds)
            val metrics = styled.fontMetrics
            val glyphTop = if (glyphBounds.height() > 0) {
                baseline + glyphBounds.top
            } else {
                baseline + metrics.ascent
            }
            val glyphBottom = if (glyphBounds.height() > 0) {
                baseline + glyphBounds.bottom
            } else {
                baseline + metrics.descent
            }
            val rect = RectF(
                x,
                glyphTop - verticalPadding,
                x + textWidth + horizontalPadding * 2f,
                glyphBottom + verticalPadding,
            )
            val config = style
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val baseAlpha = Color.alpha(config.backgroundColorArgb)
                val alpha = (
                    baseAlpha * config.backgroundAlpha.coerceIn(0f, 1f)
                ).roundToInt().coerceIn(0, 255)
                color = Color.argb(
                    alpha,
                    Color.red(config.backgroundColorArgb),
                    Color.green(config.backgroundColorArgb),
                    Color.blue(config.backgroundColorArgb),
                )
                this.style = Paint.Style.FILL
            }
            val radius = config.cornerRadiusDp.coerceIn(0f, 16f) * density
            canvas.drawRoundRect(rect, radius, radius, bgPaint)

            if (config.backgroundBorderEnabled && borderWidthPx > 0f) {
                val strokeRect = RectF(rect)
                val halfStroke = borderWidthPx / 2f
                strokeRect.inset(halfStroke, halfStroke)
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = borderWidthPx
                    color = config.backgroundBorderColorArgb
                }
                canvas.drawRoundRect(
                    strokeRect,
                    maxOf(0f, radius - halfStroke),
                    maxOf(0f, radius - halfStroke),
                    borderPaint,
                )
            }
        }

        canvas.drawText(
            text,
            start,
            end,
            x + horizontalPadding,
            baseline,
            styled,
        )
    }
}
