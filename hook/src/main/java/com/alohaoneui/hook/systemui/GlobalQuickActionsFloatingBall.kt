package com.alohaoneui.hook.systemui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Point
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
            val rightEdgeX = (displayWidth - ballWidth).coerceAtLeast(0)
            val alreadyAtEdge = existingParams.x <= 0 || existingParams.x >= rightEdgeX
            if (!alreadyAtEdge) {
                recordSystemServerDiagnosticEvent(
                    context,
                    "floating-ball idle timer missing away from edge; rearming: delaySeconds=$delaySeconds " +
                        "x=${existingParams.x} rightEdgeX=$rightEdgeX touchActive=$floatingBallTouchActive",
                )
                scheduleFloatingBallAutoEdge(context, existingManager, existingView, existingParams, settings)
            }
        }
        return
    }

    val windowManager = context.getSystemService(WindowManager::class.java) ?: return
    floatingBallWindowManager = windowManager
    val size = dp(context, settings.sizeDp)
    val params = WindowManager.LayoutParams(
        size,
        size,
        TYPE_NAVIGATION_BAR_PANEL,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_SECURE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = floatingBallX.takeIf { it > 0 } ?: (context.resources.displayMetrics.widthPixels - size - dp(context, 18))
        y = floatingBallY.takeIf { it > 0 } ?: (statusBarHeightPx(context) + dp(context, 160))
    }
    val view = object : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
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
    }.apply {
        setOnTouchListener { v, event ->
            handleFloatingBallTouch(context, windowManager, v, params, event)
            true
        }
    }
    runCatching {
        windowManager.addView(view, params)
        floatingBallView = view
        floatingBallLayoutParams = params
        floatingBallX = params.x
        floatingBallY = params.y
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
    floatingBallDragging = false
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
            cancelFloatingBallAutoEdge()
            recordSystemServerDiagnosticEvent(context, "floating-ball touch down x=${params.x} y=${params.y}; auto-edge timer cancelled")
            floatingBallTouchStartX = event.rawX
            floatingBallTouchStartY = event.rawY
            floatingBallStartX = params.x
            floatingBallStartY = params.y
            floatingBallTouchDownTime = event.eventTime
            floatingBallMoved = false
            floatingBallDragging = false
            floatingBallInnerOffsetX = 0f
            floatingBallInnerOffsetY = 0f
            floatingBallPendingClickRunnable?.let { mainHandler?.removeCallbacks(it) }
            floatingBallPendingClickRunnable = null
            view.invalidate()
            floatingBallLongPressRunnable?.let { mainHandler?.removeCallbacks(it) }
            floatingBallLongPressRunnable = Runnable {
                floatingBallDragging = true
                floatingBallMoved = true
            }.also { runnable ->
                postOnMainDelayed(runnable, FLOATING_BALL_LONG_PRESS_MS)
            }
        }
        MotionEvent.ACTION_MOVE -> {
            val dx = event.rawX - floatingBallTouchStartX
            val dy = event.rawY - floatingBallTouchStartY
            updateFloatingBallInnerOffset(context, view, dx, dy)
            if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) > dp(context, FLOATING_BALL_TAP_SLOP_DP)) {
                floatingBallMoved = true
                if (!floatingBallDragging) {
                    floatingBallLongPressRunnable?.let { mainHandler?.removeCallbacks(it) }
                    floatingBallLongPressRunnable = null
                    return
                }
                params.x = (floatingBallStartX + dx).roundToInt().coerceIn(0, context.resources.displayMetrics.widthPixels - view.width)
                params.y = (floatingBallStartY + dy).roundToInt().coerceIn(0, context.resources.displayMetrics.heightPixels - view.height)
                floatingBallX = params.x
                floatingBallY = params.y
                runCatching { windowManager.updateViewLayout(view, params) }
            }
        }
        MotionEvent.ACTION_UP -> {
            floatingBallLongPressRunnable?.let { mainHandler?.removeCallbacks(it) }
            floatingBallLongPressRunnable = null
            if (floatingBallDragging) {
                floatingBallDragging = false
                floatingBallTouchActive = false
                resetFloatingBallInnerOffset(view)
                recordSystemServerDiagnosticEvent(
                    context,
                    "floating-ball drag ended x=${params.x} y=${params.y}; restarting auto-edge countdown",
                )
                scheduleFloatingBallAutoEdge(context, windowManager, view, params, floatingBallSettings(context))
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
                        performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
                        performGlobalGestureAction(context, clickAction)
                        floatingBallPendingClickRunnable = null
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
                performHapticFeedback(context, settings.hapticEnabled, settings.hapticStrength)
                performGlobalGestureAction(context, action)
            }
            floatingBallTouchActive = false
            scheduleFloatingBallAutoEdge(context, windowManager, view, params, floatingBallSettings(context))
        }
        MotionEvent.ACTION_CANCEL -> {
            floatingBallLongPressRunnable?.let { mainHandler?.removeCallbacks(it) }
            floatingBallLongPressRunnable = null
            floatingBallPendingClickRunnable?.let { mainHandler?.removeCallbacks(it) }
            floatingBallPendingClickRunnable = null
            floatingBallDragging = false
            floatingBallTouchActive = false
            resetFloatingBallInnerOffset(view)
            scheduleFloatingBallAutoEdge(context, windowManager, view, params, floatingBallSettings(context))
        }
    }
}

/**
 * If moving the overlay prevents its own View from receiving UP/CANCEL, use the global
 * SystemServer pointer stream as a fallback. A grace period lets normal View dispatch
 * finish first; a new gesture or normal touch completion invalidates the fallback.
 */
private const val FLOATING_BALL_TOUCH_END_RECOVERY_DELAY_MS = 500L

internal fun GlobalQuickActions.recoverFloatingBallTouchEndFromGlobalInput(event: MotionEvent) {
    val action = event.actionMasked
    if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) return
    if (!floatingBallTouchActive) return

    val context = systemServerContext ?: currentApplicationContext() ?: return
    val view = floatingBallView ?: return
    val params = floatingBallLayoutParams ?: return
    val windowManager = floatingBallWindowManager ?: return
    val capturedDownTime = floatingBallTouchDownTime

    mainHandler.postDelayed({
        if (
            !floatingBallTouchActive ||
            floatingBallTouchDownTime != capturedDownTime ||
            floatingBallView !== view ||
            floatingBallLayoutParams !== params
        ) {
            return@postDelayed
        }

        val wasDragging = floatingBallDragging
        floatingBallTouchActive = false
        floatingBallDragging = false
        floatingBallLongPressRunnable?.let { mainHandler.removeCallbacks(it) }
        floatingBallLongPressRunnable = null
        resetFloatingBallInnerOffset(view)
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball recovered missed touch end from global input: " +
                "action=${if (action == MotionEvent.ACTION_UP) "UP" else "CANCEL"} " +
                "wasDragging=$wasDragging x=${params.x} y=${params.y}; restarting auto-edge countdown",
        )
        scheduleFloatingBallAutoEdge(context, windowManager, view, params, floatingBallSettings(context))
    }, FLOATING_BALL_TOUCH_END_RECOVERY_DELAY_MS)
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
        if (!currentSettings.enabled || currentSettings.autoEdgeDelaySeconds <= 0 ||
            floatingBallTouchActive || floatingBallDragging
        ) {
            recordSystemServerDiagnosticEvent(
                context,
                "floating-ball auto-edge skipped current state: enabled=${currentSettings.enabled} " +
                    "delaySeconds=${currentSettings.autoEdgeDelaySeconds} touchActive=$floatingBallTouchActive " +
                    "dragging=$floatingBallDragging",
            )
            if (floatingBallAutoEdgeRunnable === runnable) {
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

    val maxX = (displayWidth - ballWidth).coerceAtLeast(0)
    val previousX = params.x
    val leftHalf = previousX + ballWidth / 2 < displayWidth / 2
    val targetX = if (leftHalf) 0 else maxX
    val side = if (leftHalf) "left" else "right"
    if (previousX == targetX) {
        floatingBallX = params.x
        floatingBallY = params.y
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball auto-edge already at target: side=$side x=$previousX maxX=$maxX " +
                "displayWidth=$displayWidth ballWidth=$ballWidth",
        )
        return true
    }

    recordSystemServerDiagnosticEvent(
        context,
        "floating-ball auto-edge applying: side=$side displayWidth=$displayWidth ballWidth=$ballWidth " +
            "fromX=$previousX targetX=$targetX y=${params.y}",
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
        recordSystemServerDiagnosticEvent(
            context,
            "floating-ball auto-edge applied: side=$side fromX=$previousX toX=${params.x} y=${params.y} " +
                "displayWidth=$displayWidth ballWidth=$ballWidth maxX=$maxX",
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
