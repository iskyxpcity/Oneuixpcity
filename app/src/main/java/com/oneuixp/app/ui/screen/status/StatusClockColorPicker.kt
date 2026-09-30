package com.oneuixp.app.ui.screen.status

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.oneuixp.app.R
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import io.github.soclear.oneuix.ui.component.OneuiXActionButton
import io.github.soclear.oneuix.ui.component.OneuiXInlineTextField
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.math.roundToInt

private val CLOCK_COLOR_PRESETS = listOf(
    0xFFFFFFFF.toInt(),
    0xFFBDBDBD.toInt(),
    0xFF757575.toInt(),
    0xFF212121.toInt(),
    0xFFF44336.toInt(),
    0xFFFF9800.toInt(),
    0xFFFFEB3B.toInt(),
    0xFF4CAF50.toInt(),
    0xFF00BCD4.toInt(),
    0xFF2196F3.toInt(),
    0xFF673AB7.toInt(),
    0xFFE91E63.toInt(),
)

private fun clockColorText(color: Int): String =
    "#%08X".format(color.toLong() and 0xFFFFFFFFL)

private fun parseClockColor(value: String): Int? {
    val raw = value.trim().removePrefix("#")
    if (raw.length != 6 && raw.length != 8) return null
    val parsed = raw.toLongOrNull(16) ?: return null
    return if (raw.length == 6) {
        (0xFF000000L or parsed).toInt()
    } else {
        parsed.toInt()
    }
}

@Composable
private fun PickerText(
    text: String,
    modifier: Modifier = Modifier,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialText(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
        )

        UiMode.Miuix -> MiuixText(
            text = text,
            modifier = modifier,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
        )
    }
}

@Composable
private fun SaturationValuePicker(
    colorArgb: Int,
    onColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hsv = remember(colorArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(colorArgb, it) }
    }
    val hue = hsv[0]
    val saturation = hsv[1]
    val brightness = hsv[2]
    val alpha = AndroidColor.alpha(colorArgb)

    fun select(position: Offset, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        val nextSaturation = (position.x / width).coerceIn(0f, 1f)
        val nextBrightness = (1f - position.y / height).coerceIn(0f, 1f)
        onColorChange(
            AndroidColor.HSVToColor(
                alpha,
                floatArrayOf(hue, nextSaturation, nextBrightness),
            )
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(hue, alpha) {
                detectTapGestures { position ->
                    select(position, size.width.toFloat(), size.height.toFloat())
                }
            }
            .pointerInput(hue, alpha) {
                detectDragGestures(
                    onDragStart = { position ->
                        select(position, size.width.toFloat(), size.height.toFloat())
                    },
                    onDrag = { change, _ ->
                        select(
                            change.position,
                            size.width.toFloat(),
                            size.height.toFloat(),
                        )
                    },
                )
            },
    ) {
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.White,
                    Color.hsv(hue, 1f, 1f),
                )
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    Color.Black,
                )
            )
        )

        val marker = Offset(
            x = saturation.coerceIn(0f, 1f) * size.width,
            y = (1f - brightness.coerceIn(0f, 1f)) * size.height,
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.72f),
            radius = 9.dp.toPx(),
            center = marker,
            style = Stroke(width = 3.dp.toPx()),
        )
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = marker,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

@Composable
private fun ColorPickerBody(
    colorArgb: Int,
    onColorPreview: (Int) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hsv = remember(colorArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(colorArgb, it) }
    }
    val alpha = AndroidColor.alpha(colorArgb)
    var hexInput by remember { mutableStateOf(clockColorText(colorArgb)) }
    var hexError by remember { mutableStateOf(false) }

    fun setColor(value: Int) {
        hexInput = clockColorText(value)
        hexError = false
        onColorPreview(value)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(colorArgb))
                    .border(
                        width = 1.dp,
                        color = Color(0x55000000),
                        shape = CircleShape,
                    )
            )
            PickerText(
                text = clockColorText(colorArgb),
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        PickerText(text = stringResource(id = R.string.oneuix_clockRichCommonColors))
        CLOCK_COLOR_PRESETS.chunked(6).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { preset ->
                    val selected = (preset and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(selected))
                            .border(
                                width = if (
                                    (selected and 0x00FFFFFF) ==
                                    (colorArgb and 0x00FFFFFF)
                                ) 3.dp else 1.dp,
                                color = if (
                                    (selected and 0x00FFFFFF) ==
                                    (colorArgb and 0x00FFFFFF)
                                ) Color.White else Color(0x55000000),
                                shape = CircleShape,
                            )
                            .pointerInput(selected) {
                                detectTapGestures {
                                    setColor(selected)
                                }
                            },
                    )
                }
            }
        }

        SaturationValuePicker(
            colorArgb = colorArgb,
            onColorChange = ::setColor,
        )

        PickerText(
            text = stringResource(
                id = R.string.oneuix_clockRichHue,
                hsv[0].roundToInt(),
            )
        )
        OneuiXSlider(
            value = hsv[0],
            onValueChange = { hue ->
                setColor(
                    AndroidColor.HSVToColor(
                        alpha,
                        floatArrayOf(
                            hue.coerceIn(0f, 360f),
                            hsv[1],
                            hsv[2],
                        ),
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..360f,
        )

        OneuiXInlineTextField(
            value = hexInput,
            onValueChange = { value ->
                hexInput = value
                val parsed = parseClockColor(value)
                hexError =
                    parsed == null && value.trim().removePrefix("#").length >= 6
                if (parsed != null) {
                    onColorPreview(parsed)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            labelText = stringResource(
                id = if (hexError) {
                    R.string.oneuix_clockRichInvalidColor
                } else {
                    R.string.oneuix_clockRichColorHint
                }
            ),
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OneuiXActionButton(
                text = stringResource(id = android.R.string.cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            OneuiXActionButton(
                text = stringResource(id = R.string.oneuix_clockRichApplyColor),
                onClick = onApply,
                modifier = Modifier.weight(1f),
                primary = true,
            )
        }
    }
}

@Composable
fun StatusClockColorPicker(
    colorArgb: Int,
    onColorChange: (Int) -> Unit,
    onColorPreview: (Int) -> Unit = onColorChange,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    var originalColor by remember { mutableIntStateOf(colorArgb) }
    var draftColor by remember(colorArgb) { mutableIntStateOf(colorArgb) }

    fun openPicker() {
        originalColor = colorArgb
        draftColor = colorArgb
        showPicker = true
    }

    fun cancelPicker() {
        onColorPreview(originalColor)
        draftColor = originalColor
        showPicker = false
    }

    fun applyPicker() {
        onColorChange(draftColor)
        showPicker = false
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(colorArgb))
                .border(
                    width = 1.dp,
                    color = Color(0x55000000),
                    shape = CircleShape,
                )
        )
        PickerText(
            text = clockColorText(colorArgb),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        OneuiXActionButton(
            text = stringResource(id = R.string.oneuix_clockRichChooseColor),
            onClick = ::openPicker,
        )
    }

    if (showPicker) {
        when (LocalUiMode.current) {
            UiMode.Material -> Dialog(onDismissRequest = ::cancelPicker) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .heightIn(max = 680.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Column {
                        MaterialText(
                            text = stringResource(id = R.string.oneuix_clockRichColorPicker_title),
                            modifier = Modifier.padding(
                                start = 24.dp,
                                end = 24.dp,
                                top = 22.dp,
                                bottom = 4.dp,
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        ColorPickerBody(
                            colorArgb = draftColor,
                            onColorPreview = { selected ->
                                draftColor = selected
                                onColorPreview(selected)
                            },
                            onApply = ::applyPicker,
                            onCancel = ::cancelPicker,
                        )
                    }
                }
            }

            UiMode.Miuix -> WindowDialog(
                show = true,
                title = stringResource(id = R.string.oneuix_clockRichColorPicker_title),
                onDismissRequest = ::cancelPicker,
                content = {
                    ColorPickerBody(
                        colorArgb = draftColor,
                        onColorPreview = { selected ->
                            draftColor = selected
                            onColorPreview(selected)
                        },
                        onApply = ::applyPicker,
                        onCancel = ::cancelPicker,
                    )
                },
            )
        }
    }
}
