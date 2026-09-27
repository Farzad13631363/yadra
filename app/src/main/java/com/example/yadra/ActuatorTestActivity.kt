package com.example.yadra

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ActuatorTestActivity : AppCompatActivity() {

    private lateinit var txtActuatorStatus: TextView
    private lateinit var actuatorContainer: LinearLayout
    private lateinit var btnScanEcu: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_actuator_test)

        txtActuatorStatus =
            findViewById(R.id.txtActuatorStatus)

        actuatorContainer =
            findViewById(R.id.actuatorContainer)

        btnScanEcu =
            findViewById(R.id.btnScanEcu)

        txtActuatorStatus.text =
            "آماده اسکن ECU"

        txtActuatorStatus.setTextColor(
            Color.rgb(255, 215, 64)
        )

        btnScanEcu.setOnClickListener {
            scanEcu()
        }
    }

    override fun onResume() {
        super.onResume()
        updateConnectionStatus()
    }

    private fun updateConnectionStatus() {

        when (ConnectionSource.activeSource) {

            ConnectionSource.Source.WIFI -> {

                if (YadraConnectionManager.elmConnected) {

                    txtActuatorStatus.text =
                        "🟢 Wi-Fi / ELM327 متصل است"

                    txtActuatorStatus.setTextColor(
                        Color.rgb(0, 230, 118)
                    )

                } else {

                    txtActuatorStatus.text =
                        "🔴 Wi-Fi / ELM327 قطع است"

                    txtActuatorStatus.setTextColor(
                        Color.rgb(255, 82, 82)
                    )
                }
            }

            ConnectionSource.Source.BLUETOOTH -> {

                if (BluetoothConnectionManager.elmConnected) {

                    txtActuatorStatus.text =
                        "🟢 Bluetooth / ELM327 متصل است"

                    txtActuatorStatus.setTextColor(
                        Color.rgb(0, 230, 118)
                    )

                } else {

                    txtActuatorStatus.text =
                        "🔴 Bluetooth / ELM327 قطع است"

                    txtActuatorStatus.setTextColor(
                        Color.rgb(255, 82, 82)
                    )
                }
            }

            ConnectionSource.Source.NONE -> {

                txtActuatorStatus.text =
                    "🔴 هیچ اتصال ELM327 فعالی وجود ندارد"

                txtActuatorStatus.setTextColor(
                    Color.rgb(255, 82, 82)
                )
            }
        }
    }

    private fun scanEcu() {

        btnScanEcu.isEnabled = false
        actuatorContainer.removeAllViews()

        txtActuatorStatus.text =
            "در حال بررسی ECU..."

        txtActuatorStatus.setTextColor(
            Color.rgb(255, 215, 64)
        )

        Thread {

            try {

                val source =
                    ConnectionSource.activeSource

                val connected =
                    when (source) {

                        ConnectionSource.Source.WIFI ->
                            YadraConnectionManager.elmConnected

                        ConnectionSource.Source.BLUETOOTH ->
                            BluetoothConnectionManager.elmConnected

                        ConnectionSource.Source.NONE ->
                            false
                    }

                if (!connected) {

                    runOnUiThread {

                        txtActuatorStatus.text =
                            "🔴 ELM327 متصل نیست"

                        txtActuatorStatus.setTextColor(
                            Color.rgb(255, 82, 82)
                        )

                        btnScanEcu.isEnabled = true
                    }

                    return@Thread
                }

                runOnUiThread {

                    txtActuatorStatus.text =
                        "🟢 ELM327 متصل است — ECU آماده بررسی"

                    txtActuatorStatus.setTextColor(
                        Color.rgb(0, 230, 118)
                    )
                }

                /*
                 * فعلاً فقط وضعیت اتصال ECU را نمایش می‌دهیم.
                 *
                 * هیچ فرمان actuator ارسال نمی‌شود.
                 */
                runOnUiThread {

                    showInfoCard()

                    btnScanEcu.isEnabled = true
                }

            } catch (e: Exception) {

                runOnUiThread {

                    txtActuatorStatus.text =
                        "❌ خطا: ${
                            e.message
                                ?: e.javaClass.simpleName
                        }"

                    txtActuatorStatus.setTextColor(
                        Color.rgb(255, 82, 82)
                    )

                    btnScanEcu.isEnabled = true
                }
            }
        }.start()
    }

    private fun showInfoCard() {

        val info =
            TextView(this)

        val source =
            ConnectionSource.activeSource

        val protocol =
            when (source) {

                ConnectionSource.Source.WIFI ->
                    YadraConnectionManager
                        .detectedProtocol

                ConnectionSource.Source.BLUETOOTH ->
                    BluetoothConnectionManager
                        .protocol

                ConnectionSource.Source.NONE ->
                    ""
            }

        val text =
            buildString {

                append("ECU / ELM327\n\n")

                when (source) {

                    ConnectionSource.Source.WIFI -> {
                        append(
                            "منبع اتصال: Wi-Fi\n"
                        )

                        append(
                            "وضعیت ELM327: متصل\n"
                        )
                    }

                    ConnectionSource.Source.BLUETOOTH -> {
                        append(
                            "منبع اتصال: Bluetooth\n"
                        )

                        append(
                            "وضعیت ELM327: متصل\n"
                        )
                    }

                    ConnectionSource.Source.NONE -> {
                        append(
                            "منبع اتصال: —\n"
                        )
                    }
                }

                append(
                    "Protocol: ${
                        if (protocol.isBlank()) {
                            "Unknown"
                        } else {
                            protocol
                        }
                    }\n"
                )

                append("\n")

                append(
                    "در این مرحله هنوز هیچ عملگری فعال نشده است."
                )
            }

        info.text = text

        info.setTextColor(
            Color.WHITE
        )

        info.textSize = 16f

        info.setPadding(
            20,
            20,
            20,
            20
        )

        actuatorContainer.addView(info)
    }
}