package com.alohaoneui.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Shared One UI 8.5 / 9-inspired feature tiles: rounded colorful gradients,
 * bold symbols, restrained gloss and soft depth. Original artwork for Aloha OneUI.
 */
internal enum class OneUiFeatureIconKind {
    OtherFunctions, GlobalShortcuts,
    StatusPadding, StatusTime, StatusNotification, StatusNetworkSpeed, StatusPower,
    StatusCombo, StatusSignal, StatusWifi, StatusBluetooth, StatusLockscreen,
    StatusNotificationCenter, StatusControlCenter, StatusOther,
}

@Composable
internal fun OneUiFeatureIcon(
    kind: OneUiFeatureIconKind,
    size: Dp = 48.dp,
    contentDescription: String? = null,
) {
    val palette = when (kind) {
        OneUiFeatureIconKind.OtherFunctions -> listOf(Color(0xFF12D0AE), Color(0xFF128ED8), Color(0xFF4C5CE4))
        OneUiFeatureIconKind.GlobalShortcuts -> listOf(Color(0xFF27C9EF), Color(0xFF3C79EE), Color(0xFF7650E8))
        OneUiFeatureIconKind.StatusPadding -> listOf(Color(0xFF40D2C7), Color(0xFF198FD6), Color(0xFF4B66E8))
        OneUiFeatureIconKind.StatusTime -> listOf(Color(0xFF54B9F5), Color(0xFF6865E9), Color(0xFF8755D9))
        OneUiFeatureIconKind.StatusNotification -> listOf(Color(0xFFFF9B80), Color(0xFFEF5B85), Color(0xFFB54DD4))
        OneUiFeatureIconKind.StatusNetworkSpeed -> listOf(Color(0xFF4BE0A0), Color(0xFF13B7AE), Color(0xFF168EC8))
        OneUiFeatureIconKind.StatusPower -> listOf(Color(0xFFFFD168), Color(0xFFFF9955), Color(0xFFE95E69))
        OneUiFeatureIconKind.StatusCombo -> listOf(Color(0xFFB28AF7), Color(0xFF7468EB), Color(0xFF4D7CEB))
        OneUiFeatureIconKind.StatusSignal -> listOf(Color(0xFFFFD06A), Color(0xFFFFA047), Color(0xFFEE6A66))
        OneUiFeatureIconKind.StatusWifi -> listOf(Color(0xFF60E0E4), Color(0xFF278FE3), Color(0xFF5363E9))
        OneUiFeatureIconKind.StatusBluetooth -> listOf(Color(0xFF62C9FF), Color(0xFF3E81F1), Color(0xFF5859D9))
        OneUiFeatureIconKind.StatusLockscreen -> listOf(Color(0xFFC29AF9), Color(0xFF8D69EC), Color(0xFF5D5FE3))
        OneUiFeatureIconKind.StatusNotificationCenter -> listOf(Color(0xFF72E3A9), Color(0xFF1CBFA8), Color(0xFF218FD7))
        OneUiFeatureIconKind.StatusControlCenter -> listOf(Color(0xFF61DBD1), Color(0xFF299FD9), Color(0xFF586CE7))
        OneUiFeatureIconKind.StatusOther -> listOf(Color(0xFFAEB9CB), Color(0xFF7385A2), Color(0xFF485C83))
    }

    Canvas(
        modifier = Modifier.size(size).semantics {
            contentDescription?.let { this.contentDescription = it }
        },
    ) {
        val w = this.size.width
        val h = this.size.height
        val u = minOf(w, h)
        val white = Color.White
        val shade = Color(0xFF16305A).copy(alpha = 0.25f)
        val tileRadius = CornerRadius(u * 0.255f, u * 0.255f)
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        fun rr(x: Float, y: Float, width: Float, height: Float, radius: Float, color: Color) {
            drawRoundRect(color, p(x, y), Size(width * u, height * u), CornerRadius(radius * u, radius * u))
        }
        fun ln(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Color = white) {
            drawLine(shade, p(x1, y1 + 0.014f), p(x2, y2 + 0.014f), thickness * u, StrokeCap.Round)
            drawLine(color, p(x1, y1), p(x2, y2), thickness * u, StrokeCap.Round)
        }
        fun dot(x: Float, y: Float, radius: Float, color: Color = white) {
            drawCircle(shade, radius * u, p(x, y + 0.014f))
            drawCircle(color, radius * u, p(x, y))
        }
        fun strokeRect(x: Float, y: Float, width: Float, height: Float, radius: Float, color: Color = white) {
            drawRoundRect(shade, p(x, y + 0.012f), Size(width * u, height * u), CornerRadius(radius * u, radius * u), style = Stroke(u * 0.055f))
            drawRoundRect(color, p(x, y), Size(width * u, height * u), CornerRadius(radius * u, radius * u), style = Stroke(u * 0.055f))
        }
        fun poly(points: List<Offset>, width: Float, color: Color = white, fill: Boolean = false) {
            val path = Path().apply {
                moveTo(points.first().x * u, points.first().y * u)
                points.drop(1).forEach { lineTo(it.x * u, it.y * u) }
                if (fill) close()
            }
            drawPath(path, shade, style = if (fill) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width * u, cap = StrokeCap.Round))
            drawPath(path, color, style = if (fill) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width * u, cap = StrokeCap.Round))
        }

        drawRoundRect(Color.Black.copy(alpha = 0.20f), Offset(0f, u * 0.026f), Size(w, h), tileRadius)
        drawRoundRect(Brush.linearGradient(palette, Offset(0f, 0f), Offset(w, h)), size = Size(w, h), cornerRadius = tileRadius)
        drawRoundRect(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.04f), Color.Transparent), 0f, h * 0.72f),
            size = Size(w, h), cornerRadius = tileRadius,
        )
        drawRoundRect(
            Color.White.copy(alpha = 0.24f), Offset(u * 0.018f, u * 0.018f),
            Size(w - u * 0.036f, h - u * 0.036f),
            CornerRadius(u * 0.235f, u * 0.235f), style = Stroke(u * 0.018f),
        )

        when (kind) {
            OneUiFeatureIconKind.OtherFunctions -> {
                listOf(0.35f, 0.50f, 0.65f).forEachIndexed { i, y ->
                    ln(0.23f, y, 0.77f, y, 0.065f, Color.White.copy(alpha = 0.72f))
                    val x = listOf(0.42f, 0.62f, 0.49f)[i]
                    dot(x, y, 0.075f)
                    drawCircle(palette[1], u * 0.034f, p(x, y - 0.008f))
                }
            }
            OneUiFeatureIconKind.GlobalShortcuts -> {
                ln(0.34f, 0.29f, 0.34f, 0.52f, 0.09f)
                ln(0.50f, 0.23f, 0.50f, 0.54f, 0.09f)
                ln(0.66f, 0.29f, 0.66f, 0.52f, 0.09f)
                ln(0.27f, 0.71f, 0.69f, 0.71f, 0.07f)
                ln(0.57f, 0.60f, 0.69f, 0.71f, 0.07f)
                ln(0.57f, 0.82f, 0.69f, 0.71f, 0.07f)
            }
            OneUiFeatureIconKind.StatusPadding -> {
                strokeRect(0.22f, 0.25f, 0.56f, 0.50f, 0.06f)
                strokeRect(0.36f, 0.37f, 0.28f, 0.26f, 0.035f, Color.White.copy(alpha = 0.96f))
                ln(0.12f, 0.50f, 0.29f, 0.50f, 0.045f)
                ln(0.22f, 0.43f, 0.29f, 0.50f, 0.045f)
                ln(0.22f, 0.57f, 0.29f, 0.50f, 0.045f)
                ln(0.71f, 0.50f, 0.88f, 0.50f, 0.045f)
            }
            OneUiFeatureIconKind.StatusTime -> {
                dot(0.50f, 0.50f, 0.29f)
                drawCircle(palette[2], u * 0.235f, p(0.50f, 0.50f))
                drawCircle(Color.White.copy(alpha = 0.96f), u * 0.205f, p(0.50f, 0.50f))
                ln(0.50f, 0.50f, 0.50f, 0.34f, 0.045f, palette[2])
                ln(0.50f, 0.50f, 0.64f, 0.57f, 0.045f, palette[2])
                dot(0.50f, 0.50f, 0.032f, palette[1])
            }
            OneUiFeatureIconKind.StatusNotification -> {
                val bell = Path().apply {
                    moveTo(u * 0.50f, u * 0.22f)
                    quadraticTo(u * 0.39f, u * 0.22f, u * 0.38f, u * 0.37f)
                    lineTo(u * 0.35f, u * 0.57f)
                    quadraticTo(u * 0.34f, u * 0.64f, u * 0.28f, u * 0.67f)
                    lineTo(u * 0.72f, u * 0.67f)
                    quadraticTo(u * 0.66f, u * 0.64f, u * 0.65f, u * 0.57f)
                    lineTo(u * 0.62f, u * 0.37f)
                    quadraticTo(u * 0.61f, u * 0.22f, u * 0.50f, u * 0.22f)
                    close()
                }
                drawPath(bell, shade)
                drawPath(bell, white)
                dot(0.50f, 0.74f, 0.045f)
                dot(0.50f, 0.20f, 0.022f, Color.White.copy(alpha = 0.90f))
            }
            OneUiFeatureIconKind.StatusNetworkSpeed -> {
                ln(0.34f, 0.69f, 0.34f, 0.31f, 0.075f)
                ln(0.23f, 0.42f, 0.34f, 0.31f, 0.075f)
                ln(0.45f, 0.42f, 0.34f, 0.31f, 0.075f)
                ln(0.66f, 0.30f, 0.66f, 0.69f, 0.075f)
                ln(0.55f, 0.58f, 0.66f, 0.69f, 0.075f)
                ln(0.77f, 0.58f, 0.66f, 0.69f, 0.075f)
            }
            OneUiFeatureIconKind.StatusPower -> {
                strokeRect(0.18f, 0.33f, 0.63f, 0.35f, 0.07f)
                rr(0.81f, 0.43f, 0.06f, 0.15f, 0.02f, white)
                poly(listOf(Offset(0.55f,0.36f),Offset(0.40f,0.52f),Offset(0.50f,0.52f),Offset(0.44f,0.64f),Offset(0.63f,0.46f),Offset(0.52f,0.46f)), 0.04f, Color(0xFFFFD05D), true)
            }
            OneUiFeatureIconKind.StatusCombo -> {
                rr(0.23f,0.29f,0.54f,0.13f,0.055f,white)
                rr(0.23f,0.47f,0.39f,0.10f,0.04f,Color.White.copy(alpha=0.92f))
                rr(0.23f,0.63f,0.49f,0.10f,0.04f,Color.White.copy(alpha=0.80f))
                dot(0.73f,0.52f,0.06f)
            }
            OneUiFeatureIconKind.StatusSignal -> {
                val xs=listOf(0.22f,0.37f,0.52f,0.67f); val hs=listOf(0.20f,0.34f,0.49f,0.64f)
                xs.indices.forEach { i -> rr(xs[i],0.76f-hs[i],0.085f,hs[i],0.035f,white.copy(alpha=0.72f+i*0.09f)) }
            }
            OneUiFeatureIconKind.StatusWifi -> {
                listOf(0.31f,0.22f,0.12f).forEachIndexed { i, r ->
                    val y=0.33f+i*0.12f
                    val arc=Path().apply { moveTo(u*(0.50f-r*1.55f),u*y); quadraticTo(u*0.50f,u*(y-r*1.45f),u*(0.50f+r*1.55f),u*y) }
                    drawPath(arc,shade,style=Stroke(u*0.065f,cap=StrokeCap.Round)); drawPath(arc,white,style=Stroke(u*0.065f,cap=StrokeCap.Round))
                }
                dot(0.50f,0.72f,0.055f)
            }
            OneUiFeatureIconKind.StatusBluetooth -> {
                ln(0.49f,0.22f,0.49f,0.78f,0.065f)
                ln(0.49f,0.22f,0.70f,0.39f,0.065f); ln(0.70f,0.39f,0.32f,0.64f,0.065f)
                ln(0.32f,0.36f,0.70f,0.61f,0.065f); ln(0.70f,0.61f,0.49f,0.78f,0.065f)
            }
            OneUiFeatureIconKind.StatusLockscreen -> {
                rr(0.25f,0.45f,0.50f,0.32f,0.075f,white)
                val arch=Path().apply { moveTo(u*0.34f,u*0.47f); lineTo(u*0.34f,u*0.39f); quadraticTo(u*0.34f,u*0.24f,u*0.50f,u*0.24f); quadraticTo(u*0.66f,u*0.24f,u*0.66f,u*0.39f); lineTo(u*0.66f,u*0.47f) }
                drawPath(arch,shade,style=Stroke(u*0.08f,cap=StrokeCap.Round)); drawPath(arch,white,style=Stroke(u*0.08f,cap=StrokeCap.Round))
                dot(0.50f,0.58f,0.035f,palette[2]); ln(0.50f,0.59f,0.50f,0.66f,0.035f,palette[2])
            }
            OneUiFeatureIconKind.StatusNotificationCenter -> {
                rr(0.26f,0.23f,0.51f,0.43f,0.045f,Color.White.copy(alpha=0.48f))
                rr(0.21f,0.34f,0.51f,0.43f,0.045f,Color.White.copy(alpha=0.72f))
                rr(0.27f,0.45f,0.51f,0.34f,0.045f,white)
                rr(0.35f,0.53f,0.26f,0.045f,0.022f,palette[1]); rr(0.35f,0.62f,0.32f,0.035f,0.018f,palette[0])
            }
            OneUiFeatureIconKind.StatusControlCenter -> {
                listOf(listOf(0.22f,0.24f),listOf(0.53f,0.24f),listOf(0.22f,0.53f),listOf(0.53f,0.53f)).forEachIndexed { i, pos ->
                    rr(pos[0],pos[1],0.25f,0.22f,0.055f,if(i==0) white else white.copy(alpha=0.78f))
                }
                dot(0.36f,0.35f,0.045f,palette[1]); dot(0.66f,0.64f,0.045f,palette[1])
            }
            OneUiFeatureIconKind.StatusOther -> {
                drawCircle(white.copy(alpha=0.15f),u*0.24f,p(0.50f,0.50f))
                drawCircle(shade,u*0.25f,p(0.50f,0.51f),style=Stroke(u*0.07f))
                drawCircle(white,u*0.25f,p(0.50f,0.50f),style=Stroke(u*0.07f))
                drawCircle(palette[2],u*0.09f,p(0.50f,0.50f))
                listOf(0,60,120,180,240,300).forEach { a -> val rad=a*PI/180.0; dot(0.5f+cos(rad).toFloat()*0.32f,0.5f+sin(rad).toFloat()*0.32f,0.025f) }
            }
        }
    }
}
