package com.example.yadra

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class LiveChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val values = mutableListOf<Float>()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF42A5F5.toInt()
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = 0x334A5563
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var lineColor = 0xFF42A5F5.toInt()

    private val maxPoints = 40

    init {
        setBackgroundColor(0x00171D24)
        linePaint.color = lineColor
    }

    fun setLineColor(color: Int) {
        lineColor = color
        linePaint.color = color
        invalidate()
    }

    fun addValue(value: Float) {
        if (!value.isFinite()) {
            return
        }

        values.add(value)

        if (values.size > maxPoints) {
            values.removeAt(0)
        }

        invalidate()
    }

    fun clearChart() {
        values.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        if (width <= 16f || height <= 20f) {
            return
        }

        drawGrid(canvas, width, height)

        if (values.isEmpty()) {
            return
        }

        val minValue = values.minOrNull() ?: 0f
        val maxValue = values.maxOrNull() ?: minValue

        val range = max(
            1f,
            maxValue - minValue
        )

        val chartLeft = 8f
        val chartRight = width - 8f
        val chartTop = 10f
        val chartBottom = height - 10f

        val path = Path()

        values.forEachIndexed { index, value ->

            val x = if (values.size == 1) {
                (chartLeft + chartRight) / 2f
            } else {
                chartLeft +
                        (chartRight - chartLeft) *
                        index /
                        (values.size - 1).toFloat()
            }

            val normalized =
                (value - minValue) / range

            val y =
                chartBottom -
                        normalized *
                        (chartBottom - chartTop)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        linePaint.color = lineColor

        canvas.drawPath(
            path,
            linePaint
        )

        drawFill(
            canvas,
            width,
            height,
            minValue,
            range
        )
    }

    private fun drawGrid(
        canvas: Canvas,
        width: Float,
        height: Float
    ) {
        gridPaint.color = 0x334A5563

        val horizontalLines = 4

        for (i in 0..horizontalLines) {

            val y =
                10f +
                        (height - 20f) *
                        i /
                        horizontalLines.toFloat()

            canvas.drawLine(
                8f,
                y,
                width - 8f,
                y,
                gridPaint
            )
        }

        val verticalLines = 5

        for (i in 0..verticalLines) {

            val x =
                8f +
                        (width - 16f) *
                        i /
                        verticalLines.toFloat()

            canvas.drawLine(
                x,
                10f,
                x,
                height - 10f,
                gridPaint
            )
        }
    }

    private fun drawFill(
        canvas: Canvas,
        width: Float,
        height: Float,
        minValue: Float,
        range: Float
    ) {
        if (values.size < 2) {
            return
        }

        val chartLeft = 8f
        val chartRight = width - 8f
        val chartTop = 10f
        val chartBottom = height - 10f

        val fillPath = Path()

        values.forEachIndexed { index, value ->

            val x =
                chartLeft +
                        (chartRight - chartLeft) *
                        index /
                        (values.size - 1).toFloat()

            val normalized =
                (value - minValue) / range

            val y =
                chartBottom -
                        normalized *
                        (chartBottom - chartTop)

            if (index == 0) {
                fillPath.moveTo(x, chartBottom)
                fillPath.lineTo(x, y)
            } else {
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(chartRight, chartBottom)
        fillPath.close()

        fillPaint.color =
            (lineColor and 0x00FFFFFF) or 0x18000000

        canvas.drawPath(
            fillPath,
            fillPaint
        )
    }
}