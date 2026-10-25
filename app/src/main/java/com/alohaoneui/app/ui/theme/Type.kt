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


fun Typography.withMaterialFontFamily(value: String, boldText: Boolean = false): Typography {
    val family = when (value) {
        "SansSerif" -> FontFamily.SansSerif
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }
    fun TextStyle.withFamilyAndWeight(): TextStyle = copy(
        fontFamily = family,
        fontWeight = if (boldText) FontWeight.Bold else fontWeight,
    )
    return copy(
        displayLarge = displayLarge.withFamilyAndWeight(),
        displayMedium = displayMedium.withFamilyAndWeight(),
        displaySmall = displaySmall.withFamilyAndWeight(),
        headlineLarge = headlineLarge.withFamilyAndWeight(),
        headlineMedium = headlineMedium.withFamilyAndWeight(),
        headlineSmall = headlineSmall.withFamilyAndWeight(),
        titleLarge = titleLarge.withFamilyAndWeight(),
        titleMedium = titleMedium.withFamilyAndWeight(),
        titleSmall = titleSmall.withFamilyAndWeight(),
        bodyLarge = bodyLarge.withFamilyAndWeight(),
        bodyMedium = bodyMedium.withFamilyAndWeight(),
        bodySmall = bodySmall.withFamilyAndWeight(),
        labelLarge = labelLarge.withFamilyAndWeight(),
        labelMedium = labelMedium.withFamilyAndWeight(),
        labelSmall = labelSmall.withFamilyAndWeight(),
    )
}
