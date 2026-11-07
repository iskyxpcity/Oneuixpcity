package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.alohaoneui.app.ui.theme.LocalMaterialShadowSwitches
import com.alohaoneui.app.ui.theme.LocalMaterialSwitchStyle
import com.alohaoneui.app.ui.component.liquid.InnerShadow
import com.alohaoneui.app.ui.component.liquid.innerShadow
import com.alohaoneui.app.ui.component.liquid.switchLens
import com.alohaoneui.app.ui.component.liquid.vibrancy
import com.alohaoneui.app.ui.component.miuix.animation.DampedDragAnimation
import com.alohaoneui.app.ui.component.miuix.animation.InteractiveHighlight
import top.yukonga.miuix.kmp.blur.blur
import com.alohaoneui.app.ui.component.liquid.rememberCombinedBackdrop
import com.alohaoneui.app.ui.component.liquid.rememberVerticallyScaledBackdrop
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.sensor.rememberDeviceTilt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

val LocalMaterialSwitchBackdrop = staticCompositionLocalOf<Backdrop?> { null }

private val liquidGlassSwitchSpecular: Highlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

private const val SWITCH_LIGHT_REF_X = 0.5f
private const val SWITCH_LIGHT_REF_Y = 0.7f
private const val SWITCH_GRAVITY_DIR_THRESHOLD_SQ = 0.01f

@Composable
private fun rememberLiquidGlassSwitchHighlight(
    base: Highlight,
    extraDegrees: Float = 90f,
): Highlight {
    val baseStyle = base.style as BloomStroke
    val tilt by rememberDeviceTilt()
    val rotatedPrimary = remember(tilt, baseStyle.primaryLight, extraDegrees) {
        val basePrimary = baseStyle.primaryLight
        val gx = tilt.gravityX
        val gy = tilt.gravityY
        val gMagSq = gx * gx + gy * gy
        val (lx0, ly0) = if (gMagSq > SWITCH_GRAVITY_DIR_THRESHOLD_SQ) {
            val invMag = 1f / sqrt(gMagSq)
            (gx * invMag) to (gy * invMag)
        } else {
            0f to -1f
        }
        val rad = extraDegrees * PI / 180.0
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        val lx = c * lx0 - s * ly0
        val ly = s * lx0 + c * ly0
        basePrimary.copy(
            position = LightPosition(
                x = SWITCH_LIGHT_REF_X + lx,
                y = SWITCH_LIGHT_REF_Y + ly,
                z = basePrimary.position.z,
            ),
        )
    }
    return remember(base, rotatedPrimary) {
        base.copy(style = baseStyle.copy(primaryLight = rotatedPrimary))
    }
}

/**
 * Aloha's eight selectable switch families. The selector changes the actual
 * track/thumb geometry and palette rather than applying a cosmetic scale to
 * the platform Material switch. Toggle semantics and animated thumb travel
 * remain shared across all styles.
 */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = expressiveSwitchColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    showThumbIcon: Boolean = true,
) {
    val style = LocalMaterialSwitchStyle.current.coerceIn(0, 7)

    // Material You is rendered by the official Material 3 Switch component.
    // This keeps its measured geometry, thumb motion, semantics and state
    // treatment aligned with the AndroidX implementation instead of a
    // hand-drawn approximation.
    if (style == 0) {
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            thumbContent = thumbContent,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
        )
        return
    }

    // Compose style uses the official Compose Material (Material 2) Switch.
    // It deliberately retains Material 2 proportions and color behavior;
    // it is not a recolored Material 3 control.
    if (style == 1) {
        val scheme = MaterialTheme.colorScheme
        androidx.compose.material.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            interactionSource = interactionSource,
            colors = androidx.compose.material.SwitchDefaults.colors(
                checkedThumbColor = scheme.secondary,
                checkedTrackColor = scheme.secondary.copy(alpha = 0.54f),
                uncheckedThumbColor = scheme.outline,
                uncheckedTrackColor = scheme.surfaceVariant,
                disabledCheckedThumbColor = scheme.onSurface.copy(alpha = 0.38f),
                disabledCheckedTrackColor = scheme.onSurface.copy(alpha = 0.12f),
                disabledUncheckedThumbColor = scheme.onSurface.copy(alpha = 0.38f),
                disabledUncheckedTrackColor = scheme.onSurface.copy(alpha = 0.12f),
            ),
        )
        return
    }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val pressedOrHovered = pressed || hovered
    val interactive = pressed || hovered || focused
    val interactionProgress by animateFloatAsState(targetValue = if (interactive && enabled) 1f else 0f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 700f), label = "materialSwitchInteraction")
    // Liquid Glass uses the same drag, spring, velocity and highlight machinery
    // as FloatingBottomBar. Only the travel range and lens footprint are switch-sized.
    val animationScope = rememberCoroutineScope()
    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(LocalDensity.current) { 4.dp.toPx() }
    val scheme = MaterialTheme.colorScheme
    val isDarkTheme = scheme.surface.luminance() < 0.5f
    // Each family has its own OFF and ON color recipe. Every role is read
    // from the active Aloha Material scheme, so light/dark and user theme
    // changes propagate without embedding platform brand colors.
    val checkedTrack = when (style) {
        0 -> scheme.primary
        1 -> scheme.primaryContainer
        2 -> scheme.primary
        3 -> scheme.secondaryContainer
        4 -> scheme.tertiary
        5 -> scheme.primary
        6 -> scheme.secondary
        else -> scheme.primary
    }
    val uncheckedTrack = when (style) {
        0 -> scheme.surfaceContainerHighest
        1 -> scheme.surfaceContainerLow
        2 -> scheme.surfaceContainer
        3 -> scheme.surfaceContainerHigh
        4 -> scheme.surfaceContainerHighest
        5 -> scheme.surfaceContainerHigh
        6 -> scheme.surfaceContainer
        else -> scheme.surfaceContainerHigh
    }
    val checkedThumb = when (style) {
        0 -> scheme.onPrimary
        1 -> scheme.onPrimaryContainer
        2 -> scheme.onPrimary
        3 -> scheme.onSecondaryContainer
        4 -> scheme.onTertiary
        5 -> scheme.onPrimary
        6 -> scheme.onSecondary
        else -> scheme.onPrimary
    }
    val uncheckedThumb = when (style) {
        0 -> scheme.outline
        1 -> scheme.onSurfaceVariant
        2 -> scheme.outline
        3 -> scheme.onSurface
        4 -> scheme.surface
        5 -> Color.White
        6 -> scheme.onSurfaceVariant
        else -> scheme.surface
    }
    val checkedBorder = when (style) {
        0, 2, 7 -> scheme.primary
        1 -> scheme.primaryContainer
        3 -> scheme.secondary
        4 -> scheme.tertiary
        5 -> scheme.outlineVariant
        6 -> scheme.secondary
        else -> scheme.primary
    }
    val uncheckedBorder = when (style) {
        0 -> scheme.outline
        1 -> scheme.outlineVariant
        2 -> scheme.outline
        3 -> scheme.outline
        4 -> scheme.outlineVariant
        5 -> scheme.outlineVariant
        6 -> scheme.outline
        else -> scheme.outlineVariant
    }
    // Per-family control footprints. These are deliberately independent:
    // the styles do not collapse into one shared capsule geometry.
    val trackSize = when (style) {
        0 -> DpSize(52.dp, 32.dp)       // Material You
        1 -> DpSize(52.dp, 32.dp)       // Compose
        2 -> DpSize(56.dp, 28.dp)       // Pixel
        3 -> DpSize(48.dp, 24.dp)       // Fluent
        4 -> DpSize(64.dp, 28.dp)       // Cupertino
        5 -> DpSize(64.dp, 28.dp)       // Liquid Glass
        6 -> DpSize(50.dp, 28.5.dp)    // HyperOS
        else -> DpSize(39.dp, 22.dp)   // One UI outer touch target
    }
    // Each family owns its handle proportions and pressed-state response.
    val sourceThumbDiameter = when (style) {
        0 -> when {
            pressed -> 28.dp
            checked -> 24.dp
            else -> 16.dp
        }
        1 -> when {
            pressed -> 28.dp
            checked || thumbContent != null -> 24.dp
            else -> 16.dp
        }
        2 -> 20.dp
        3 -> if (enabled && pressedOrHovered) 17.dp else 12.dp
        4, 5 -> 20.dp
        6 -> if (enabled && interactive) 23.dp else 20.dp
        7 -> 22.dp
        else -> 20.dp
    }
    val thumbWidth = when (style) {
        0, 1 -> sourceThumbDiameter
        2 -> 20.dp
        3 -> if (enabled && pressedOrHovered) 17.dp else 12.dp
        4 -> 20.dp
        5 -> 40.dp
        6 -> if (enabled && interactive) 23.dp else 20.dp
        else -> 22.dp
    }
    val thumbHeight = when (style) {
        0, 1 -> sourceThumbDiameter
        2 -> 20.dp
        3 -> if (enabled && pressedOrHovered) 17.dp else 12.dp
        4 -> 20.dp
        5 -> 24.dp
        6 -> if (enabled && interactive) 23.dp else 20.dp
        else -> 22.dp
    }
    val thumbEdgeInset = when (style) {
        0, 1, 2 -> 4.dp
        3 -> 3.dp
        4 -> 2.dp
        5 -> 2.dp
        6 -> 2.5.dp
        else -> 0.dp
    }
    val trackCornerRadius = when (style) {
        0, 1 -> 16.dp
        2 -> 14.dp
        3 -> 4.dp
        4 -> 14.dp
        5 -> 14.dp
        6 -> 14.25.dp
        else -> 9.25.dp
    }
    // Motion profiles follow each control family's interaction character.
    val travelSpec = when (style) {
        0, 1 -> spring<Float>(dampingRatio = 0.78f, stiffness = 620f)
        2 -> androidx.compose.animation.core.tween<Float>(durationMillis = 500)
        3 -> androidx.compose.animation.core.tween<Float>(durationMillis = 600)
        4 -> androidx.compose.animation.core.tween<Float>(durationMillis = 300)
        5 -> spring<Float>(dampingRatio = 0.62f, stiffness = 560f)
        6 -> spring<Float>(dampingRatio = 0.75f, stiffness = 1500f)
        else -> androidx.compose.animation.core.tween<Float>(durationMillis = 250)
    }
    val travel by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = travelSpec,
        label = "materialSwitchThumb",
    )
    val glassDragTravelPx = with(LocalDensity.current) {
        (trackSize.width - thumbWidth - thumbEdgeInset * 2).toPx().coerceAtLeast(1f)
    }
    val latestChecked by rememberUpdatedState(checked)
    val latestOnCheckedChange by rememberUpdatedState(onCheckedChange)
    var glassFraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    var glassDidDrag by remember { mutableStateOf(false) }
    val glassDragAnimation = remember(animationScope, glassDragTravelPx) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = if (checked) 1f else 0f,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            // AndroidLiquidGlass LiquidToggle / Voyage: 1.5x press scale.
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                // Match the upstream LiquidToggle contract: one gesture owns
                // both tap and drag. A tap toggles; a drag commits its final side.
                val target = if (glassDidDrag) {
                    if (targetValue >= 0.5f) 1f else 0f
                } else {
                    if (latestChecked) 0f else 1f
                }
                glassDidDrag = false
                glassFraction = target
                latestOnCheckedChange?.invoke(target == 1f)
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (dragAmount.x != 0f) glassDidDrag = true
                glassFraction = (glassFraction + dragAmount.x / glassDragTravelPx).coerceIn(0f, 1f)
                if (dragAmount.x != 0f) {
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            },
        )
    }
    LaunchedEffect(glassFraction, style) {
        if (style == 5) glassDragAnimation.updateValue(glassFraction)
    }
    LaunchedEffect(checked, style) {
        if (style == 5) {
            val target = if (checked) 1f else 0f
            // External owner changes animate through the source press/pop path.
            // A local tap/drag already updates glassFraction before dispatching,
            // so it does not start a second competing animation here.
            if (target != glassFraction) {
                glassFraction = target
                glassDragAnimation.animateToValue(target)
            }
        }
    }
    val panelOffset by remember(rubberBandPx, glassDragTravelPx) {
        derivedStateOf {
            val fraction = (offsetAnimation.value / glassDragTravelPx).fastCoerceIn(-1f, 1f)
            rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
        }
    }
    val displayedTravel = if (style == 5) glassDragAnimation.value else travel
    val glassInteraction = if (style == 5) glassDragAnimation.pressProgress else 0f
    val liquidGlassSwitchPillHighlight = rememberLiquidGlassSwitchHighlight(liquidGlassSwitchSpecular, extraDegrees = 90f)
    // Keep switch outlines robust for every global Aloha shape preset.
    val trackShape = if (com.alohaoneui.app.ui.theme.LocalMaterialShapeStyle.current == 3) {
        remember(trackCornerRadius) { RoundedCornerShape(trackCornerRadius) }
    } else {
        voyageCornerShape(trackCornerRadius)
    }
    // Handle silhouette is style-specific, rather than a shared capsule.
    val thumbShape = when (style) {
        0 -> RoundedCornerShape(50)
        1 -> RoundedCornerShape(36)
        2 -> RoundedCornerShape(50)
        3 -> RoundedCornerShape(7.dp)
        4 -> RoundedCornerShape(50)
        5 -> RoundedCornerShape(50)
        6 -> RoundedCornerShape(38)
        else -> RoundedCornerShape(50)
    }
    val haloShape = RoundedCornerShape(50)
    // Shared by the Canvas thumb and the Liquid Glass fallback layer.
    val thumbColor = if (!enabled) {
        if (checked) colors.disabledCheckedThumbColor else colors.disabledUncheckedThumbColor
    } else if (interactive && style != 5) {
        if (checked) androidx.compose.ui.graphics.lerp(checkedThumb, scheme.primaryContainer, 0.12f)
        else androidx.compose.ui.graphics.lerp(uncheckedThumb, scheme.onSurfaceVariant, 0.10f)
    } else if (checked) checkedThumb else uncheckedThumb
    Box(
        modifier = modifier
            // Liquid Glass uses a real foreground overlay footprint: the host
            // layout reserves the expanded lens height instead of constraining
            // that lens to the 28dp rail. The rail itself remains 64x28dp.
            .graphicsLayer { clip = false }
            .then(
                if (style == 5) {
                    Modifier.requiredSize(
                        width = trackSize.width,
                        height = maxOf(trackSize.height, 28.dp * 1.5f),
                    )
                } else {
                    Modifier.size(width = trackSize.width, height = trackSize.height)
                }
            )
            .then(if (LocalMaterialShadowSwitches.current && style != 5) Modifier.shadow(2.dp, trackShape) else Modifier)
            .then(
                if (style == 5) {
                    // Liquid Glass tap and drag are handled by the same thumb
                    // pointer recognizer below; keep only switch semantics here.
                    Modifier.semantics {
                        role = Role.Switch
                        toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                        onClick {
                            if (enabled && onCheckedChange != null) {
                                val target = if (checked) 0f else 1f
                                glassFraction = target
                                glassDragAnimation.animateToValue(target)
                                onCheckedChange(target == 1f)
                                true
                            } else false
                        }
                    }
                } else {
                    Modifier.toggleable(
                        value = checked,
                        enabled = enabled && onCheckedChange != null,
                        role = Role.Switch,
                        interactionSource = interactionSource,
                        indication = null,
                        onValueChange = { onCheckedChange?.invoke(it) },
                    )
                }
            )
,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(trackSize)) {
            // Style 5's rail and resting handle are separate foreground layers.
            // Drawing the handle here would place it underneath the rail.
            if (style == 5) return@Canvas
            val w = size.width
            val h = size.height
            val trackColor = if (enabled) {
                if (checked) checkedTrack else uncheckedTrack
            } else if (style == 0) {
                if (checked) colors.disabledCheckedTrackColor else colors.disabledUncheckedTrackColor
            } else {
                (if (checked) checkedTrack else uncheckedTrack).copy(alpha = 0.38f)
            }
            // The actual outline, not a hard-coded rounded rectangle, keeps the
            // global Voyage shape family and corner scale linked to each style.
            val trackOutline = trackShape.createOutline(
                size = Size(w, h),
                layoutDirection = layoutDirection,
                density = this,
            )
            drawVoyageOutline(trackOutline, trackColor)
            if (interactionProgress > 0.001f && style != 5) {
                drawVoyageOutline(trackOutline, (if (checked) scheme.onPrimary else scheme.onSurfaceVariant).copy(alpha = interactionProgress * if (pressed) 0.12f else 0.08f))
            }
            if (focused && enabled) {
                drawVoyageOutline(trackOutline, scheme.primary.copy(alpha = 0.72f), style = Stroke(width = 1.25.dp.toPx()))
            }
            // Material You (Material 3) alone uses the documented track
            // decoration: outline when OFF, transparent when ON. Other switch
            // families must not inherit this perimeter.
            if (style == 0 && !checked) {
                drawVoyageOutline(
                    trackOutline,
                    scheme.outline.copy(alpha = if (enabled) 1f else 0.38f),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            val thumbWidthPx = thumbWidth.toPx()
            val thumbHeightPx = thumbHeight.toPx()
            val edgeInsetPx = thumbEdgeInset.toPx()
            val left = thumbWidthPx / 2f + edgeInsetPx
            val right = w - thumbWidthPx / 2f - edgeInsetPx
            val cx = left + (right - left) * travel
            val cy = h / 2f
            if (style == 5 && glassInteraction > 0.001f) {
                val haloWidth = thumbWidthPx + 4.dp.toPx()
                val haloHeight = thumbHeightPx + 4.dp.toPx()
                val haloOutline = haloShape.createOutline(
                    size = Size(haloWidth, haloHeight),
                    layoutDirection = layoutDirection,
                    density = this,
                )
                withTransform({ translate(cx - haloWidth / 2f, cy - haloHeight / 2f) }) {
                    drawVoyageOutline(
                        haloOutline,
                        Color.White.copy(alpha = glassInteraction * if (checked) 0.42f else 0.30f),
                    )
                }
            }
            // The resting thumb is a separate Material-colored layer. For
            // Liquid Glass it fades out only while the transient glass lens
            // is active; it must never supply the lens tint/color.
            val thumbOutline = thumbShape.createOutline(
                size = Size(thumbWidthPx, thumbHeightPx),
                layoutDirection = layoutDirection,
                density = this,
            )
            withTransform({ translate(cx - thumbWidthPx / 2f, cy - thumbHeightPx / 2f) }) {
                if (enabled && interactionProgress > 0.001f && style != 5) {
                    drawVoyageOutline(thumbOutline, (if (checked) scheme.onPrimary else scheme.onSurfaceVariant).copy(alpha = interactionProgress * 0.10f))
                }
                if (style != 5) {
                    drawVoyageOutline(thumbOutline, thumbColor)
                } else if (glassInteraction < 0.999f) {
                    drawVoyageOutline(thumbOutline, thumbColor.copy(alpha = 1f - glassInteraction))
                }
            }
            // Switch thumb icons/checkmarks are intentionally omitted; the
            // thumb itself communicates the on/off state by position and color.
        }
        if (style == 5) {
            val backdrop = LocalMaterialSwitchBackdrop.current
            val trackBackdrop = rememberLayerBackdrop()
            val thumbTravel = trackSize.width - thumbWidth - thumbEdgeInset * 2
            val glassRailCenterOffsetX = with(LocalDensity.current) {
                -(thumbTravel * displayedTravel - thumbTravel / 2f).toPx()
            }
            // Voyage Liquid Glass renderer reserves a 40×24dp thumb slot.
            // The transient glass surface scales inside this source-sized slot.
            val glassThumbWidth = 40.dp
            val glassThumbHeight = 24.dp
            // Keep the OFF rail visibly tinted in every Material theme.
            // surfaceContainerHigh can be nearly indistinguishable from the page
            // surface in some palettes, so blend in a small amount of the
            // theme's outlineVariant without introducing a fixed gray.
            val liquidGlassUncheckedTrack = lerp(uncheckedTrack, scheme.outlineVariant, 0.32f)
            val trackColor = lerp(liquidGlassUncheckedTrack, checkedTrack, displayedTravel).let {
                if (enabled) it else it.copy(alpha = 0.38f)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .layerBackdrop(trackBackdrop)
                    .clip(trackShape)
                    .background(trackColor)
                    // Keep a 1dp capture bleed at each end; the visible refracted
                    // rail is clipped by the shorter 56dp glass lens.
                    .size(width = trackSize.width + 2.dp, height = trackSize.height),
            )

            // Voyage's LiquidToggle vertically insets the sampled track backdrop
            // inside the glass surface (0f idle -> 0.75f while pressed).
            // Keep X sampling unchanged: only the top/bottom edges move inward.
            val verticallyInsetTrack = rememberVerticallyScaledBackdrop(trackBackdrop) {
                lerp(0f, 0.75f, glassDragAnimation.pressProgress)
            }
            // Capture the track first so the lens refracts its actual ON/OFF color.
            // The app backdrop remains untouched; only the switch rail is vertically inset.
            val glassBackdrop = if (backdrop != null) {
                rememberCombinedBackdrop(backdrop, verticallyInsetTrack)
            } else {
                verticallyInsetTrack
            }

            // Draw the theme-dynamic 40x24dp resting handle ABOVE the rail.
            // It remains the only visible thumb when the switch is idle.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(1f)
                    .offset(x = thumbTravel * displayedTravel - thumbTravel / 2f)
                    .size(width = thumbWidth, height = thumbHeight)
                    .graphicsLayer { alpha = 1f - glassInteraction }
                    .clip(thumbShape)
                    .background(thumbColor),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    // Explicitly place the glass handle in the foreground layer.
                    .zIndex(2f)
                    .graphicsLayer { clip = false }
                    .offset(x = thumbTravel * displayedTravel - thumbTravel / 2f)
                    // The glass capsule is intentionally switch-sized; its deformation,
                    // drag velocity and return spring come from the same bottom-bar engine.
                    .requiredSize(width = glassThumbWidth, height = glassThumbHeight)
                    .graphicsLayer {
                        alpha = glassInteraction
                        translationX = panelOffset
                    }
                    .then(if (enabled && onCheckedChange != null) glassDragAnimation.modifier else Modifier)
                    .drawBackdrop(
                        backdrop = glassBackdrop,
                        shape = { thumbShape },
                        effects = {
                            val progress = glassDragAnimation.pressProgress
                            blur(8.dp.toPx() * (1f - progress))
                            switchLens(
                                // Match Voyage's glass edge profile in density-independent units.
                                // Its animated progress controls both the refraction depth and capture bounds.
                                refractionHeight = 5.dp.toPx() * glassInteraction,
                                refractionAmount = 10.dp.toPx() * glassInteraction,
                                depthEffect = false,
                                chromaticAberration = 1f,
                                capturePadding = 8.dp.toPx() * (1f - glassInteraction),
                            )
                        },
                        highlight = {
                            val progress = glassDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        // Keep the backdrop layer unscaled. The visible glass
                        // surface is scaled outside drawBackdrop below so its
                        // bounds expand beyond the rail without magnifying the
                        // sampled track/refraction image.
                        onDrawSurface = {
                            // Same subtle surface treatment as the floating bottom-bar
                            // indicator: a theme-aware base wash plus a restrained dark veil.
                            drawRect(
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f),
                                alpha = 1f - glassInteraction,
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f * glassInteraction))
                        },
                    )
                    .innerShadow(shape = thumbShape) {
                        InnerShadow(
                            radius = 4.dp * glassInteraction,
                            color = Color.Black.copy(alpha = 0.15f),
                            alpha = glassInteraction,
                        )
                    }
                    // This outer graphics layer transforms the complete glass
                    // capsule, not its backdrop sampling. It is intentionally
                    // outside drawBackdrop to make the expansion visibly escape
                    // the 64x28dp track while preserving #2470 refraction.
                    .graphicsLayer {
                        val velocity = glassDragAnimation.velocity / 50f
                        scaleX = glassDragAnimation.scaleX /
                            (1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f))
                        scaleY = glassDragAnimation.scaleY *
                            (1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f))
                    },
            )
        }
        if (thumbContent != null && style != 5) {
            val thumbTravel = trackSize.width - thumbWidth - thumbEdgeInset * 2
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = thumbTravel * displayedTravel - thumbTravel / 2f)
                    .size(width = thumbWidth, height = thumbHeight)
                    .clip(thumbShape),
                contentAlignment = Alignment.Center,
            ) {
                thumbContent()
            }
        }
    }
}

@Composable
fun expressiveSwitchColors(
    checkedIconColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedIconColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    disabledCheckedThumbColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
    disabledCheckedTrackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledCheckedIconColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledUncheckedThumbColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
    disabledUncheckedTrackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.12f),
    disabledUncheckedBorderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledUncheckedIconColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
): SwitchColors = SwitchDefaults.colors(
    checkedIconColor = checkedIconColor,
    uncheckedIconColor = uncheckedIconColor,
    disabledCheckedThumbColor = disabledCheckedThumbColor,
    disabledCheckedTrackColor = disabledCheckedTrackColor,
    disabledCheckedIconColor = disabledCheckedIconColor,
    disabledUncheckedThumbColor = disabledUncheckedThumbColor,
    disabledUncheckedTrackColor = disabledUncheckedTrackColor,
    disabledUncheckedBorderColor = disabledUncheckedBorderColor,
    disabledUncheckedIconColor = disabledUncheckedIconColor,
)
