package com.oneuixp.app.ui.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Adds a swipe-to-return gesture to a nested navigation destination.
 *
 * The gesture starts in an inner edge strip so Android's system gesture can keep its own
 * outermost edge. Pointer handling runs in Main pass and yields whenever a child scrollable
 * has already consumed the gesture; this protects horizontal color/preview lists.
 */
@Composable
fun Modifier.swipeBackToPop(
    enabled: Boolean,
    onBack: () -> Unit,
): Modifier {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val commitDistancePx = with(density) { 112.dp.toPx() }
    val innerEdgeStartPx = with(density) { 28.dp.toPx() }
    val innerEdgeEndPx = with(density) { 84.dp.toPx() }

    return pointerInput(
        enabled,
        onBack,
        layoutDirection,
        commitDistancePx,
        innerEdgeStartPx,
        innerEdgeEndPx,
    ) {
        if (!enabled) return@pointerInput

        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            val startsAtBackEdge = if (layoutDirection == LayoutDirection.Ltr) {
                down.position.x in innerEdgeStartPx..innerEdgeEndPx
            } else {
                down.position.x in (size.width - innerEdgeEndPx)..(size.width - innerEdgeStartPx)
            }
            if (!startsAtBackEdge) return@awaitEachGesture

            var previous = down.position
            var totalX = 0f
            var totalY = 0f
            var horizontalBackDrag = false
            var rejected = false

            while (!rejected) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                val delta = change.position - previous
                totalX += delta.x
                totalY += delta.y
                previous = change.position

                if (!change.pressed) {
                    if (
                        horizontalBackDrag &&
                        abs(totalX) >= maxOf(commitDistancePx, size.width * 0.22f)
                    ) {
                        onBack()
                    }
                    break
                }

                if (!horizontalBackDrag) {
                    val crossedTouchSlop =
                        abs(totalX) > viewConfiguration.touchSlop ||
                            abs(totalY) > viewConfiguration.touchSlop

                    // A clickable commonly consumes DOWN. Do not reject the gesture for that;
                    // yield only when a child has consumed the actual drag crossing touch slop.
                    if (crossedTouchSlop && change.isConsumed) {
                        rejected = true
                        continue
                    }

                    if (abs(totalX) > viewConfiguration.touchSlop) {
                        val isHorizontal = abs(totalX) > abs(totalY) * 1.35f
                        val isBackDirection = if (layoutDirection == LayoutDirection.Ltr) {
                            totalX > 0f
                        } else {
                            totalX < 0f
                        }
                        if (!isHorizontal || !isBackDirection) {
                            rejected = true
                        } else {
                            horizontalBackDrag = true
                        }
                    } else if (abs(totalY) > viewConfiguration.touchSlop) {
                        rejected = true
                    }
                }

                if (horizontalBackDrag) change.consume()
            }
        }
    }
}

/**
 * Allows the four primary pages to remain gesture-reachable while a nested destination
 * is on top of the Navigation 3 stack. Edge bands remain reserved for system/back gestures.
 *
 * Horizontal child scrollables keep priority: this handler yields when a child consumes
 * the drag that crosses touch slop, rather than reacting to a consumed DOWN event.
 */
@Composable
fun Modifier.swipeToChangeMainPage(
    enabled: Boolean,
    currentPage: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
): Modifier {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val edgeExclusionPx = with(density) { 84.dp.toPx() }
    val commitDistancePx = with(density) { 96.dp.toPx() }

    return pointerInput(
        enabled,
        currentPage,
        pageCount,
        onPageChange,
        layoutDirection,
        edgeExclusionPx,
        commitDistancePx,
    ) {
        if (!enabled || pageCount < 2) return@pointerInput

        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            if (
                down.position.x <= edgeExclusionPx ||
                down.position.x >= size.width - edgeExclusionPx
            ) {
                return@awaitEachGesture
            }

            var previous = down.position
            var totalX = 0f
            var totalY = 0f
            var changingPage = false
            var rejected = false

            while (!rejected) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                val delta = change.position - previous
                totalX += delta.x
                totalY += delta.y
                previous = change.position

                if (!change.pressed) {
                    if (
                        changingPage &&
                        abs(totalX) >= maxOf(commitDistancePx, size.width * 0.18f)
                    ) {
                        val pageDelta = when (layoutDirection) {
                            LayoutDirection.Ltr -> if (totalX < 0f) 1 else -1
                            LayoutDirection.Rtl -> if (totalX > 0f) 1 else -1
                        }
                        val targetPage = (currentPage + pageDelta).coerceIn(0, pageCount - 1)
                        if (targetPage != currentPage) onPageChange(targetPage)
                    }
                    break
                }

                if (!changingPage) {
                    val crossedTouchSlop =
                        abs(totalX) > viewConfiguration.touchSlop ||
                            abs(totalY) > viewConfiguration.touchSlop

                    // Let horizontal/vertical child scrollables claim their drag. A consumed
                    // DOWN alone is ignored so tappable rows still support a deliberate swipe.
                    if (crossedTouchSlop && change.isConsumed) {
                        rejected = true
                        continue
                    }

                    val isHorizontal =
                        abs(totalX) > viewConfiguration.touchSlop &&
                            abs(totalX) > abs(totalY) * 1.35f
                    if (isHorizontal) {
                        changingPage = true
                    } else if (abs(totalY) > viewConfiguration.touchSlop) {
                        rejected = true
                    }
                }

                if (changingPage) change.consume()
            }
        }
    }
}
