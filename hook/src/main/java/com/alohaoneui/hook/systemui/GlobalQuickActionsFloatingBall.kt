package com.alohaoneui.hook.systemui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
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
    if (floatingBallView != null) return

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
        // Arm the timer immediately from SystemServer's main Handler. Do not make
        // View.post() the only path: the initial attach/layout callback was not
        // reliable on the affected Samsung SystemUI lifecycle.
        scheduleFloatingBallAutoEdge(context, windowManager, view, params, floatingBallSettings(context))
        // Layout gives us a second, event-driven chance if neither queue accepted
        // the initial timer. Never reset an already-armed countdown here.
        view.post {
            if (floatingBallView === view && view.isAttachedToWindow) {
                recordSystemServerDiagnosticEvent(
                    context,
                    "floating-ball first-layout callback size=${view.width}x${view.height} timerArmed=${floatingBallAutoEdgeRunnable != null}",
                )
                if (floatingBallAutoEdgeRunnable == null) {
                    scheduleFloatingBallAutoEdge(context, windowManager, view, params, floatingBallSettings(context))
                }
            } else {
                recordSystemServerDiagnosticEvent(
                    context,
                    "floating-ball first-layout callback ignored currentView=${floatingBallView === view} attached=${view.isAttachedToWindow}",
                )
            }
        }
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
    cancelFloatingBallAutoEdge()
    val view = floatingBallView ?: return
    val context = systemServerContext ?: currentApplicationContext() ?: return
    val windowManager = floatingBallWindowManager ?: context.getSystemService(WindowManager::class.java) ?: return
    runCatching { windowManager.removeView(view) }
        .onFailure {
            recordSystemServerDiagnosticEvent(
                context,
                "floating-ball remove failed error=${it.javaClass.simpleName}:${it.message.orEmpty()}",
            )
        }
    recordSystemServerDiagnosticEvent(context, "floating-ball removed")
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
            cancelFloatingBallAutoEdge()
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
            scheduleFloatingBallAutoEdge(context, windowManager, view, params, settings)
        }
        MotionEvent.ACTION_CANCEL -> {
            floatingBallLongPressRunnable?.let { mainHandler?.removeCallbacks(it) }
            floatingBallLongPressRunnable = null
            floatingBallPendingClickRunnable?.let { mainHandler?.removeCallbacks(it) }
            floatingBallPendingClickRunnable = null
            floatingBallDragging = false
            resetFloatingBallInnerOffset(view)
            scheduleFloatingBallAutoEdge(context, windowManager, view, params, settings)
        }
    }
}

internal fun GlobalQuickActions.scheduleFloatingBallAutoEdge(
    context: Context,
    windowManager: WindowManager,
    view: View,
    params: WindowManager.LayoutParams,
    settings: FloatingBallSettings
) {
    cancelFloatingBallAutoEdge()
    val delaySeconds = settings.autoEdgeDelaySeconds
    if (delaySeconds <= 0 || !settings.enabled) {
        recordSystemServerDiagnosticEvent(
            context,
            "auto-edge not scheduled enabled=${settings.enabled} delaySeconds=$delaySeconds x=${params.x} attached=${view.isAttachedToWindow}",
        )
        return
    }

    val delayMillis = delaySeconds * 1000L
    val generation = floatingBallAutoEdgeGeneration
    var fired = false
    val runnable = object : Runnable {
        override fun run() {
            if (fired) return
            fired = true
            if (generation != floatingBallAutoEdgeGeneration || floatingBallAutoEdgeRunnable !== this) {
                recordSystemServerDiagnosticEvent(
                    context,
                    "auto-edge stale callback skipped taskGeneration=$generation currentGeneration=$floatingBallAutoEdgeGeneration currentView=${floatingBallView === view} currentParams=${floatingBallLayoutParams === params}",
                )
                return
            }

            val host = floatingBallAutoEdgeHost
            mainHandler.removeCallbacks(this)
            host?.removeCallbacks(this)
            floatingBallAutoEdgeRunnable = null
            floatingBallAutoEdgeHost = null
            floatingBallAutoEdgeGeneration += 1

            val currentSettings = floatingBallSettings(context)
            if (!currentSettings.enabled || currentSettings.autoEdgeDelaySeconds <= 0) {
                recordSystemServerDiagnosticEvent(
                    context,
                    "auto-edge callback cancelled by current settings enabled=${currentSettings.enabled} delaySeconds=${currentSettings.autoEdgeDelaySeconds}",
                )
                return
            }
            if (floatingBallView !== view || floatingBallLayoutParams !== params || !view.isAttachedToWindow) {
                recordSystemServerDiagnosticEvent(
                    context,
                    "auto-edge callback skipped stale window currentView=${floatingBallView === view} currentParams=${floatingBallLayoutParams === params} attached=${view.isAttachedToWindow}",
                )
                return
            }
            if (floatingBallDragging) {
                recordSystemServerDiagnosticEvent(context, "auto-edge callback skipped because floating-ball is dragging")
                return
            }

            recordSystemServerDiagnosticEvent(
                context,
                "auto-edge timer fired generation=$generation x=${params.x} y=${params.y} viewSize=${view.width}x${view.height} paramsSize=${params.width}x${params.height}",
            )
            snapFloatingBallToNearestEdge(context, windowManager, view, params)
        }
    }

    floatingBallAutoEdgeRunnable = runnable
    floatingBallAutoEdgeHost = view
    val handlerAccepted = mainHandler.postDelayed(runnable, delayMillis)
    val viewAccepted = view.postDelayed(runnable, delayMillis)
    if (!handlerAccepted && !viewAccepted) {
        mainHandler.removeCallbacks(runnable)
        view.removeCallbacks(runnable)
        if (floatingBallAutoEdgeRunnable === runnable) {
            floatingBallAutoEdgeRunnable = null
            floatingBallAutoEdgeHost = null
            floatingBallAutoEdgeGeneration += 1
        }
        recordSystemServerDiagnosticEvent(
            context,
            "auto-edge scheduling rejected delayMillis=$delayMillis handlerAccepted=$handlerAccepted viewAccepted=$viewAccepted attached=${view.isAttachedToWindow}",
        )
        return
    }
    recordSystemServerDiagnosticEvent(
        context,
        "auto-edge scheduled delaySeconds=$delaySeconds delayMillis=$delayMillis handlerAccepted=$handlerAccepted viewAccepted=$viewAccepted attached=${view.isAttachedToWindow} x=${params.x} y=${params.y} viewSize=${view.width}x${view.height} paramsSize=${params.width}x${params.height} generation=$generation",
    )
}

internal fun GlobalQuickActions.cancelFloatingBallAutoEdge() {
    val runnable = floatingBallAutoEdgeRunnable
    val host = floatingBallAutoEdgeHost
    floatingBallAutoEdgeGeneration += 1
    if (runnable != null) {
        mainHandler.removeCallbacks(runnable)
        host?.removeCallbacks(runnable)
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
    val displayWidth = context.resources.displayMetrics.widthPixels
    // The first auto-edge callback can race the overlay's initial layout. Use the
    // actual layout width when available, otherwise the configured WindowManager width.
    val ballWidth = view.width.takeIf { it > 0 } ?: params.width.takeIf { it > 0 }
    if (displayWidth <= 0 || ballWidth == null || ballWidth <= 0) {
        recordSystemServerDiagnosticEvent(
            context,
            "auto-edge failed invalid geometry displayWidth=$displayWidth viewWidth=${view.width} paramsWidth=${params.width}",
        )
        return false
    }
    val maxX = (displayWidth - ballWidth).coerceAtLeast(0)
    val targetX = if (params.x + ballWidth / 2f < displayWidth / 2f) 0 else maxX
    val side = if (targetX == 0) "left" else "right"
    val fromX = params.x
    if (fromX == targetX) {
        floatingBallX = params.x
        floatingBallY = params.y
        recordSystemServerDiagnosticEvent(
            context,
            "auto-edge already at target side=$side x=$fromX maxX=$maxX displayWidth=$displayWidth ballWidth=$ballWidth",
        )
        return true
    }

    params.x = targetX
    val updateSucceeded = runCatching {
        windowManager.updateViewLayout(view, params)
        true
    }.getOrElse {
        params.x = fromX
        recordSystemServerDiagnosticEvent(
            context,
            "auto-edge update failed side=$side fromX=$fromX targetX=$targetX y=${params.y} error=${it.javaClass.simpleName}:${it.message.orEmpty()}",
        )
        false
    }
    if (updateSucceeded) {
        floatingBallX = params.x
        floatingBallY = params.y
        recordSystemServerDiagnosticEvent(
            context,
            "auto-edge applied side=$side fromX=$fromX toX=${params.x} y=${params.y} displayWidth=$displayWidth ballWidth=$ballWidth maxX=$maxX",
        )
    }
    return updateSucceeded
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
