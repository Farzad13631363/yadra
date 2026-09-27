package com.example.yadra

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EcuTestActivity : AppCompatActivity() {

    private lateinit var txtEcuStatus: TextView
    private lateinit var txtProtocol: TextView
    private lateinit var txtEcuResponse: TextView
    private lateinit var txtRpm: TextView
    private lateinit var btnTestEcu: Button
    private lateinit var spinnerProtocol: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_ecu_test)

        txtEcuStatus = findViewById(R.id.txtEcuStatus)
        txtProtocol = findViewById(R.id.txtProtocol)
        txtEcuResponse = findViewById(R.id.txtEcuResponse)
        txtRpm = findViewById(R.id.txtRpm)
        btnTestEcu = findViewById(R.id.btnTestEcu)
        spinnerProtocol = findViewById(R.id.spinnerProtocol)

        setupProtocolSpinner()

        btnTestEcu.setOnClickListener {
            testEcu()
        }
    }

    private fun setupProtocolSpinner() {

        val items = listOf(
            "Automatic - Detect ECU Protocol"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            items
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerProtocol.adapter = adapter

        // پروتکل به صورت خودکار تشخیص داده می‌شود.
        spinnerProtocol.isEnabled = false
    }

    override fun onResume() {
        super.onResume()

        updateStatus()
    }

    private fun updateStatus() {

        when (ConnectionSource.activeSource) {

            ConnectionSource.Source.WIFI -> {

                if (!YadraConnectionManager.elmConnected) {

                    txtEcuStatus.text =
                        "🔴 Wi-Fi / ELM327: Disconnected"

                    txtProtocol.text =
                        "Protocol: —"

                    return
                }

                if (EcuConnectionManager.connected ||
                    YadraConnectionManager.ecuConnected
                ) {

                    txtEcuStatus.text =
                        "🟢 Wi-Fi / ECU: Connected"

                } else {

                    txtEcuStatus.text =
                        "🟡 ELM327 Connected / ECU Not Tested"
                }

                showProtocol()
            }

            ConnectionSource.Source.BLUETOOTH -> {

                if (!BluetoothConnectionManager.elmConnected) {

                    txtEcuStatus.text =
                        "🔴 Bluetooth / ELM327: Disconnected"

                    txtProtocol.text =
                        "Protocol: —"

                    return
                }

                if (EcuConnectionManager.connected ||
                    BluetoothConnectionManager.ecuConnected
                ) {

                    txtEcuStatus.text =
                        "🟢 Bluetooth / ECU: Connected"

                } else {

                    txtEcuStatus.text =
                        "🟡 ELM327 Connected / ECU Not Tested"
                }

                showProtocol()
            }

            ConnectionSource.Source.NONE -> {

                txtEcuStatus.text =
                    "🔴 No ELM327 Connection"

                txtProtocol.text =
                    "Protocol: —"
            }
        }
    }

    private fun showProtocol() {

        var protocol =
            EcuConnectionManager.protocol

        if (protocol.isBlank()) {

            when (ConnectionSource.activeSource) {

                ConnectionSource.Source.WIFI -> {

                    protocol =
                        YadraConnectionManager.detectedProtocol
                }

                ConnectionSource.Source.BLUETOOTH -> {

                    protocol =
                        BluetoothConnectionManager.protocol
                }

                ConnectionSource.Source.NONE -> {

                    protocol = ""
                }
            }
        }

        if (protocol.isBlank()) {
            protocol = "Unknown"
        }

        txtProtocol.text =
            "Protocol: $protocol"
    }

    private fun testEcu() {

        btnTestEcu.isEnabled = false

        txtEcuStatus.text =
            "Testing ECU..."

        txtProtocol.text =
            "Protocol: detecting..."

        txtEcuResponse.text =
            "0100 Response: waiting..."

        txtRpm.text =
            "RPM: —"

        Thread {

            try {

                val success =
                    EcuConnectionManager.connectToEcu()

                val protocol =
                    EcuConnectionManager.protocol

                val response =
                    EcuConnectionManager.lastResponse

                val rpm =
                    if (success) {
                        EcuConnectionManager.readRpm()
                    } else {
                        null
                    }

                showResult(
                    success = success,
                    protocol = protocol,
                    response = response,
                    rpm = rpm
                )

            } catch (e: Exception) {

                showError(
                    e.message ?: "Unknown error"
                )
            }

        }.start()
    }

    private fun showResult(
        success: Boolean,
        protocol: String,
        response: String,
        rpm: Int?
    ) {

        runOnUiThread {

            if (success) {

                txtEcuStatus.text =
                    "🟢 ECU Connected"

            } else {

                txtEcuStatus.text =
                    "🔴 ECU Not Detected"
            }

            if (protocol.isBlank()) {

                txtProtocol.text =
                    "Protocol: Unknown"

            } else {

                txtProtocol.text =
                    "Protocol: $protocol"
            }

            if (response.isBlank()) {

                txtEcuResponse.text =
                    "0100 Response: No response"

            } else {

                txtEcuResponse.text =
                    "0100 Response:\n${cleanResponse(response)}"
            }

            if (rpm != null) {

                txtRpm.text =
                    "RPM: $rpm"

            } else {

                txtRpm.text =
                    "RPM: —"
            }

            btnTestEcu.isEnabled = true
        }
    }

    private fun showError(
        message: String
    ) {

        runOnUiThread {

            txtEcuStatus.text =
                "❌ ECU Test Failed"

            txtProtocol.text =
                "Protocol: —"

            txtEcuResponse.text =
                "Error: $message"

            txtRpm.text =
                "RPM: —"

            btnTestEcu.isEnabled = true
        }
    }

    private fun cleanResponse(
        response: String
    ): String {

        return response
            .replace("\r", " ")
            .replace("\n", " ")
            .replace(">", " ")
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }
}