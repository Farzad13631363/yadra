package com.example.yadra

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ErrorsActivity : AppCompatActivity() {

    private lateinit var txtStatus: TextView
    private lateinit var txtRawResponse: TextView
    private lateinit var errorContainer: LinearLayout
    private lateinit var btnReadErrors: Button
    private lateinit var btnClearErrors: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_errors)

        txtStatus = findViewById(R.id.txtStatus)
        txtRawResponse = findViewById(R.id.txtRawResponse)
        errorContainer = findViewById(R.id.errorContainer)
        btnReadErrors = findViewById(R.id.btnReadErrors)
        btnClearErrors = findViewById(R.id.btnClearErrors)

        btnReadErrors.setOnClickListener {
            readErrors()
        }

        btnClearErrors.setOnClickListener {
            showClearConfirmation()
        }

        updateInitialStatus()
    }

    // =========================================================
    // CONNECTION STATE
    // =========================================================

    private fun isUnknownEcuConnected(): Boolean {

        return EcuRuntimeState.mode == EcuMode.UNKNOWN &&
                EcuRuntimeState.connected
    }

    private fun isStandardEcuConnected(): Boolean {

        return when (ConnectionSource.activeSource) {

            ConnectionSource.Source.WIFI -> {

                YadraConnectionManager.isWifiConnected() &&
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

    private fun updateInitialStatus() {

        if (isUnknownEcuConnected()) {

            val protocol =
                EcuRuntimeState.detectedProtocol

            val protocolName =
                EcuRuntimeState.detectedProtocolName

            txtStatus.text =
                if (protocolName.isNotBlank()) {
                    "UNKNOWN ECU CONNECTED\n$protocol - $protocolName"
                } else {
                    "UNKNOWN ECU CONNECTED\n$protocol"
                }

            txtStatus.setTextColor(
                Color.rgb(
                    0,
                    140,
                    60
                )
            )

            return
        }

        if (isStandardEcuConnected()) {

            txtStatus.setText(
                R.string.errors_ready
            )

            txtStatus.setTextColor(
                Color.rgb(
                    0,
                    140,
                    60
                )
            )

            return
        }

        txtStatus.setText(
            R.string.errors_ready
        )

        txtStatus.setTextColor(
            Color.rgb(
                34,
                34,
                34
            )
        )
    }

    // =========================================================
    // READ ERRORS
    // =========================================================

    private fun readErrors() {

        txtRawResponse.text = ""

        txtRawResponse.visibility =
            TextView.GONE

        errorContainer.removeAllViews()

        /*
         * -----------------------------------------------------
         * UNKNOWN ECU
         * -----------------------------------------------------
         *
         * Do NOT send standard Mode 03 to an Unknown ECU.
         */

        if (isUnknownEcuConnected()) {

            readUnknownEcuErrors()

            return
        }

        /*
         * -----------------------------------------------------
         * STANDARD ECU
         * -----------------------------------------------------
         */

        txtStatus.setText(
            R.string.connecting_elm327
        )

        Thread {

            try {

                when (ConnectionSource.activeSource) {

                    ConnectionSource.Source.WIFI -> {

                        val manager =
                            YadraConnectionManager

                        if (!manager.isWifiConnected()) {

                            runOnUiThread {

                                txtStatus.setText(
                                    R.string.connection_failed
                                )
                            }

                            return@Thread
                        }

                        if (!manager.elmConnected) {

                            val elmConnected =
                                manager.connectToElm327()

                            if (!elmConnected) {

                                runOnUiThread {

                                    txtStatus.setText(
                                        R.string.connection_failed
                                    )
                                }

                                return@Thread
                            }
                        }

                        val response =
                            EcuDataGateway.readErrors()

                        if (response == null) {

                            runOnUiThread {

                                txtStatus.setText(
                                    R.string.connection_failed
                                )
                            }

                            return@Thread
                        }

                        showErrorResult(
                            response,
                            "Wi-Fi"
                        )
                    }

                    ConnectionSource.Source.BLUETOOTH -> {

                        if (!BluetoothConnectionManager.elmConnected) {

                            runOnUiThread {

                                txtStatus.setText(
                                    R.string.connection_failed
                                )
                            }

                            return@Thread
                        }

                        val response =
                            EcuDataGateway.readErrors()

                        if (response == null) {

                            runOnUiThread {

                                txtStatus.setText(
                                    R.string.connection_failed
                                )
                            }

                            return@Thread
                        }

                        showErrorResult(
                            response,
                            "Bluetooth"
                        )
                    }

                    ConnectionSource.Source.NONE -> {

                        runOnUiThread {

                            txtStatus.setText(
                                R.string.connection_failed
                            )
                        }
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    txtStatus.setText(
                        R.string.connection_failed
                    )

                    txtRawResponse.text =
                        e.message
                            ?: getString(
                                R.string.elm_no_response
                            )

                    txtRawResponse.visibility =
                        TextView.VISIBLE
                }
            }

        }.start()
    }

    // =========================================================
    // UNKNOWN ECU
    // =========================================================

    private fun readUnknownEcuErrors() {

        runOnUiThread {

            txtStatus.text =
                "UNKNOWN ECU CONNECTED"

            txtStatus.setTextColor(
                Color.rgb(
                    0,
                    140,
                    60
                )
            )

            errorContainer.removeAllViews()

            val protocol =
                EcuRuntimeState.detectedProtocol

            val protocolName =
                EcuRuntimeState.detectedProtocolName

            val address =
                EcuRuntimeState.ecuAddress

            val identifier =
                EcuRuntimeState.ecuIdentifier

            val lastRawResponse =
                EcuRuntimeState.rawResponse

            val message =
                TextView(this@ErrorsActivity)

            message.text =
                buildString {

                    append(
                        "ECU is connected."
                    )

                    append(
                        "\n\n"
                    )

                    if (protocol.isNotBlank()) {

                        append(
                            "Protocol: "
                        )

                        append(
                            protocol
                        )

                        append(
                            "\n"
                        )
                    }

                    if (protocolName.isNotBlank()) {

                        append(
                            "Protocol Name: "
                        )

                        append(
                            protocolName
                        )

                        append(
                            "\n"
                        )
                    }

                    if (address.isNotBlank()) {

                        append(
                            "ECU Address: "
                        )

                        append(
                            address
                        )

                        append(
                            "\n"
                        )
                    }

                    if (identifier.isNotBlank()) {

                        append(
                            "ECU Identifier: "
                        )

                        append(
                            identifier
                        )

                        append(
                            "\n"
                        )
                    }

                    append(
                        "\n"
                    )

                    append(
                        "Unknown ECU diagnostic protocol is active."
                    )

                    append(
                        "\n\n"
                    )

                    append(
                        "Standard OBD Mode 03 is not sent to this ECU."
                    )

                    if (lastRawResponse.isNotBlank()) {

                        append(
                            "\n\nLast ECU response:\n"
                        )

                        append(
                            lastRawResponse
                        )
                    }
                }

            message.textSize =
                16f

            message.setPadding(
                16,
                16,
                16,
                16
            )

            errorContainer.addView(
                message
            )
        }
    }

    // =========================================================
    // SHOW ERROR RESULT
    // =========================================================

    private fun showErrorResult(
        response: String,
        source: String
    ) {

        runOnUiThread {

            if (response.isBlank()) {

                txtStatus.setText(
                    R.string.elm_no_response
                )

                txtRawResponse.visibility =
                    TextView.GONE

                return@runOnUiThread
            }

            txtStatus.text =
                "$source ELM327: Connected"

            txtRawResponse.text =
                response

            txtRawResponse.visibility =
                TextView.VISIBLE

            displayErrors(response)
        }
    }

    // =========================================================
    // CLEAR ERRORS CONFIRMATION
    // =========================================================

    private fun showClearConfirmation() {

        if (isUnknownEcuConnected()) {

            AlertDialog.Builder(this)
                .setTitle(
                    "پاک کردن خطاها"
                )
                .setMessage(
                    "این ECU ناشناس است و فرمان استاندارد پاک کردن خطا برای آن ارسال نمی‌شود."
                )
                .setPositiveButton(
                    "باشه",
                    null
                )
                .show()

            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                "پاک کردن خطاها"
            )
            .setMessage(
                "آیا از پاک کردن خطاهای ECU اطمینان دارید؟"
            )
            .setPositiveButton(
                "بله، پاک کن"
            ) { _, _ ->

                clearErrors()
            }
            .setNegativeButton(
                "خیر",
                null
            )
            .show()
    }

    // =========================================================
    // CLEAR ERRORS
    // =========================================================

    private fun clearErrors() {

        /*
         * Unknown ECU:
         * Never send standard Mode 04.
         */

        if (isUnknownEcuConnected()) {

            txtStatus.text =
                "UNKNOWN ECU CONNECTED"

            txtStatus.setTextColor(
                Color.rgb(
                    0,
                    140,
                    60
                )
            )

            return
        }

        txtStatus.setText(
            R.string.clearing_errors
        )

        Thread {

            try {

                when (ConnectionSource.activeSource) {

                    ConnectionSource.Source.WIFI -> {

                        val manager =
                            YadraConnectionManager

                        if (
                            !manager.isWifiConnected() ||
                            !manager.elmConnected
                        ) {

                            runOnUiThread {

                                txtStatus.setText(
                                    R.string.connection_failed
                                )
                            }

                            return@Thread
                        }

                        val response =
                            EcuDataGateway.clearErrors()

                        if (response == null) {

                            runOnUiThread {

                                txtStatus.setText(
                                    R.string.connection_failed
                                )
                            }

                            return@Thread
                        }

                        showClearResult(
                            response
                        )
                    }

                    ConnectionSource.Source.BLUETOOTH -> {

                        if (
                            !BluetoothConnectionManager
                                .elmConnected
                        ) {

                            runOnUiThread {

                                txtStatus.setText(
                                    R.string.connection_failed
                                )
                            }

                            return@Thread
                        }

                        val response =
                            EcuDataGateway.clearErrors()

                        if (response == null) {

                            runOnUiThread {

                                txtStatus.setText(
                                    R.string.connection_failed
                                )
                            }

                            return@Thread
                        }

                        showClearResult(
                            response
                        )
                    }

                    ConnectionSource.Source.NONE -> {

                        runOnUiThread {

                            txtStatus.setText(
                                R.string.connection_failed
                            )
                        }
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    txtStatus.setText(
                        R.string.connection_failed
                    )

                    txtRawResponse.text =
                        e.message
                            ?: getString(
                                R.string.elm_no_response
                            )

                    txtRawResponse.visibility =
                        TextView.VISIBLE
                }
            }

        }.start()
    }

    // =========================================================
    // SHOW CLEAR RESULT
    // =========================================================

    private fun showClearResult(
        response: String
    ) {

        runOnUiThread {

            if (response.isBlank()) {

                txtStatus.setText(
                    R.string.elm_no_response
                )

                return@runOnUiThread
            }

            txtStatus.setText(
                R.string.clear_command_sent
            )

            txtRawResponse.text =
                response

            txtRawResponse.visibility =
                TextView.VISIBLE

            errorContainer.removeAllViews()

            val message =
                TextView(this@ErrorsActivity)

            message.text =
                response.trim()

            message.textSize =
                16f

            message.setPadding(
                12,
                12,
                12,
                12
            )

            errorContainer.addView(
                message
            )
        }
    }

    // =========================================================
    // DISPLAY ERRORS
    // =========================================================

    private fun displayErrors(
        response: String
    ) {

        errorContainer.removeAllViews()

        val cleaned =
            response
                .uppercase()
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
                .replace(
                    "\u0000",
                    " "
                )
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        if (cleaned.isBlank()) {

            addMessage(
                getString(
                    R.string.no_registered_errors
                )
            )

            return
        }

        val dtcCodes =
            parseDtcCodes(cleaned)

        if (dtcCodes.isEmpty()) {

            if (
                cleaned.contains("43") &&
                cleaned.length <= 4
            ) {

                addMessage(
                    getString(
                        R.string.no_registered_errors
                    )
                )

                return
            }

            addMessage(
                cleaned
            )

            return
        }

        for (code in dtcCodes) {

            addDtcItem(code)
        }
    }

    // =========================================================
    // PARSE DTC
    // =========================================================

    private fun parseDtcCodes(
        response: String
    ): List<String> {

        val compact =
            response
                .replace(
                    Regex("[^0-9A-F]"),
                    ""
                )

        val startIndex =
            findDtcResponseIndex(
                compact
            )

        if (startIndex == -1) {
            return emptyList()
        }

        val data =
            compact.substring(
                startIndex + 2
            )

        if (data.length < 4) {
            return emptyList()
        }

        val result =
            mutableListOf<String>()

        var index = 0

        while (
            index + 4 <= data.length
        ) {

            val byte1 =
                data
                    .substring(
                        index,
                        index + 2
                    )
                    .toIntOrNull(16)

            val byte2 =
                data
                    .substring(
                        index + 2,
                        index + 4
                    )
                    .toIntOrNull(16)

            if (
                byte1 == null ||
                byte2 == null
            ) {
                break
            }

            if (
                byte1 == 0 &&
                byte2 == 0
            ) {

                index += 4

                continue
            }

            val code =
                decodeDtc(
                    byte1,
                    byte2
                )

            if (code.isNotBlank()) {

                result.add(code)
            }

            index += 4
        }

        return result
    }

    // =========================================================
    // FIND 43 RESPONSE
    // =========================================================

    private fun findDtcResponseIndex(
        response: String
    ): Int {

        val index =
            response.indexOf("43")

        if (index >= 0) {
            return index
        }

        return -1
    }

    // =========================================================
    // DECODE DTC
    // =========================================================

    private fun decodeDtc(
        byte1: Int,
        byte2: Int
    ): String {

        val type =
            when (
                (byte1 and 0xC0) shr 6
            ) {

                0 -> "P"
                1 -> "C"
                2 -> "B"
                3 -> "U"
                else -> "P"
            }

        val digit1 =
            (byte1 and 0x30) shr 4

        val digit2 =
            byte1 and 0x0F

        val digit3 =
            (byte2 and 0xF0) shr 4

        val digit4 =
            byte2 and 0x0F

        return "$type$digit1$digit2$digit3$digit4"
    }

    // =========================================================
    // ADD DTC ITEM
    // =========================================================

    private fun addDtcItem(
        code: String
    ) {

        val info =
            DtcDatabase.get(
                this,
                code
            )

        val item =
            TextView(this)

        if (info != null) {

            item.text =
                buildString {

                    append("⚠️  ")

                    append(
                        info.code
                    )

                    append(
                        "\n\n"
                    )

                    append(
                        info.description
                    )

                    append(
                        "\n\n"
                    )

                    append(
                        "System: "
                    )

                    append(
                        if (
                            info.typeName.isNotBlank()
                        ) {
                            info.typeName
                        } else {
                            info.type
                        }
                    )

                    append(
                        "\n"
                    )

                    append(
                        "Manufacturer: "
                    )

                    append(
                        info.manufacturer
                    )

                    append(
                        "\n"
                    )

                    append(
                        "Status: Stored"
                    )
                }

        } else {

            item.text =
                buildString {

                    append("⚠️  ")

                    append(
                        code
                    )

                    append(
                        "\n\n"
                    )

                    append(
                        "Description: Not available"
                    )

                    append(
                        "\n\n"
                    )

                    append(
                        "Status: Stored"
                    )
                }
        }

        item.textSize =
            17f

        item.setTextColor(
            Color.rgb(
                80,
                0,
                0
            )
        )

        item.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.dtc_error_background
            )

        item.setPadding(
            16,
            16,
            16,
            16
        )

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.setMargins(
            8,
            8,
            8,
            8
        )

        item.layoutParams =
            params

        errorContainer.addView(
            item
        )
    }

    // =========================================================
    // ADD MESSAGE
    // =========================================================

    private fun addMessage(
        message: String
    ) {

        val item =
            TextView(this)

        item.text =
            message

        item.textSize =
            16f

        item.setPadding(
            12,
            12,
            12,
            12
        )

        errorContainer.addView(
            item
        )
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {
        super.onDestroy()
    }
}