package com.example.yadra

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var value = 0f
    private var maximum = 100f

    private var title = ""
    private var unit = ""

    private val startAngle = 135f
    private val sweepAngle = 270f

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        arcPaint.style = Paint.Style.STROKE
        arcPaint.strokeWidth = 22f
        arcPaint.strokeCap = Paint.Cap.BUTT

        tickPaint.style = Paint.Style.STROKE
        tickPaint.strokeWidth = 3f

        textPaint.textAlign = Paint.Align.CENTER

        needlePaint.style = Paint.Style.STROKE
        needlePaint.strokeWidth = 6f
        needlePaint.strokeCap = Paint.Cap.ROUND

        centerPaint.style = Paint.Style.FILL
    }

    fun setTitle(title: String) {
        this.title = title
        invalidate()
    }

    fun setUnit(unit: String) {
        this.unit = unit
        invalidate()
    }

    fun setMaximum(maximum: Float) {
        this.maximum = maximum.coerceAtLeast(1f)

        if (value > this.maximum) {
            value = this.maximum
        }

        invalidate()
    }

    fun setValue(value: Float) {
        this.value = value.coerceIn(0f, maximum)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        if (width <= 0f || height <= 0f) {
            return
        }

        val centerX = width / 2f
        val centerY = height * 0.57f

        val radius = min(width, height) * 0.37f

        drawBackground(canvas, centerX, centerY, radius)
        drawGaugeRanges(canvas, centerX, centerY, radius)
        drawTicks(canvas, centerX, centerY, radius)
        drawNumbers(canvas, centerX, centerY, radius)
        drawNeedle(canvas, centerX, centerY, radius)
        drawCenter(canvas, centerX, centerY)
        drawTitle(canvas, centerX, centerY, radius)
    }

    private fun drawBackground(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.color = Color.rgb(20, 27, 34)

        canvas.drawCircle(
            centerX,
            centerY,
            radius + 30f,
            backgroundPaint
        )

        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = 2f
        backgroundPaint.color = Color.rgb(55, 70, 82)

        canvas.drawCircle(
            centerX,
            centerY,
            radius + 30f,
            backgroundPaint
        )
    }

    private fun drawGaugeRanges(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        val greenSweep = sweepAngle * 0.60f
        val yellowSweep = sweepAngle * 0.20f
        val redSweep = sweepAngle * 0.20f

        arcPaint.strokeWidth = 22f

        arcPaint.color = Color.rgb(0, 230, 118)

        canvas.drawArc(
            rect,
            startAngle,
            greenSweep,
            false,
            arcPaint
        )

        arcPaint.color = Color.rgb(255, 193, 7)

        canvas.drawArc(
            rect,
            startAngle + greenSweep,
            yellowSweep,
            false,
            arcPaint
        )

        arcPaint.color = Color.rgb(244, 67, 54)

        canvas.drawArc(
            rect,
            startAngle + greenSweep + yellowSweep,
            redSweep,
            false,
            arcPaint
        )

        arcPaint.strokeWidth = 5f
        arcPaint.color = Color.rgb(80, 95, 105)

        canvas.drawArc(
            rect,
            startAngle,
            sweepAngle,
            false,
            arcPaint
        )
    }

    private fun drawTicks(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        val majorTicks = 10
        val minorTicksPerSection = 4

        for (i in 0..majorTicks) {

            val fraction = i / majorTicks.toFloat()

            val angleDegrees =
                startAngle + sweepAngle * fraction

            val angleRadians =
                Math.toRadians(angleDegrees.toDouble())

            val outerRadius = radius - 5f

            val innerRadius =
                if (i % 2 == 0) {
                    radius - 27f
                } else {
                    radius - 20f
                }

            val startX =
                centerX +
                        cos(angleRadians).toFloat() *
                        innerRadius

            val startY =
                centerY +
                        sin(angleRadians).toFloat() *
                        innerRadius

            val endX =
                centerX +
                        cos(angleRadians).toFloat() *
                        outerRadius

            val endY =
                centerY +
                        sin(angleRadians).toFloat() *
                        outerRadius

            tickPaint.color = Color.rgb(220, 230, 235)

            tickPaint.strokeWidth =
                if (i % 2 == 0) {
                    4f
                } else {
                    2f
                }

            canvas.drawLine(
                startX,
                startY,
                endX,
                endY,
                tickPaint
            )

            if (i < majorTicks) {

                for (j in 1 until minorTicksPerSection) {

                    val minorFraction =
                        (i + j.toFloat() / minorTicksPerSection) /
                                majorTicks.toFloat()

                    val minorAngle =
                        Math.toRadians(
                            (
                                    startAngle +
                                            sweepAngle * minorFraction
                                    ).toDouble()
                        )

                    val minorOuter =
                        radius - 6f

                    val minorInner =
                        radius - 15f

                    val minorStartX =
                        centerX +
                                cos(minorAngle).toFloat() *
                                minorInner

                    val minorStartY =
                        centerY +
                                sin(minorAngle).toFloat() *
                                minorInner

                    val minorEndX =
                        centerX +
                                cos(minorAngle).toFloat() *
                                minorOuter

                    val minorEndY =
                        centerY +
                                sin(minorAngle).toFloat() *
                                minorOuter

                    tickPaint.color =
                        Color.rgb(120, 135, 145)

                    tickPaint.strokeWidth = 1.5f

                    canvas.drawLine(
                        minorStartX,
                        minorStartY,
                        minorEndX,
                        minorEndY,
                        tickPaint
                    )
                }
            }
        }
    }

    private fun drawNumbers(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        textPaint.color = Color.WHITE
        textPaint.textSize = radius * 0.12f

        textPaint.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

        for (i in 0..10) {

            val fraction = i / 10f

            val angle =
                Math.toRadians(
                    (
                            startAngle +
                                    sweepAngle * fraction
                            ).toDouble()
                )

            val numberRadius = radius - 52f

            val x =
                centerX +
                        cos(angle).toFloat() *
                        numberRadius

            val y =
                centerY +
                        sin(angle).toFloat() *
                        numberRadius +
                        textPaint.textSize * 0.35f

            val number =
                (maximum * fraction).toInt()

            canvas.drawText(
                number.toString(),
                x,
                y,
                textPaint
            )
        }
    }

    private fun drawNeedle(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        val fraction =
            (value / maximum).coerceIn(0f, 1f)

        val angleDegrees =
            startAngle + sweepAngle * fraction

        val angleRadians =
            Math.toRadians(angleDegrees.toDouble())

        val needleLength =
            radius - 38f

        val endX =
            centerX +
                    cos(angleRadians).toFloat() *
                    needleLength

        val endY =
            centerY +
                    sin(angleRadians).toFloat() *
                    needleLength

        needlePaint.shader =
            LinearGradient(
                centerX,
                centerY,
                endX,
                endY,
                Color.rgb(0, 180, 255),
                Color.rgb(80, 220, 255),
                Shader.TileMode.CLAMP
            )

        needlePaint.setShadowLayer(
            14f,
            0f,
            0f,
            Color.rgb(0, 170, 255)
        )

        canvas.drawLine(
            centerX,
            centerY,
            endX,
            endY,
            needlePaint
        )

        needlePaint.shader = null
        needlePaint.clearShadowLayer()

        centerPaint.color =
            Color.rgb(10, 15, 20)

        centerPaint.style = Paint.Style.FILL

        canvas.drawCircle(
            centerX,
            centerY,
            18f,
            centerPaint
        )

        centerPaint.style = Paint.Style.STROKE
        centerPaint.strokeWidth = 4f
        centerPaint.color =
            Color.rgb(0, 190, 255)

        canvas.drawCircle(
            centerX,
            centerY,
            18f,
            centerPaint
        )

        centerPaint.style = Paint.Style.FILL
    }

    private fun drawCenter(
        canvas: Canvas,
        centerX: Float,
        centerY: Float
    ) {
        val valueText =
            if (value % 1f == 0f) {
                value.toInt().toString()
            } else {
                String.format("%.1f", value)
            }

        textPaint.color = Color.WHITE
        textPaint.textSize = 32f

        textPaint.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

        canvas.drawText(
            valueText,
            centerX,
            centerY + 70f,
            textPaint
        )

        textPaint.color =
            Color.rgb(0, 190, 255)

        textPaint.textSize = 15f

        canvas.drawText(
            unit,
            centerX,
            centerY + 92f,
            textPaint
        )
    }

    private fun drawTitle(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        textPaint.color = Color.WHITE
        textPaint.textSize = 20f

        textPaint.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

        canvas.drawText(
            title,
            centerX,
            centerY - radius - 42f,
            textPaint
        )
    }
}