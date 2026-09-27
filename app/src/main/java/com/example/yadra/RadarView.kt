package com.example.yadra

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class RadarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val radarColor = Color.rgb(0, 255, 80)

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = radarColor
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = radarColor
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = radarColor
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(35, 0, 255, 80)
        style = Paint.Style.FILL
    }

    private var sweepAngle = 0f

    private val radarPoints = mutableListOf<Pair<Float, Float>>()

    private val animationRunnable = object : Runnable {
        override fun run() {
            sweepAngle += 2.5f

            if (sweepAngle >= 360f) {
                sweepAngle = 0f
            }

            invalidate()
            postDelayed(this, 16)
        }
    }

    init {
        setBackgroundColor(Color.rgb(2, 12, 7))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(animationRunnable)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animationRunnable)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        val radius =
            (minOf(width, height) / 2f) - 30f

        // دایره مرکزی کم‌رنگ
        canvas.drawCircle(
            centerX,
            centerY,
            radius,
            glowPaint
        )

        // حلقه‌های رادار
        val ringCount = 4

        for (i in 1..ringCount) {

            val ringRadius =
                radius * i / ringCount

            canvas.drawCircle(
                centerX,
                centerY,
                ringRadius,
                circlePaint
            )
        }

        // خط عمودی
        canvas.drawLine(
            centerX,
            centerY - radius,
            centerX,
            centerY + radius,
            circlePaint
        )

        // خط افقی
        canvas.drawLine(
            centerX - radius,
            centerY,
            centerX + radius,
            centerY,
            circlePaint
        )

        // خط اسکن
        val angleRadians =
            Math.toRadians(
                (sweepAngle - 90f).toDouble()
            )

        val endX =
            centerX + radius * cos(angleRadians).toFloat()

        val endY =
            centerY + radius * sin(angleRadians).toFloat()

        canvas.drawLine(
            centerX,
            centerY,
            endX,
            endY,
            linePaint
        )

        // نوک عقربه
        canvas.drawCircle(
            endX,
            endY,
            5f,
            dotPaint
        )

        // نقطه مرکزی
        canvas.drawCircle(
            centerX,
            centerY,
            8f,
            dotPaint
        )

        // چند نقطه نمونه روی رادار
        drawRadarPoint(
            canvas,
            centerX,
            centerY,
            radius,
            45f,
            0.55f
        )

        drawRadarPoint(
            canvas,
            centerX,
            centerY,
            radius,
            140f,
            0.75f
        )

        drawRadarPoint(
            canvas,
            centerX,
            centerY,
            radius,
            225f,
            0.40f
        )

        drawRadarPoint(
            canvas,
            centerX,
            centerY,
            radius,
            310f,
            0.82f
        )
    }

    private fun drawRadarPoint(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        angle: Float,
        distance: Float
    ) {

        val radians =
            Math.toRadians(
                (angle - 90f).toDouble()
            )

        val x =
            centerX +
                    radius *
                    distance *
                    cos(radians).toFloat()

        val y =
            centerY +
                    radius *
                    distance *
                    sin(radians).toFloat()

        canvas.drawCircle(
            x,
            y,
            6f,
            dotPaint
        )
    }
}