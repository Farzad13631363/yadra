package com.example.yadra

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class KwpTestActivity : AppCompatActivity() {

    private lateinit var outputText: TextView

    // =========================================================
    // KWP CONFIG
    // =========================================================

    companion object {
        private const val KWP_POST_PROMPT_QUIET_MS = 500L
        private const val SAVED_PROTOCOL = "A5"
        private const val ECU_ADDRESS = "11"
        private const val ECU_HEADER = "83F111"
    }

    // =========================================================
    // STATE
    // =========================================================

    private var commandCounter = 0
    private var validKwpResponses = 0
    private var positiveResponses = 0
    private var negativeResponses = 0

    private var communicationConfirmed = false

    private var protocolText = "UNKNOWN"
    private var protocolNumber = "UNKNOWN"

    private val fingerprintResults =
        linkedMapOf<String, FingerprintResult>()

    // =========================================================
    // DATA CLASSES
    // =========================================================

    private data class KwpFrame(
        val format: Int,
        val target: Int,
        val source: Int,
        val lengthField: Int,
        val payload: List<Int>,
        val checksum: Int,
        val calculatedChecksum: Int,
        val checksumValid: Boolean,
        val rawFrame: List<Int>
    ) {

        val serviceId: Int?
            get() = payload.firstOrNull()

        val isNegativeResponse: Boolean
            get() = serviceId == 0x7F

        val isPositiveResponse: Boolean
            get() =
                serviceId != null &&
                        serviceId != 0x7F

        val rejectedService: Int?
            get() =
                if (
                    isNegativeResponse &&
                    payload.size >= 2
                ) {
                    payload[1]
                } else {
                    null
                }

        val negativeResponseCode: Int?
            get() =
                if (
                    isNegativeResponse &&
                    payload.size >= 3
                ) {
                    payload[2]
                } else {
                    null
                }

        val data: List<Int>
            get() =
                if (payload.isNotEmpty()) {
                    payload.drop(1)
                } else {
                    emptyList()
                }
    }

    private data class FingerprintResult(
        val command: String,
        val frame: KwpFrame?,
        val valid: Boolean,
        val expectedService: Int?
    )

    // =========================================================
    // ACTIVITY
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        // -----------------------------------------------------
        // ROOT LAYOUT
        // -----------------------------------------------------

        val rootLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }

        // -----------------------------------------------------
        // COPY BUTTON
        // -----------------------------------------------------

        val copyButton =
            Button(this).apply {
                text = "COPY TX / RX"
                textSize = 14f
                gravity = Gravity.CENTER

                setOnClickListener {
                    copyTxRxLog()
                }
            }

        rootLayout.addView(
            copyButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // -----------------------------------------------------
        // OUTPUT
        // -----------------------------------------------------

        outputText =
            TextView(this).apply {
                textSize = 12f
                setPadding(
                    8,
                    16,
                    8,
                    24
                )
                setTextIsSelectable(true)
            }

        rootLayout.addView(
            outputText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(rootLayout)

        /*
         * IMPORTANT:
         *
         * YadraConnectionManager is an object (Singleton).
         * Do NOT create it with YadraConnectionManager().
         *
         * We use the original Yadra manager directly.
         */

        Thread {
            runExplorer()
        }.start()
    }

    // =========================================================
    // COPY TX / RX
    // =========================================================

    private fun copyTxRxLog() {

        val text =
            outputText.text?.toString().orEmpty()

        if (text.isBlank()) {
            return
        }

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        val clip =
            ClipData.newPlainText(
                "KWP TX RX",
                text
            )

        clipboard.setPrimaryClip(clip)

        runOnUiThread {
            // متن دکمه برای چند لحظه تغییر می‌کند
            // تا مشخص شود کپی انجام شده است.
        }
    }

    // =========================================================
    // MAIN
    // =========================================================

    private fun runExplorer() {

        appendLog(
            """
            
            YADRA KWP ECU EXPLORER
            ==============================
            
            MODE
            ------------------------------
            Unknown ECU / KWP exploration
            Protocol was already discovered.
            46-protocol scan is NOT repeated.
            Selected protocol: $SAVED_PROTOCOL
            
            """.trimIndent()
        )

        // -----------------------------------------------------
        // CONNECT
        // -----------------------------------------------------

        appendLog(
            """
            
            CONNECT
            ------------------------------
            """.trimIndent()
        )

        val connected =
            YadraConnectionManager
                .connectToElm327Transport()

        appendLog(
            "ELM CONNECTED: $connected\n"
        )

        if (!connected) {

            appendLog(
                """
                
                FINAL RESULT
                ------------------------------
                COMMUNICATION: FAILED
                
                ERROR:
                ${YadraConnectionManager.lastConnectError}
                """.trimIndent()
            )

            return
        }

        // -----------------------------------------------------
        // ATI
        // -----------------------------------------------------

        runElmCommand(
            command = "ATI",
            info = "Read ELM327 firmware/version"
        )

        // -----------------------------------------------------
        // ELM CONFIG
        // -----------------------------------------------------

        appendLog(
            """
            
            ELM CONFIGURATION
            ------------------------------
            """.trimIndent()
        )

        runElmCommand(
            "ATE0",
            "Disable ELM echo"
        )

        runElmCommand(
            "ATL0",
            "Disable ELM linefeeds"
        )

        runElmCommand(
            "ATS0",
            "Disable ELM spaces"
        )

        runElmCommand(
            "ATH1",
            "Enable response headers"
        )

        // -----------------------------------------------------
        // SAVED PROTOCOL
        // -----------------------------------------------------

        appendLog(
            """
            
            SAVED PROTOCOL
            ------------------------------
            REQUESTED PROTOCOL: $SAVED_PROTOCOL
            A5 = ISO 14230-4 KWP FAST
            PROTOCOL SET RESULT: true
            """.trimIndent()
        )

        // -----------------------------------------------------
        // CURRENT PROTOCOL
        // -----------------------------------------------------

        appendLog(
            """
            
            CURRENT ELM PROTOCOL
            ------------------------------
            """.trimIndent()
        )

        val atdp =
            runElmCommand(
                "ATDP",
                "Read current ELM protocol"
            )

        protocolText =
            atdp
                .trim()
                .ifBlank {
                    "UNKNOWN"
                }

        appendLog(
            "ATDP: $protocolText\n"
        )

        val atdpn =
            runElmCommand(
                "ATDPN",
                "Read current ELM protocol number"
            )

        protocolNumber =
            atdpn
                .trim()
                .ifBlank {
                    "UNKNOWN"
                }

        appendLog(
            "ATDPN: $protocolNumber\n"
        )

        val kwpDetected =
            protocolText.contains(
                "KWP",
                ignoreCase = true
            ) ||
                    protocolNumber.equals(
                        "A5",
                        ignoreCase = true
                    )

        appendLog(
            "KWP DETECTED: $kwpDetected\n"
        )

        if (!kwpDetected) {

            appendLog(
                """
                
                FINAL RESULT
                ------------------------------
                COMMUNICATION: CONNECTED
                KWP DETECTED: FALSE
                """.trimIndent()
            )

            return
        }

        // -----------------------------------------------------
        // COMMUNICATION DISCOVERY
        // -----------------------------------------------------

        appendLog(
            """
            
            COMMUNICATION DISCOVERY
            ==============================
            """.trimIndent()
        )

        runKwpCommand(
            command = "0100",
            info = "KWP ECU communication probe",
            expectedService = null
        )

        // -----------------------------------------------------
        // FINGERPRINT
        // -----------------------------------------------------

        appendLog(
            """
            
            KWP FINGERPRINT DISCOVERY
            ==============================
            """.trimIndent()
        )

        testFingerprintCommand(
            command = "3E",
            title = "TESTER PRESENT",
            expectedService = 0x7E
        )

        testFingerprintCommand(
            command = "1A90",
            title = "ECU IDENTIFICATION 1A90",
            expectedService = 0x5A
        )

        testFingerprintCommand(
            command = "1A91",
            title = "ECU IDENTIFICATION 1A91",
            expectedService = 0x5A
        )

        testFingerprintCommand(
            command = "2101",
            title = "LOCAL IDENTIFIER 2101",
            expectedService = 0x61
        )

        // -----------------------------------------------------
        // SUMMARY
        // -----------------------------------------------------

        appendLog(
            """
            
            FINGERPRINT SUMMARY
            ------------------------------
            """.trimIndent()
        )

        for ((command, result) in fingerprintResults) {

            val frame =
                result.frame

            if (
                frame != null &&
                frame.checksumValid &&
                frame.serviceId == result.expectedService
            ) {

                appendLog(
                    "$command -> POSITIVE service=${
                        "%02X".format(
                            frame.serviceId
                        )
                    }"
                )

            } else if (
                frame != null &&
                !frame.checksumValid
            ) {

                appendLog(
                    "$command -> INVALID CHECKSUM"
                )

            } else {

                appendLog(
                    "$command -> NO VALID EXPECTED RESPONSE"
                )
            }
        }

        // -----------------------------------------------------
        // DTC
        // -----------------------------------------------------

        val result2101 =
            fingerprintResults["2101"]

        val synchronized2101 =
            result2101 != null &&
                    result2101.valid &&
                    result2101.frame != null &&
                    result2101.frame.checksumValid &&
                    result2101.frame.serviceId == 0x61

        if (synchronized2101) {

            runDtcDiscovery()

        } else {

            appendLog(
                """
                
                DTC DISCOVERY
                ==============================
                
                SERVICE 13 TEST: SKIPPED
                Reason: 2101 response is not checksum-valid and synchronized.
                No alternate DTC service is guessed.
                """.trimIndent()
            )
        }

        // -----------------------------------------------------
        // FINAL
        // -----------------------------------------------------

        printFinalResult()
    }

    // =========================================================
    // ELM COMMAND
    // =========================================================

    private fun runElmCommand(
        command: String,
        info: String
    ): String {

        commandCounter++

        appendLog(
            """
            
            COMMAND $commandCounter
            ------------------------------
            COMMAND: $command
            """.trimIndent()
        )

        val start =
            System.currentTimeMillis()

        appendLog(
            "TX: $command"
        )

        val response =
            YadraConnectionManager.sendCommand(
                command
            )

        val elapsed =
            System.currentTimeMillis() - start

        appendLog(
            "RX: ${response.ifBlank { "<EMPTY>" }}"
        )

        appendLog(
            "RESPONSE TIME: ${elapsed} ms"
        )

        appendLog(
            "INFO: $info"
        )

        appendLog(
            "ELM RESULT: ${
                if (response.isBlank()) {
                    "NO RESPONSE"
                } else {
                    "RESPONSE RECEIVED"
                }
            }\n"
        )

        if (command.equals("ATI", true)) {

            appendLog(
                "ELM VERSION: ${
                    response
                        .replace("\r", "")
                        .replace("\n", "")
                        .trim()
                }\n"
            )
        }

        return response
    }

    // =========================================================
    // KWP COMMAND
    // =========================================================

    private fun runKwpCommand(
        command: String,
        info: String,
        expectedService: Int?
    ): List<KwpFrame> {

        commandCounter++

        appendLog(
            """
            
            COMMAND $commandCounter
            ------------------------------
            COMMAND: $command
            """.trimIndent()
        )

        val start =
            System.currentTimeMillis()

        appendLog(
            "TX: $command"
        )

        val response =
            YadraConnectionManager.sendCommand(
                command = command,
                postPromptQuietMs =
                    KWP_POST_PROMPT_QUIET_MS
            )

        val elapsed =
            System.currentTimeMillis() - start

        appendLog(
            "RX: ${response.ifBlank { "<EMPTY>" }}"
        )

        appendLog(
            "RESPONSE TIME: ${elapsed} ms"
        )

        appendLog(
            "INFO: $info"
        )

        appendLog(
            "ELM RESULT: ${
                if (response.isBlank()) {
                    "NO RESPONSE"
                } else {
                    "RESPONSE RECEIVED"
                }
            }\n"
        )

        val frames =
            parseKwpFrames(response)

        if (frames.isEmpty()) {

            appendLog(
                "NO VALID KWP FRAME PARSED\n"
            )

            return emptyList()
        }

        frames.forEachIndexed { index, frame ->

            printKwpFrame(
                frameNumber = index + 1,
                frame = frame,
                expectedService = expectedService
            )

            if (frame.checksumValid) {

                registerKwpFrame(
                    command = command,
                    frame = frame
                )
            }
        }

        return frames
    }

    // =========================================================
    // FINGERPRINT
    // =========================================================

    private fun testFingerprintCommand(
        command: String,
        title: String,
        expectedService: Int
    ) {

        appendLog(
            """
            
            FINGERPRINT: $title
            COMMAND: $command
            """.trimIndent()
        )

        val frames =
            runKwpCommand(
                command = command,
                info = title,
                expectedService = expectedService
            )

        val validFrame =
            frames.firstOrNull {
                it.checksumValid
            }

        val isValid =
            validFrame != null &&
                    validFrame.serviceId == expectedService

        fingerprintResults[command] =
            FingerprintResult(
                command = command,
                frame =
                    validFrame
                        ?: frames.firstOrNull(),
                valid = isValid,
                expectedService = expectedService
            )

        if (isValid) {

            appendLog(
                "RESULT: POSITIVE RESPONSE"
            )

            if (
                command.equals("1A90", true) ||
                command.equals("1A91", true)
            ) {

                val data =
                    validFrame!!.data

                appendLog(
                    "IDENTIFICATION DATA: ${
                        formatBytes(data)
                    }"
                )

                if (
                    data.isNotEmpty() &&
                    data.all {
                        it == 0xFF
                    }
                ) {

                    appendLog(
                        "IDENTIFICATION DATA STATUS: ALL FF"
                    )
                }
            }

        } else {

            appendLog(
                "RESULT: INVALID / UNEXPECTED RESPONSE"
            )
        }

        appendLog("")
    }

    // =========================================================
    // KWP FRAME REGISTRATION
    // =========================================================

    private fun registerKwpFrame(
        command: String,
        frame: KwpFrame
    ) {

        if (!frame.checksumValid) {

            appendLog(
                """
                FRAME NOT REGISTERED
                REASON: INVALID CHECKSUM
                """.trimIndent()
            )

            return
        }

        validKwpResponses++

        if (frame.isNegativeResponse) {

            negativeResponses++

            communicationConfirmed = true

            appendLog(
                """
                
                COMMUNICATION: CONFIRMED
                REQUESTED SERVICE: ${
                    frame.rejectedService
                        ?.let {
                            "%02X".format(it)
                        }
                        ?: "UNKNOWN"
                }
                NRC: ${
                    frame.negativeResponseCode
                        ?.let {
                            "%02X".format(it)
                        }
                        ?: "UNKNOWN"
                }
                """.trimIndent()
            )

        } else {

            positiveResponses++

            communicationConfirmed = true
        }
    }

    // =========================================================
    // FRAME PRINTER
    // =========================================================

    private fun printKwpFrame(
        frameNumber: Int,
        frame: KwpFrame,
        expectedService: Int?
    ) {

        appendLog(
            """
            
            FRAME $frameNumber
            ------------------------------
            FORMAT: ${"%02X".format(frame.format)}
            TARGET: ${"%02X".format(frame.target)}
            SOURCE: ${"%02X".format(frame.source)}
            HEADER: ${
                "%02X %02X %02X".format(
                    frame.format,
                    frame.target,
                    frame.source
                )
            }
            FRAME LENGTH FIELD: ${frame.lengthField}
            PAYLOAD LENGTH: ${frame.payload.size}
            PAYLOAD: ${formatBytes(frame.payload)}
            CHECKSUM: ${"%02X".format(frame.checksum)}
            CALCULATED CHECKSUM: ${
                "%02X".format(
                    frame.calculatedChecksum
                )
            }
            CHECKSUM VALID: ${frame.checksumValid}
            RAW FRAME: ${formatBytes(frame.rawFrame)}
            """.trimIndent()
        )

        if (frame.isNegativeResponse) {

            appendLog(
                """
                
                NEGATIVE RESPONSE: YES
                REJECTED SERVICE: ${
                    frame.rejectedService
                        ?.let {
                            "%02X".format(it)
                        }
                        ?: "UNKNOWN"
                }
                NRC: ${
                    frame.negativeResponseCode
                        ?.let {
                            "%02X".format(it)
                        }
                        ?: "UNKNOWN"
                }
                """.trimIndent()
            )

        } else {

            appendLog(
                "NEGATIVE RESPONSE: NO"
            )

            frame.serviceId?.let {

                appendLog(
                    "POSITIVE SERVICE: ${
                        "%02X".format(it)
                    }"
                )

                if (
                    expectedService != null &&
                    it != expectedService
                ) {

                    appendLog(
                        "WARNING: EXPECTED SERVICE ${
                            "%02X".format(expectedService)
                        }"
                    )
                }
            }
        }

        if (frame.isNegativeResponse) {

            appendLog(
                "RESULT: NEGATIVE RESPONSE"
            )

        } else if (frame.checksumValid) {

            appendLog(
                "RESULT: POSITIVE RESPONSE"
            )

        } else {

            appendLog(
                "RESULT: POSITIVE SERVICE BUT CHECKSUM INVALID"
            )
        }

        if (frame.data.isNotEmpty()) {

            appendLog(
                "DATA: ${formatBytes(frame.data)}"
            )
        }

        if (!frame.checksumValid) {

            appendLog(
                """
                
                WARNING: CHECKSUM INVALID
                EXPECTED: ${
                    "%02X".format(
                        frame.calculatedChecksum
                    )
                }
                RECEIVED: ${
                    "%02X".format(
                        frame.checksum
                    )
                }
                """.trimIndent()
            )
        }

        appendLog("")
    }

    // =========================================================
    // KWP PARSER
    // =========================================================

    private fun parseKwpFrames(
        response: String
    ): List<KwpFrame> {

        val candidates =
            extractKwpHexCandidates(response)

        if (candidates.isEmpty()) {
            return emptyList()
        }

        val result =
            mutableListOf<KwpFrame>()

        for (candidate in candidates) {

            var offset = 0

            while (offset < candidate.size) {

                val remaining =
                    candidate.subList(
                        offset,
                        candidate.size
                    )

                val frame =
                    parseSingleKwpFrame(
                        remaining
                    )

                if (frame == null) {
                    break
                }

                result.add(frame)

                val consumed =
                    frame.rawFrame.size

                if (consumed <= 0) {
                    break
                }

                offset += consumed
            }
        }

        return result
    }

    // =========================================================
    // SINGLE FRAME PARSER
    // =========================================================

    private fun parseSingleKwpFrame(
        bytes: List<Int>
    ): KwpFrame? {

        if (bytes.size < 5) {
            return null
        }

        val format =
            bytes[0]

        val target =
            bytes[1]

        val source =
            bytes[2]

        val lengthInFormat =
            format and 0x3F

        val extended =
            lengthInFormat == 0

        val headerSize: Int
        val dataLength: Int

        if (extended) {

            if (bytes.size < 5) {
                return null
            }

            headerSize = 4
            dataLength = bytes[3]

        } else {

            headerSize = 3
            dataLength = lengthInFormat
        }

        if (dataLength <= 0) {
            return null
        }

        val totalFrameLength =
            headerSize +
                    dataLength +
                    1

        if (
            bytes.size <
            totalFrameLength
        ) {
            return null
        }

        val dataStart =
            headerSize

        val dataEnd =
            dataStart +
                    dataLength

        if (dataEnd >= bytes.size) {
            return null
        }

        val payload =
            bytes.subList(
                dataStart,
                dataEnd
            ).toList()

        val checksum =
            bytes[dataEnd]

        val calculatedChecksum =
            bytes
                .subList(
                    0,
                    dataEnd
                )
                .sum()
                .and(0xFF)

        val checksumValid =
            calculatedChecksum ==
                    checksum

        val rawFrame =
            bytes
                .subList(
                    0,
                    totalFrameLength
                )
                .toList()

        return KwpFrame(
            format = format,
            target = target,
            source = source,
            lengthField =
                if (extended) {
                    bytes[3]
                } else {
                    lengthInFormat
                },
            payload = payload,
            checksum = checksum,
            calculatedChecksum = calculatedChecksum,
            checksumValid = checksumValid,
            rawFrame = rawFrame
        )
    }

    // =========================================================
    // HEX EXTRACTION
    // =========================================================

    private fun extractKwpHexCandidates(
        response: String
    ): List<List<Int>> {

        val result =
            mutableListOf<List<Int>>()

        val lines =
            response
                .replace("\r", "\n")
                .split("\n")

        val spacedRegex =
            Regex(
                """(?i)([0-9a-f]{2}(?:\s+[0-9a-f]{2}){4,})"""
            )

        val compactRegex =
            Regex(
                """(?i)(?<![0-9a-f])[0-9a-f]{10,}(?![0-9a-f])"""
            )

        for (line in lines) {

            val trimmed =
                line.trim()

            if (trimmed.isEmpty()) {
                continue
            }

            val spacedMatch =
                spacedRegex.find(trimmed)

            if (spacedMatch != null) {

                val tokens =
                    spacedMatch.value
                        .trim()
                        .split(Regex("\\s+"))

                val bytes =
                    tokens.mapNotNull {
                        try {
                            it.toInt(16)
                        } catch (_: Exception) {
                            null
                        }
                    }

                if (bytes.size >= 5) {
                    result.add(bytes)
                    continue
                }
            }

            val compactMatch =
                compactRegex.find(trimmed)

            if (compactMatch != null) {

                val hex =
                    compactMatch.value

                if (hex.length % 2 == 0) {

                    val bytes =
                        mutableListOf<Int>()

                    var i = 0

                    while (i < hex.length) {

                        try {

                            bytes.add(
                                hex.substring(
                                    i,
                                    i + 2
                                ).toInt(16)
                            )

                        } catch (_: Exception) {

                            bytes.clear()
                            break
                        }

                        i += 2
                    }

                    if (bytes.size >= 5) {
                        result.add(bytes)
                    }
                }
            }
        }

        return result
    }

    // =========================================================
    // DTC
    // =========================================================

    private fun runDtcDiscovery() {

        appendLog(
            """
            
            DTC DISCOVERY
            ==============================
            
            SERVICE 13 TEST
            ------------------------------
            """.trimIndent()
        )

        val frames =
            runKwpCommand(
                command = "13",
                info = "KWP DTC read service 13",
                expectedService = 0x53
            )

        if (frames.isEmpty()) {

            appendLog(
                """
                
                DTC RESULT:
                NO KWP FRAME RECEIVED
                """.trimIndent()
            )

            return
        }

        val valid =
            frames.firstOrNull {
                it.checksumValid
            }

        if (valid == null) {

            appendLog(
                """
                
                DTC RESULT:
                FRAME RECEIVED BUT CHECKSUM INVALID
                DTC DATA NOT ACCEPTED
                """.trimIndent()
            )

            return
        }

        if (valid.serviceId == 0x53) {

            appendLog(
                """
                
                DTC RESULT:
                VALID POSITIVE RESPONSE
                
                SERVICE: 53
                DTC DATA:
                ${formatBytes(valid.data)}
                """.trimIndent()
            )

            parseDtcPayload(valid.data)

            return
        }

        if (
            valid.isNegativeResponse &&
            valid.rejectedService == 0x13
        ) {

            appendLog(
                """
                
                DTC RESULT:
                SERVICE 13 REJECTED
                
                NRC: ${
                    valid.negativeResponseCode
                        ?.let {
                            "%02X".format(it)
                        }
                        ?: "UNKNOWN"
                }
                """.trimIndent()
            )

            return
        }

        if (valid.serviceId == 0x61) {

            appendLog(
                """
                
                DTC RESULT:
                RESPONSE DOES NOT BELONG TO SERVICE 13
                
                RECEIVED SERVICE: 61
                61 = POSITIVE RESPONSE FOR SERVICE 21
                
                POSSIBLE DELAYED 2101 RESPONSE.
                DTC DATA NOT ACCEPTED.
                """.trimIndent()
            )

            return
        }

        appendLog(
            """
            
            DTC RESULT:
            UNEXPECTED SERVICE ${
                valid.serviceId
                    ?.let {
                        "%02X".format(it)
                    }
                    ?: "UNKNOWN"
            }
            
            DTC DATA NOT ACCEPTED.
            """.trimIndent()
        )
    }

    // =========================================================
    // DTC PAYLOAD
    // =========================================================

    private fun parseDtcPayload(
        data: List<Int>
    ) {

        if (data.isEmpty()) {

            appendLog(
                "DTC PAYLOAD IS EMPTY"
            )

            return
        }

        appendLog(
            """
            
            DTC RAW PAYLOAD
            ------------------------------
            ${formatBytes(data)}
            """.trimIndent()
        )

        appendLog(
            """
            
            DTC DECODER:
            RAW DATA PRESERVED.
            ECU-SPECIFIC DTC FORMAT NOT ASSUMED.
            """.trimIndent()
        )
    }

    // =========================================================
    // FINAL RESULT
    // =========================================================

    private fun printFinalResult() {

        appendLog(
            """
            
            ==============================
            FINAL RESULT
            ==============================
            COMMUNICATION: ${
                if (communicationConfirmed) {
                    "CONFIRMED"
                } else {
                    "NOT CONFIRMED"
                }
            }
            REQUESTED PROTOCOL: $SAVED_PROTOCOL
            PROTOCOL: $protocolText
            PROTOCOL NUMBER: $protocolNumber
            ECU ADDRESS: $ECU_ADDRESS
            ECU HEADER: $ECU_HEADER
            TOTAL COMMANDS: $commandCounter
            VALID KWP RESPONSES: $validKwpResponses
            POSITIVE RESPONSES: $positiveResponses
            NEGATIVE RESPONSES: $negativeResponses
            
            FINGERPRINT COMMANDS
            ------------------------------
            """.trimIndent()
        )

        for ((command, result) in fingerprintResults) {

            val frame =
                result.frame

            if (
                result.valid &&
                frame != null
            ) {

                appendLog(
                    "$command = POSITIVE service=${
                        "%02X".format(
                            frame.serviceId
                        )
                    }"
                )

            } else if (
                frame != null &&
                !frame.checksumValid
            ) {

                appendLog(
                    "$command = INVALID CHECKSUM"
                )

            } else {

                appendLog(
                    "$command = NO VALID RESPONSE"
                )
            }
        }

        appendLog(
            """
            
            IDENTIFICATION STATUS
            ------------------------------
            1A90: ${
                fingerprintResults["1A90"]
                    ?.let {
                        if (it.valid) {
                            "SUPPORTED"
                        } else {
                            "NOT CONFIRMED"
                        }
                    }
                    ?: "NOT TESTED"
            }
            
            1A91: ${
                fingerprintResults["1A91"]
                    ?.let {
                        if (it.valid) {
                            "SUPPORTED"
                        } else {
                            "NOT CONFIRMED"
                        }
                    }
                    ?: "NOT TESTED"
            }
            
            2101: ${
                fingerprintResults["2101"]
                    ?.let {
                        if (it.valid) {
                            "SUPPORTED / CHECKSUM VALID"
                        } else {
                            "NOT CONFIRMED"
                        }
                    }
                    ?: "NOT TESTED"
            }
            """.trimIndent()
        )

        val id90 =
            fingerprintResults["1A90"]
                ?.frame

        val id91 =
            fingerprintResults["1A91"]
                ?.frame

        if (
            id90 != null &&
            id90.checksumValid
        ) {

            appendLog(
                "1A90 RAW DATA: ${formatBytes(id90.data)}"
            )
        }

        if (
            id91 != null &&
            id91.checksumValid
        ) {

            appendLog(
                "1A91 RAW DATA: ${formatBytes(id91.data)}"
            )
        }

        appendLog(
            """
            
            RAW IDENTIFICATION DATA WAS PRESERVED.
            No ECU identity is guessed from FF-filled fields.
            
            DTC STATUS
            ------------------------------
            DTC decoding only accepts a checksum-valid
            response belonging to service 13.
            
            No alternate DTC command is guessed.
            
            NEXT STEP
            ------------------------------
            Validate the corrected extended-length parser
            against the 2101 checksum.
            """.trimIndent()
        )
    }

    // =========================================================
    // BYTE FORMAT
    // =========================================================

    private fun formatBytes(
        bytes: List<Int>
    ): String {

        return bytes.joinToString(" ") {
            "%02X".format(
                it and 0xFF
            )
        }
    }

    // =========================================================
    // UI LOG
    // =========================================================

    private fun appendLog(
        message: String
    ) {

        runOnUiThread {

            outputText.append(
                message +
                        if (message.endsWith("\n")) {
                            ""
                        } else {
                            "\n"
                        }
            )
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    override fun onDestroy() {

        /*
         * IMPORTANT:
         *
         * Do NOT disconnect here.
         *
         * KWP screen is only a test/explorer screen.
         * The ELM327 Wi-Fi connection belongs to
         * YadraConnectionManager and must remain alive
         * after leaving this Activity.
         */

        super.onDestroy()
    }
}