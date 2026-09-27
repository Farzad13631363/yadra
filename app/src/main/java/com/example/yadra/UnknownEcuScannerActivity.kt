package com.example.yadra

import java.util.UUID
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

class UnknownEcuScannerActivity : AppCompatActivity() {

    // ============================================================
    // UI
    // ============================================================

    private lateinit var txtStatus: TextView
    private lateinit var txtSelectedProtocol: TextView
    private lateinit var txtLastProtocol: TextView
    private lateinit var txtResult: TextView
    private lateinit var txtProgress: TextView

    private lateinit var txtEcuStatus: TextView
    private lateinit var protocolSpinner: Spinner

    private lateinit var progressBar: ProgressBar

    private lateinit var btnConnect: Button
    private lateinit var btnScan: Button
    private lateinit var btnDisconnect: Button

    private lateinit var commandScrollView: ScrollView
    private lateinit var commandResultContainer: LinearLayout

    // ============================================================
    // STATE
    // ============================================================

    private var scanning = false

    private val executor =
        Executors.newSingleThreadExecutor()

    // ============================================================
    // ACTIVITY
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        UnknownEcuScanner.initialize(
            applicationContext
        )

        UnknownEcuScanner.setScanProgressListener {
                current,
                total,
                code,
                name ->

            runOnUiThread {

                txtStatus.text =
                    "در حال جستجو...\n" +
                            "[$current / $total] $code - $name"
            }
        }

        buildUi()

        loadProtocolList()

        restoreRuntimeEcuState()

        updateConnectionStatus()
    }

    override fun onResume() {
        super.onResume()

        restoreRuntimeEcuState()

        updateConnectionStatus()
    }

    override fun onDestroy() {

        executor.shutdownNow()

        super.onDestroy()
    }

    // ============================================================
    // BUILD UI
    // ============================================================

    private fun buildUi() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    20,
                    20,
                    20,
                    20
                )

                setBackgroundColor(
                    Color.BLACK
                )
            }

        // --------------------------------------------------------
        // TITLE
        // --------------------------------------------------------

        val title =
            TextView(this).apply {

                text =
                    "Unknown ECU Scanner"

                textSize =
                    24f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setPadding(
                    0,
                    0,
                    0,
                    16
                )
            }

        root.addView(title)

        // --------------------------------------------------------
        // STATUS
        // --------------------------------------------------------

        txtStatus =
            TextView(this).apply {

                text =
                    "Status: Checking connection..."

                textSize =
                    16f

                setTextColor(
                    Color.LTGRAY
                )

                setPadding(
                    0,
                    4,
                    0,
                    8
                )
            }

        root.addView(txtStatus)

        // --------------------------------------------------------
        // ECU CONNECTION STATUS
        // --------------------------------------------------------

        txtEcuStatus =
            TextView(this).apply {

                text =
                    "ECU: در حال اتصال..."

                textSize =
                    16f

                setTextColor(
                    Color.RED
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setPadding(
                    0,
                    2,
                    0,
                    12
                )
            }

        root.addView(
            txtEcuStatus
        )

        // --------------------------------------------------------
        // PROTOCOL LABEL
        // --------------------------------------------------------

        val protocolLabel =
            TextView(this).apply {

                text =
                    "Select ECU Protocol"

                textSize =
                    16f

                setTextColor(
                    Color.CYAN
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setPadding(
                    0,
                    6,
                    0,
                    6
                )
            }

        root.addView(
            protocolLabel
        )

        // --------------------------------------------------------
        // PROTOCOL SPINNER
        // --------------------------------------------------------

        protocolSpinner =
            Spinner(this).apply {

                setBackgroundColor(
                    Color.WHITE
                )
            }

        root.addView(
            protocolSpinner,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // --------------------------------------------------------
        // SELECTED PROTOCOL
        // --------------------------------------------------------

        txtSelectedProtocol =
            TextView(this).apply {

                text =
                    "Selected: ---"

                textSize =
                    14f

                setTextColor(
                    Color.YELLOW
                )

                setPadding(
                    0,
                    8,
                    0,
                    10
                )
            }

        root.addView(
            txtSelectedProtocol
        )

        // --------------------------------------------------------
        // CONNECT BUTTON
        // --------------------------------------------------------

        btnConnect =
            Button(this).apply {

                text =
                    "CONNECT SELECTED PROTOCOL"

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.rgb(
                        0,
                        170,
                        0
                    )
                )

                setOnClickListener {

                    connectSelectedProtocol()
                }
            }

        root.addView(
            btnConnect
        )

        // --------------------------------------------------------
        // AUTO SCAN BUTTON
        // --------------------------------------------------------

        btnScan =
            Button(this).apply {

                text =
                    "AUTO SCAN 46 PROTOCOLS"

                setTextColor(
                    Color.BLACK
                )

                setBackgroundColor(
                    Color.WHITE
                )

                setOnClickListener {

                    startUnknownEcuScan()
                }
            }

        root.addView(
            btnScan
        )

        // --------------------------------------------------------
        // DISCONNECT / CLEAR BUTTON
        // --------------------------------------------------------

        btnDisconnect =
            Button(this).apply {

                text =
                    "Clear / Disconnect"

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.rgb(
                        200,
                        0,
                        0
                    )
                )

                setOnClickListener {

                    disconnectCurrentTransport()
                }
            }

        root.addView(
            btnDisconnect
        )

        // --------------------------------------------------------
        // PROGRESS BAR
        // --------------------------------------------------------

        progressBar =
            ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {

                max = 100
                progress = 0
            }

        root.addView(
            progressBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // --------------------------------------------------------
        // PROGRESS TEXT
        // --------------------------------------------------------

        txtProgress =
            TextView(this).apply {

                text =
                    "Ready"

                textSize =
                    14f

                setTextColor(
                    Color.YELLOW
                )

                setPadding(
                    0,
                    8,
                    0,
                    8
                )
            }

        root.addView(txtProgress)

        // --------------------------------------------------------
        // LAST PROTOCOL
        // --------------------------------------------------------

        txtLastProtocol =
            TextView(this).apply {

                text =
                    "Detected protocol: ---"

                textSize =
                    15f

                setTextColor(
                    Color.CYAN
                )

                setPadding(
                    0,
                    0,
                    0,
                    10
                )
            }

        root.addView(
            txtLastProtocol
        )

        // --------------------------------------------------------
        // MAIN RESULT
        // --------------------------------------------------------

        txtResult =
            TextView(this).apply {

                text =
                    "No result yet."

                textSize =
                    15f

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    0,
                    8,
                    0,
                    12
                )
            }

        root.addView(
            txtResult
        )

        // --------------------------------------------------------
        // INFORMATION TITLE
        // --------------------------------------------------------

        val commandTitle =
            TextView(this).apply {

                text =
                    "ECU Information"

                textSize =
                    18f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setPadding(
                    0,
                    8,
                    0,
                    8
                )
            }

        root.addView(commandTitle)

        // --------------------------------------------------------
        // RESULT CONTAINER
        // --------------------------------------------------------

        commandResultContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    4,
                    4,
                    4,
                    20
                )
            }

        commandScrollView =
            ScrollView(this).apply {

                isFillViewport =
                    true

                addView(
                    commandResultContainer
                )
            }

        root.addView(
            commandScrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    // ============================================================
    // LOAD 46 PROTOCOLS
    // ============================================================

    private fun loadProtocolList() {

        val count =
            UnknownEcuScanner
                .getScanProfileCount()

        val items =
            mutableListOf<String>()

        items.add(
            "AUTO SCAN - Detect ECU Automatically"
        )

        for (index in 0 until count) {

            val code =
                UnknownEcuScanner
                    .getScanProfileCode(index)
                    .orEmpty()

            val name =
                UnknownEcuScanner
                    .getScanProfileName(index)
                    .orEmpty()

            items.add(
                "$index - $code - $name"
            )
        }

        val adapter =
            object : ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                items
            ) {

                override fun getView(
                    position: Int,
                    convertView: View?,
                    parent: android.view.ViewGroup
                ): View {

                    val view =
                        super.getView(
                            position,
                            convertView,
                            parent
                        )

                    val textView =
                        view as? TextView

                    textView?.setTextColor(
                        Color.BLACK
                    )

                    textView?.textSize =
                        14f

                    return view
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: android.view.ViewGroup
                ): View {

                    val view =
                        super.getDropDownView(
                            position,
                            convertView,
                            parent
                        )

                    val textView =
                        view as? TextView

                    textView?.setTextColor(
                        Color.WHITE
                    )

                    textView?.textSize =
                        14f

                    textView?.setPadding(
                        12,
                        12,
                        12,
                        12
                    )

                    return view
                }
            }

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        protocolSpinner.adapter =
            adapter

        protocolSpinner.setSelection(0)

        protocolSpinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (position == 0) {

                        txtSelectedProtocol.text =
                            "Selected: AUTO SCAN"

                        return
                    }

                    val profileIndex =
                        position - 1

                    val code =
                        UnknownEcuScanner
                            .getScanProfileCode(
                                profileIndex
                            )
                            .orEmpty()

                    val name =
                        UnknownEcuScanner
                            .getScanProfileName(
                                profileIndex
                            )
                            .orEmpty()

                    txtSelectedProtocol.text =
                        "Selected: $code - $name"
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {

                    txtSelectedProtocol.text =
                        "Selected: ---"
                }
            }
    }

    // ============================================================
    // CONNECT SELECTED PROTOCOL
    // ============================================================

    private fun connectSelectedProtocol() {

        if (scanning) {
            return
        }

        val selectedPosition =
            protocolSpinner.selectedItemPosition

        if (selectedPosition <= 0) {
            startUnknownEcuScan()
            return
        }

        val selectedIndex =
            selectedPosition - 1

        if (selectedIndex < 0) {
            return
        }

        val code =
            UnknownEcuScanner
                .getScanProfileCode(
                    selectedIndex
                )
                .orEmpty()

        val name =
            UnknownEcuScanner
                .getScanProfileName(
                    selectedIndex
                )
                .orEmpty()

        clearResults()

        setBusy(true)

        progressBar.progress =
            10

        txtProgress.text =
            "Configuring $code..."

        txtResult.text =
            "Connecting to selected protocol..."

        showEcuStatus(
            "در حال اتصال...",
            Color.RED
        )

        executor.execute {

            val result =
                try {

                    UnknownEcuScanner
                        .connectSelectedProfile(
                            selectedIndex
                        )

                } catch (
                    _: Exception
                ) {

                    null
                }

            runOnUiThread {

                setBusy(false)

                progressBar.progress =
                    100

                if (result == null) {

                    txtResult.text =
                        "Connection failed.\n\n" +
                                "Protocol: $code\n" +
                                "Name: $name\n\n" +
                                "No result returned."

                    txtProgress.text =
                        "Connection failed"

                    showStatus(
                        "Connection failed",
                        Color.RED
                    )

                    showEcuStatus(
                        "اتصال برقرار نشد",
                        Color.RED
                    )

                } else {

                    showProtocolResult(
                        result
                    )
                }
            }
        }
    }

    // ============================================================
    // AUTO SCAN
    // ============================================================

    private fun startUnknownEcuScan() {

        if (scanning) {
            return
        }

        clearResults()

        setBusy(true)

        showEcuStatus(
            "در حال اتصال...",
            Color.RED
        )

        progressBar.progress =
            0

        txtResult.text =
            "Starting unknown ECU scan..."

        txtProgress.text =
            "Searching ECU..."

        UnknownEcuScanner.setScanProgressListener {
                current,
                total,
                code,
                name ->

            runOnUiThread {

                txtProgress.text =
                    "Searching ECU...\n" +
                            "[$current / $total] $code - $name"

                progressBar.progress =
                    ((current.toFloat() / total.toFloat()) * 100)
                        .toInt()
            }
        }

        executor.execute {

            val protocolResult =
                try {

                    UnknownEcuScanner
                        .scanUnknownEcu(
                            maxCommands = 500
                        )
                        .lastOrNull()

                } catch (
                    _: Exception
                ) {

                    null
                }

            UnknownEcuScanner.setScanProgressListener(null)

            runOnUiThread {

                setBusy(false)

                progressBar.progress =
                    100

                if (
                    protocolResult == null ||
                    !protocolResult.success
                ) {

                    txtResult.text =
                        "ECU not detected.\n\n" +
                                "No supported ECU protocol was detected."

                    txtProgress.text =
                        "Scan finished"

                    showStatus(
                        "ECU not detected",
                        Color.RED
                    )

                    showEcuStatus(
                        "اتصال برقرار نشد",
                        Color.RED
                    )

                    EcuRuntimeState.setConnected(
                        false
                    )

                    txtLastProtocol.text =
                        "Detected protocol: ---"

                } else {

                    showProtocolResult(
                        protocolResult
                    )
                }
            }
        }
    }

    // ============================================================
    // SHOW PROTOCOL RESULT
    // ============================================================

    private fun showProtocolResult(
        result:
        UnknownEcuScanner.ProtocolResult
    ) {

        val profile =
            result.profile

        val fingerprint =
            result.fingerprint

        if (!result.success) {

            EcuRuntimeState.setConnected(
                false
            )

            txtResult.text =
                buildString {

                    appendLine(
                        "ECU not detected"
                    )

                    appendLine()

                    appendLine(
                        "Protocol: ${profile.code}"
                    )

                    appendLine(
                        "Name: ${profile.name}"
                    )

                    if (
                        result.response.isNotBlank()
                    ) {

                        appendLine()

                        appendLine(
                            "Response:"
                        )

                        appendLine(
                            result.response.trim()
                        )
                    }
                }

            txtProgress.text =
                "Connection failed"

            showStatus(
                "ECU not detected",
                Color.RED
            )

            showEcuStatus(
                "اتصال برقرار نشد",
                Color.RED
            )

            return
        }

        val protocol =
            profile.code

        val protocolName =
            profile.name

        val ecuAddress =
            fingerprint?.ecuAddress

        val ecuIdentifier =
            fingerprint?.ecuIdentifier

        val vin =
            fingerprint?.vin

        val confidence =
            fingerprint?.confidence ?: 0

        val responseIds =
            fingerprint?.responseIds
                ?: emptyList()

        val successfulCommands =
            fingerprint?.positiveCommands
                ?: emptyList()

        val softwareVersion =
            fingerprint?.softwareVersion

        val hardwareVersion =
            fingerprint?.hardwareVersion

        // --------------------------------------------------------
        // RUNTIME STATE
        // --------------------------------------------------------

        EcuRuntimeState.setUnknown(
            protocol = protocol,
            protocolName = protocolName
        )

        EcuRuntimeState.setUnknownDetails(
            ecuAddress = ecuAddress,
            ecuIdentifier = ecuIdentifier,
            vin = vin,
            softwareVersion = softwareVersion,
            hardwareVersion = hardwareVersion,
            confidence = confidence,
            responseIds = responseIds,
            successfulCommands = successfulCommands,
            rawResponse = result.response
        )

        EcuRuntimeState.setConnected(
            true
        )

        // --------------------------------------------------------
        // SHOW RESULT
        // --------------------------------------------------------

        displayRuntimeEcuState()

        // --------------------------------------------------------
        // SAVE SUCCESSFUL ECU PROFILE
        // --------------------------------------------------------

        val savedProfile =
            EcuConnectionProfile(
                id = UUID.randomUUID().toString(),

                source =
                    ConnectionSource
                        .activeSource
                        .name,

                protocolCode =
                    protocol,

                protocolName =
                    protocolName,

                scanType =
                    profile.scanType.name,

                elmProtocol =
                    profile.elmProtocol,

                baudRate =
                    profile.baudRate,

                canExtended =
                    profile.canExtended,

                requestId =
                    fingerprint?.requestId,

                responseIds =
                    responseIds,

                ecuAddress =
                    ecuAddress,

                ecuIdentifier =
                    ecuIdentifier,

                vin =
                    vin,

                softwareVersion =
                    softwareVersion,

                hardwareVersion =
                    hardwareVersion,

                successfulCommands =
                    successfulCommands,

                fingerprintResponses =
                    emptyMap(),

                elmVersion =
                    "",

                confidence =
                    confidence,

                createdAt =
                    System.currentTimeMillis(),

                lastSeen =
                    System.currentTimeMillis(),

                successCount =
                    1,

                failureCount =
                    0
            )

        EcuProfileStore.saveOrUpdate(
            this,
            savedProfile
        )
    }

    // ============================================================
    // RESTORE RUNTIME ECU STATE
    // ============================================================

    private fun restoreRuntimeEcuState() {

        if (
            EcuRuntimeState.mode !=
            EcuMode.UNKNOWN
        ) {
            return
        }

        if (
            !EcuRuntimeState.connected
        ) {
            return
        }

        if (
            EcuRuntimeState.detectedProtocol
                .isBlank()
        ) {
            return
        }

        displayRuntimeEcuState()
    }

    // ============================================================
    // DISPLAY SAVED RUNTIME ECU STATE
    // ============================================================

    private fun displayRuntimeEcuState() {

        val protocol =
            EcuRuntimeState.detectedProtocol

        val protocolName =
            EcuRuntimeState.detectedProtocolName

        val confidence =
            EcuRuntimeState.confidence

        val ecuAddress =
            EcuRuntimeState.ecuAddress

        val ecuIdentifier =
            EcuRuntimeState.ecuIdentifier

        val vin =
            EcuRuntimeState.vin

        val softwareVersion =
            EcuRuntimeState.softwareVersion

        val hardwareVersion =
            EcuRuntimeState.hardwareVersion

        val responseIds =
            EcuRuntimeState.responseIds

        val successfulCommands =
            EcuRuntimeState.successfulCommands

        val rawResponse =
            EcuRuntimeState.rawResponse

        // --------------------------------------------------------
        // LAST PROTOCOL
        // --------------------------------------------------------

        txtLastProtocol.text =
            if (
                protocolName.isNotBlank()
            ) {

                "Detected protocol: " +
                        "$protocol - $protocolName"

            } else {

                "Detected protocol: $protocol"
            }

        // --------------------------------------------------------
        // SELECTED PROTOCOL
        // --------------------------------------------------------

        txtSelectedProtocol.text =
            if (
                protocolName.isNotBlank()
            ) {

                "Selected: $protocol - $protocolName"

            } else {

                "Selected: $protocol"
            }

        // --------------------------------------------------------
        // MAIN RESULT
        // --------------------------------------------------------

        val resultText =
            buildString {

                appendLine(
                    "ECU CONNECTED"
                )

                appendLine()

                appendLine(
                    "Protocol: $protocol"
                )

                if (
                    protocolName.isNotBlank()
                ) {

                    appendLine(
                        "Name: $protocolName"
                    )
                }

                appendLine(
                    "Confidence: $confidence%"
                )

                if (
                    ecuAddress.isNotBlank()
                ) {

                    appendLine(
                        "ECU Address: $ecuAddress"
                    )
                }

                if (
                    ecuIdentifier.isNotBlank()
                ) {

                    appendLine(
                        "ECU Identifier: $ecuIdentifier"
                    )
                }

                if (
                    vin.isNotBlank()
                ) {

                    appendLine(
                        "VIN: $vin"
                    )
                }

                if (
                    softwareVersion.isNotBlank()
                ) {

                    appendLine(
                        "Software Version: $softwareVersion"
                    )
                }

                if (
                    hardwareVersion.isNotBlank()
                ) {

                    appendLine(
                        "Hardware Version: $hardwareVersion"
                    )
                }

                if (
                    responseIds.isNotEmpty()
                ) {

                    appendLine()

                    appendLine(
                        "Response IDs:"
                    )

                    responseIds.forEach { id ->

                        appendLine(
                            "  $id"
                        )
                    }
                }

                if (
                    successfulCommands.isNotEmpty()
                ) {

                    appendLine()

                    appendLine(
                        "Successful Commands:"
                    )

                    successfulCommands.forEach { command ->

                        appendLine(
                            "  $command"
                        )
                    }
                }

                if (
                    rawResponse.isNotBlank()
                ) {

                    appendLine()

                    appendLine(
                        "Response:"
                    )

                    appendLine(
                        rawResponse.trim()
                    )
                }
            }

        txtResult.text =
            resultText

        txtProgress.text =
            "ECU connected successfully"

        showStatus(
            "ECU connected",
            Color.GREEN
        )

        showEcuStatus(
            "ECU Connected",
            Color.GREEN
        )

        // --------------------------------------------------------
        // INFORMATION
        // --------------------------------------------------------

        clearResults()

        addInfoRow(
            "Protocol",
            protocol
        )

        if (
            protocolName.isNotBlank()
        ) {

            addInfoRow(
                "Protocol Name",
                protocolName
            )
        }

        addInfoRow(
            "Confidence",
            "$confidence%"
        )

        if (
            ecuAddress.isNotBlank()
        ) {

            addInfoRow(
                "ECU Address",
                ecuAddress
            )
        }

        if (
            ecuIdentifier.isNotBlank()
        ) {

            addInfoRow(
                "ECU Identifier",
                ecuIdentifier
            )
        }

        if (
            vin.isNotBlank()
        ) {

            addInfoRow(
                "VIN",
                vin
            )
        }

        if (
            softwareVersion.isNotBlank()
        ) {

            addInfoRow(
                "Software Version",
                softwareVersion
            )
        }

        if (
            hardwareVersion.isNotBlank()
        ) {

            addInfoRow(
                "Hardware Version",
                hardwareVersion
            )
        }

        if (
            responseIds.isNotEmpty()
        ) {

            addInfoRow(
                "Response IDs",
                responseIds.joinToString(", ")
            )
        }

        if (
            successfulCommands.isNotEmpty()
        ) {

            addInfoRow(
                "Successful Commands",
                successfulCommands.joinToString(", ")
            )
        }

        if (
            rawResponse.isNotBlank()
        ) {

            addInfoRow(
                "Raw Response",
                rawResponse.trim()
            )
        }
    }

    // ============================================================
    // ADD INFORMATION ROW
    // ============================================================

    private fun addInfoRow(
        title: String,
        value: String
    ) {

        val row =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    12,
                    10,
                    12,
                    10
                )

                setBackgroundColor(
                    Color.rgb(
                        35,
                        35,
                        35
                    )
                )
            }

        val titleView =
            TextView(this).apply {

                text =
                    title

                textSize =
                    15f

                setTextColor(
                    Color.CYAN
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

        row.addView(
            titleView
        )

        val valueView =
            TextView(this).apply {

                text =
                    value

                textSize =
                    14f

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    0,
                    5,
                    0,
                    0
                )
            }

        row.addView(
            valueView
        )

        commandResultContainer.addView(
            row
        )

        commandScrollView.post {

            commandScrollView.fullScroll(
                View.FOCUS_DOWN
            )
        }
    }

    // ============================================================
    // CLEAR RESULTS
    // ============================================================

    private fun clearResults() {

        commandResultContainer
            .removeAllViews()
    }

    // ============================================================
    // CONNECTION STATUS
    // ============================================================

    private fun updateConnectionStatus() {

        // --------------------------------------------------------
        // UNKNOWN ECU RUNTIME STATE
        // --------------------------------------------------------

        if (
            EcuRuntimeState.mode ==
            EcuMode.UNKNOWN &&
            EcuRuntimeState.connected
        ) {

            showEcuStatus(
                "ECU Connected",
                Color.GREEN
            )

            showStatus(
                "ECU connected",
                Color.GREEN
            )

            val protocol =
                EcuRuntimeState.detectedProtocol

            val name =
                EcuRuntimeState.detectedProtocolName

            if (
                protocol.isNotBlank()
            ) {

                txtLastProtocol.text =
                    if (
                        name.isNotBlank()
                    ) {

                        "Detected protocol: " +
                                "$protocol - $name"

                    } else {

                        "Detected protocol: $protocol"
                    }
            }

            return
        }

        // --------------------------------------------------------
        // NORMAL ELM CONNECTION STATUS
        // --------------------------------------------------------

        when (
            ConnectionSource.activeSource
        ) {

            ConnectionSource.Source.WIFI -> {

                if (
                    YadraConnectionManager.elmConnected
                ) {

                    showStatus(
                        "Wi-Fi ELM327 connected",
                        Color.GREEN
                    )

                } else {

                    showStatus(
                        "Wi-Fi ELM327 disconnected",
                        Color.RED
                    )
                }
            }

            ConnectionSource.Source.BLUETOOTH -> {

                if (
                    BluetoothConnectionManager.elmConnected
                ) {

                    showStatus(
                        "Bluetooth ELM327 connected",
                        Color.GREEN
                    )

                } else {

                    showStatus(
                        "Bluetooth ELM327 disconnected",
                        Color.RED
                    )
                }
            }

            ConnectionSource.Source.NONE -> {

                showStatus(
                    "No ELM327 connection",
                    Color.RED
                )
            }
        }

        // --------------------------------------------------------
        // ECU STATUS
        // --------------------------------------------------------

        if (
            EcuRuntimeState.connected
        ) {

            showEcuStatus(
                "ECU Connected",
                Color.GREEN
            )

        } else {

            showEcuStatus(
                "اتصال برقرار نیست",
                Color.RED
            )
        }

        // --------------------------------------------------------
        // LAST PROTOCOL
        // --------------------------------------------------------

        val protocol =
            EcuRuntimeState.detectedProtocol

        val name =
            EcuRuntimeState.detectedProtocolName

        if (
            protocol.isNotBlank()
        ) {

            txtLastProtocol.text =
                if (
                    name.isNotBlank()
                ) {

                    "Detected protocol: " +
                            "$protocol - $name"

                } else {

                    "Detected protocol: $protocol"
                }
        }
    }

    // ============================================================
    // CLEAR / DISCONNECT
    // ============================================================

    private fun disconnectCurrentTransport() {

        if (scanning) {
            return
        }

        EcuRuntimeState.clear()

        clearResults()

        progressBar.progress =
            0

        txtResult.text =
            "Runtime ECU state cleared."

        txtProgress.text =
            "Ready"

        txtLastProtocol.text =
            "Detected protocol: ---"

        txtSelectedProtocol.text =
            "Selected: ---"

        showEcuStatus(
            "اتصال برقرار نیست",
            Color.RED
        )

        updateConnectionStatus()
    }

    // ============================================================
    // STATUS
    // ============================================================

    private fun showStatus(
        text: String,
        color: Int
    ) {

        txtStatus.text =
            "Status: $text"

        txtStatus.setTextColor(
            color
        )
    }

    // ============================================================
    // ECU STATUS
    // ============================================================

    private fun showEcuStatus(
        text: String,
        color: Int
    ) {

        txtEcuStatus.text =
            "ECU: $text"

        txtEcuStatus.setTextColor(
            color
        )
    }

    // ============================================================
    // BUSY
    // ============================================================

    private fun setBusy(
        busy: Boolean
    ) {

        scanning = busy

        btnConnect.isEnabled =
            !busy

        btnScan.isEnabled =
            !busy

        btnDisconnect.isEnabled =
            !busy

        protocolSpinner.isEnabled =
            !busy

        progressBar.visibility =
            View.VISIBLE
    }
}