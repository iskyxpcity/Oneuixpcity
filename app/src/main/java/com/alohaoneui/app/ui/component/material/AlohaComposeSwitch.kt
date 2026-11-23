/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.LocalRippleThemeConfiguration
import androidx.compose.material3.RippleThemeConfiguration
import androidx.compose.material3.ripple
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * [Material Design switch](https://m3.material.io/components/switch)
 *
 * Switches toggle the state of a single item on or off.
 *
 * ![Switch
 * image](https://developer.android.com/images/reference/androidx/compose/material3/switch.png)
 *
 * @sample androidx.compose.material3.samples.SwitchSample
 *
 * Switch can be used with a custom icon via [thumbContent] parameter
 *
 * @sample androidx.compose.material3.samples.SwitchWithThumbIconSample
 * @param checked whether or not this switch is checked
 * @param onCheckedChange called when this switch is clicked. If `null`, then this switch will not
 *   be interactable, unless something else handles its input events and updates its state.
 * @param modifier the [Modifier] to be applied to this switch
 * @param thumbContent content that will be drawn inside the thumb, expected to measure
 *   [SwitchDefaults.IconSize]
 * @param enabled controls the enabled state of this switch. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [SwitchColors] that will be used to resolve the colors used for this switch in
 *   different states. See [SwitchDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this switch. You can use this to change the switch's appearance or
 *   preview the switch in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 */
@Composable
@Suppress("ComposableLambdaParameterNaming", "ComposableLambdaParameterPosition")
fun AlohaComposeSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    thumbShape: Shape,
    trackShape: Shape,
    interactionSource: MutableInteractionSource? = null,
    showThumbIcon: Boolean = true,
) {
    ComposeSwitchImpl(
        modifier = modifier,
        onCheckedChange = onCheckedChange,
        checked = checked,
        enabled = enabled,
        thumbContent = thumbContent,
        thumbShape = thumbShape,
        trackShape = trackShape,
        thumbColor = if (enabled) { if (checked) colors.checkedThumbColor else colors.uncheckedThumbColor } else { if (checked) colors.disabledCheckedThumbColor else colors.disabledUncheckedThumbColor },
        iconColor = if (enabled) { if (checked) colors.checkedIconColor else colors.uncheckedIconColor } else { if (checked) colors.disabledCheckedIconColor else colors.disabledUncheckedIconColor },
        trackColor = if (enabled) { if (checked) colors.checkedTrackColor else colors.uncheckedTrackColor } else { if (checked) colors.disabledCheckedTrackColor else colors.disabledUncheckedTrackColor },
        borderColor = if (enabled) { if (checked) colors.checkedBorderColor else colors.uncheckedBorderColor } else { if (checked) colors.disabledCheckedBorderColor else colors.disabledUncheckedBorderColor },
        interactionSource = interactionSource,
        showThumbIcon = showThumbIcon,
    )
}

@Composable
@Suppress("ComposableLambdaParameterNaming", "ComposableLambdaParameterPosition")
private fun ComposeSwitchImpl(
    modifier: Modifier,
    onCheckedChange: ((Boolean) -> Unit)?,
    checked: Boolean,
    enabled: Boolean,
    thumbContent: (@Composable () -> Unit)?,
    thumbShape: Shape,
    trackShape: Shape,
    thumbColor: Color,
    iconColor: Color,
    trackColor: Color,
    borderColor: Color,
    interactionSource: MutableInteractionSource?,
    showThumbIcon: Boolean,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    // TODO Load the motionScheme tokens from the component tokens file
    val animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val thumbTravelPx = with(LocalDensity.current) { (ComposeSwitchWidth - ComposeSwitchHeight).toPx() }
    var forceAnimationCheck by remember { mutableStateOf(false) }
    val anchoredDraggableState =
        remember(thumbTravelPx) {
            AnchoredDraggableState(
                initialValue = checked,
                anchors =
                    DraggableAnchors {
                        false at 0f
                        true at thumbTravelPx
                    },
            )
        }
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val currentChecked by rememberUpdatedState(checked)
    LaunchedEffect(anchoredDraggableState) {
        snapshotFlow { anchoredDraggableState.settledValue }
            .collectLatest { newValue ->
                if (currentChecked != newValue) {
                    currentOnCheckedChange?.invoke(newValue)
                    forceAnimationCheck = !forceAnimationCheck
                }
            }
    }
    LaunchedEffect(checked, forceAnimationCheck) {
        val targetOffset = anchoredDraggableState.anchors.positionOf(checked)
        if (
            checked != anchoredDraggableState.settledValue ||
                (!targetOffset.isNaN() && anchoredDraggableState.offset != targetOffset)
        ) {
            anchoredDraggableState.animateTo(checked, animationSpec)
        }
    }

    val toggleableModifier =
        if (onCheckedChange != null) {
            Modifier.minimumInteractiveComponentSize()
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = interactionSource,
                    indication = null,
                )
        } else {
            Modifier
        }

    val dragModifier =
        if (onCheckedChange != null) {
            val flingBehavior =
                AnchoredDraggableDefaults.flingBehavior(
                    state = anchoredDraggableState,
                    animationSpec = animationSpec,
                )
            val draggableState =
                remember(anchoredDraggableState) {
                    object : DraggableState {
                        override fun dispatchRawDelta(delta: Float) {
                            anchoredDraggableState.dispatchRawDelta(delta)
                        }

                        override suspend fun drag(
                            dragPriority: MutatePriority,
                            block: suspend DragScope.() -> Unit,
                        ) {
                            anchoredDraggableState.anchoredDrag(dragPriority) {
                                val dragScope =
                                    object : DragScope {
                                        override fun dragBy(pixels: Float) {
                                            anchoredDraggableState.dispatchRawDelta(pixels)
                                        }
                                    }
                                dragScope.block()
                            }
                        }
                    }
                }
            val onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit =
                remember(anchoredDraggableState, flingBehavior) {
                    { velocity ->
                        anchoredDraggableState.anchoredDrag {
                            val scrollScope =
                                object : ScrollScope {
                                    override fun scrollBy(pixels: Float): Float {
                                        return anchoredDraggableState.dispatchRawDelta(pixels)
                                    }
                                }
                            with(flingBehavior) { scrollScope.performFling(velocity) }
                        }
                    }
                }
            Modifier.draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                enabled = enabled,
                interactionSource = interactionSource,
                startDragImmediately = false,
                reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
                onDragStopped = onDragStopped,
            )
        } else {
            Modifier
        }

    val useInsetRing =
        LocalRippleThemeConfiguration.current.focus is RippleThemeConfiguration.Focus.InsetRing
    val focusRingModifier =
        if (useInsetRing) {
            Modifier.indication(
                interactionSource = interactionSource,
                indication =
                    ripple(
                        focusRingShape = trackShape,
                        enablePressIndication = false,
                        enableFocusIndication = true,
                        enableDragIndication = false,
                        enableHoverIndication = false,
                    ),
            )
        } else {
            Modifier
        }

    Box(
        modifier
            .then(toggleableModifier)
            .then(dragModifier)
            .wrapContentSize(Alignment.Center)
            .requiredSize(ComposeSwitchWidth, ComposeSwitchHeight)
            .border(2.dp, borderColor, trackShape)
            .background(trackColor, trackShape)
            .then(focusRingModifier)
    ) {
        Box(
            modifier =
                Modifier.align(Alignment.CenterStart)
                    .then(
                        ComposeThumbElement(
                            interactionSource = interactionSource,
                            checked = checked,
                            anchoredDraggableState = anchoredDraggableState,
                            animationSpec = animationSpec,
                        )
                    )
                    .indication(
                        interactionSource = interactionSource,
                        indication =
                            ripple(
                                bounded = false,
                                radius = 40.dp / 2,
                                enableFocusIndication = !useInsetRing,
                            ),
                    )
                    .background(thumbColor, thumbShape),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbContent != null) {
                CompositionLocalProvider(
                    LocalContentColor provides iconColor,
                    content = thumbContent,
                )
            } else if (checked && showThumbIcon) {
                CompositionLocalProvider(LocalContentColor provides iconColor) {
                    ComposeSwitchCheckmark()
                }
            }
        }
    }
}

private data class ComposeThumbElement(
    val interactionSource: InteractionSource,
    val checked: Boolean,
    val anchoredDraggableState: AnchoredDraggableState<Boolean>,
    val animationSpec: FiniteAnimationSpec<Float>,
) : ModifierNodeElement<ComposeThumbNode>() {
    override fun create() =
        ComposeThumbNode(interactionSource, checked, anchoredDraggableState, animationSpec)

    override fun update(node: ComposeThumbNode) {
        val interactionSourceChanged = node.interactionSource != interactionSource
        node.interactionSource = interactionSource
        if (interactionSourceChanged) {
            node.restartInteractionCollection()
        }
        if (node.checked != checked || node.anchoredDraggableState != anchoredDraggableState) {
            node.invalidateMeasurement()
        }
        node.checked = checked
        node.anchoredDraggableState = anchoredDraggableState
        node.animationSpec = animationSpec
        node.update()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "switchThumb"
        properties["interactionSource"] = interactionSource
        properties["checked"] = checked
        properties["animationSpec"] = animationSpec
    }
}

private class ComposeThumbNode(
    var interactionSource: InteractionSource,
    var checked: Boolean,
    var anchoredDraggableState: AnchoredDraggableState<Boolean>,
    var animationSpec: FiniteAnimationSpec<Float>,
) : Modifier.Node(), LayoutModifierNode {

    override val shouldAutoInvalidate: Boolean
        get() = false

    private var isPressed = false
    private var isDragged = false
    private var interactionCollectionJob: Job? = null
    private var sizeAnim: Animatable<Float, AnimationVector1D>? = null
    private var initialSize: Float = Float.NaN

    override fun onAttach() {
        restartInteractionCollection()
    }

    override fun onDetach() {
        super.onDetach()
        isPressed = false
        isDragged = false
    }

    fun restartInteractionCollection() {
        interactionCollectionJob?.cancel()
        isPressed = false
        isDragged = false
        if (!isAttached) return
        interactionCollectionJob = coroutineScope.launch {
            var pressCount = 0
            var dragCount = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressCount++
                    is PressInteraction.Release -> pressCount--
                    is PressInteraction.Cancel -> pressCount--
                    is DragInteraction.Start -> dragCount++
                    is DragInteraction.Stop -> dragCount--
                    is DragInteraction.Cancel -> dragCount--
                }
                val pressed = pressCount > 0
                val dragged = dragCount > 0
                if (isPressed != pressed || isDragged != dragged) {
                    isPressed = pressed
                    isDragged = dragged
                    invalidateMeasurement()
                }
            }
        }
    }

    override fun onReset() {
        super.onReset()
        sizeAnim = null
        initialSize = Float.NaN
        isPressed = false
        isDragged = false
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val hasContent =
            measurable.maxIntrinsicHeight(constraints.maxWidth) != 0 &&
                measurable.maxIntrinsicWidth(constraints.maxHeight) != 0
        val size =
            when {
                isPressed || isDragged -> 28.dp
                hasContent || checked -> ComposeThumbDiameter
                else -> ComposeUncheckedThumbDiameter
            }.toPx()

        if (sizeAnim == null && !initialSize.isNaN()) {
            sizeAnim = Animatable(initialSize)
        }
        val actualSize = (sizeAnim?.value ?: size).toInt().coerceAtLeast(0)
        val placeable = measurable.measure(Constraints.fixed(actualSize, actualSize))

        if (sizeAnim?.targetValue != size) {
            coroutineScope.launch { sizeAnim?.animateTo(size, animationSpec) }
        }

        if (initialSize.isNaN()) {
            initialSize = size
        }

        val minCenterX = (ComposeSwitchHeight / 2f).toPx()
        val fallbackTravel = (ComposeSwitchWidth - ComposeSwitchHeight).toPx()

        return layout(actualSize, actualSize) {
            val dragOffset =
                if (!anchoredDraggableState.offset.isNaN()) {
                    anchoredDraggableState.offset
                } else {
                    if (checked) fallbackTravel else 0f
                }
            val currentThumbSize = sizeAnim?.value ?: size
            val leftOffset = minCenterX + dragOffset - currentThumbSize / 2f
            placeable.placeRelative(leftOffset.toInt(), 0)
        }
    }

    fun update() {
        if (sizeAnim == null && !initialSize.isNaN()) {
            sizeAnim = Animatable(initialSize)
        }
    }
}

private val ComposeThumbDiameter = 24.dp
private val ComposeUncheckedThumbDiameter = 16.dp
private val ComposeSwitchWidth = 52.dp
private val ComposeSwitchHeight = 32.dp


/** Compose-style checked mark, drawn independently inside its own thumb. */
@Composable
private fun ComposeSwitchCheckmark() {
    val iconColor = LocalContentColor.current
    Canvas(Modifier.size(16.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawLine(
            color = iconColor,
            start = Offset(size.width * 0.18f, size.height * 0.52f),
            end = Offset(size.width * 0.42f, size.height * 0.72f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = iconColor,
            start = Offset(size.width * 0.42f, size.height * 0.72f),
            end = Offset(size.width * 0.82f, size.height * 0.28f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}
