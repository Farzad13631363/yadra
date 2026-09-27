package com.example.yadra

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class RotatingDashboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class DashboardItem(
        val title: String,
        val iconRes: Int,
        val onClick: () -> Unit
    )

    private val items =
        mutableListOf<DashboardItem>()

    private var selectedIndex = 0
    private var rpm: Int? = null

    // true = Professional / Rotating
    // false = Standard / Fixed
    private var professionalMode = true

    private var downX = 0f
    private var downY = 0f

    private var lastAngle = 0f
    private var rotating = false
    private var accumulatedRotation = 0f

    private val backgroundPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private val smallTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private val gaugePaint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private val needlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private val buttonPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    init {

        setLayerType(
            View.LAYER_TYPE_SOFTWARE,
            null
        )

        textPaint.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

        smallTextPaint.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )
    }

    // ==================================================
    // ITEMS
    // ==================================================

    fun setItems(
        newItems: List<DashboardItem>
    ) {

        items.clear()
        items.addAll(newItems)

        if (selectedIndex >= items.size) {
            selectedIndex = 0
        }

        invalidate()
    }

    // ==================================================
    // ECU CONNECTION STATE
    // ==================================================

    /**
     * وضعیت اتصال ECU معمولی
     *
     * اگر Unknown ECU وصل باشد، ECU معمولی
     * نباید به عنوان ECU فعال در نظر گرفته شود.
     */
    private fun isAnyEcuConnected(): Boolean {

        // ----------------------------------------------
        // UNKNOWN ECU
        // ----------------------------------------------

        if (
            EcuRuntimeState.mode ==
            EcuMode.UNKNOWN &&
            EcuRuntimeState.connected
        ) {

            return false
        }

        // ----------------------------------------------
        // NORMAL ECU
        // ----------------------------------------------

        return when (
            ConnectionSource.activeSource
        ) {

            ConnectionSource.Source.WIFI -> {

                YadraConnectionManager.elmConnected &&
                        YadraConnectionManager.ecuConnected
            }

            ConnectionSource.Source.BLUETOOTH -> {

                BluetoothConnectionManager.elmConnected &&
                        BluetoothConnectionManager.ecuConnected
            }

            ConnectionSource.Source.NONE -> {

                false
            }
        }
    }

    /**
     * تعیین می‌کند هر آیتم صفحه اصلی فعال باشد یا خیر.
     *
     * Unknown ECU وصل باشد:
     * ECU معمولی = غیرفعال
     *
     * ECU معمولی وصل باشد:
     * Unknown ECU = غیرفعال
     */
    private fun isItemEnabled(
        item: DashboardItem
    ): Boolean {

        // ==================================================
        // UNKNOWN ECU CONNECTED
        // ==================================================

        if (
            item.title == "ECU" &&
            EcuRuntimeState.mode ==
            EcuMode.UNKNOWN &&
            EcuRuntimeState.connected
        ) {

            return false
        }

        // ==================================================
        // NORMAL ECU CONNECTED
        // ==================================================

        if (
            item.title == "Unknown ECU"
        ) {

            return !isAnyEcuConnected()
        }

        // ==================================================
        // OTHER ITEMS
        // ==================================================

        return true
    }

    /**
     * بررسی می‌کند آیا Unknown ECU در حال حاضر
     * متصل است یا خیر.
     */
    private fun isUnknownEcuConnected(): Boolean {

        return EcuRuntimeState.mode ==
                EcuMode.UNKNOWN &&
                EcuRuntimeState.connected
    }

    // ==================================================
    // RPM
    // ==================================================

    fun setRpm(
        value: Int?
    ) {

        rpm =
            value?.coerceIn(
                0,
                8000
            )

        invalidate()
    }

    // ==================================================
    // DASHBOARD MODE
    // ==================================================

    fun setProfessionalMode(
        enabled: Boolean
    ) {

        professionalMode =
            enabled

        rotating = false
        accumulatedRotation = 0f

        invalidate()
    }

    // ==================================================
    // DRAW
    // ==================================================

    override fun onDraw(
        canvas: Canvas
    ) {

        super.onDraw(canvas)

        val width =
            width.toFloat()

        val height =
            height.toFloat()

        drawBackground(
            canvas,
            width,
            height
        )

        drawHeader(
            canvas,
            width
        )

        if (professionalMode) {

            drawGauge(
                canvas,
                width,
                height
            )

            drawRotatingMenu(
                canvas,
                width,
                height
            )

        } else {

            drawStandardMenu(
                canvas,
                width,
                height
            )
        }
    }

    // ==================================================
    // BACKGROUND
    // ==================================================

    private fun drawBackground(
        canvas: Canvas,
        width: Float,
        height: Float
    ) {

        val gradient =
            LinearGradient(
                0f,
                0f,
                width,
                height,
                Color.rgb(
                    7,
                    9,
                    13
                ),
                Color.rgb(
                    19,
                    21,
                    27
                ),
                Shader.TileMode.CLAMP
            )

        backgroundPaint.shader =
            gradient

        canvas.drawRect(
            0f,
            0f,
            width,
            height,
            backgroundPaint
        )

        backgroundPaint.shader =
            null

        val centerX =
            width * 0.73f

        val centerY =
            height * 0.78f

        val glow =
            RadialGradient(
                centerX,
                centerY,
                min(
                    width,
                    height
                ) * 0.65f,
                Color.argb(
                    28,
                    255,
                    120,
                    30
                ),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )

        glowPaint.shader =
            glow

        canvas.drawCircle(
            centerX,
            centerY,
            min(
                width,
                height
            ) * 0.65f,
            glowPaint
        )

        glowPaint.shader =
            null
    }

    // ==================================================
    // HEADER
    // ==================================================

    private fun drawHeader(
        canvas: Canvas,
        width: Float
    ) {

        val elmConnected =
            when (
                ConnectionSource.activeSource
            ) {

                ConnectionSource.Source.WIFI ->
                    YadraConnectionManager.elmConnected

                ConnectionSource.Source.BLUETOOTH ->
                    BluetoothConnectionManager.elmConnected

                ConnectionSource.Source.NONE ->
                    false
            }

        val ecuConnected =
            when (
                ConnectionSource.activeSource
            ) {

                ConnectionSource.Source.WIFI ->
                    YadraConnectionManager.elmConnected &&
                            YadraConnectionManager.ecuConnected

                ConnectionSource.Source.BLUETOOTH ->
                    BluetoothConnectionManager.elmConnected &&
                            BluetoothConnectionManager.ecuConnected

                ConnectionSource.Source.NONE ->
                    false
            }

        // ==================================================
        // UNKNOWN ECU HEADER
        // ==================================================

        if (
            isUnknownEcuConnected()
        ) {

            smallTextPaint.color =
                Color.rgb(
                    70,
                    230,
                    100
                )

            smallTextPaint.textSize =
                dp(13f)

            smallTextPaint.textAlign =
                Paint.Align.RIGHT

            canvas.drawText(
                "● UNKNOWN ECU CONNECTED",
                width - dp(20f),
                dp(31f),
                smallTextPaint
            )

            return
        }

        // ==================================================
        // NORMAL ECU HEADER
        // ==================================================

        if (
            elmConnected &&
            ecuConnected
        ) {

            smallTextPaint.color =
                Color.rgb(
                    70,
                    230,
                    100
                )

            smallTextPaint.textSize =
                dp(13f)

            smallTextPaint.textAlign =
                Paint.Align.RIGHT

            canvas.drawText(
                "● CONNECTED",
                width - dp(20f),
                dp(31f),
                smallTextPaint
            )

            return
        }

        // ==================================================
        // ELM / ECU STATUS
        // ==================================================

        smallTextPaint.textSize =
            dp(12f)

        smallTextPaint.textAlign =
            Paint.Align.LEFT

        smallTextPaint.color =
            if (elmConnected) {

                Color.rgb(
                    70,
                    220,
                    100
                )

            } else {

                Color.rgb(
                    220,
                    70,
                    70
                )
            }

        canvas.drawText(
            "● ELM327",
            width - dp(145f),
            dp(27f),
            smallTextPaint
        )

        smallTextPaint.color =
            if (ecuConnected) {

                Color.rgb(
                    70,
                    220,
                    100
                )

            } else {

                Color.rgb(
                    220,
                    70,
                    70
                )
            }

        canvas.drawText(
            "● ECU",
            width - dp(145f),
            dp(47f),
            smallTextPaint
        )
    }

    // ==================================================
    // RPM GAUGE
    // ==================================================

    private fun drawGauge(
        canvas: Canvas,
        width: Float,
        height: Float
    ) {

        val centerX =
            width * 0.73f

        val centerY =
            height * 0.78f

        val radius =
            min(
                width,
                height
            ) * 0.30f

        val rect =
            RectF(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
            )

        gaugePaint.style =
            Paint.Style.STROKE

        gaugePaint.strokeWidth =
            dp(14f)

        gaugePaint.strokeCap =
            Paint.Cap.BUTT

        gaugePaint.color =
            Color.rgb(
                50,
                210,
                100
            )

        canvas.drawArc(
            rect,
            135f,
            75f,
            false,
            gaugePaint
        )

        gaugePaint.color =
            Color.rgb(
                245,
                190,
                50
            )

        canvas.drawArc(
            rect,
            210f,
            75f,
            false,
            gaugePaint
        )

        gaugePaint.color =
            Color.rgb(
                235,
                55,
                55
            )

        canvas.drawArc(
            rect,
            285f,
            30f,
            false,
            gaugePaint
        )

        gaugePaint.style =
            Paint.Style.FILL

        val currentRpm =
            rpm ?: 0

        textPaint.color =
            Color.WHITE

        textPaint.textSize =
            dp(38f)

        textPaint.textAlign =
            Paint.Align.CENTER

        canvas.drawText(
            if (rpm == null) {
                "----"
            } else {
                currentRpm.toString()
            },
            centerX,
            centerY + dp(12f),
            textPaint
        )

        smallTextPaint.color =
            Color.rgb(
                130,
                140,
                150
            )

        smallTextPaint.textSize =
            dp(10f)

        smallTextPaint.textAlign =
            Paint.Align.CENTER

        canvas.drawText(
            "RPM",
            centerX,
            centerY + dp(32f),
            smallTextPaint
        )

        drawRpmLabels(
            canvas,
            centerX,
            centerY,
            radius
        )

        drawNeedle(
            canvas,
            centerX,
            centerY,
            radius
        )
    }

    // ==================================================
    // RPM LABELS
    // ==================================================

    private fun drawRpmLabels(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {

        textPaint.color =
            Color.rgb(
                190,
                195,
                200
            )

        textPaint.textSize =
            dp(11f)

        val labelRadius =
            radius + dp(22f)

        for (
        index in 0..8
        ) {

            val angle =
                Math.toRadians(
                    135.0 +
                            (
                                    270.0 /
                                            8.0
                                    ) *
                            index
                )

            val x =
                centerX +
                        cos(
                            angle
                        ).toFloat() *
                        labelRadius

            val y =
                centerY +
                        sin(
                            angle
                        ).toFloat() *
                        labelRadius

            textPaint.textAlign =
                Paint.Align.CENTER

            canvas.drawText(
                index.toString(),
                x,
                y + dp(4f),
                textPaint
            )
        }
    }

    // ==================================================
    // NEEDLE
    // ==================================================

    private fun drawNeedle(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {

        val currentRpm =
            rpm ?: 0

        val progress =
            currentRpm.toFloat() /
                    8000f

        val needleAngle =
            Math.toRadians(
                135.0 +
                        progress *
                        270.0
            )

        val needleLength =
            radius -
                    dp(10f)

        val needleX =
            centerX +
                    cos(
                        needleAngle
                    ).toFloat() *
                    needleLength

        val needleY =
            centerY +
                    sin(
                        needleAngle
                    ).toFloat() *
                    needleLength

        needlePaint.color =
            Color.rgb(
                70,
                150,
                255
            )

        needlePaint.strokeWidth =
            dp(4f)

        needlePaint.strokeCap =
            Paint.Cap.ROUND

        canvas.drawLine(
            centerX,
            centerY,
            needleX,
            needleY,
            needlePaint
        )

        needlePaint.color =
            Color.WHITE

        canvas.drawCircle(
            centerX,
            centerY,
            dp(7f),
            needlePaint
        )
    }

    // ==================================================
    // PROFESSIONAL / ROTATING MENU
    // ==================================================

    private fun drawRotatingMenu(
        canvas: Canvas,
        width: Float,
        height: Float
    ) {

        if (items.isEmpty()) {
            return
        }

        val centerX =
            width * 0.73f

        val centerY =
            height * 0.78f

        val orbit =
            min(
                width,
                height
            ) * 0.45f

        val angleStep =
            52f

        for (
        slot in -2..2
        ) {

            val index =
                (
                        selectedIndex +
                                slot +
                                items.size
                        ) % items.size

            val item =
                items[index]

            val angleDegrees =
                -90f +
                        slot *
                        angleStep +
                        accumulatedRotation

            val angle =
                Math.toRadians(
                    angleDegrees.toDouble()
                )

            val x =
                centerX +
                        cos(
                            angle
                        ).toFloat() *
                        orbit

            val y =
                centerY +
                        sin(
                            angle
                        ).toFloat() *
                        orbit

            val scale =
                when {

                    slot == 0 ->
                        1.25f

                    abs(slot) == 1 ->
                        1.03f

                    else ->
                        0.88f
                }

            drawMenuItem(
                canvas = canvas,
                item = item,
                x = x,
                y = y,
                scale = scale,
                selected = slot == 0
            )
        }
    }

    // ==================================================
    // STANDARD / FIXED MENU
    // ==================================================

    private fun drawStandardMenu(
        canvas: Canvas,
        width: Float,
        height: Float
    ) {

        if (items.isEmpty()) {
            return
        }

        val columns =
            5

        val rows =
            ceil(
                items.size.toFloat() /
                        columns.toFloat()
            ).toInt()

        val horizontalPadding =
            dp(24f)

        val top =
            dp(115f)

        val bottom =
            height - dp(45f)

        val availableWidth =
            width -
                    horizontalPadding * 2f

        val availableHeight =
            bottom - top

        val columnSpacing =
            availableWidth /
                    columns.toFloat()

        val rowSpacing =
            availableHeight /
                    rows.toFloat()

        for (
        index in items.indices
        ) {

            val row =
                index / columns

            val column =
                index % columns

            val x =
                horizontalPadding +
                        columnSpacing * column +
                        columnSpacing / 2f

            val y =
                top +
                        rowSpacing * row +
                        rowSpacing / 2f

            drawStandardItem(
                canvas = canvas,
                item = items[index],
                x = x,
                y = y
            )
        }
    }

    // ==================================================
    // STANDARD ITEM
    // ==================================================

    private fun drawStandardItem(
        canvas: Canvas,
        item: DashboardItem,
        x: Float,
        y: Float
    ) {

        val enabled =
            isItemEnabled(item)

        val radius =
            dp(43f)

        buttonPaint.style =
            Paint.Style.FILL

        buttonPaint.color =
            if (enabled) {

                Color.rgb(
                    24,
                    27,
                    32
                )

            } else {

                Color.rgb(
                    35,
                    35,
                    38
                )
            }

        canvas.drawCircle(
            x,
            y,
            radius,
            buttonPaint
        )

        buttonPaint.style =
            Paint.Style.STROKE

        buttonPaint.strokeWidth =
            dp(2.5f)

        buttonPaint.color =
            if (enabled) {

                Color.rgb(
                    210,
                    90,
                    30
                )

            } else {

                Color.rgb(
                    85,
                    85,
                    88
                )
            }

        canvas.drawCircle(
            x,
            y,
            radius,
            buttonPaint
        )

        drawVectorIcon(
            canvas = canvas,
            drawableRes = item.iconRes,
            x = x,
            y = y,
            selected = false,
            enabled = enabled
        )

        textPaint.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

        textPaint.color =
            if (enabled) {

                Color.WHITE

            } else {

                Color.rgb(
                    115,
                    115,
                    120
                )
            }

        textPaint.textSize =
            dp(13f)

        textPaint.textAlign =
            Paint.Align.CENTER

        canvas.drawText(
            item.title,
            x,
            y + radius + dp(20f),
            textPaint
        )
    }

    // ==================================================
    // MENU ITEM
    // ==================================================

    private fun drawMenuItem(
        canvas: Canvas,
        item: DashboardItem,
        x: Float,
        y: Float,
        scale: Float,
        selected: Boolean
    ) {

        val enabled =
            isItemEnabled(item)

        val baseRadius =
            dp(43f)

        val radius =
            baseRadius *
                    scale

        if (
            selected &&
            enabled
        ) {

            glowPaint.style =
                Paint.Style.FILL

            glowPaint.color =
                Color.argb(
                    45,
                    255,
                    120,
                    30
                )

            glowPaint.setShadowLayer(
                dp(18f),
                0f,
                0f,
                Color.rgb(
                    255,
                    110,
                    20
                )
            )

            canvas.drawCircle(
                x,
                y,
                radius + dp(5f),
                glowPaint
            )

            glowPaint.clearShadowLayer()
        }

        buttonPaint.style =
            Paint.Style.FILL

        buttonPaint.color =
            if (enabled) {

                Color.rgb(
                    24,
                    27,
                    32
                )

            } else {

                Color.rgb(
                    35,
                    35,
                    38
                )
            }

        canvas.drawCircle(
            x,
            y,
            radius,
            buttonPaint
        )

        buttonPaint.style =
            Paint.Style.STROKE

        buttonPaint.strokeWidth =
            if (
                selected &&
                enabled
            ) {
                dp(3f)
            } else {
                dp(1.5f)
            }

        buttonPaint.color =
            when {

                !enabled ->
                    Color.rgb(
                        85,
                        85,
                        88
                    )

                selected ->
                    Color.rgb(
                        255,
                        125,
                        35
                    )

                else ->
                    Color.rgb(
                        170,
                        80,
                        30
                    )
            }

        canvas.drawCircle(
            x,
            y,
            radius,
            buttonPaint
        )

        drawVectorIcon(
            canvas = canvas,
            drawableRes = item.iconRes,
            x = x,
            y = y,
            selected = selected && enabled,
            enabled = enabled
        )

        if (selected) {

            drawSelectedItemTitle(
                canvas = canvas,
                title = item.title,
                x = x,
                y = y,
                radius = radius,
                enabled = enabled
            )
        }
    }

    // ==================================================
    // SELECTED TITLE
    // ==================================================

    private fun drawSelectedItemTitle(
        canvas: Canvas,
        title: String,
        x: Float,
        y: Float,
        radius: Float,
        enabled: Boolean
    ) {

        textPaint.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

        textPaint.color =
            if (enabled) {

                Color.WHITE

            } else {

                Color.rgb(
                    115,
                    115,
                    120
                )
            }

        textPaint.textSize =
            dp(20f)

        textPaint.textAlign =
            Paint.Align.CENTER

        val fontMetrics =
            textPaint.fontMetrics

        val textHeight =
            fontMetrics.bottom -
                    fontMetrics.top

        val gap =
            dp(12f)

        val rawTextY =
            y -
                    radius -
                    gap -
                    fontMetrics.bottom

        val horizontalPadding =
            dp(18f)

        val textWidth =
            textPaint.measureText(
                title
            )

        val minX =
            horizontalPadding +
                    textWidth / 2f

        val maxX =
            width.toFloat() -
                    horizontalPadding -
                    textWidth / 2f

        val safeX =
            x.coerceIn(
                minX,
                maxX
            )

        val minY =
            textHeight +
                    dp(8f)

        val safeY =
            rawTextY.coerceAtLeast(
                minY
            )

        if (enabled) {

            textPaint.setShadowLayer(
                dp(8f),
                0f,
                0f,
                Color.rgb(
                    255,
                    110,
                    20
                )
            )
        }

        canvas.drawText(
            title,
            safeX,
            safeY,
            textPaint
        )

        textPaint.clearShadowLayer()
    }

    // ==================================================
    // VECTOR ICON
    // ==================================================

    private fun drawVectorIcon(
        canvas: Canvas,
        drawableRes: Int,
        x: Float,
        y: Float,
        selected: Boolean,
        enabled: Boolean
    ) {

        if (drawableRes == 0) {
            return
        }

        val drawable: Drawable =
            ResourcesCompat.getDrawable(
                resources,
                drawableRes,
                context.theme
            ) ?: return

        val iconSize =
            dp(
                if (selected) {
                    52f
                } else {
                    46f
                }
            )

        drawable.setBounds(
            (
                    x -
                            iconSize / 2f
                    ).toInt(),

            (
                    y -
                            iconSize / 2f
                    ).toInt(),

            (
                    x +
                            iconSize / 2f
                    ).toInt(),

            (
                    y +
                            iconSize / 2f
                    ).toInt()
        )

        if (
            selected &&
            enabled
        ) {

            glowPaint.style =
                Paint.Style.FILL

            glowPaint.color =
                Color.argb(
                    90,
                    255,
                    115,
                    30
                )

            glowPaint.setShadowLayer(
                dp(12f),
                0f,
                0f,
                Color.rgb(
                    255,
                    110,
                    20
                )
            )

            canvas.drawCircle(
                x,
                y,
                iconSize * 0.42f,
                glowPaint
            )

            glowPaint.clearShadowLayer()
        }

        drawable.alpha =
            when {

                !enabled ->
                    70

                selected ->
                    255

                else ->
                    235
            }

        drawable.draw(canvas)
    }

    // ==================================================
    // TOUCH
    // ==================================================

    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {

        if (!professionalMode) {

            if (
                event.actionMasked ==
                MotionEvent.ACTION_UP
            ) {

                val clickedIndex =
                    findStandardItem(
                        event.x,
                        event.y
                    )

                if (
                    clickedIndex >= 0 &&
                    clickedIndex < items.size
                ) {

                    val item =
                        items[clickedIndex]

                    if (
                        isItemEnabled(item)
                    ) {

                        item.onClick()
                    }
                }
            }

            return true
        }

        val width =
            width.toFloat()

        val height =
            height.toFloat()

        val centerX =
            width * 0.73f

        val centerY =
            height * 0.78f

        val orbit =
            min(
                width,
                height
            ) * 0.45f

        when (
            event.actionMasked
        ) {

            MotionEvent.ACTION_DOWN -> {

                downX =
                    event.x

                downY =
                    event.y

                lastAngle =
                    touchAngle(
                        event.x,
                        event.y,
                        centerX,
                        centerY
                    )

                rotating =
                    false

                return true
            }

            MotionEvent.ACTION_MOVE -> {

                val currentAngle =
                    touchAngle(
                        event.x,
                        event.y,
                        centerX,
                        centerY
                    )

                val distance =
                    distanceFromCenter(
                        event.x,
                        event.y,
                        centerX,
                        centerY
                    )

                if (
                    abs(
                        distance - orbit
                    ) < dp(85f)
                ) {

                    var delta =
                        currentAngle -
                                lastAngle

                    if (delta > 180f) {
                        delta -= 360f
                    }

                    if (delta < -180f) {
                        delta += 360f
                    }

                    if (
                        abs(delta) > 0.3f
                    ) {

                        rotating =
                            true

                        accumulatedRotation +=
                            delta

                        while (
                            accumulatedRotation >= 52f
                        ) {

                            accumulatedRotation -=
                                52f

                            selectPrevious()
                        }

                        while (
                            accumulatedRotation <= -52f
                        ) {

                            accumulatedRotation +=
                                52f

                            selectNext()
                        }

                        invalidate()
                    }
                }

                lastAngle =
                    currentAngle

                return true
            }

            MotionEvent.ACTION_UP -> {

                val deltaX =
                    event.x -
                            downX

                val deltaY =
                    event.y -
                            downY

                if (!rotating) {

                    if (
                        abs(deltaY) > dp(35f) &&
                        abs(deltaY) > abs(deltaX)
                    ) {

                        if (deltaY < 0) {
                            selectNext()
                        } else {
                            selectPrevious()
                        }

                        return true
                    }

                    if (
                        abs(deltaX) < dp(20f) &&
                        abs(deltaY) < dp(20f)
                    ) {

                        val distance =
                            distanceFromCenter(
                                event.x,
                                event.y,
                                centerX,
                                centerY
                            )

                        if (
                            abs(
                                distance - orbit
                            ) < dp(65f)
                        ) {

                            if (
                                items.isNotEmpty()
                            ) {

                                val item =
                                    items[selectedIndex]

                                // ==========================================
                                // IMPORTANT:
                                // اگر Unknown ECU وصل باشد و آیتم ECU انتخاب
                                // شده باشد، اینجا هم کلیک مسدود می‌شود.
                                // ==========================================

                                if (
                                    isItemEnabled(item)
                                ) {

                                    item.onClick()
                                }
                            }
                        }
                    }
                }

                rotating =
                    false

                return true
            }
        }

        return true
    }

    // ==================================================
    // STANDARD ITEM FINDER
    // ==================================================

    private fun findStandardItem(
        x: Float,
        y: Float
    ): Int {

        if (items.isEmpty()) {
            return -1
        }

        val columns =
            5

        val rows =
            ceil(
                items.size.toFloat() /
                        columns.toFloat()
            ).toInt()

        val horizontalPadding =
            dp(24f)

        val top =
            dp(115f)

        val bottom =
            height.toFloat() -
                    dp(45f)

        val availableWidth =
            width.toFloat() -
                    horizontalPadding * 2f

        val availableHeight =
            bottom - top

        val columnSpacing =
            availableWidth /
                    columns.toFloat()

        val rowSpacing =
            availableHeight /
                    rows.toFloat()

        for (
        index in items.indices
        ) {

            val row =
                index / columns

            val column =
                index % columns

            val itemX =
                horizontalPadding +
                        columnSpacing * column +
                        columnSpacing / 2f

            val itemY =
                top +
                        rowSpacing * row +
                        rowSpacing / 2f

            val dx =
                x - itemX

            val dy =
                y - itemY

            val distance =
                sqrt(
                    dx * dx +
                            dy * dy
                )

            if (
                distance <= dp(55f)
            ) {

                return index
            }
        }

        return -1
    }

    // ==================================================
    // ANGLE
    // ==================================================

    private fun touchAngle(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float
    ): Float {

        return Math.toDegrees(
            atan2(
                (
                        y - centerY
                        ).toDouble(),

                (
                        x - centerX
                        ).toDouble()
            )
        ).toFloat()
    }

    // ==================================================
    // DISTANCE
    // ==================================================

    private fun distanceFromCenter(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float
    ): Float {

        return sqrt(
            (x - centerX) *
                    (x - centerX) +

                    (y - centerY) *
                    (y - centerY)
        )
    }

    // ==================================================
    // NEXT
    // ==================================================

    private fun selectNext() {

        if (items.isEmpty()) {
            return
        }

        selectedIndex++

        if (
            selectedIndex >=
            items.size
        ) {

            selectedIndex =
                0
        }

        invalidate()
    }

    // ==================================================
    // PREVIOUS
    // ==================================================

    private fun selectPrevious() {

        if (items.isEmpty()) {
            return
        }

        selectedIndex--

        if (selectedIndex < 0) {

            selectedIndex =
                items.size - 1
        }

        invalidate()
    }

    // ==================================================
    // ICON MAP
    // ==================================================

    companion object {

        fun iconFor(
            title: String
        ): Int {

            return when (title) {

                "Connection" ->
                    R.drawable.ic_connection

                "Warning" ->
                    R.drawable.ic_warning_lights

                "Sensors" ->
                    R.drawable.ic_sensors

                "Errors" ->
                    R.drawable.ic_error_codes

                "ECU" ->
                    R.drawable.ic_ecu_chip

                "Actuators" ->
                    R.drawable.ic_service

                "AI" ->
                    R.drawable.ic_ai

                "Settings" ->
                    R.drawable.ic_settings

                "Trip" ->
                    R.drawable.ic_trip

                "Charts" ->
                    R.drawable.ic_charts

                "Profile" ->
                    R.drawable.ic_vehicle_profile

                "Support" ->
                    R.drawable.ic_support

                "KWP TEST" ->
                    R.drawable.ic_service

                else ->
                    0
            }
        }
    }

    // ==================================================
    // DP
    // ==================================================

    private fun dp(
        value: Float
    ): Float {

        return value *
                resources.displayMetrics.density
    }
}