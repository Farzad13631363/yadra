package com.example.yadra

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class SensorsActivity : AppCompatActivity() {

    private lateinit var txtSensorStatus: TextView
    private lateinit var txtSelectedSensorCount: TextView
    private lateinit var sensorContainer: LinearLayout
    private lateinit var btnStartSensorTest: Button

    @Volatile
    private var testRunning = false

    private val sensorValueViews =
        LinkedHashMap<String, TextView>()

    private var availableSensors =
        emptyList<ObdSensor>()

    private var testThread: Thread? = null

    private companion object {
        const val TEST_INTERVAL_MS = 1000L
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_sensors
        )

        txtSensorStatus =
            findViewById(
                R.id.txtSensorStatus
            )

        txtSelectedSensorCount =
            findViewById(
                R.id.txtSelectedSensorCount
            )

        sensorContainer =
            findViewById(
                R.id.sensorContainer
            )

        btnStartSensorTest =
            findViewById(
                R.id.btnStartSensorTest
            )

        txtSelectedSensorCount.visibility =
            TextView.GONE

        btnStartSensorTest.visibility =
            Button.GONE

        detectSensors()
    }

    override fun onDestroy() {

        stopSensorTest()

        super.onDestroy()
    }

    private fun detectSensors() {

        txtSensorStatus.text =
            "Checking ECU sensors..."

        Thread {

            try {

                if (
                    ConnectionSource.activeSource ==
                    ConnectionSource.Source.NONE
                ) {
                    showStatus(
                        "🔴 No active ELM327 connection"
                    )
                    return@Thread
                }

                if (!isElmConnected()) {
                    showStatus(
                        "🔴 ELM327 is disconnected"
                    )
                    return@Thread
                }

                val supportedPids =
                    ObdPidSupport.getSupportedPids()

                if (supportedPids == null) {
                    showStatus(
                        "🟡 Could not read ECU supported PIDs"
                    )
                    return@Thread
                }

                val detectedSensors =
                    ObdSensorCatalog.sensors.filter { sensor ->
                        supportedPids.contains(
                            sensor.pid
                        )
                    }

                /*
                 * Battery Voltage is read directly
                 * with AT RV, so keep the voltage
                 * sensor in the list even if PID 0142
                 * is not supported by the ECU.
                 */
                val batteryVoltageSensor =
                    ObdSensorCatalog.sensors.firstOrNull { sensor ->
                        sensor.pid == "0142"
                    }

                availableSensors =
                    if (
                        batteryVoltageSensor != null &&
                        detectedSensors.none { sensor ->
                            sensor.pid == "0142"
                        }
                    ) {
                        detectedSensors +
                                batteryVoltageSensor
                    } else {
                        detectedSensors
                    }

                if (availableSensors.isEmpty()) {
                    showStatus(
                        "🟡 No catalog sensors are supported by ECU"
                    )
                    return@Thread
                }

                buildSensorList()

                showStatus(
                    "🟢 ${availableSensors.size} supported sensors found"
                )

                startSensorTest()

            } catch (e: Exception) {

                showStatus(
                    "❌ Sensor detection failed: " +
                            (
                                    e.message
                                        ?: "Unknown error"
                                    )
                )
            }

        }.start()
    }

    private fun buildSensorList() {

        runOnUiThread {

            sensorContainer.removeAllViews()

            sensorValueViews.clear()

            for (sensor in availableSensors) {

                val row =
                    createSensorRow(sensor)

                sensorContainer.addView(row)
            }
        }
    }

    private fun createSensorRow(
        sensor: ObdSensor
    ): LinearLayout {

        val row =
            LinearLayout(this)

        row.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(6)
            }

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        row.setPadding(
            dp(12),
            dp(10),
            dp(12),
            dp(10)
        )

        row.setBackgroundColor(
            Color.WHITE
        )

        val nameView =
            TextView(this)

        nameView.layoutParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        nameView.text =
            buildString {

                append(sensor.name)

                if (sensor.unit.isNotBlank()) {
                    append("  •  ")
                    append(sensor.unit)
                }

                append("\nPID: ")
                append(sensor.pid)
            }

        nameView.textSize =
            15f

        nameView.setTextColor(
            Color.rgb(
                35,
                35,
                35
            )
        )

        val valueView =
            TextView(this)

        valueView.layoutParams =
            LinearLayout.LayoutParams(
                dp(120),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        valueView.gravity =
            Gravity.CENTER

        valueView.text =
            "—"

        valueView.textSize =
            18f

        valueView.setTextColor(
            Color.rgb(
                230,
                100,
                20
            )
        )

        valueView.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        row.addView(nameView)
        row.addView(valueView)

        sensorValueViews[
            sensor.id
        ] = valueView

        return row
    }

    private fun startSensorTest() {

        if (testRunning) return

        if (
            ConnectionSource.activeSource ==
            ConnectionSource.Source.NONE
        ) {
            showStatus(
                "🔴 No active ELM327 connection"
            )
            return
        }

        if (!isElmConnected()) {
            showStatus(
                "🔴 ELM327 is disconnected"
            )
            return
        }

        testRunning =
            true

        showStatus(
            "🟢 Reading ${availableSensors.size} sensors..."
        )

        testThread =
            Thread {

                while (testRunning) {

                    try {

                        for (sensor in availableSensors) {

                            if (!testRunning) {
                                break
                            }

                            readAndDisplaySensor(
                                sensor
                            )
                        }

                        if (testRunning) {

                            Thread.sleep(
                                TEST_INTERVAL_MS
                            )
                        }

                    } catch (
                        interrupted: InterruptedException
                    ) {

                        break

                    } catch (e: Exception) {

                        showStatus(
                            "❌ Sensor read error: " +
                                    (
                                            e.message
                                                ?: "Unknown error"
                                            )
                        )

                        break
                    }
                }
            }

        testThread?.start()
    }

    private fun readAndDisplaySensor(
        sensor: ObdSensor
    ) {

        /*
         * Battery voltage is NOT read with PID 0142.
         *
         * AT RV asks the ELM327 for its measured
         * vehicle battery voltage.
         */
        if (
            sensor.pid.equals(
                "0142",
                ignoreCase = true
            )
        ) {

            val voltage =
                readBatteryVoltage()

            updateSensorValue(
                sensor,
                voltage
            )

            return
        }

        /*
         * All normal sensor reads now go through
         * the common ECU data gateway.
         *
         * STANDARD:
         * EcuDataGateway delegates to the existing
         * ObdSensorReader without changing it.
         *
         * UNKNOWN:
         * EcuDataGateway will later use the special
         * commands/parser supplied for the unknown ECU.
         */
        val value =
            EcuDataGateway.readSensor(
                sensor
            )

        updateSensorValue(
            sensor,
            value
        )
    }

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

    private fun parseBatteryVoltage(
        response: String
    ): Float? {

        if (response.isBlank()) {
            return null
        }

        val upper =
            response.uppercase(
                Locale.US
            )

        if (
            upper.contains("NO DATA") ||
            upper.contains("NODATA") ||
            upper.contains("ERROR") ||
            upper.contains("?") ||
            upper.contains("UNABLE TO CONNECT")
        ) {
            return null
        }

        /*
         * Typical ELM327 AT RV response:
         *
         * 12.6V
         *
         * Sometimes there may be spaces,
         * line breaks or the prompt.
         */
        val voltageRegex =
            Regex(
                """(\d+(?:\.\d+)?)\s*V""",
                RegexOption.IGNORE_CASE
            )

        val match =
            voltageRegex.find(
                upper
            )

        if (match != null) {

            return match.groupValues[1]
                .toFloatOrNull()
        }

        /*
         * Fallback:
         * Some ELM327 devices return only the
         * numeric value without the V character.
         */
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

        if (fallback != null) {

            val value =
                fallback.groupValues[1]
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

    private fun updateSensorValue(
        sensor: ObdSensor,
        value: Float?
    ) {

        runOnUiThread {

            val valueView =
                sensorValueViews[
                    sensor.id
                ]
                    ?: return@runOnUiThread

            if (value == null) {

                valueView.text =
                    "—"

                return@runOnUiThread
            }

            valueView.text =
                formatValue(
                    value,
                    sensor.unit
                )
        }
    }

    private fun formatValue(
        value: Float,
        unit: String
    ): String {

        val number =
            if (value % 1f == 0f) {

                value.toInt()
                    .toString()

            } else {

                String.format(
                    Locale.US,
                    "%.2f",
                    value
                )
            }

        return if (unit.isBlank()) {

            number

        } else {

            "$number $unit"
        }
    }

    private fun stopSensorTest() {

        testRunning =
            false

        testThread?.interrupt()

        testThread =
            null
    }

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

    private fun showStatus(
        status: String
    ) {

        runOnUiThread {

            txtSensorStatus.text =
                status
        }
    }

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
}