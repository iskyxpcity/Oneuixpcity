package com.alohaoneui.app.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp

/**
 * Corner-based Voyage shape variants used by Material 3 components.
 *
 * Rounded and cut corners use Compose's native primitives; this class supplies the
 * additional squircle, smooth, wavy, scoop and notch variants so changing the setting
 * changes the actual component outline rather than only the selected label.
 */
internal class VoyageExpressiveCornerShape(
    private val style: Int,
    radius: Dp,
) : CornerBasedShape(
    topStart = CornerSize(radius),
    topEnd = CornerSize(radius),
    bottomEnd = CornerSize(radius),
    bottomStart = CornerSize(radius),
) {
    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
    ): Outline {
        val limit = minOf(size.width, size.height) / 2f
        val tl = topStart.coerceIn(0f, limit)
        val tr = topEnd.coerceIn(0f, limit)
        val br = bottomEnd.coerceIn(0f, limit)
        val bl = bottomStart.coerceIn(0f, limit)
        val w = size.width
        val h = size.height
        val path = Path()
        val wave = minOf(tl, tr, br, bl).coerceAtLeast(0f) * 0.16f

        path.moveTo(tl, 0f)
        if (style == 4) waveHorizontal(path, tl, w - tr, 0f, wave) else path.lineTo(w - tr, 0f)
        topRight(path, w, tr)
        if (style == 4) waveVertical(path, tr, h - br, w, wave) else path.lineTo(w, h - br)
        bottomRight(path, w, h, br)
        if (style == 4) waveHorizontal(path, w - br, bl, h, wave) else path.lineTo(bl, h)
        bottomLeft(path, h, bl)
        if (style == 4) waveVertical(path, h - bl, tl, 0f, wave) else path.lineTo(0f, tl)
        topLeft(path, tl)
        path.close()
        return Outline.Generic(path)
    }

    private fun topRight(path: Path, w: Float, r: Float) {
        when (style) {
            5 -> path.cubicTo(w - r, r * 0.68f, w - r * 0.68f, r, w, r)
            6 -> {
                path.lineTo(w - r * 0.46f, 0f)
                path.lineTo(w - r * 0.46f, r * 0.46f)
                path.lineTo(w, r * 0.46f)
                path.lineTo(w, r)
            }
            else -> {
                val k = cornerControl()
                path.cubicTo(w - r + r * k, 0f, w, r - r * k, w, r)
            }
        }
    }

    private fun bottomRight(path: Path, w: Float, h: Float, r: Float) {
        when (style) {
            5 -> path.cubicTo(w - r * 0.68f, h - r, w - r, h - r * 0.68f, w - r, h)
            6 -> {
                path.lineTo(w, h - r * 0.46f)
                path.lineTo(w - r * 0.46f, h - r * 0.46f)
                path.lineTo(w - r * 0.46f, h)
                path.lineTo(w - r, h)
            }
            else -> {
                val k = cornerControl()
                path.cubicTo(w, h - r + r * k, w - r + r * k, h, w - r, h)
            }
        }
    }

    private fun bottomLeft(path: Path, h: Float, r: Float) {
        when (style) {
            5 -> path.cubicTo(r, h - r * 0.68f, r * 0.68f, h - r, 0f, h - r)
            6 -> {
                path.lineTo(r * 0.46f, h)
                path.lineTo(r * 0.46f, h - r * 0.46f)
                path.lineTo(0f, h - r * 0.46f)
                path.lineTo(0f, h - r)
            }
            else -> {
                val k = cornerControl()
                path.cubicTo(r - r * k, h, 0f, h - r + r * k, 0f, h - r)
            }
        }
    }

    private fun topLeft(path: Path, r: Float) {
        when (style) {
            5 -> path.cubicTo(r * 0.68f, r, r, r * 0.68f, r, 0f)
            6 -> {
                path.lineTo(0f, r * 0.46f)
                path.lineTo(r * 0.46f, r * 0.46f)
                path.lineTo(r * 0.46f, 0f)
                path.lineTo(r, 0f)
            }
            else -> {
                val k = cornerControl()
                path.cubicTo(0f, r - r * k, r - r * k, 0f, r, 0f)
            }
        }
    }

    private fun cornerControl(): Float = when (style) {
        2 -> 0.38f // Squircle: flatter sides and tighter corner transition.
        3 -> 0.68f // Smooth: broader continuous corner transition.
        else -> 0.5522848f
    }

    private fun waveHorizontal(path: Path, start: Float, end: Float, y: Float, amplitude: Float) {
        if (amplitude <= 0f || start == end) {
            path.lineTo(end, y)
            return
        }
        var current = start
        repeat(3) { index ->
            val next = start + (end - start) * (index + 1) / 3f
            val middle = (current + next) / 2f
            path.quadraticTo(middle, y + if (index % 2 == 0) amplitude else -amplitude, next, y)
            current = next
        }
    }

    private fun waveVertical(path: Path, start: Float, end: Float, x: Float, amplitude: Float) {
        if (amplitude <= 0f || start == end) {
            path.lineTo(x, end)
            return
        }
        var current = start
        repeat(3) { index ->
            val next = start + (end - start) * (index + 1) / 3f
            val middle = (current + next) / 2f
            path.quadraticTo(x + if (index % 2 == 0) amplitude else -amplitude, middle, x, next)
            current = next
        }
    }
}
