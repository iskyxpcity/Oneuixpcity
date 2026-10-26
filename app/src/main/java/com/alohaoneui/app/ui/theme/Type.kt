package com.alohaoneui.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material text defaults aligned with the KernelSU v3.3.1 expressive theme.
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)


/** Applies Voyage's exposed font-family and bold-text preferences to Material typography. */
fun androidx.compose.material3.Typography.withMaterialFontFamily(
    family: String,
    bold: Boolean,
): androidx.compose.material3.Typography {
    val selectedFamily = if (family.equals("Serif", ignoreCase = true)) FontFamily.Serif else FontFamily.Default
    fun TextStyle.withFont() = copy(
        fontFamily = selectedFamily,
        fontWeight = if (bold) FontWeight.Bold else fontWeight,
    )
    return copy(
        displayLarge = displayLarge.withFont(),
        displayMedium = displayMedium.withFont(),
        displaySmall = displaySmall.withFont(),
        headlineLarge = headlineLarge.withFont(),
        headlineMedium = headlineMedium.withFont(),
        headlineSmall = headlineSmall.withFont(),
        titleLarge = titleLarge.withFont(),
        titleMedium = titleMedium.withFont(),
        titleSmall = titleSmall.withFont(),
        bodyLarge = bodyLarge.withFont(),
        bodyMedium = bodyMedium.withFont(),
        bodySmall = bodySmall.withFont(),
        labelLarge = labelLarge.withFont(),
        labelMedium = labelMedium.withFont(),
        labelSmall = labelSmall.withFont(),
    )
}
