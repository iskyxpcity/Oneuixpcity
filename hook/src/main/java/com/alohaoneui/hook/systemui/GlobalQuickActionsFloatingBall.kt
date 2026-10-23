package com.alohaoneui.hook.systemui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.roundToInt

internal fun GlobalQuickActions.ensureFloatingBall(context: Context) {
    val settings = floatingBallSettings(context)
    if (!settings.enabled) {
        removeFloatingBall()
        return
    }
    val existingView = floatingBallView
    if (existingView != null) {
        val existingParams = floatingBallLayoutParams
        val existingManager = floatingBallWindowManager
        val delaySeconds = settings.autoEdgeDelaySeconds.coerceIn(0, 10)
        // Rebuild when the configured size no longer matches the live window. The preference
        // observer usually handles this, but a dropped broadcast would otherwise leave the ball
        // stuck at the old diameter forever because ensure() returns early for an existing view.
        val requestedSizeDp = settings.sizeDp.coerceIn(MIN_FLOATING_BALL_SIZE_DP, MAX_FLOATING_BALL_SIZE_DP)
        val expectedSize = dp(context, requestedSizeDp)
        val paramsSizeMismatch =
            existingParams?.width != expectedSize || existingParams?.height != expectedSize
        val measuredWidthMismatch = existingView.width > 0 && existingView.width != expectedSize
        val measuredHeightMismatch = existingView.height > 0 && existingView.height != expectedSize
        if (paramsSizeMismatch || measuredWidthMismatch || measuredHeightMismatch) {
            recordSystemServerDiagnosticEvent(
                context,
                "floating-ball size mismatch; rebuilding: " +
                    "params=${existingParams?.width}x${existingParams?.height} " +
                    "view=${existingView.width}x${existingView.height} expected=${expectedSize}px " +
                    "sizeDp=${settings.sizeDp} normalizedSizeDp=$requestedSizeDp",
            )
            removeFloatingBall()
            ensureFloatingBall(context)
            return
        }
        // Self-heal a leaked touch state: if UP/CANCEL was swallowed (window detached mid-gesture,
        // an action that stalled the input stream, ...), the ball would otherwise never re-arm.
        if (floatingBallTouchActive && floatingBallTouchDownAtMs > 0L &&
            SystemClock.uptimeMillis() - floatingBallTouchDownAtMs > FLOATING_BALL_TOUCH_STALE_MS
        ) {
            recordSystemServerDiagnosticEvent(
                context,
                "floating-ball stale touch state reset: downAtMs=$floatingBallTouchDownAtMs " +
                    "ageMs=${SystemClock.uptimeMillis() - floatingBallTouchDownAtMs} " +
                    "dragging=$floatingBallDragging positionChanged=$floatingBallPositionChanged",
            )
            floatingBallTouchActive = false
            floatingBallDragging = false
            floatingBallPositionChanged = false
            floatingBallTouchDownAtMs = 0L
        }
        if (
            delaySeconds > 0 &&
            !floatingBallTouchActive &&
            floatingBallAutoEdgeRunnable == null &&
            existingView.isAttachedToWindow &&
            existingParams != null &&
            existingManager != null
        ) {
            val ballWidth = existingView.width.takeIf { it > 0 }
                ?: existingParams.width.takeIf { it > 0 }
                ?: 0
            val displayWidth = floatingBallDisplayWidth(context, existingManager, existingView)
            val indicatorWidth = floatingBallDockIndicatorWidth(context, ballWidth)
            val leftTargetX = -(ballWidth - indicatorWidth)
            val rightTargetX = displayWidth - indicatorWidth
            val tolerance = dp(context, 2)
            val alreadyAtEdge = displayWidth <= 0 || ballWidth <= 0 ||
                kotlin.math.abs(existingParams.x - leftTargetX) <= tolerance ||
                kotlin.math.abs(existingParams.x - rightTargetX) <= tolerance
            if (!alreadyAtEdge) {
                recordSystemServerDiagnosticEvent(
                    context,
                    "floating-ball idle timer missing away from edge; rearming: delaySeconds=$delaySeconds " +
                        "x=${existingParams.x} leftTargetX=$leftTargetX rightTargetX=$rightTargetX " +
                        "indicatorWidth=$indicatorWidth touchActive=$floatingBallTouchActive",
                )
                scheduleFloatingBallAutoEdge(context, existingManager, existingView, existingParams, settings)
            }
        }
        return
    }

    val windowManager = context.getSystemService(WindowManager::class.java) ?: return
    floatingBallWindowManager = windowManager
    val size = dp(context, settings.sizeDp.coerceIn(MIN_FLOATING_BALL_SIZE_DP, MAX_FLOATING_BALL_SIZE_DP))
    val params = WindowManager.LayoutParams(
        size,
        size,
        TYPE_NAVIGATION_BAR_PANEL,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            // Docking leaves half the ball outside the display; without NO_LIMITS the window
            // manager clamps the negative x back to 0 and the ball stays fully visible.
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_SECURE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        // Fresh instances dock to the right edge immediately: x=0 is "not initialised yet",
        // a docked position may legitimately be negative or equal to 0.
        x = floatingBallX.takeIf { it != 0 }
            ?: (context.resources.displayMetrics.widthPixels -
                dp(context, FLOATING_BALL_DOCK_INDICATOR_WIDTH_DP))
        y = floatingBallY.takeIf { it > 0 } ?: (statusBarHeightPx(context) + dp(context, 160))
    }
    val view = object : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val markerRect = RectF()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val dockSide = floatingBallDockedSide
            if (dockSide != 0) {
                drawDockMarker(canvas, dockSide)
                // Breathing loop: keep requesting frames only while docked. Once the ball is
                // restored the next frame takes the drawBall branch and the loop stops.
                postInvalidateDelayed(FLOATING_BALL_DOCK_BREATH_FRAME_MS)
                return
            }
            drawBall(canvas)
        }

        /**
         * Docked state: the ball itself is parked off-screen and only a small half-disc plate
         * stays on the edge, breathing slowly. Tapping it brings the ball back.
         */
        private fun drawDockMarker(canvas: Canvas, side: Int) {
            val depth = dp(context, FLOATING_BALL_DOCK_INDICATOR_WIDTH_DP)
                .coerceIn(1, minOf(width, height).coerceAtLeast(1))
                .toFloat()
            val period = FLOATING_BALL_DOCK_BREATH_PERIOD_MS
            val phase = (SystemClock.uptimeMillis() % period).toFloat() / period
            // 0 = exhaled, 1 = inhaled; eased with a sine so the pulse never snaps.
            val breath = 0.5f + 0.5f * kotlin.math.sin(phase * (2f * Math.PI).toFloat())
            val radius = depth * (0.9f + 0.1f * breath)
            // Flat side of the half-disc sits exactly on the screen edge.
            val centerX = if (side < 0) width - depth else depth
            val centerY = height / 2f
            markerRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
            val startAngle = if (side < 0) -90f else 90f

            val ballColor = settings.color
            val baseAlpha = Color.alpha(ballColor)
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(ballColor, (baseAlpha * (0.55f + 0.4f * breath)).roundToInt().coerceIn(40, 240))
            canvas.drawArc(markerRect, startAngle, 180f, true, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = resources.displayMetrics.density * 1.5f
            paint.color = withAlpha(Color.WHITE, (110 + 100 * breath).roundToInt().coerceIn(0, 255))
            canvas.drawArc(markerRect, startAngle, 180f, true, paint)
        }

        private fun drawBall(canvas: Canvas) {
            val centerX = width / 2f
            val centerY = height / 2f
            val outerRadius = minOf(width, height) / 2f - dp(context, 1)
            val innerRadius = outerRadius * FLOATING_BALL_INNER_RADIUS_RATIO
            val innerMoveLimit = (outerRadius - innerRadius - dp(context, 3)).coerceAtLeast(0f)
            val innerOffsetX = floatingBallInnerOffsetX.coerceIn(-innerMoveLimit, innerMoveLimit)
            val innerOffsetY = floatingBallInnerOffsetY.coerceIn(-innerMoveLimit, innerMoveLimit)
            val innerCenterX = centerX + innerOffsetX
            val innerCenterY = centerY + innerOffsetY
            val ballColor = settings.color

            paint.style = Paint.Style.FILL
            paint.color = withAlpha(ballColor, ((Color.alpha(ballColor) * 0.42f).roundToInt()).coerceIn(28, 180))
            canvas.drawCircle(centerX, centerY, outerRadius, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(context, 2).toFloat()
            paint.color = withAlpha(Color.WHITE, 150)
            canvas.drawCircle(centerX, centerY, outerRadius, paint)

            paint.style = Paint.Style.FILL
            paint.color = ballColor
            canvas.drawCircle(innerCenterX, innerCenterY, innerRadius, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(context, 1).toFloat()
            paint.color = withAlpha(Color.WHITE, 205)
            canvas.drawCircle(innerCenterX, innerCenterY, innerRadius, paint)
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            // A docked marker sits inside the system edge-gesture inset; without the exclusion
            // rect the back gesture / edge panel steals the tap and the pull never reaches us.
            applyFloatingBallGestureExclusion(this)
        }
    }.apply {
        setOnTouchListener { v, event ->
            handleFloatingBallTouch(context, windowManager, v, params, event)
            true
        }
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit

            override fun onViewDetachedFromWindow(v: View) {
                // A detach mid-gesture never delivers UP/CANCEL; drop the touch state so the
                // next re-attach can arm the idle timer again.
                floatingBallTouchActive = false
                floatingBallTouchPointerId = -1
                floatingBallDragging = false
                floatingBallPositionChanged = false
                floatingBallTouchDownAtMs = 0L
                floatingBallDockedAtDown = 0
                cancelFloatingBallAutoEdge()
            }
        })
    }
    runCatching {
        windowManager.addView(view, params)
        floatingBallView = view
        floatingBallLayoutParams = params
        floatingBallX = params.x
        floatingBallY = params.y
        floatingBallDockedSide = dockedSideOf(context, windowManager, view, params)
        if (floatingBallDockedSide != 0) {
            if (floatingBallFreeX < 0) {
                // First run: nothing was ever free-floating, pick a sensible restore target.
                floatingBallFreeX = (context.resources.displayMetrics.widthPixels - size - dp(context, 18))
                    .coerceAtLeast(0)
                floatingBallFreeY = params.y
            }
        } else {
            floatingBallFreeX = params.x
            floatingBallFreeY = params.y
        }
        applyFloatingBallGestureExclusion(view)
        view.invalidate()
        log("floating ball added")
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball added x=${params.x} y=${params.y} width=${params.width} attached=${view.isAttachedToWindow} enabled=${settings.enabled} autoEdgeDelaySeconds=${settings.autoEdgeDelaySeconds}",
        )
        // Aloha exposes the auto-edge delay as an app setting; arm its timer after addView.
        // The upstream runtime has internal handling, but its app UI does not expose this control.
        // It is owned by the SystemServer main Handler; no View.post or duplicate queue.
        scheduleFloatingBallAutoEdge(context, windowManager, view, params, settings)
    }.onFailure {
        floatingBallView = null
        floatingBallLayoutParams = null
        floatingBallWindowManager = null
        floatingBallPositionChanged = false
        log("floating ball add failed")
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball add failed error=${it.javaClass.simpleName}:${it.message.orEmpty()}",
        )
        Log.w(TAG, "Source floating-ball operation failed", it)
    }
}

internal fun GlobalQuickActions.removeFloatingBall() {
    floatingBallTouchActive = false
    floatingBallTouchPointerId = -1
    floatingBallDragging = false
    floatingBallPositionChanged = false
    floatingBallTouchDownAtMs = 0L
    floatingBallLongPressCancelled = false
    cancelFloatingBallAutoEdge()
    floatingBallPendingClickRunnable?.let { mainHandler.removeCallbacks(it) }
    floatingBallPendingClickRunnable = null
    floatingBallLongPressRunnable?.let { mainHandler.removeCallbacks(it) }
    floatingBallLongPressRunnable = null

    val view = floatingBallView
    val context = systemServerContext ?: currentApplicationContext()
    val windowManager = floatingBallWindowManager
        ?: context?.getSystemService(WindowManager::class.java)
    if (view != null && windowManager != null) {
        runCatching { windowManager.removeView(view) }
            .onFailure { failure ->
                context?.let { diagnosticContext ->
                    recordSystemServerDiagnosticEvent(
                        diagnosticContext,
                        "floating-ball remove failed error=${failure.javaClass.simpleName}:${failure.message.orEmpty()}",
                    )
                }
            }
    }
    context?.let { recordSystemServerDiagnosticEvent(it, "floating-ball removed") }
    floatingBallView = null
    floatingBallLayoutParams = null
    floatingBallWindowManager = null
}

internal fun GlobalQuickActions.handleFloatingBallTouch(
    context: Context,
    windowManager: WindowManager,
    view: View,
    params: WindowManager.LayoutParams,
    event: MotionEvent
) {
    val settings = floatingBallSettings(context)
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            floatingBallTouchActive = true
            floatingBallTouchPointerId = event.getPointerId(event.actionIndex.coerceIn(0, event.pointerCount - 1))
            floatingBallTouchDownAtMs = SystemClock.uptimeMillis()
            floatingBallPositionChanged = false
            cancelFloatingBallAutoEdge()
            recordSystemServerDiagnosticEvent(context, "floating-ball touch down x=${params.x} y=${params.y}; auto-edge timer cancelled")
            floatingBallTouchStartX = event.rawX
            floatingBallTouchStartY = event.rawY
            floatingBallLastRawX = event.rawX
            floatingBallLastRawY = event.rawY
            floatingBallStartX = params.x
            floatingBallStartY = params.y
            floatingBallTouchDownTime = event.eventTime
            floatingBallMoved = false
            floatingBallDragging = false
            floatingBallLongPressCancelled = false
            floatingBallDragDisplayWidth = floatingBallDisplayWidth(context, windowManager, view)
            floatingBallDockedAtDown = dockedSideOf(context, windowManager, view, params)
            if (floatingBallDockedAtDown != 0) {
                recordSystemServerDiagnosticEvent(
                    context,
                    "floating-ball touch down on docked sliver: side=$floatingBallDockedAtDown x=${params.x} " +
                        "width=${params.width}; pull-out drag will start without long press",
                )
            }
            floatingBallInnerOffsetX = 0f
            floatingBallInnerOffsetY = 0f
            floatingBallPendingClickRunnable?.let { mainHandler?.removeCallbacks(it) }
            floatingBallPendingClickRunnable = null
            view.invalidate()
            floatingBallLongPressRunnable?.let { mainHandler?.removeCallbacks(it) }
            floatingBallLongPressRunnable = Runnable {
                floatingBallDragging = true
                floatingBallMoved = true
                // Rebase the drag origin on the finger's current spot so tremor accumulated
                // before the long press fired cannot teleport the ball.
                floatingBallTouchStartX = floatingBallLastRawX
                floatingBallTouchStartY = floatingBallLastRawY
                floatingBallStartX = params.x
                floatingBallStartY = params.y
                recordSystemServerDiagnosticEvent(
                    context,
                    "floating-ball long press armed drag: rebaseX=${params.x} rebaseY=${params.y} " +
                        "fingerX=$floatingBallLastRawX fingerY=$floatingBallLastRawY",
                )
            }.also { runnable ->
                postOnMainDelayed(runnable, FLOATING_BALL_LONG_PRESS_MS)
            }
        }
        MotionEvent.ACTION_MOVE -> {
            floatingBallLastRawX = event.rawX
            floatingBallLastRawY = event.rawY
            val dx = event.rawX - floatingBallTouchStartX
            val dy = event.rawY - floatingBallTouchStartY
            updateFloatingBallInnerOffset(context, view, dx, dy)
            val travel = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
            if (!floatingBallDragging) {
                if (travel <= dp(context, FLOATING_BALL_TAP_SLOP_DP)) return
                floatingBallMoved = true
                // A docked ball lives on the screen edge; requiring a 520ms stationary hold
                // there makes it feel stuck. An inward horizontal pull grabs it right away.
                val dockedPull = floatingBallDockedAtDown != 0 &&
                    (-floatingBallDockedAtDown) * dx > 0f &&
                    kotlin.math.abs(dx) >= kotlin.math.abs(dy)
                if (dockedPull) {
                    floatingBallDragging = true
                    floatingBallLongPressCancelled = true
                    floatingBallDockedSide = 0
                    view.invalidate()
                    floatingBallLongPressRunnable?.let { mainHandler?.removeCallbacks(it) }
                    floatingBallLongPressRunnable = null
                    recordSystemServerDiagnosticEvent(
                        context,
                        "floating-ball docked pull started drag: side=$floatingBallDockedAtDown " +
                            "dx=$dx dy=$dy dockedX=${params.x}",
                    )
                } else {
                    // Only real swipe travel cancels the pending drag; finger tremor inside the
                    // long-press slop must not kill it, otherwise the ball can never be moved.
                    if (travel > dp(context, FLOATING_BALL_LONG_PRESS_SLOP_DP) && !floatingBallLongPressCancelled) {
                        floatingBallLongPressCancelled = true
                        floatingBallLongPressRunnable?.let { mainHandler?.removeCallbacks(it) }
                        floatingBallLongPressRunnable = null
                        recordSystemServerDiagnosticEvent(
                            context,
                            "floating-ball long press cancelled by swipe: travel=$travel " +
                                "longPressSlop=${dp(context, FLOATING_BALL_LONG_PRESS_SLOP_DP)}",
                        )
                    }
                    return
                }
            }
            val previousX = params.x
            val previousY = params.y
            // Same bounds the docked marker uses, so a docked ball can be grabbed back
            // without the window snapping to x=0 first.
            val ballWidth = view.width.takeIf { it > 0 } ?: params.width
            val indicatorWidth = floatingBallDockIndicatorWidth(context, ballWidth)
            val dragMinX = -(ballWidth - indicatorWidth)
            val dragDisplayWidth = floatingBallDragDisplayWidth.takeIf { it > 0 }
                ?: floatingBallDisplayWidth(context, windowManager, view)
            val dragMaxX = dragDisplayWidth - indicatorWidth
            val requestedX = (floatingBallStartX + dx).roundToInt()
                .coerceIn(dragMinX, dragMaxX)
            val requestedY = (floatingBallStartY + dy).roundToInt()
                .coerceIn(0, (context.resources.displayMetrics.heightPixels - view.height).coerceAtLeast(0))
            if (requestedX != previousX || requestedY != previousY) {
                params.x = requestedX
                params.y = requestedY
                val updateSucceeded = runCatching {
                    windowManager.updateViewLayout(view, params)
                    true
                }.getOrElse { failure ->
                    params.x = previousX
                    params.y = previousY
                    recordSystemServerDiagnosticEvent(
                        context,
                        "floating-ball drag position update failed fromX=$previousX fromY=$previousY " +
                            "requestedX=$requestedX requestedY=$requestedY " +
                            "error=${failure.javaClass.simpleName}:${failure.message.orEmpty()}",
                    )
                    false
                }
                if (updateSucceeded) {
                    floatingBallX = params.x
                    floatingBallY = params.y
                    if (!floatingBallPositionChanged &&
                        (params.x != floatingBallStartX || params.y != floatingBallStartY)
                    ) {
                        floatingBallPositionChanged = true
                        recordSystemServerDiagnosticEvent(
                            context,
                            "floating-ball manual position changed: startX=$floatingBallStartX startY=$floatingBallStartY " +
                                "currentX=${params.x} currentY=${params.y}; auto-edge countdown will start on release " +
                                "independent of action execution",
                        )
                    }
                }
            }
        }
        MotionEvent.ACTION_UP -> {
            floatingBallTouchPointerId = -1
            floatingBallTouchDownAtMs = 0L
            val dockedAtDown = floatingBallDockedAtDown
            floatingBallDockedAtDown = 0
            floatingBallLongPressRunnable?.let { mainHandler?.removeCallbacks(it) }
            floatingBallLongPressRunnable = null
            val manualPositionChanged = floatingBallPositionChanged
            if (floatingBallDragging || manualPositionChanged) {
                floatingBallDragging = false
                floatingBallTouchActive = false
                floatingBallPositionChanged = false
                floatingBallX = params.x
                floatingBallY = params.y
                resetFloatingBallInnerOffset(view)
                // Keep the last genuinely free position so a later marker tap can restore it.
                val sideAfterDrag = dockedSideOf(context, windowManager, view, params)
                if (sideAfterDrag != 0) {
                    floatingBallDockedSide = sideAfterDrag
                } else {
                    val displayWidth = floatingBallDisplayWidth(context, windowManager, view)
                    val ballWidth = view.width.takeIf { it > 0 } ?: params.width
                    if (params.x < 0 || params.x + ballWidth > displayWidth) {
                        // Released while still half off-screen: collapse straight back into the
                        // edge marker instead of leaving a clipped circle on the edge.
                        if (snapFloatingBallToNearestEdge(context, windowManager, view, params)) {
                            floatingBallX = params.x
                            armFloatingBallIdleAutoEdge(context, windowManager, view, params)
                            return
                        }
                    }
                    floatingBallDockedSide = 0
                    floatingBallFreeX = params.x
                    floatingBallFreeY = params.y
                }
                view.invalidate()
                recordSystemServerDiagnosticEvent(
                    context,
                    "floating-ball drag ended without requiring an action: positionChanged=$manualPositionChanged " +
                        "x=${params.x} y=${params.y} dockedSide=$floatingBallDockedSide " +
                        "restoreX=$floatingBallFreeX restoreY=$floatingBallFreeY; " +
                        "restarting auto-edge countdown for configured delay",
                )
                armFloatingBallIdleAutoEdge(context, windowManager, view, params)
                return
            }
            // A tap on the docked marker brings the ball back instead of running the configured
            // click action. Anything under the swipe threshold counts as a tap: the visible
            // marker is only ~20dp wide, so holding a strict 8dp slop there is unrealistic.
            val markerDx = event.rawX - floatingBallTouchStartX
            val markerDy = event.rawY - floatingBallTouchStartY
            val markerTap = dockedAtDown != 0 &&
                maxOf(kotlin.math.abs(markerDx), kotlin.math.abs(markerDy)) <
                dp(context, FLOATING_BALL_SWIPE_THRESHOLD_DP)
            if (markerTap) {
                floatingBallTouchActive = false
                resetFloatingBallInnerOffset(view)
                runCatching {
                    performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
                }
                if (restoreFloatingBallFromDock(context, windowManager, view, params)) {
                    recordSystemServerDiagnosticEvent(
                        context,
                        "floating-ball restored from dock marker: side=$dockedAtDown " +
                            "toX=${params.x} toY=${params.y}",
                    )
                }
                armFloatingBallIdleAutoEdge(context, windowManager, view, params)
                return
            }
            val dx = event.rawX - floatingBallTouchStartX
            val dy = event.rawY - floatingBallTouchStartY
            val absX = kotlin.math.abs(dx)
            val absY = kotlin.math.abs(dy)
            val action = if (maxOf(absX, absY) >= dp(context, FLOATING_BALL_SWIPE_THRESHOLD_DP)) {
                when {
                    absY >= absX && dy < 0 -> settings.swipeUpAction
                    absY >= absX -> settings.swipeDownAction
                    dx < 0 -> settings.swipeLeftAction
                    else -> settings.swipeRightAction
                }
            } else if (!floatingBallMoved && event.eventTime - floatingBallLastTapTime <= FLOATING_BALL_DOUBLE_TAP_MS) {
                floatingBallPendingClickRunnable?.let { mainHandler?.removeCallbacks(it) }
                floatingBallPendingClickRunnable = null
                settings.doubleClickAction
            } else if (!floatingBallMoved) {
                val clickAction = settings.clickAction
                if (clickAction != ACTION_NONE) {
                    floatingBallPendingClickRunnable = Runnable {
                        runCatching {
                            performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
                            performGlobalGestureAction(context, clickAction)
                        }.onFailure { failure ->
                            recordSystemServerDiagnosticEvent(
                                context,
                                "floating-ball delayed click action failed action=$clickAction " +
                                    "error=${failure.javaClass.simpleName}:${failure.message.orEmpty()}",
                            )
                        }
                        floatingBallPendingClickRunnable = null
                        // Re-arm after the delayed click so a slow action cannot eat the countdown.
                        armFloatingBallIdleAutoEdge(context, windowManager, view, params)
                    }.also { runnable ->
                        postOnMainDelayed(runnable, FLOATING_BALL_DOUBLE_TAP_MS)
                    }
                }
                ACTION_NONE
            } else {
                ACTION_NONE
            }
            floatingBallLastTapTime = event.eventTime
            resetFloatingBallInnerOffset(view)
            if (action != ACTION_NONE) {
                runCatching {
                    performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
                    performGlobalGestureAction(context, action)
                }.onFailure { failure ->
                    recordSystemServerDiagnosticEvent(
                        context,
                        "floating-ball gesture action failed action=$action " +
                            "error=${failure.javaClass.simpleName}:${failure.message.orEmpty()}",
                    )
                }
            }
            floatingBallTouchActive = false
            armFloatingBallIdleAutoEdge(context, windowManager, view, params)
        }
        MotionEvent.ACTION_CANCEL -> {
            floatingBallTouchPointerId = -1
            floatingBallTouchDownAtMs = 0L
            floatingBallDockedAtDown = 0
            val manualPositionChanged = floatingBallPositionChanged
            floatingBallLongPressRunnable?.let { mainHandler?.removeCallbacks(it) }
            floatingBallLongPressRunnable = null
            floatingBallPendingClickRunnable?.let { mainHandler?.removeCallbacks(it) }
            floatingBallPendingClickRunnable = null
            floatingBallDragging = false
            floatingBallTouchActive = false
            floatingBallPositionChanged = false
            floatingBallX = params.x
            floatingBallY = params.y
            resetFloatingBallInnerOffset(view)
            recordSystemServerDiagnosticEvent(
                context,
                "floating-ball touch cancelled: positionChanged=$manualPositionChanged x=${params.x} y=${params.y}; " +
                    "restarting auto-edge countdown independent of action execution",
            )
            armFloatingBallIdleAutoEdge(context, windowManager, view, params)
        }
    }
}

private const val FLOATING_BALL_TOUCH_END_RECOVERY_DELAY_MS = 500L
private const val FLOATING_BALL_TOUCH_RELEASE_MARKER_MAX_AGE_MS = 30_000L

internal fun GlobalQuickActions.recoverFloatingBallTouchEndFromGlobalInput(event: MotionEvent) {
    val action = event.actionMasked
    if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) return

    val trackedPointerId = floatingBallTouchPointerId
    val eventPointerId = if (
        action == MotionEvent.ACTION_UP && event.pointerCount > 0
    ) {
        event.getPointerId(event.actionIndex.coerceIn(0, event.pointerCount - 1))
    } else {
        -1
    }
    val markerAge = SystemClock.uptimeMillis() - floatingBallTouchDownTime
    val recentMatchingReleaseMarker =
        trackedPointerId >= 0 &&
            markerAge in 0..FLOATING_BALL_TOUCH_RELEASE_MARKER_MAX_AGE_MS &&
            (eventPointerId < 0 || trackedPointerId == eventPointerId)

    // Do not rely on touchActive alone: Samsung may clear one touch flag while the
    // matching floating-ball release still has no successfully armed idle timer.
    if (!floatingBallTouchActive && !floatingBallDragging && !recentMatchingReleaseMarker) return
    if (eventPointerId >= 0 && trackedPointerId >= 0 && eventPointerId != trackedPointerId) return

    val context = systemServerContext ?: currentApplicationContext() ?: return
    val view = floatingBallView ?: return
    val params = floatingBallLayoutParams ?: return
    val windowManager = floatingBallWindowManager ?: return
    val capturedDownTime = floatingBallTouchDownTime
    val capturedPointerId = trackedPointerId

    mainHandler.postDelayed({
        if (
            floatingBallTouchDownTime != capturedDownTime ||
            floatingBallView !== view ||
            floatingBallLayoutParams !== params
        ) {
            return@postDelayed
        }

        val touchStillActive = floatingBallTouchActive || floatingBallDragging
        val sameReleaseMarker = capturedPointerId >= 0 &&
            floatingBallTouchPointerId == capturedPointerId
        // A normally delivered UP already arms the timer and clears the marker.
        // Recover only if the normal path left no timer behind.
        if (floatingBallAutoEdgeRunnable != null ||
            (!touchStillActive && !sameReleaseMarker)
        ) {
            return@postDelayed
        }

        val wasDragging = floatingBallDragging
        floatingBallTouchActive = false
        floatingBallTouchPointerId = -1
        floatingBallDragging = false
        floatingBallLongPressRunnable?.let { mainHandler.removeCallbacks(it) }
        floatingBallLongPressRunnable = null
        resetFloatingBallInnerOffset(view)
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball recovered missing release/auto-edge timer: " +
                "action=${if (action == MotionEvent.ACTION_UP) "UP" else "CANCEL"} " +
                "wasDragging=$wasDragging touchWasActive=$touchStillActive " +
                "pointerId=$capturedPointerId x=${params.x} y=${params.y}; restarting countdown",
        )
        scheduleFloatingBallAutoEdge(context, windowManager, view, params, floatingBallSettings(context))
    }, FLOATING_BALL_TOUCH_END_RECOVERY_DELAY_MS)
}

/**
 * Single entry point for "restart the idle countdown". Every touch end (tap, double tap,
 * long-press drag, gesture action, cancelled touch) funnels through here, so the ball returns
 * to the nearest edge after the configured idle delay even when no action was performed.
 */
internal fun GlobalQuickActions.armFloatingBallIdleAutoEdge(
    context: Context,
    windowManager: WindowManager,
    view: View,
    params: WindowManager.LayoutParams
) {
    if (floatingBallTouchActive || floatingBallDragging) {
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball idle countdown deferred: touch still active touchActive=$floatingBallTouchActive " +
                "dragging=$floatingBallDragging",
        )
        return
    }
    scheduleFloatingBallAutoEdge(context, windowManager, view, params, floatingBallSettings(context))
}

internal fun GlobalQuickActions.scheduleFloatingBallAutoEdge(
    context: Context,
    windowManager: WindowManager,
    view: View,
    params: WindowManager.LayoutParams,
    settings: FloatingBallSettings
) {
    cancelFloatingBallAutoEdge()
    val delaySeconds = settings.autoEdgeDelaySeconds.coerceIn(0, 10)
    if (!settings.enabled || delaySeconds <= 0) {
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball auto-edge not scheduled: enabled=${settings.enabled} delaySeconds=$delaySeconds " +
                "x=${params.x} y=${params.y} attached=${view.isAttachedToWindow} touchActive=$floatingBallTouchActive",
        )
        return
    }

    val generation = floatingBallAutoEdgeGeneration
    val delayMillis = delaySeconds * 1000L
    recordSystemServerDiagnosticEvent(
        context,
        "floating-ball auto-edge scheduled: delaySeconds=$delaySeconds delayMillis=$delayMillis " +
            "generation=$generation x=${params.x} y=${params.y} attached=${view.isAttachedToWindow} " +
            "viewSize=${view.width}x${view.height} paramsSize=${params.width}x${params.height}",
    )

    lateinit var runnable: Runnable
    runnable = Runnable {
        val currentGeneration = floatingBallAutoEdgeGeneration
        val currentView = floatingBallView === view
        val currentParams = floatingBallLayoutParams === params
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball auto-edge timer fired: scheduledGeneration=$generation currentGeneration=$currentGeneration " +
                "currentView=$currentView currentParams=$currentParams attached=${view.isAttachedToWindow} " +
                "touchActive=$floatingBallTouchActive dragging=$floatingBallDragging x=${params.x} y=${params.y}",
        )

        if (generation != currentGeneration || floatingBallAutoEdgeRunnable !== runnable ||
            !currentView || !currentParams || !view.isAttachedToWindow
        ) {
            recordSystemServerDiagnosticEvent(
                context,
                "floating-ball auto-edge skipped stale task: generation=$generation currentGeneration=$currentGeneration " +
                    "currentView=$currentView currentParams=$currentParams attached=${view.isAttachedToWindow}",
            )
            return@Runnable
        }

        val currentSettings = floatingBallSettings(context)
        if (!currentSettings.enabled || currentSettings.autoEdgeDelaySeconds <= 0) {
            recordSystemServerDiagnosticEvent(
                context,
                "floating-ball auto-edge skipped current state: enabled=${currentSettings.enabled} " +
                    "delaySeconds=${currentSettings.autoEdgeDelaySeconds}",
            )
            if (floatingBallAutoEdgeRunnable === runnable) {
                floatingBallAutoEdgeGeneration += 1L
                floatingBallAutoEdgeRunnable = null
                floatingBallAutoEdgeHost = null
            }
            return@Runnable
        }
        // Same self-heal as ensureFloatingBall, but driven by this timer so a leaked touch
        // recovers even when no further input event arrives to trigger ensure.
        val downAtMs = floatingBallTouchDownAtMs
        if (floatingBallTouchActive && downAtMs > 0L &&
            SystemClock.uptimeMillis() - downAtMs > FLOATING_BALL_TOUCH_STALE_MS
        ) {
            recordSystemServerDiagnosticEvent(
                context,
                "floating-ball auto-edge stale touch reset: ageMs=${SystemClock.uptimeMillis() - downAtMs} " +
                    "dragging=$floatingBallDragging",
            )
            floatingBallTouchActive = false
            floatingBallDragging = false
            floatingBallTouchDownAtMs = 0L
        }
        if (floatingBallTouchActive || floatingBallDragging) {
            // Finger is still down: keep the task instead of dropping it, and re-check shortly.
            // The touch-end path also re-arms, so this is only a safety net.
            recordSystemServerDiagnosticEvent(
                context,
                "floating-ball auto-edge deferred: touchActive=$floatingBallTouchActive " +
                    "dragging=$floatingBallDragging retryMs=$FLOATING_BALL_IDLE_RETRY_MS",
            )
            val retried = mainHandler.postDelayed(runnable, FLOATING_BALL_IDLE_RETRY_MS)
            if (!retried && floatingBallAutoEdgeRunnable === runnable) {
                floatingBallAutoEdgeGeneration += 1L
                floatingBallAutoEdgeRunnable = null
                floatingBallAutoEdgeHost = null
            }
            return@Runnable
        }

        // Single main-handler queue owns the timer, matching the upstream runtime.
        floatingBallAutoEdgeGeneration += 1L
        mainHandler.removeCallbacks(runnable)
        floatingBallAutoEdgeRunnable = null
        floatingBallAutoEdgeHost = null
        snapFloatingBallToNearestEdge(context, windowManager, view, params)
    }

    floatingBallAutoEdgeRunnable = runnable
    floatingBallAutoEdgeHost = view
    val handlerPosted = mainHandler.postDelayed(runnable, delayMillis)
    if (!handlerPosted) {
        if (floatingBallAutoEdgeRunnable === runnable) {
            floatingBallAutoEdgeGeneration += 1L
            floatingBallAutoEdgeRunnable = null
            floatingBallAutoEdgeHost = null
        }
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball auto-edge scheduler rejected delay=${delayMillis}ms on main handler",
        )
    } else {
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball auto-edge timer armed: delaySeconds=$delaySeconds handlerPosted=$handlerPosted " +
                "x=${params.x} y=${params.y} generation=$generation",
        )
    }
}

internal fun GlobalQuickActions.cancelFloatingBallAutoEdge() {
    floatingBallAutoEdgeGeneration += 1L
    val runnable = floatingBallAutoEdgeRunnable
    if (runnable != null) {
        mainHandler.removeCallbacks(runnable)
    }
    floatingBallAutoEdgeRunnable = null
    floatingBallAutoEdgeHost = null
}

/**
 * Side the ball is currently docked on: -1 left, 1 right, 0 free-floating.
 * Mirrors the reveal math in [snapFloatingBallToNearestEdge] so a docked ball is recognised
 * exactly where the snap left it.
 */
internal fun GlobalQuickActions.dockedSideOf(
    context: Context,
    windowManager: WindowManager,
    view: View,
    params: WindowManager.LayoutParams
): Int {
    val ballWidth = view.width.takeIf { it > 0 } ?: params.width.takeIf { it > 0 } ?: return 0
    if (ballWidth <= 0) return 0
    val displayWidth = floatingBallDragDisplayWidth.takeIf { it > 0 }
        ?: floatingBallDisplayWidth(context, windowManager, view)
    if (displayWidth <= 0) return 0
    val indicatorWidth = floatingBallDockIndicatorWidth(context, ballWidth)
    val tolerance = dp(context, 3)
    return when {
        kotlin.math.abs(params.x - -(ballWidth - indicatorWidth)) <= tolerance -> -1
        kotlin.math.abs(params.x - (displayWidth - indicatorWidth)) <= tolerance -> 1
        else -> 0
    }
}

private fun GlobalQuickActions.floatingBallDockIndicatorWidth(context: Context, ballWidth: Int): Int =
    dp(context, FLOATING_BALL_DOCK_INDICATOR_WIDTH_DP).coerceIn(1, ballWidth.coerceAtLeast(1))

/**
 * Reserve the ball's rect from the system edge gestures. The docked marker sits exactly in the
 * back-gesture / edge-panel inset, so without this the tap or pull is swallowed by the system
 * and the marker feels dead.
 */
private fun applyFloatingBallGestureExclusion(view: View) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    if (view.width <= 0 || view.height <= 0) return
    runCatching {
        view.systemGestureExclusionRects = listOf(Rect(0, 0, view.width, view.height))
    }.onFailure { failure ->
        Log.w(
            GlobalQuickActions.TAG,
            "gesture exclusion rejected: ${failure.javaClass.simpleName}:${failure.message.orEmpty()}",
        )
    }
}

/**
 * Bring the ball back from the docked edge marker to the last free-floating position.
 * The idle countdown is re-armed by the caller, so docking stays the resting state.
 */
internal fun GlobalQuickActions.restoreFloatingBallFromDock(
    context: Context,
    windowManager: WindowManager,
    view: View,
    params: WindowManager.LayoutParams
): Boolean {
    val ballWidth = view.width.takeIf { it > 0 } ?: params.width.takeIf { it > 0 } ?: 0
    if (ballWidth <= 0) {
        recordSystemServerDiagnosticEvent(context, "floating-ball restore skipped: ball width unknown")
        return false
    }
    val displayWidth = floatingBallDisplayWidth(context, windowManager, view)
    val maxX = (displayWidth - ballWidth).coerceAtLeast(0)
    val maxY = (context.resources.displayMetrics.heightPixels - view.height).coerceAtLeast(0)
    val fallbackX = ((displayWidth - ballWidth) / 2).coerceIn(0, maxX)
    val targetX = floatingBallFreeX.takeIf { it >= 0 }?.coerceIn(0, maxX) ?: fallbackX
    val targetY = floatingBallFreeY.takeIf { it >= 0 }?.coerceIn(0, maxY) ?: params.y.coerceIn(0, maxY)
    val previousX = params.x
    val previousY = params.y
    params.x = targetX
    params.y = targetY
    val applied = runCatching {
        windowManager.updateViewLayout(view, params)
        true
    }.getOrElse { failure ->
        params.x = previousX
        params.y = previousY
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball restore failed fromX=$previousX fromY=$previousY toX=$targetX toY=$targetY " +
                "error=${failure.javaClass.simpleName}:${failure.message.orEmpty()}",
        )
        false
    }
    if (!applied) return false
    floatingBallX = params.x
    floatingBallY = params.y
    floatingBallFreeX = params.x
    floatingBallFreeY = params.y
    floatingBallDockedSide = 0
    view.invalidate()
    recordSystemServerDiagnosticEvent(
        context,
        "floating-ball restore applied: fromX=$previousX fromY=$previousY toX=${params.x} toY=${params.y} " +
            "displayWidth=$displayWidth ballWidth=$ballWidth",
    )
    return true
}

internal fun GlobalQuickActions.snapFloatingBallToNearestEdge(
    context: Context,
    windowManager: WindowManager,
    view: View,
    params: WindowManager.LayoutParams
): Boolean {
    val displayWidth = floatingBallDisplayWidth(context, windowManager, view)
    val ballWidth = view.width.takeIf { it > 0 } ?: params.width.takeIf { it > 0 } ?: 0
    if (displayWidth <= 0 || ballWidth <= 0) {
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball auto-edge failed invalid geometry displayWidth=$displayWidth " +
                "viewWidth=${view.width} paramsWidth=${params.width}",
        )
        return false
    }

    // Docking collapses the ball into an edge marker: only the marker width stays on-screen,
    // the rest of the window hangs outside. A tap on the marker restores the free position.
    val indicatorWidth = floatingBallDockIndicatorWidth(context, ballWidth)
    val leftTargetX = -(ballWidth - indicatorWidth)
    val rightTargetX = displayWidth - indicatorWidth
    val previousX = params.x
    val leftHalf = previousX + ballWidth / 2 < displayWidth / 2
    val targetX = if (leftHalf) leftTargetX else rightTargetX
    val side = if (leftHalf) "left" else "right"
    if (previousX == targetX && floatingBallDockedSide != 0) {
        floatingBallX = params.x
        floatingBallY = params.y
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball auto-edge already at target: side=$side x=$previousX indicatorWidth=$indicatorWidth " +
                "displayWidth=$displayWidth ballWidth=$ballWidth",
        )
        return true
    }

    // Remember where the user had put the ball before it collapsed, so the marker tap can
    // return it to that exact spot. Only fully visible positions qualify.
    if (floatingBallDockedSide == 0 && previousX != targetX &&
        previousX >= 0 && previousX + ballWidth <= displayWidth
    ) {
        floatingBallFreeX = previousX
        floatingBallFreeY = params.y
    }

    recordSystemServerDiagnosticEvent(
        context,
        "floating-ball auto-edge applying: side=$side displayWidth=$displayWidth ballWidth=$ballWidth " +
            "indicatorWidth=$indicatorWidth fromX=$previousX targetX=$targetX y=${params.y} " +
            "restoreX=$floatingBallFreeX restoreY=$floatingBallFreeY",
    )
    params.x = targetX
    val applied = runCatching {
        windowManager.updateViewLayout(view, params)
        true
    }.getOrElse {
        params.x = previousX
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball auto-edge update failed: side=$side fromX=$previousX targetX=$targetX " +
                "error=${it.javaClass.simpleName}:${it.message.orEmpty()}",
        )
        false
    }
    if (applied) {
        floatingBallX = params.x
        floatingBallY = params.y
        floatingBallDockedSide = if (leftHalf) -1 else 1
        applyFloatingBallGestureExclusion(view)
        view.invalidate()
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball auto-edge applied: side=$side fromX=$previousX toX=${params.x} y=${params.y} " +
                "displayWidth=$displayWidth ballWidth=$ballWidth indicatorWidth=$indicatorWidth",
        )
    }
    return applied
}

private fun GlobalQuickActions.floatingBallDisplayWidth(
    context: Context,
    windowManager: WindowManager,
    view: View
): Int {
    val point = Point()
    return runCatching {
        (view.display ?: windowManager.defaultDisplay).getRealSize(point)
        point.x
    }.getOrNull()?.takeIf { it > 0 } ?: context.resources.displayMetrics.widthPixels
}

internal fun GlobalQuickActions.updateFloatingBallInnerOffset(context: Context, view: View, dx: Float, dy: Float) {
    val limit = dp(context, FLOATING_BALL_INNER_OFFSET_LIMIT_DP).toFloat()
    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
    if (distance <= 0f) {
        floatingBallInnerOffsetX = 0f
        floatingBallInnerOffsetY = 0f
    } else {
        val ratio = minOf(1f, distance / dp(context, FLOATING_BALL_SWIPE_THRESHOLD_DP))
        floatingBallInnerOffsetX = dx / distance * limit * ratio
        floatingBallInnerOffsetY = dy / distance * limit * ratio
    }
    view.invalidate()
}

internal fun GlobalQuickActions.resetFloatingBallInnerOffset(view: View) {
    floatingBallInnerOffsetX = 0f
    floatingBallInnerOffsetY = 0f
    view.invalidate()
}
