// Adapted from Kyant0/AndroidLiquidGlass — https://github.com/Kyant0/AndroidLiquidGlass (Apache 2.0).
// Mirrored from compose-miuix-ui example.

package com.alohaoneui.app.ui.component.liquid

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtMost
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.runtimeShaderEffect

fun BackdropEffectScope.lens(
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Float = 0f,
    imageCompression: Float = 0f,
) {
    if (!isRuntimeShaderSupported()) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return

    if (padding < refractionAmount) {
        padding = refractionAmount
    }

    val radii = roundedRectCornerRadii() ?: return

    val dispersionEnabled = chromaticAberration > 0f
    val shaderString =
        if (dispersionEnabled) {
            ROUNDED_RECT_REFRACTION_WITH_DISPERSION_SHADER
        } else {
            ROUNDED_RECT_REFRACTION_SHADER
        }
    val key = if (dispersionEnabled) "LiquidGlassLensDispersion" else "LiquidGlassLens"

    val sf = downscaleFactor.coerceAtLeast(1).toFloat()
    val scaledSizeW = size.width / sf
    val scaledSizeH = size.height / sf
    val scaledPadding = padding / sf
    val scaledRefractionHeight = refractionHeight / sf
    val scaledRefractionAmount = refractionAmount / sf
    val scaledRadii = FloatArray(radii.size) { radii[it] / sf }

    runtimeShaderEffect(
        key = key,
        shaderString = shaderString,
        uniformShaderName = "content",
    ) {
        setFloatUniform("size", scaledSizeW, scaledSizeH)
        setFloatUniform("offset", -scaledPadding, -scaledPadding)
        setFloatUniform("cornerRadii", scaledRadii)
        setFloatUniform("refractionHeight", scaledRefractionHeight)
        // The rounded-rect gradient points outward. A negative sample offset
        // therefore pulls the sampled image inward, matching the bottom-bar lens.
        setFloatUniform("refractionAmount", -scaledRefractionAmount)
        setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
        setFloatUniform("imageCompression", imageCompression.coerceIn(0f, 0.75f))
        if (dispersionEnabled) {
            setFloatUniform("chromaticAberration", chromaticAberration)
        }
    }
}

private fun BackdropEffectScope.roundedRectCornerRadii(): FloatArray? {
    val cornerShape = shape as? CornerBasedShape ?: return null
    val sizePx = size
    val maxRadius = sizePx.minDimension / 2f
    val isLtr = layoutDirection == LayoutDirection.Ltr
    val topLeft = if (isLtr) cornerShape.topStart.toPx(sizePx, this) else cornerShape.topEnd.toPx(sizePx, this)
    val topRight = if (isLtr) cornerShape.topEnd.toPx(sizePx, this) else cornerShape.topStart.toPx(sizePx, this)
    val bottomRight = if (isLtr) cornerShape.bottomEnd.toPx(sizePx, this) else cornerShape.bottomStart.toPx(sizePx, this)
    val bottomLeft = if (isLtr) cornerShape.bottomStart.toPx(sizePx, this) else cornerShape.bottomEnd.toPx(sizePx, this)
    return floatArrayOf(
        topLeft.fastCoerceAtMost(maxRadius),
        topRight.fastCoerceAtMost(maxRadius),
        bottomRight.fastCoerceAtMost(maxRadius),
        bottomLeft.fastCoerceAtMost(maxRadius),
    )
}

private const val ROUNDED_RECT_SDF = """
float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}
"""

private const val ROUNDED_RECT_REFRACTION_SHADER = """
uniform shader content;

uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
uniform float imageCompression;

$ROUNDED_RECT_SDF

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = (coord + offset) - halfSize;
    float radius = radiusAt(coord, cornerRadii);

    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);

    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    // Derive the normal from the signed-distance field itself. Finite
    // differences keep the top/bottom edges and both capsule caps in the
    // same continuous normal field, avoiding the end-cap pinching artifact.
    float gradientStep = 0.5;
    float distanceRight = sdRoundedRect(centeredCoord + float2(gradientStep, 0.0), halfSize, radius);
    float distanceLeft = sdRoundedRect(centeredCoord - float2(gradientStep, 0.0), halfSize, radius);
    float distanceDown = sdRoundedRect(centeredCoord + float2(0.0, gradientStep), halfSize, radius);
    float distanceUp = sdRoundedRect(centeredCoord - float2(0.0, gradientStep), halfSize, radius);
    float2 grad = normalize(float2(distanceRight - distanceLeft, distanceDown - distanceUp) + float2(0.0001, 0.0001));

    // Use the capsule's centerline as the optical axis. A rectangular/elliptic
    // radial field over-compresses the two ends and creates a round "nipple"
    // bulge. The capsule field stays vertical along the straight sides and
    // turns smoothly around each semicircular end.
    float capsuleRadius = min(min(cornerRadii.x, cornerRadii.y), halfSize.y);
    capsuleRadius = min(capsuleRadius, min(cornerRadii.z, cornerRadii.w));
    capsuleRadius = max(capsuleRadius, 0.001);
    float axisHalfLength = max(halfSize.x - capsuleRadius, 0.0);
    float2 capsuleAxisPoint = float2(clamp(centeredCoord.x, -axisHalfLength, axisHalfLength), 0.0);
    float2 capsuleRadial = centeredCoord - capsuleAxisPoint;
    float radialPosition = clamp(length(capsuleRadial) / capsuleRadius, 0.0, 1.0);
    // Compress relative to the capsule centerline, not the switch's global
    // center. On the straight rail this is purely vertical; around each end
    // cap it follows the local circular radius, preserving a smooth pill shape.
    float compression = imageCompression
        * smoothstep(0.0, 1.0, radialPosition);
    // Pull the sampled image inward along each capsule end cap. This shortens
    // the refracted rail symmetrically without shifting the whole image.
    float2 compressedCoord = coord - float2(capsuleRadial.x * compression * 0.55, capsuleRadial.y * compression);
    // Let horizontal refraction build inward from the edge, but make it zero
    // exactly at the boundary so the captured rail remains connected to both
    // left and right edges without a clipped or detached end-cap.
    float edgeRefraction = smoothstep(0.0, refractionHeight, -sd);
    float capRefraction = (1.0 - 0.55 * abs(grad.x)) * edgeRefraction;
    float2 opticalOffset = d * float2(grad.x * capRefraction, grad.y);
    float2 refractedCoord = compressedCoord + opticalOffset;
    return content.eval(refractedCoord);
}
"""

private const val ROUNDED_RECT_REFRACTION_WITH_DISPERSION_SHADER = """
uniform shader content;

uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
uniform float imageCompression;
uniform float chromaticAberration;

$ROUNDED_RECT_SDF

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = (coord + offset) - halfSize;
    float radius = radiusAt(coord, cornerRadii);

    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);

    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    // Derive the normal from the signed-distance field itself. Finite
    // differences keep the top/bottom edges and both capsule caps in the
    // same continuous normal field, avoiding the end-cap pinching artifact.
    float gradientStep = 0.5;
    float distanceRight = sdRoundedRect(centeredCoord + float2(gradientStep, 0.0), halfSize, radius);
    float distanceLeft = sdRoundedRect(centeredCoord - float2(gradientStep, 0.0), halfSize, radius);
    float distanceDown = sdRoundedRect(centeredCoord + float2(0.0, gradientStep), halfSize, radius);
    float distanceUp = sdRoundedRect(centeredCoord - float2(0.0, gradientStep), halfSize, radius);
    float2 grad = normalize(float2(distanceRight - distanceLeft, distanceDown - distanceUp) + float2(0.0001, 0.0001));

    // Use the capsule's centerline as the optical axis. A rectangular/elliptic
    // radial field over-compresses the two ends and creates a round "nipple"
    // bulge. The capsule field stays vertical along the straight sides and
    // turns smoothly around each semicircular end.
    float capsuleRadius = min(min(cornerRadii.x, cornerRadii.y), halfSize.y);
    capsuleRadius = min(capsuleRadius, min(cornerRadii.z, cornerRadii.w));
    capsuleRadius = max(capsuleRadius, 0.001);
    float axisHalfLength = max(halfSize.x - capsuleRadius, 0.0);
    float2 capsuleAxisPoint = float2(clamp(centeredCoord.x, -axisHalfLength, axisHalfLength), 0.0);
    float2 capsuleRadial = centeredCoord - capsuleAxisPoint;
    float radialPosition = clamp(length(capsuleRadial) / capsuleRadius, 0.0, 1.0);
    // Compress relative to the capsule centerline, not the switch's global
    // center. On the straight rail this is purely vertical; around each end
    // cap it follows the local circular radius, preserving a smooth pill shape.
    float compression = imageCompression
        * smoothstep(0.0, 1.0, radialPosition);
    // Pull the sampled image inward along each capsule end cap. This shortens
    // the refracted rail symmetrically without shifting the whole image.
    float2 compressedCoord = coord - float2(capsuleRadial.x * compression * 0.55, capsuleRadial.y * compression);
    // Let horizontal refraction build inward from the edge, but make it zero
    // exactly at the boundary so the captured rail remains connected to both
    // left and right edges without a clipped or detached end-cap.
    float edgeRefraction = smoothstep(0.0, refractionHeight, -sd);
    float capRefraction = (1.0 - 0.55 * abs(grad.x)) * edgeRefraction;
    float2 opticalOffset = d * float2(grad.x * capRefraction, grad.y);
    float2 refractedCoord = compressedCoord + opticalOffset;
    float dispersionIntensity = chromaticAberration * ((centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y));
    float2 dispersedCoord = opticalOffset * dispersionIntensity;

    half4 color = half4(0.0);

    half4 red = content.eval(refractedCoord + dispersedCoord);
    color.r += red.r / 3.5;
    color.a += red.a / 7.0;

    half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
    color.r += orange.r / 3.5;
    color.g += orange.g / 7.0;
    color.a += orange.a / 7.0;

    half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
    color.r += yellow.r / 3.5;
    color.g += yellow.g / 3.5;
    color.a += yellow.a / 7.0;

    half4 green = content.eval(refractedCoord);
    color.g += green.g / 3.5;
    color.a += green.a / 7.0;

    half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
    color.g += cyan.g / 3.5;
    color.b += cyan.b / 3.0;
    color.a += cyan.a / 7.0;

    half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
    color.b += blue.b / 3.0;
    color.a += blue.a / 7.0;

    half4 purple = content.eval(refractedCoord - dispersedCoord);
    color.r += purple.r / 7.0;
    color.b += purple.b / 3.0;
    color.a += purple.a / 7.0;

    return color;
}
"""
