package com.example.yadra

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ChartsActivity : AppCompatActivity() {

    // ------------------------------------------------------------
    // صفحه انتخاب سنسورها
    // ------------------------------------------------------------

    private lateinit var layoutSensorSelection: View
    private lateinit var layoutSensorDisplay: View

    private lateinit var sensorSelectionContainer: LinearLayout

    private lateinit var txtSensorCount: TextView
    private lateinit var txtSensorStatus: TextView

    private lateinit var btnContinueSensors: Button
    private lateinit var btnChangeSensors: Button

    // ------------------------------------------------------------
    // اتصال
    // ------------------------------------------------------------

    private lateinit var txtConnection: TextView

    // ------------------------------------------------------------
    // حالت Chart / Gauge
    // ------------------------------------------------------------

    private lateinit var btnChartMode: Button
    private lateinit var btnGaugeMode: Button

    private lateinit var layoutChartMode: View
    private lateinit var layoutGaugeMode: View

    // ------------------------------------------------------------
    // Chart
    // ------------------------------------------------------------

    private lateinit var txtChartPage: TextView
    private lateinit var chartContainer: LinearLayout

    // ------------------------------------------------------------
    // Gauge
    // ------------------------------------------------------------

    private lateinit var txtGaugePage: TextView
    private lateinit var gaugeContainer: LinearLayout

    private lateinit var btnGaugePrevious: Button
    private lateinit var btnGaugeNext: Button

    // ------------------------------------------------------------
    // سنسورها
    // ------------------------------------------------------------

    private var availableSensors =
        emptyList<ObdSensor>()

    private val selectedSensors =
        LinkedHashMap<String, ObdSensor>()

    private val checkBoxes =
        LinkedHashMap<String, CheckBox>()

    private val chartViews =
        LinkedHashMap<String, LiveChartView>()

    private val chartValueViews =
        LinkedHashMap<String, TextView>()

    private val gaugeViews =
        LinkedHashMap<String, GaugeView>()

    private var checkAllBox: CheckBox? = null

    private var updatingAllSelection = false

    // ------------------------------------------------------------
    // صفحه Gauge
    // ------------------------------------------------------------

    private var gaugePage = 0

    // ------------------------------------------------------------
    // بروزرسانی چرخشی سنسورها
    // ------------------------------------------------------------

    private var sensorUpdateIndex = 0

    private val sensorsPerUpdate = 5

    // ------------------------------------------------------------
    // Handler
    // ------------------------------------------------------------

    private val handler =
        Handler(Looper.getMainLooper())

    // ------------------------------------------------------------
    // ECU Executor
    // ------------------------------------------------------------

    private val chartExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    // ------------------------------------------------------------
    // بروزرسانی زنده
    // ------------------------------------------------------------

    private val updateRunnable =
        object : Runnable {

            override fun run() {

                if (
                    selectedSensors.isNotEmpty() &&
                    layoutSensorDisplay.visibility ==
                    View.VISIBLE
                ) {

                    chartExecutor.execute {
                        updateSelectedSensors()
                    }
                }

                handler.postDelayed(
                    this,
                    1000L
                )
            }
        }

    // ============================================================
    // onCreate
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_charts
        )

        initializeViews()

        setupModeSelector()

        setupGaugeNavigation()

        setupSensorNavigation()

        detectSensors()
    }

    // ============================================================
    // Initialize Views
    // ============================================================

    private fun initializeViews() {

        layoutSensorSelection =
            findViewById(
                R.id.layoutSensorSelection
            )

        layoutSensorDisplay =
            findViewById(
                R.id.layoutSensorDisplay
            )

        sensorSelectionContainer =
            findViewById(
                R.id.chartSensorSelectionContainer
            )

        txtSensorCount =
            findViewById(
                R.id.txtChartSensorCount
            )

        txtSensorStatus =
            findViewById(
                R.id.txtChartSensorStatus
            )

        btnContinueSensors =
            findViewById(
                R.id.btnContinueSensors
            )

        btnChangeSensors =
            findViewById(
                R.id.btnChangeSensors
            )

        txtConnection =
            findViewById(
                R.id.txtChartsConnection
            )

        btnChartMode =
            findViewById(
                R.id.btnChartMode
            )

        btnGaugeMode =
            findViewById(
                R.id.btnGaugeMode
            )

        layoutChartMode =
            findViewById(
                R.id.layoutChartMode
            )

        layoutGaugeMode =
            findViewById(
                R.id.layoutGaugeMode
            )

        txtChartPage =
            findViewById(
                R.id.txtChartPage
            )

        chartContainer =
            findViewById(
                R.id.chartContainer
            )

        txtGaugePage =
            findViewById(
                R.id.txtGaugePage
            )

        gaugeContainer =
            findViewById(
                R.id.gaugeContainer
            )

        btnGaugePrevious =
            findViewById(
                R.id.btnGaugePrevious
            )

        btnGaugeNext =
            findViewById(
                R.id.btnGaugeNext
            )
    }

    // ============================================================
    // Sensor Navigation
    // ============================================================

    private fun setupSensorNavigation() {

        btnContinueSensors.setOnClickListener {

            if (
                selectedSensors.isEmpty()
            ) {

                showSensorStatus(
                    "⚠️ حداقل یک سنسور انتخاب کنید"
                )

                return@setOnClickListener
            }

            layoutSensorSelection.visibility =
                View.GONE

            layoutSensorDisplay.visibility =
                View.VISIBLE

            gaugePage = 0

            sensorUpdateIndex = 0

            rebuildCharts()

            rebuildGaugePage()

            showChartMode()

            handler.removeCallbacks(
                updateRunnable
            )

            handler.post(
                updateRunnable
            )
        }

        btnChangeSensors.setOnClickListener {

            layoutSensorDisplay.visibility =
                View.GONE

            layoutSensorSelection.visibility =
                View.VISIBLE

            handler.removeCallbacks(
                updateRunnable
            )
        }
    }

    // ============================================================
    // Mode Selector
    // ============================================================

    private fun setupModeSelector() {

        btnChartMode.setOnClickListener {
            showChartMode()
        }

        btnGaugeMode.setOnClickListener {
            showGaugeMode()
        }

        showChartMode()
    }

    private fun showChartMode() {

        layoutChartMode.visibility =
            View.VISIBLE

        layoutGaugeMode.visibility =
            View.GONE

        btnChartMode.setBackgroundColor(
            Color.rgb(
                230,
                100,
                20
            )
        )

        btnChartMode.setTextColor(
            Color.WHITE
        )

        btnGaugeMode.setBackgroundColor(
            Color.rgb(
                38,
                50,
                56
            )
        )

        btnGaugeMode.setTextColor(
            Color.rgb(
                144,
                164,
                174
            )
        )
    }

    private fun showGaugeMode() {

        layoutChartMode.visibility =
            View.GONE

        layoutGaugeMode.visibility =
            View.VISIBLE

        btnGaugeMode.setBackgroundColor(
            Color.rgb(
                230,
                100,
                20
            )
        )

        btnGaugeMode.setTextColor(
            Color.WHITE
        )

        btnChartMode.setBackgroundColor(
            Color.rgb(
                38,
                50,
                56
            )
        )

        btnChartMode.setTextColor(
            Color.rgb(
                144,
                164,
                174
            )
        )

        rebuildGaugePage()
    }

    // ============================================================
    // Gauge Navigation
    // ============================================================

    private fun setupGaugeNavigation() {

        btnGaugePrevious.setOnClickListener {

            if (
                gaugePage > 0
            ) {

                gaugePage--

                rebuildGaugePage()
            }
        }

        btnGaugeNext.setOnClickListener {

            val pageCount =
                getGaugePageCount()

            if (
                gaugePage <
                pageCount - 1
            ) {

                gaugePage++

                rebuildGaugePage()
            }
        }

        updateGaugeNavigation()
    }

    // ============================================================
    // Detect Sensors
    // ============================================================

    private fun detectSensors() {

        txtSensorStatus.text =
            "⏳ لطفاً منتظر بمانید، در حال شناسایی سنسورهای ECU..."

        txtSensorCount.text =
            "انتخاب شده: 0"

        handler.removeCallbacks(
            updateRunnable
        )

        chartExecutor.execute {

            try {

                if (
                    ConnectionSource.activeSource ==
                    ConnectionSource.Source.NONE
                ) {

                    showSensorStatus(
                        "🔴 اتصال ELM327 فعال نیست"
                    )

                    return@execute
                }

                if (
                    !isElmConnected()
                ) {

                    showSensorStatus(
                        "🔴 ELM327 متصل نیست"
                    )

                    return@execute
                }

                val supportedPids =
                    ObdPidSupport.getSupportedPids()

                if (
                    supportedPids == null
                ) {

                    showSensorStatus(
                        "🟡 دریافت PIDهای ECU ناموفق بود"
                    )

                    return@execute
                }

                val detectedSensors =
                    ObdSensorCatalog.sensors
                        .filter { sensor ->

                            supportedPids.contains(
                                sensor.pid
                            )
                        }
                        .distinctBy { sensor ->

                            sensor.id
                        }
                        .toMutableList()

                // ------------------------------------------------
                // Battery Voltage
                // ------------------------------------------------

                val batteryVoltageSensor =
                    ObdSensorCatalog.sensors
                        .firstOrNull { sensor ->

                            sensor.id.equals(
                                "battery_voltage",
                                ignoreCase = true
                            )
                        }

                if (
                    batteryVoltageSensor != null &&
                    detectedSensors.none { sensor ->

                        sensor.id.equals(
                            "battery_voltage",
                            ignoreCase = true
                        )
                    }
                ) {

                    detectedSensors.add(
                        batteryVoltageSensor
                    )
                }

                availableSensors =
                    detectedSensors

                runOnUiThread {

                    buildSensorSelection()

                    txtSensorStatus.text =
                        "🟢 ${availableSensors.size} سنسور قابل استفاده پیدا شد"

                    updateConnectionStatus()
                }

            } catch (
                e: Exception
            ) {

                showSensorStatus(
                    "❌ خطا در شناسایی سنسورها: " +
                            (
                                    e.message
                                        ?: "Unknown error"
                                    )
                )
            }
        }
    }

    // ============================================================
    // Build Sensor Selection
    // ============================================================

    private fun buildSensorSelection() {

        sensorSelectionContainer.removeAllViews()

        checkBoxes.clear()

        checkAllBox = null

        val allBox =
            CheckBox(this)

        checkAllBox =
            allBox

        allBox.text =
            "همه"

        allBox.textSize =
            17f

        allBox.setTextColor(
            Color.rgb(
                255,
                152,
                0
            )
        )

        allBox.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        allBox.buttonTintList =
            ColorStateList.valueOf(
                Color.rgb(
                    230,
                    100,
                    20
                )
            )

        allBox.setPadding(
            dp(4),
            dp(8),
            dp(4),
            dp(8)
        )

        allBox.setOnCheckedChangeListener {
                _,
                isChecked ->

            if (
                updatingAllSelection
            ) {
                return@setOnCheckedChangeListener
            }

            updatingAllSelection =
                true

            try {

                if (
                    isChecked
                ) {

                    selectedSensors.clear()

                    for (
                    sensor in availableSensors
                    ) {

                        selectedSensors[
                            sensor.id
                        ] = sensor
                    }

                    for (
                    sensor in availableSensors
                    ) {

                        checkBoxes[
                            sensor.id
                        ]?.isChecked =
                            true
                    }

                } else {

                    selectedSensors.clear()

                    for (
                    checkBox in checkBoxes.values
                    ) {

                        checkBox.isChecked =
                            false
                    }
                }

                gaugePage = 0

                sensorUpdateIndex = 0

                updateSelectedSensorCount()

                if (
                    layoutSensorDisplay.visibility ==
                    View.VISIBLE
                ) {

                    rebuildCharts()

                    rebuildGaugePage()
                }

            } finally {

                updatingAllSelection =
                    false
            }
        }

        sensorSelectionContainer.addView(
            allBox
        )

        val divider =
            View(this)

        divider.setBackgroundColor(
            Color.rgb(
                55,
                65,
                72
            )
        )

        divider.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {

                topMargin =
                    dp(4)

                bottomMargin =
                    dp(6)
            }

        sensorSelectionContainer.addView(
            divider
        )

        for (
        sensor in availableSensors
        ) {

            val checkBox =
                CheckBox(this)

            checkBox.text =
                buildString {

                    append(
                        sensor.name
                    )

                    if (
                        sensor.unit.isNotBlank()
                    ) {

                        append(
                            "  •  "
                        )

                        append(
                            sensor.unit
                        )
                    }

                    append(
                        "   ["
                    )

                    append(
                        sensor.pid
                    )

                    append(
                        "]"
                    )
                }

            checkBox.textSize =
                15f

            checkBox.setTextColor(
                Color.WHITE
            )

            checkBox.buttonTintList =
                ColorStateList.valueOf(
                    Color.rgb(
                        230,
                        100,
                        20
                    )
                )

            checkBox.setPadding(
                dp(4),
                dp(5),
                dp(4),
                dp(5)
            )

            checkBox.setOnCheckedChangeListener(
                createSensorCheckListener(
                    sensor,
                    checkBox
                )
            )

            checkBoxes[
                sensor.id
            ] = checkBox

            sensorSelectionContainer.addView(
                checkBox
            )
        }

        updateAllCheckState()

        updateSelectedSensorCount()
    }

    // ============================================================
    // Listener برای Checkbox سنسور
    // ============================================================

    private fun createSensorCheckListener(
        sensor: ObdSensor,
        checkBox: CheckBox
    ): android.widget.CompoundButton.OnCheckedChangeListener {

        return android.widget.CompoundButton.OnCheckedChangeListener {
                _,
                isChecked ->

            if (
                updatingAllSelection
            ) {
                return@OnCheckedChangeListener
            }

            if (
                isChecked
            ) {

                selectedSensors[
                    sensor.id
                ] = sensor

            } else {

                selectedSensors.remove(
                    sensor.id
                )
            }

            updateAllCheckState()

            gaugePage = 0

            sensorUpdateIndex = 0

            updateSelectedSensorCount()

            if (
                layoutSensorDisplay.visibility ==
                View.VISIBLE
            ) {

                rebuildCharts()

                rebuildGaugePage()
            }
        }
    }

    // ============================================================
    // وضعیت Checkbox «همه»
    // ============================================================

    private fun updateAllCheckState() {

        val all =
            checkAllBox
                ?: return

        if (
            availableSensors.isEmpty()
        ) {

            all.isChecked =
                false

            return
        }

        val selectedCount =
            selectedSensors.size

        updatingAllSelection =
            true

        all.isChecked =
            selectedCount >= availableSensors.size &&
                    availableSensors.isNotEmpty()

        updatingAllSelection =
            false
    }

    // ============================================================
    // Selected Count
    // ============================================================

    private fun updateSelectedSensorCount() {

        txtSensorCount.text =
            "انتخاب شده: ${selectedSensors.size} سنسور"
    }

    // ============================================================
    // Rebuild Charts
    // ============================================================

    private fun rebuildCharts() {

        chartContainer.removeAllViews()

        chartViews.clear()

        chartValueViews.clear()

        if (
            selectedSensors.isEmpty()
        ) {

            txtChartPage.text =
                "هیچ سنسوری انتخاب نشده"

            return
        }

        txtChartPage.text =
            "${selectedSensors.size} نمودار زنده"

        for (
        sensor in selectedSensors.values
        ) {

            val card =
                createChartCard(
                    sensor
                )

            chartContainer.addView(
                card
            )
        }
    }

    // ============================================================
    // Create Chart Card
    // ============================================================

    private fun createChartCard(
        sensor: ObdSensor
    ): CardView {

        val card =
            CardView(this)

        card.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(230)
            ).apply {

                bottomMargin =
                    dp(12)
            }

        card.radius =
            dp(18).toFloat()

        card.cardElevation =
            dp(6).toFloat()

        card.setCardBackgroundColor(
            Color.rgb(
                23,
                29,
                36
            )
        )

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            dp(14),
            dp(14),
            dp(14),
            dp(14)
        )

        val header =
            LinearLayout(this)

        header.orientation =
            LinearLayout.HORIZONTAL

        header.gravity =
            Gravity.CENTER_VERTICAL

        val title =
            TextView(this)

        title.layoutParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        title.text =
            sensor.name

        title.textSize =
            17f

        title.setTextColor(
            Color.WHITE
        )

        title.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val value =
            TextView(this)

        value.text =
            "—"

        value.textSize =
            18f

        value.setTextColor(
            Color.rgb(
                230,
                100,
                20
            )
        )

        value.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        header.addView(
            title
        )

        header.addView(
            value
        )

        val chart =
            LiveChartView(this)

        chart.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {

                topMargin =
                    dp(8)
            }

        chart.setLineColor(
            Color.rgb(
                230,
                100,
                20
            )
        )

        layout.addView(
            header
        )

        layout.addView(
            chart
        )

        card.addView(
            layout
        )

        chartViews[
            sensor.id
        ] = chart

        chartValueViews[
            sensor.id
        ] = value

        return card
    }

    // ============================================================
    // Rebuild Gauge Page
    // ============================================================

    private fun rebuildGaugePage() {

        gaugeContainer.removeAllViews()

        gaugeViews.clear()

        val sensors =
            selectedSensors.values.toList()

        val pageCount =
            getGaugePageCount()

        if (
            pageCount == 0
        ) {

            txtGaugePage.text =
                "هیچ سنسوری انتخاب نشده"

            updateGaugeNavigation()

            return
        }

        if (
            gaugePage >= pageCount
        ) {

            gaugePage =
                pageCount - 1
        }

        val start =
            gaugePage * 6

        val end =
            minOf(
                start + 6,
                sensors.size
            )

        val pageSensors =
            sensors.subList(
                start,
                end
            )

        for (
        sensor in pageSensors
        ) {

            val card =
                createGaugeCard(
                    sensor
                )

            gaugeContainer.addView(
                card
            )
        }

        txtGaugePage.text =
            "گیج‌های ${start + 1} تا $end"

        updateGaugeNavigation()
    }

    // ============================================================
    // Create Gauge Card
    // ============================================================

    private fun createGaugeCard(
        sensor: ObdSensor
    ): CardView {

        val card =
            CardView(this)

        card.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(320)
            ).apply {

                bottomMargin =
                    dp(14)
            }

        card.radius =
            dp(20).toFloat()

        card.cardElevation =
            dp(7).toFloat()

        card.setCardBackgroundColor(
            Color.rgb(
                23,
                29,
                36
            )
        )

        val gauge =
            GaugeView(this)

        gauge.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

        gauge.setTitle(
            sensor.name.uppercase(
                Locale.US
            )
        )

        gauge.setUnit(
            sensor.unit
        )

        gauge.setMaximum(
            getGaugeMaximum(
                sensor
            )
        )

        gauge.setValue(
            0f
        )

        card.addView(
            gauge
        )

        gaugeViews[
            sensor.id
        ] = gauge

        return card
    }

    // ============================================================
    // Gauge Maximum
    // ============================================================

    private fun getGaugeMaximum(
        sensor: ObdSensor
    ): Float {

        return if (
            sensor.maximum >
            sensor.minimum
        ) {

            sensor.maximum

        } else {

            100f
        }
    }

    // ============================================================
    // Gauge Page Count
    // ============================================================

    private fun getGaugePageCount(): Int {

        if (
            selectedSensors.isEmpty()
        ) {
            return 0
        }

        return (
                selectedSensors.size + 5
                ) / 6
    }

    // ============================================================
    // Gauge Navigation State
    // ============================================================

    private fun updateGaugeNavigation() {

        val pageCount =
            getGaugePageCount()

        btnGaugePrevious.isEnabled =
            gaugePage > 0

        btnGaugeNext.isEnabled =
            gaugePage <
                    pageCount - 1

        btnGaugePrevious.alpha =
            if (
                btnGaugePrevious.isEnabled
            ) {
                1f
            } else {
                0.4f
            }

        btnGaugeNext.alpha =
            if (
                btnGaugeNext.isEnabled
            ) {
                1f
            } else {
                0.4f
            }
    }

    // ============================================================
    // Update Selected Sensors
    // ============================================================

    private fun updateSelectedSensors() {

        if (
            selectedSensors.isEmpty()
        ) {
            return
        }

        try {

            val sensors =
                selectedSensors.values.toList()

            if (
                sensors.isEmpty()
            ) {
                return
            }

            if (
                sensorUpdateIndex >= sensors.size
            ) {

                sensorUpdateIndex = 0
            }

            val startIndex =
                sensorUpdateIndex

            val endIndex =
                minOf(
                    startIndex + sensorsPerUpdate,
                    sensors.size
                )

            val sensorsToRead =
                sensors.subList(
                    startIndex,
                    endIndex
                )

            sensorUpdateIndex =
                if (
                    endIndex >= sensors.size
                ) {
                    0
                } else {
                    endIndex
                }

            for (
            sensor in sensorsToRead
            ) {

                val value =
                    readSensorValue(
                        sensor
                    )

                runOnUiThread {

                    updateSensorUI(
                        sensor,
                        value
                    )
                }
            }

            runOnUiThread {

                updateConnectionStatus()
            }

        } catch (
            _: Exception
        ) {

            runOnUiThread {

                updateConnectionStatus()
            }
        }
    }

    // ============================================================
    // Read Sensor
    // ============================================================

    private fun readSensorValue(
        sensor: ObdSensor
    ): Float? {

        /*
         * Battery voltage is still handled directly
         * through AT RV exactly as before.
         *
         * It is intentionally NOT routed through
         * the normal PID reader.
         */
        if (
            sensor.id.equals(
                "battery_voltage",
                ignoreCase = true
            )
        ) {

            return readBatteryVoltage()
        }

        return try {

            /*
             * Common ECU data path:
             *
             * STANDARD
             *      ↓
             * EcuDataGateway
             *      ↓
             * ObdSensorReader
             *
             * UNKNOWN
             *      ↓
             * EcuDataGateway
             *      ↓
             * Special ECU reader later
             *
             * No unknown ECU command is guessed here.
             */
            EcuDataGateway.readSensor(
                sensor
            )

        } catch (
            _: Exception
        ) {

            null
        }
    }

    // ============================================================
    // Battery Voltage
    // ============================================================

    private fun readBatteryVoltage(): Float? {

        val response =
            when (
                ConnectionSource.activeSource
            ) {

                ConnectionSource.Source.WIFI -> {

                    if (
                        !YadraConnectionManager.elmConnected
                    ) {
                        return null
                    }

                    YadraConnectionManager
                        .sendCommand(
                            "AT RV"
                        )
                }

                ConnectionSource.Source.BLUETOOTH -> {

                    if (
                        !BluetoothConnectionManager.elmConnected
                    ) {
                        return null
                    }

                    BluetoothConnectionManager
                        .sendActiveCommand(
                            "AT RV"
                        )
                }

                ConnectionSource.Source.NONE -> {
                    return null
                }
            }

        return parseBatteryVoltage(
            response
        )
    }

    // ============================================================
    // Parse Battery Voltage
    // ============================================================

    private fun parseBatteryVoltage(
        response: String
    ): Float? {

        if (
            response.isBlank()
        ) {
            return null
        }

        val upper =
            response.uppercase(
                Locale.US
            )

        if (
            upper.contains("ERROR") ||
            upper.contains("NO DATA") ||
            upper.contains("NODATA") ||
            upper.contains("?") ||
            upper.contains("UNABLE TO CONNECT")
        ) {
            return null
        }

        val voltageRegex =
            Regex(
                """(\d+(?:\.\d+)?)\s*V""",
                RegexOption.IGNORE_CASE
            )

        val match =
            voltageRegex.find(
                upper
            )

        if (
            match != null
        ) {

            return match
                .groupValues[1]
                .toFloatOrNull()
        }

        val cleaned =
            upper
                .replace(
                    ">",
                    " "
                )
                .replace(
                    "\r",
                    " "
                )
                .replace(
                    "\n",
                    " "
                )
                .trim()

        val fallbackRegex =
            Regex(
                """\b(\d{1,2}(?:\.\d{1,3})?)\b"""
            )

        val fallback =
            fallbackRegex.find(
                cleaned
            )

        if (
            fallback != null
        ) {

            val value =
                fallback
                    .groupValues[1]
                    .toFloatOrNull()

            if (
                value != null &&
                value >= 5f &&
                value <= 20f
            ) {

                return value
            }
        }

        return null
    }

    // ============================================================
    // Update Sensor UI
    // ============================================================

    private fun updateSensorUI(
        sensor: ObdSensor,
        value: Float?
    ) {

        val valueText =
            formatSensorValue(
                value,
                sensor.unit
            )

        chartValueViews[
            sensor.id
        ]?.text =
            valueText

        if (
            value != null
        ) {

            chartViews[
                sensor.id
            ]?.addValue(
                value
            )

            gaugeViews[
                sensor.id
            ]?.setValue(
                value
            )
        }
    }

    // ============================================================
    // Format Value
    // ============================================================

    private fun formatSensorValue(
        value: Float?,
        unit: String
    ): String {

        if (
            value == null
        ) {
            return "—"
        }

        val number =
            if (
                value % 1f == 0f
            ) {

                value.toInt()
                    .toString()

            } else {

                String.format(
                    Locale.US,
                    "%.2f",
                    value
                )
            }

        return if (
            unit.isBlank()
        ) {

            number

        } else {

            "$number $unit"
        }
    }

    // ============================================================
    // Connection Status
    // ============================================================

    private fun updateConnectionStatus() {

        when (
            ConnectionSource.activeSource
        ) {

            ConnectionSource.Source.WIFI -> {

                if (
                    YadraConnectionManager.elmConnected
                ) {

                    txtConnection.text =
                        "● Wi-Fi / ECU Connected"

                    txtConnection.setTextColor(
                        Color.rgb(
                            0,
                            230,
                            118
                        )
                    )

                } else {

                    txtConnection.text =
                        "● Wi-Fi / ECU Disconnected"

                    txtConnection.setTextColor(
                        Color.rgb(
                            255,
                            82,
                            82
                        )
                    )
                }
            }

            ConnectionSource.Source.BLUETOOTH -> {

                if (
                    BluetoothConnectionManager.elmConnected
                ) {

                    txtConnection.text =
                        "● Bluetooth / ECU Connected"

                    txtConnection.setTextColor(
                        Color.rgb(
                            0,
                            230,
                            118
                        )
                    )

                } else {

                    txtConnection.text =
                        "● Bluetooth / ECU Disconnected"

                    txtConnection.setTextColor(
                        Color.rgb(
                            255,
                            82,
                            82
                        )
                    )
                }
            }

            ConnectionSource.Source.NONE -> {

                txtConnection.text =
                    "● ECU Disconnected"

                txtConnection.setTextColor(
                    Color.rgb(
                        255,
                        82,
                        82
                    )
                )
            }
        }
    }

    // ============================================================
    // Status
    // ============================================================

    private fun showSensorStatus(
        status: String
    ) {

        runOnUiThread {

            txtSensorStatus.text =
                status
        }
    }

    // ============================================================
    // Check ELM
    // ============================================================

    private fun isElmConnected(): Boolean {

        return when (
            ConnectionSource.activeSource
        ) {

            ConnectionSource.Source.WIFI ->
                YadraConnectionManager.elmConnected

            ConnectionSource.Source.BLUETOOTH ->
                BluetoothConnectionManager.elmConnected

            ConnectionSource.Source.NONE ->
                false
        }
    }

    // ============================================================
    // DP
    // ============================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources
                            .displayMetrics
                            .density
                ).toInt()
    }

    // ============================================================
    // Lifecycle
    // ============================================================

    override fun onResume() {

        super.onResume()

        updateConnectionStatus()

        handler.removeCallbacks(
            updateRunnable
        )

        if (
            layoutSensorDisplay.visibility ==
            View.VISIBLE
        ) {

            handler.post(
                updateRunnable
            )
        }
    }

    override fun onPause() {

        super.onPause()

        handler.removeCallbacks(
            updateRunnable
        )
    }

    override fun onDestroy() {

        super.onDestroy()

        handler.removeCallbacks(
            updateRunnable
        )

        chartExecutor.shutdownNow()
    }
}