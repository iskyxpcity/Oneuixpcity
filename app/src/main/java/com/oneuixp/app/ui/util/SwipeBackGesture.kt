package com.oneuixp.app.ui.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Adds a horizontal swipe-to-return gesture to a nested navigation destination.
 * Vertical scrolling and swipes in the forward direction are left to page content.
 */
@Composable
fun Modifier.swipeBackToPop(
    enabled: Boolean,
    onBack: () -> Unit,
): Modifier {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val commitDistancePx = with(density) { 112.dp.toPx() }

    return pointerInput(enabled, onBack, layoutDirection, commitDistancePx) {
        if (!enabled) return@pointerInput

        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            var previous = down.position
            var totalX = 0f
            var totalY = 0f
            var horizontalBackDrag = false
            var rejected = false

            while (!rejected) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                val delta = change.position - previous
                totalX += delta.x
                totalY += delta.y
                previous = change.position

                if (!change.pressed) {
                    if (horizontalBackDrag && abs(totalX) >= maxOf(commitDistancePx, size.width * 0.22f)) {
                        onBack()
                    }
                    break
                }

                if (!horizontalBackDrag) {
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
