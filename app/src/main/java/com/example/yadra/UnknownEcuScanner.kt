package com.example.yadra

import android.content.Context
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Unknown ECU Discovery
 *
 * هدف:
 *  - پیدا کردن پروتکل ECU ناشناس
 *  - مخصوصاً KWP2000 / ISO 14230-4 FAST
 *  - ذخیره raw request / response
 *  - تشخیص positive / negative response
 *  - اعتبارسنجی KWP checksum
 *  - Discovery کنترل شده برای 1Axx و 21xx
 *
 * نکته:
 * این کلاس نباید منطق EcuTestActivity یا SensorsActivity را تغییر دهد.
 */
object UnknownEcuScanner {

    // ============================================================
    // TYPES
    // ============================================================

    enum class DiscoveryProtocol {
        OBD2,
        UDS_CAN_11,
        UDS_CAN_29,
        UDS_EXTENDED_ADDRESSING,
        KWP2000,
        ISO9141,
        J1939,
        GENERIC_CAN,
        GENERIC_ISOTP
    }

    enum class ScanType {
        ELM_STANDARD,
        CAN_USER
    }

    data class EcuFingerprint(
        val protocol: DiscoveryProtocol,
        val positiveCommands: List<String> = emptyList(),
        val responseIds: List<String> = emptyList(),
        val requestId: String = "",
        val ecuAddress: String = "",
        val ecuIdentifier: String = "",
        val vin: String = "",
        val softwareVersion: String = "",
        val hardwareVersion: String = "",
        val rawResponses: Map<String, String> = emptyMap(),
        val confidence: Int = 0
    )

    data class DiscoveryResult(
        val success: Boolean,
        val fingerprint: EcuFingerprint? = null,
        val protocolResults: List<ProtocolResult> = emptyList(),
        val commandsUsed: Int = 0
    )

    data class ProtocolResult(
        val success: Boolean,
        val profile: ScanProfile,
        val response: String = "",
        val fingerprint: EcuFingerprint? = null
    )

    data class UnknownEcuCommandResult(
        val command: String,
        val response: String,
        val positive: Boolean,
        val negative: Boolean,
        val nrc: String = "",
        val service: String = "",
        val checksumValid: Boolean? = null,
        val payloadLength: Int = 0
    )

    data class UnknownEcuIdentification(
        val ecuIdentifier: String = "",
        val vin: String = "",
        val softwareVersion: String = "",
        val hardwareVersion: String = ""
    )

    data class ErrorScanResult(
        val command: String,
        val error: String
    )

    data class SavedProtocolInfo(
        val code: String,
        val name: String,
        val elmProtocol: String = "",
        val baudRate: Int = 0
    )

    data class ScanProfile(
        val code: String,
        val name: String,
        val scanType: ScanType = ScanType.ELM_STANDARD,
        val elmProtocol: String = "",
        val baudRate: Int = 0,
        val canExtended: Boolean = false,
        val protocol: DiscoveryProtocol = DiscoveryProtocol.GENERIC_CAN
    )

    // ============================================================
    // STATE
    // ============================================================

    private var appContext: Context? = null

    private var progressListener:
            ((Int, Int, String, String) -> Unit)? = null

    private val commandCounter =
        java.util.concurrent.atomic.AtomicInteger(0)

    private val stopRequested =
        AtomicBoolean(false)

    private val scanProfiles =
        buildScanProfiles()

    /*
     * Raw result database for the current scan.
     *
     * command -> list of raw responses
     */
    private val rawDatabase =
        linkedMapOf<String, MutableList<String>>()

    private val commandResults =
        mutableListOf<UnknownEcuCommandResult>()

    // ============================================================
    // KWP CONSTANTS
    // ============================================================

    private const val KWP_POSITIVE_OFFSET = 0x40

    private const val KWP_SERVICE_TESTER_PRESENT = 0x3E
    private const val KWP_SERVICE_READ_IDENTIFICATION = 0x1A
    private const val KWP_SERVICE_READ_DATA_BY_LOCAL_ID = 0x21
    private const val KWP_SERVICE_READ_DTC = 0x13

    private const val KWP_RESPONSE_NEGATIVE = 0x7F

    /*
     * Conservative KWP Local Identifier list.
     *
     * 0x90 / 0x91 were already observed on the target ECU.
     */
    private val kwpIdentificationIds =
        listOf(
            0x90,
            0x91
        )

    /*
     * Conservative 21xx list.
     *
     * 01 is the known working identifier from the supplied ECU.
     *
     * A few additional low-risk identifiers can be tested.
     * This is deliberately NOT 00..FF brute force.
     */
    private val kwpDataIdentifiers =
        listOf(
            0x01,
            0x02,
            0x03,
            0x04,
            0x05,
            0x06,
            0x07,
            0x08,
            0x09,
            0x0A
        )

    // ============================================================
    // INITIALIZATION
    // ============================================================

    fun initialize(
        context: Context
    ) {
        appContext =
            context.applicationContext
    }

    fun setScanProgressListener(
        listener:
        ((Int, Int, String, String) -> Unit)?
    ) {
        progressListener = listener
    }

    // ============================================================
    // PROFILE API
    // ============================================================

    fun getScanProfileCount(): Int =
        scanProfiles.size

    fun getScanProfileCode(
        index: Int
    ): String? =
        scanProfiles
            .getOrNull(index)
            ?.code

    fun getScanProfileName(
        index: Int
    ): String? =
        scanProfiles
            .getOrNull(index)
            ?.name

    // ============================================================
    // MAIN SCAN
    // ============================================================

    fun scanUnknownEcu(
        maxCommands: Int = 500
    ): List<ProtocolResult> {

        stopRequested.set(false)

        commandCounter.set(0)

        rawDatabase.clear()
        commandResults.clear()

        val results =
            mutableListOf<ProtocolResult>()

        /*
         * First try the protocol currently reported by ELM.
         *
         * This is especially important for KWP FAST.
         */
        val autoResult =
            try {
                scanAutoProtocol(
                    maxCommands
                )
            } catch (_: Exception) {
                null
            }

        if (
            autoResult != null &&
            autoResult.success
        ) {

            results.add(
                autoResult
            )

            saveSuccessfulLearnedProfile(
                autoResult
            )

            return results
        }

        /*
         * Then test explicit profiles.
         */
        for (
        index in scanProfiles.indices
        ) {

            if (
                stopRequested.get()
            ) {
                break
            }

            if (
                commandCounter.get() >=
                maxCommands
            ) {
                break
            }

            val profile =
                scanProfiles[index]

            notifyProgress(
                current = index + 1,
                total = scanProfiles.size,
                code = profile.code,
                name = profile.name
            )

            val result =
                try {
                    probeProfile(
                        profile = profile,
                        maxCommands = maxCommands
                    )
                } catch (_: Exception) {
                    ProtocolResult(
                        success = false,
                        profile = profile
                    )
                }

            results.add(
                result
            )

            if (
                result.success
            ) {

                saveSuccessfulLearnedProfile(
                    result
                )

                /*
                 * Stop on the first validated protocol.
                 */
                break
            }
        }

        return results
    }

    // ============================================================
    // AUTO PROTOCOL
    // ============================================================

    private fun scanAutoProtocol(
        maxCommands: Int
    ): ProtocolResult? {

        val source =
            ConnectionSource.activeSource

        if (
            source ==
            ConnectionSource.Source.NONE
        ) {
            return null
        }

        if (
            !isTransportConnected()
        ) {
            return null
        }

        /*
         * ELM initialization.
         *
         * ATZ is intentionally not always sent here because resetting
         * an ELM can destroy an already established KWP session.
         */
        sendSafe("ATE0", maxCommands)
        sendSafe("ATL0", maxCommands)
        sendSafe("ATS0", maxCommands)
        sendSafe("ATH1", maxCommands)
        sendSafe("ATAT1", maxCommands)
        sendSafe("ATST32", maxCommands)

        val protocolResponse =
            sendSafe(
                "ATDP",
                maxCommands
            )

        val protocolNumber =
            sendSafe(
                "ATDPN",
                maxCommands
            )

        /*
         * If ELM explicitly reports A5 / ISO 14230-4 KWP FAST,
         * immediately perform KWP discovery.
         */
        if (
            isKwpFastResponse(
                protocolResponse
            ) ||
            isKwpFastResponse(
                protocolNumber
            )
        ) {

            val profile =
                ScanProfile(
                    code = "KWPFAST-AUTO",
                    name = "ISO 14230-4 KWP FAST (AUTO)",
                    scanType = ScanType.ELM_STANDARD,
                    elmProtocol = "5",
                    baudRate = 10400,
                    canExtended = false,
                    protocol = DiscoveryProtocol.KWP2000
                )

            val result =
                discoverKwp(
                    profile = profile,
                    maxCommands = maxCommands
                )

            if (
                result.success
            ) {
                return result
            }
        }

        /*
         * Even if ATDP is not reliable, try the KWP FAST profile
         * because some ELM clones report AUTO inconsistently.
         */
        val kwpProfile =
            ScanProfile(
                code = "KWPFAST-AUTO",
                name = "ISO 14230-4 KWP FAST (AUTO)",
                scanType = ScanType.ELM_STANDARD,
                elmProtocol = "5",
                baudRate = 10400,
                canExtended = false,
                protocol = DiscoveryProtocol.KWP2000
            )

        val kwpResult =
            try {
                discoverKwp(
                    profile = kwpProfile,
                    maxCommands = maxCommands
                )
            } catch (_: Exception) {
                null
            }

        if (
            kwpResult?.success == true
        ) {
            return kwpResult
        }

        /*
         * Standard OBD / UDS discovery.
         */
        val standardCommands =
            listOf(
                "0100",
                "0101",
                "0104",
                "0105",
                "010C",
                "010D",
                "010F",
                "0110",
                "0111",
                "0120",
                "0140",
                "0160",
                "0180",
                "0142",
                "0902",
                "22F190",
                "3E00",
                "1001",
                "1003",
                "03",
                "07"
            )

        val successful =
            mutableListOf<String>()

        val responses =
            linkedMapOf<String, String>()

        for (
        command in standardCommands
        ) {

            if (
                commandCounter.get() >=
                maxCommands
            ) {
                break
            }

            val response =
                sendSafe(
                    command,
                    maxCommands
                )

            if (
                response.isBlank()
            ) {
                continue
            }

            responses[command] =
                response

            val analysis =
                analyzeGenericResponse(
                    command,
                    response
                )

            if (
                analysis.positive
            ) {
                successful.add(command)
            }

            if (
                analysis.positive
            ) {

                val fingerprint =
                    buildFingerprint(
                        protocol =
                            when {
                                command.startsWith(
                                    "22",
                                    true
                                ) ->
                                    DiscoveryProtocol.UDS_CAN_11

                                command.startsWith(
                                    "09",
                                    true
                                ) ->
                                    DiscoveryProtocol.OBD2

                                else ->
                                    DiscoveryProtocol.OBD2
                            },
                        successfulCommands =
                            successful,
                        rawResponses =
                            responses
                    )

                return ProtocolResult(
                    success = true,
                    profile =
                        ScanProfile(
                            code = "AUTO",
                            name = "ELM AUTO",
                            scanType =
                                ScanType.ELM_STANDARD,
                            elmProtocol =
                                protocolNumber
                                    .trim()
                                    .ifBlank {
                                        ""
                                    },
                            protocol =
                                fingerprint.protocol
                        ),
                    response = response,
                    fingerprint = fingerprint
                )
            }
        }

        return null
    }

    // ============================================================
    // PROFILE PROBE
    // ============================================================

    private fun probeProfile(
        profile: ScanProfile,
        maxCommands: Int
    ): ProtocolResult {

        if (
            !configureProfile(
                profile
            )
        ) {

            return ProtocolResult(
                success = false,
                profile = profile
            )
        }

        return when (
            profile.protocol
        ) {

            DiscoveryProtocol.KWP2000 ->
                discoverKwp(
                    profile,
                    maxCommands
                )

            DiscoveryProtocol.ISO9141 ->
                discoverIso9141(
                    profile,
                    maxCommands
                )

            DiscoveryProtocol.UDS_CAN_11,
            DiscoveryProtocol.UDS_CAN_29,
            DiscoveryProtocol.UDS_EXTENDED_ADDRESSING ->
                discoverUds(
                    profile,
                    maxCommands
                )

            DiscoveryProtocol.J1939 ->
                discoverJ1939(
                    profile,
                    maxCommands
                )

            else ->
                discoverObd(
                    profile,
                    maxCommands
                )
        }
    }

    // ============================================================
    // PROFILE CONFIGURATION
    // ============================================================

    private fun configureProfile(
        profile: ScanProfile
    ): Boolean {

        when (
            profile.scanType
        ) {

            ScanType.ELM_STANDARD -> {

                val protocol =
                    profile.elmProtocol

                if (
                    protocol.isNotBlank()
                ) {

                    val response =
                        sendSafe(
                            "ATSP$protocol",
                            1000
                        )

                    /*
                     * ELM can respond OK or silently accept it.
                     */
                    if (
                        containsElmError(
                            response
                        )
                    ) {
                        return false
                    }
                }

                sendSafe(
                    "ATAT1",
                    1000
                )

                sendSafe(
                    "ATST32",
                    1000
                )

                return true
            }

            ScanType.CAN_USER -> {

                /*
                 * ELM327 standard AT interface cannot reliably configure
                 * arbitrary CAN baud rates through a generic command.
                 *
                 * Therefore only known ELM protocol mappings are used.
                 */
                return when (
                    profile.code
                ) {

                    "CAN11-250000" -> {
                        !containsElmError(
                            sendSafe(
                                "ATSP8",
                                1000
                            )
                        )
                    }

                    "CAN29-250000" -> {
                        !containsElmError(
                            sendSafe(
                                "ATSP9",
                                1000
                            )
                        )
                    }

                    "CAN11-500000" -> {
                        !containsElmError(
                            sendSafe(
                                "ATSP6",
                                1000
                            )
                        )
                    }

                    "CAN29-500000" -> {
                        !containsElmError(
                            sendSafe(
                                "ATSP7",
                                1000
                            )
                        )
                    }

                    "J1939-250000" -> {
                        !containsElmError(
                            sendSafe(
                                "ATSPA",
                                1000
                            )
                        )
                    }

                    "J1939-500000" -> {
                        !containsElmError(
                            sendSafe(
                                "ATSPA",
                                1000
                            )
                        )
                    }

                    else -> {
                        false
                    }
                }
            }
        }
    }

    // ============================================================
    // KWP DISCOVERY
    // ============================================================

    private fun discoverKwp(
        profile: ScanProfile,
        maxCommands: Int
    ): ProtocolResult {

        val successful =
            mutableListOf<String>()

        val responses =
            linkedMapOf<String, String>()

        /*
         * --------------------------------------------------------
         * 1. Tester Present
         * --------------------------------------------------------
         *
         * 3E -> 7E is a valid KWP positive response.
         */
        if (
            commandCounter.get() <
            maxCommands
        ) {

            val response =
                sendSafe(
                    "3E",
                    maxCommands
                )

            if (
                response.isNotBlank()
            ) {

                responses["3E"] =
                    response

                val parsed =
                    parseKwpResponse(
                        response
                    )

                recordKwpResult(
                    "3E",
                    response,
                    parsed
                )

                if (
                    parsed.positive &&
                    parsed.service ==
                    "7E"
                ) {

                    successful.add(
                        "3E"
                    )
                }
            }
        }

        /*
         * --------------------------------------------------------
         * 2. KWP identification 1A90 / 1A91
         * --------------------------------------------------------
         *
         * These are known from the supplied ECU test.
         */
        for (
        id in kwpIdentificationIds
        ) {

            if (
                commandCounter.get() >=
                maxCommands
            ) {
                break
            }

            val command =
                String.format(
                    Locale.US,
                    "1A%02X",
                    id
                )

            val response =
                sendSafe(
                    command,
                    maxCommands
                )

            if (
                response.isBlank()
            ) {
                continue
            }

            responses[command] =
                response

            val parsed =
                parseKwpResponse(
                    response
                )

            recordKwpResult(
                command,
                response,
                parsed
            )

            if (
                parsed.positive &&
                parsed.service ==
                "5A"
            ) {

                successful.add(
                    command
                )
            }
        }

        /*
         * --------------------------------------------------------
         * 3. KWP 21xx
         * --------------------------------------------------------
         *
         * Read-only discovery.
         */
        for (
        id in kwpDataIdentifiers
        ) {

            if (
                commandCounter.get() >=
                maxCommands
            ) {
                break
            }

            val command =
                String.format(
                    Locale.US,
                    "21%02X",
                    id
                )

            val response =
                sendSafe(
                    command,
                    maxCommands
                )

            if (
                response.isBlank()
            ) {
                continue
            }

            responses[command] =
                response

            val parsed =
                parseKwpResponse(
                    response
                )

            recordKwpResult(
                command,
                response,
                parsed
            )

            if (
                parsed.positive &&
                parsed.service ==
                "61"
            ) {

                successful.add(
                    command
                )
            }
        }

        /*
         * --------------------------------------------------------
         * 4. DTC read
         * --------------------------------------------------------
         *
         * Do not replace this with another guessed command.
         */
        if (
            commandCounter.get() <
            maxCommands
        ) {

            val command =
                "13"

            val response =
                sendSafe(
                    command,
                    maxCommands
                )

            if (
                response.isNotBlank()
            ) {

                responses[command] =
                    response

                val parsed =
                    parseKwpResponse(
                        response
                    )

                recordKwpResult(
                    command,
                    response,
                    parsed
                )

                if (
                    parsed.positive &&
                    parsed.service ==
                    "53"
                ) {

                    successful.add(
                        command
                    )
                }
            }
        }

        /*
         * A KWP ECU is considered detected only if the response
         * structure actually validates.
         */
        val validKwpResponses =
            commandResults.count {
                it.checksumValid == true &&
                        it.service.isNotBlank()
            }

        val hasKwpPositive =
            commandResults.any {
                it.checksumValid == true &&
                        it.positive
            }

        if (
            !hasKwpPositive ||
            validKwpResponses == 0
        ) {

            return ProtocolResult(
                success = false,
                profile = profile,
                response =
                    responses.entries
                        .lastOrNull()
                        ?.value
                        ?: ""
            )
        }

        val fingerprint =
            buildKwpFingerprint(
                profile = profile,
                successfulCommands =
                    successful,
                responses =
                    responses
            )

        return ProtocolResult(
            success = true,
            profile = profile,
            response =
                responses.entries
                    .firstOrNull {
                        it.key == "2101"
                    }
                    ?.value
                    ?: responses.entries
                        .firstOrNull()
                        ?.value
                    ?: "",
            fingerprint = fingerprint
        )
    }

    // ============================================================
    // KWP RESPONSE PARSER
    // ============================================================

    private data class KwpParsedFrame(
        val validFrame: Boolean = false,
        val checksumValid: Boolean = false,
        val format: Int = -1,
        val target: Int = -1,
        val source: Int = -1,
        val headerLength: Int = 0,
        val payloadLength: Int = 0,
        val payload: ByteArray = ByteArray(0),
        val service: String = "",
        val positive: Boolean = false,
        val negative: Boolean = false,
        val nrc: String = ""
    )

    private fun parseKwpResponse(
        rawResponse: String
    ): KwpParsedFrame {

        val bytes =
            hexToBytes(
                rawResponse
            )

        if (
            bytes.isEmpty()
        ) {
            return KwpParsedFrame()
        }

        /*
         * Some ELM responses can contain additional text.
         * Find the first plausible KWP frame.
         */
        val frame =
            extractKwpFrame(
                bytes
            )

        if (
            frame == null
        ) {

            /*
             * Fall back to payload-like response without checksum.
             * This is NOT considered a validated KWP frame.
             */
            val service =
                bytes.firstOrNull()
                    ?.toInt()
                    ?.and(0xFF)
                    ?.let {
                        String.format(
                            Locale.US,
                            "%02X",
                            it
                        )
                    }
                    ?: ""

            return KwpParsedFrame(
                validFrame = false,
                checksumValid = false,
                service = service,
                positive =
                    isPositiveKwpService(
                        service
                    ),
                negative =
                    service.equals(
                        "7F",
                        true
                    ),
                nrc =
                    if (
                        service.equals(
                            "7F",
                            true
                        ) &&
                        bytes.size >= 3
                    ) {
                        String.format(
                            Locale.US,
                            "%02X",
                            bytes[2].toInt() and 0xFF
                        )
                    } else {
                        ""
                    }
            )
        }

        return frame
    }

    private fun extractKwpFrame(
        bytes: ByteArray
    ): KwpParsedFrame? {

        if (
            bytes.size < 4
        ) {
            return null
        }

        /*
         * Try each possible start position because ELM clones can
         * occasionally prepend text-derived bytes.
         */
        for (
        start in bytes.indices
        ) {

            if (
                bytes.size - start < 4
            ) {
                continue
            }

            val format =
                bytes[start].toInt() and 0xFF

            val lowLength =
                format and 0x3F

            /*
             * Normal 3-byte KWP header.
             */
            if (
                lowLength > 0
            ) {

                val headerLength =
                    3

                val frameLength =
                    headerLength +
                            lowLength +
                            1

                if (
                    start +
                    frameLength >
                    bytes.size
                ) {
                    continue
                }

                val frame =
                    bytes.copyOfRange(
                        start,
                        start + frameLength
                    )

                val checksum =
                    frame.last()
                        .toInt()
                        .and(0xFF)

                val calculated =
                    calculateKwpChecksum(
                        frame,
                        frame.size - 1
                    )

                val checksumValid =
                    checksum ==
                            calculated

                val payloadStart =
                    3

                val payload =
                    frame.copyOfRange(
                        payloadStart,
                        frame.size - 1
                    )

                return makeParsedKwpFrame(
                    format =
                        format,
                    target =
                        frame[1].toInt()
                            .and(0xFF),
                    source =
                        frame[2].toInt()
                            .and(0xFF),
                    headerLength =
                        headerLength,
                    payload =
                        payload,
                    checksumValid =
                        checksumValid
                )
            }

            /*
             * Extended length.
             *
             * For the current ECU this is not required, but keeping
             * the parser here makes it safer for other KWP ECUs.
             */
            if (
                lowLength == 0 &&
                bytes.size - start >= 5
            ) {

                val extendedLength =
                    bytes[start + 3]
                        .toInt()
                        .and(0xFF)

                val headerLength =
                    4

                val frameLength =
                    headerLength +
                            extendedLength +
                            1

                if (
                    start +
                    frameLength >
                    bytes.size
                ) {
                    continue
                }

                val frame =
                    bytes.copyOfRange(
                        start,
                        start + frameLength
                    )

                val checksum =
                    frame.last()
                        .toInt()
                        .and(0xFF)

                val calculated =
                    calculateKwpChecksum(
                        frame,
                        frame.size - 1
                    )

                val payload =
                    frame.copyOfRange(
                        headerLength,
                        frame.size - 1
                    )

                return makeParsedKwpFrame(
                    format =
                        format,
                    target =
                        frame[1].toInt()
                            .and(0xFF),
                    source =
                        frame[2].toInt()
                            .and(0xFF),
                    headerLength =
                        headerLength,
                    payload =
                        payload,
                    checksumValid =
                        checksum ==
                                calculated
                )
            }
        }

        return null
    }

    private fun makeParsedKwpFrame(
        format: Int,
        target: Int,
        source: Int,
        headerLength: Int,
        payload: ByteArray,
        checksumValid: Boolean
    ): KwpParsedFrame {

        if (
            payload.isEmpty()
        ) {
            return KwpParsedFrame(
                validFrame = true,
                checksumValid = checksumValid,
                format = format,
                target = target,
                source = source,
                headerLength = headerLength,
                payloadLength = 0,
                payload = payload
            )
        }

        val serviceByte =
            payload[0].toInt()
                .and(0xFF)

        val service =
            String.format(
                Locale.US,
                "%02X",
                serviceByte
            )

        if (
            serviceByte ==
            KWP_RESPONSE_NEGATIVE
        ) {

            val rejectedService =
                if (
                    payload.size >= 2
                ) {
                    payload[1].toInt()
                        .and(0xFF)
                } else {
                    -1
                }

            val nrc =
                if (
                    payload.size >= 3
                ) {
                    String.format(
                        Locale.US,
                        "%02X",
                        payload[2].toInt()
                            .and(0xFF)
                    )
                } else {
                    ""
                }

            return KwpParsedFrame(
                validFrame = true,
                checksumValid = checksumValid,
                format = format,
                target = target,
                source = source,
                headerLength = headerLength,
                payloadLength = payload.size,
                payload = payload,
                service = "7F",
                positive = false,
                negative = true,
                nrc = nrc
            )
        }

        return KwpParsedFrame(
            validFrame = true,
            checksumValid = checksumValid,
            format = format,
            target = target,
            source = source,
            headerLength = headerLength,
            payloadLength = payload.size,
            payload = payload,
            service = service,
            positive =
                isPositiveKwpService(
                    service
                ),
            negative = false
        )
    }

    private fun isPositiveKwpService(
        service: String
    ): Boolean {

        if (
            service.length != 2
        ) {
            return false
        }

        return try {

            val value =
                service.toInt(
                    16
                )

            /*
             * Positive response SID is request SID + 0x40.
             */
            value in
                    0x50..0x77

        } catch (_: Exception) {
            false
        }
    }

    private fun calculateKwpChecksum(
        frame: ByteArray,
        checksumIndex: Int
    ): Int {

        var sum = 0

        for (
        index in 0 until checksumIndex
        ) {

            sum =
                (
                        sum +
                                (
                                        frame[index]
                                            .toInt()
                                            .and(0xFF)
                                        )
                        ) and 0xFF
        }

        return sum
    }

    // ============================================================
    // KWP RESULT RECORDING
    // ============================================================

    private fun recordKwpResult(
        command: String,
        response: String,
        parsed: KwpParsedFrame
    ) {

        val result =
            UnknownEcuCommandResult(
                command = command,
                response = response,
                positive =
                    parsed.positive &&
                            parsed.checksumValid,
                negative =
                    parsed.negative,
                nrc =
                    parsed.nrc,
                service =
                    parsed.service,
                checksumValid =
                    parsed.checksumValid,
                payloadLength =
                    parsed.payloadLength
            )

        commandResults.add(
            result
        )

        rawDatabase
            .getOrPut(command) {
                mutableListOf()
            }
            .add(response)
    }

    // ============================================================
    // KWP FINGERPRINT
    // ============================================================

    private fun buildKwpFingerprint(
        profile: ScanProfile,
        successfulCommands: List<String>,
        responses: Map<String, String>
    ): EcuFingerprint {

        val responseIds =
            linkedSetOf<String>()

        var requestId =
            ""

        var ecuAddress =
            ""

        /*
         * Inspect validated KWP frames.
         */
        for (
        response in responses.values
        ) {

            val parsed =
                parseKwpResponse(
                    response
                )

            if (
                !parsed.validFrame
            ) {
                continue
            }

            if (
                parsed.checksumValid
            ) {

                if (
                    parsed.source >= 0
                ) {

                    val source =
                        String.format(
                            Locale.US,
                            "%02X",
                            parsed.source
                        )

                    responseIds.add(
                        source
                    )

                    if (
                        ecuAddress.isBlank()
                    ) {
                        ecuAddress =
                            source
                    }
                }

                if (
                    parsed.target >= 0
                ) {

                    requestId =
                        String.format(
                            Locale.US,
                            "%02X",
                            parsed.target
                        )
                }
            }
        }

        /*
         * For KWP:
         *
         * target/source are address bytes, not CAN IDs.
         */
        if (
            requestId.isBlank()
        ) {
            requestId = "F1"
        }

        /*
         * Extract textual identifiers only when there is actual
         * printable data. Never call arbitrary binary bytes VIN/SW.
         */
        val identification =
            extractKwpIdentification(
                responses
            )

        /*
         * Confidence is deliberately based on validated protocol
         * evidence rather than generic response patterns.
         */
        var confidence =
            35

        if (
            successfulCommands.contains(
                "3E"
            )
        ) {
            confidence += 10
        }

        if (
            successfulCommands.any {
                it.startsWith(
                    "1A",
                    true
                )
            }
        ) {
            confidence += 20
        }

        if (
            successfulCommands.any {
                it.startsWith(
                    "21",
                    true
                )
            }
        ) {
            confidence += 20
        }

        if (
            successfulCommands.contains(
                "13"
            )
        ) {
            confidence += 5
        }

        if (
            ecuAddress.isNotBlank()
        ) {
            confidence += 5
        }

        confidence =
            confidence.coerceIn(
                0,
                100
            )

        return EcuFingerprint(
            protocol =
                DiscoveryProtocol.KWP2000,
            positiveCommands =
                successfulCommands.distinct(),
            responseIds =
                responseIds.toList(),
            requestId =
                requestId,
            ecuAddress =
                ecuAddress,
            ecuIdentifier =
                identification.ecuIdentifier,
            vin =
                identification.vin,
            softwareVersion =
                identification.softwareVersion,
            hardwareVersion =
                identification.hardwareVersion,
            rawResponses =
                responses,
            confidence =
                confidence
        )
    }

    private fun extractKwpIdentification(
        responses: Map<String, String>
    ): UnknownEcuIdentification {

        var identifier =
            ""

        var vin =
            ""

        var software =
            ""

        var hardware =
            ""

        for (
        entry in responses
        ) {

            val command =
                entry.key

            val raw =
                entry.value

            val parsed =
                parseKwpResponse(
                    raw
                )

            if (
                !parsed.validFrame ||
                !parsed.checksumValid
            ) {
                continue
            }

            val data =
                parsed.payload
                    .drop(1)
                    .toByteArray()

            val printable =
                bytesToPrintableAscii(
                    data
                )

            if (
                printable.length >= 4
            ) {

                if (
                    identifier.isBlank()
                ) {
                    identifier =
                        printable
                }

                if (
                    vin.isBlank() &&
                    printable.matches(
                        Regex(
                            "[A-HJ-NPR-Z0-9]{17}"
                        )
                    )
                ) {
                    vin =
                        printable
                }
            }

            /*
             * Only label software/hardware when text itself
             * provides a clear indication.
             */
            val upper =
                printable.uppercase(
                    Locale.US
                )

            if (
                software.isBlank() &&
                (
                        upper.contains("SW") ||
                                upper.contains(
                                    "SOFTWARE"
                                )
                        )
            ) {
                software =
                    printable
            }

            if (
                hardware.isBlank() &&
                (
                        upper.contains("HW") ||
                                upper.contains(
                                    "HARDWARE"
                                )
                        )
            ) {
                hardware =
                    printable
            }

            /*
             * Do not invent semantic meanings for binary 21xx data.
             */
            if (
                command.startsWith(
                    "21",
                    true
                ) &&
                identifier.isBlank()
            ) {

                /*
                 * A binary payload is not automatically an ECU ID.
                 * Keep it raw in rawResponses instead.
                 */
            }
        }

        return UnknownEcuIdentification(
            ecuIdentifier =
                identifier,
            vin =
                vin,
            softwareVersion =
                software,
            hardwareVersion =
                hardware
        )
    }

    // ============================================================
    // ISO9141 DISCOVERY
    // ============================================================

    private fun discoverIso9141(
        profile: ScanProfile,
        maxCommands: Int
    ): ProtocolResult {

        val commands =
            listOf(
                "0100",
                "0101",
                "010C",
                "010D",
                "0142",
                "0902"
            )

        val successful =
            mutableListOf<String>()

        val responses =
            linkedMapOf<String, String>()

        for (
        command in commands
        ) {

            if (
                commandCounter.get() >=
                maxCommands
            ) {
                break
            }

            val response =
                sendSafe(
                    command,
                    maxCommands
                )

            if (
                response.isBlank()
            ) {
                continue
            }

            responses[command] =
                response

            val analysis =
                analyzeGenericResponse(
                    command,
                    response
                )

            if (
                analysis.positive
            ) {
                successful.add(
                    command
                )
            }
        }

        if (
            successful.isEmpty()
        ) {

            return ProtocolResult(
                false,
                profile,
                responses.values.lastOrNull()
                    ?: ""
            )
        }

        val fingerprint =
            buildFingerprint(
                protocol =
                    DiscoveryProtocol.ISO9141,
                successfulCommands =
                    successful,
                rawResponses =
                    responses
            )

        return ProtocolResult(
            success = true,
            profile = profile,
            response =
                responses.values
                    .firstOrNull()
                    ?: "",
            fingerprint = fingerprint
        )
    }

    // ============================================================
    // OBD DISCOVERY
    // ============================================================

    private fun discoverObd(
        profile: ScanProfile,
        maxCommands: Int
    ): ProtocolResult {

        val commands =
            listOf(
                "0100",
                "0101",
                "010C",
                "010D",
                "0142",
                "0902"
            )

        val successful =
            mutableListOf<String>()

        val responses =
            linkedMapOf<String, String>()

        for (
        command in commands
        ) {

            if (
                commandCounter.get() >=
                maxCommands
            ) {
                break
            }

            val response =
                sendSafe(
                    command,
                    maxCommands
                )

            if (
                response.isBlank()
            ) {
                continue
            }

            responses[command] =
                response

            val analysis =
                analyzeGenericResponse(
                    command,
                    response
                )

            if (
                analysis.positive
            ) {
                successful.add(
                    command
                )
            }
        }

        if (
            successful.isEmpty()
        ) {

            return ProtocolResult(
                success = false,
                profile = profile
            )
        }

        val fingerprint =
            buildFingerprint(
                protocol =
                    profile.protocol,
                successfulCommands =
                    successful,
                rawResponses =
                    responses
            )

        return ProtocolResult(
            success = true,
            profile = profile,
            response =
                responses.values
                    .firstOrNull()
                    ?: "",
            fingerprint =
                fingerprint
        )
    }

    // ============================================================
    // UDS DISCOVERY
    // ============================================================

    private fun discoverUds(
        profile: ScanProfile,
        maxCommands: Int
    ): ProtocolResult {

        val commands =
            listOf(
                "1001",
                "1003",
                "3E00",
                "22F190",
                "0902"
            )

        val successful =
            mutableListOf<String>()

        val responses =
            linkedMapOf<String, String>()

        for (
        command in commands
        ) {

            if (
                commandCounter.get() >=
                maxCommands
            ) {
                break
            }

            val response =
                sendSafe(
                    command,
                    maxCommands
                )

            if (
                response.isBlank()
            ) {
                continue
            }

            responses[command] =
                response

            val analysis =
                analyzeGenericResponse(
                    command,
                    response
                )

            if (
                analysis.positive
            ) {
                successful.add(
                    command
                )
            }
        }

        if (
            successful.isEmpty()
        ) {

            return ProtocolResult(
                success = false,
                profile = profile
            )
        }

        val fingerprint =
            buildFingerprint(
                protocol =
                    profile.protocol,
                successfulCommands =
                    successful,
                rawResponses =
                    responses
            )

        return ProtocolResult(
            success = true,
            profile = profile,
            response =
                responses.values
                    .firstOrNull()
                    ?: "",
            fingerprint =
                fingerprint
        )
    }

    // ============================================================
    // J1939
    // ============================================================

    private fun discoverJ1939(
        profile: ScanProfile,
        maxCommands: Int
    ): ProtocolResult {

        val commands =
            listOf(
                "00FEF1",
                "00F004",
                "0100"
            )

        val successful =
            mutableListOf<String>()

        val responses =
            linkedMapOf<String, String>()

        for (
        command in commands
        ) {

            if (
                commandCounter.get() >=
                maxCommands
            ) {
                break
            }

            val response =
                sendSafe(
                    command,
                    maxCommands
                )

            if (
                response.isBlank()
            ) {
                continue
            }

            responses[command] =
                response

            val analysis =
                analyzeGenericResponse(
                    command,
                    response
                )

            if (
                analysis.positive
            ) {
                successful.add(
                    command
                )
            }
        }

        if (
            successful.isEmpty()
        ) {

            return ProtocolResult(
                success = false,
                profile = profile
            )
        }

        val fingerprint =
            buildFingerprint(
                protocol =
                    DiscoveryProtocol.J1939,
                successfulCommands =
                    successful,
                rawResponses =
                    responses
            )

        return ProtocolResult(
            true,
            profile,
            responses.values.firstOrNull()
                ?: "",
            fingerprint
        )
    }

    // ============================================================
    // GENERIC RESPONSE ANALYSIS
    // ============================================================

    private data class GenericAnalysis(
        val positive: Boolean,
        val negative: Boolean,
        val nrc: String = ""
    )

    private fun analyzeGenericResponse(
        command: String,
        response: String
    ): GenericAnalysis {

        val normalized =
            normalizeHex(
                response
            )

        if (
            normalized.isBlank()
        ) {
            return GenericAnalysis(
                false,
                false
            )
        }

        val tokens =
            normalized
                .split(
                    Regex("\\s+")
                )

        /*
         * Negative OBD/UDS/KWP response.
         */
        for (
        index in 0 until
                tokens.size
        ) {

            if (
                tokens[index]
                    .equals(
                        "7F",
                        true
                    )
            ) {

                return GenericAnalysis(
                    positive = false,
                    negative = true,
                    nrc =
                        if (
                            index + 2 <
                            tokens.size
                        ) {
                            tokens[index + 2]
                        } else {
                            ""
                        }
                )
            }
        }

        /*
         * OBD positive response.
         *
         * Request 01xx -> 41xx
         * Request 09xx -> 49xx
         */
        if (
            command.length >= 2
        ) {

            val requestService =
                command
                    .take(2)
                    .uppercase(
                        Locale.US
                    )

            val positiveService =
                try {

                    String.format(
                        Locale.US,
                        "%02X",
                        (
                                requestService
                                    .toInt(16) +
                                        0x40
                                ) and 0xFF
                    )

                } catch (_: Exception) {
                    ""
                }

            if (
                positiveService.isNotBlank() &&
                normalized.contains(
                    positiveService
                )
            ) {

                return GenericAnalysis(
                    positive = true,
                    negative = false
                )
            }
        }

        /*
         * UDS positive response.
         */
        if (
            command.length >= 2
        ) {

            try {

                val sid =
                    command
                        .take(2)
                        .toInt(
                            16
                        )

                val positive =
                    String.format(
                        Locale.US,
                        "%02X",
                        (sid + 0x40) and 0xFF
                    )

                if (
                    normalized.contains(
                        positive
                    )
                ) {

                    return GenericAnalysis(
                        positive = true,
                        negative = false
                    )
                }

            } catch (_: Exception) {
            }
        }

        return GenericAnalysis(
            positive = false,
            negative = false
        )
    }

    // ============================================================
    // FINGERPRINT GENERIC
    // ============================================================

    private fun buildFingerprint(
        protocol: DiscoveryProtocol,
        successfulCommands: List<String>,
        rawResponses: Map<String, String>
    ): EcuFingerprint {

        val responseIds =
            extractCanResponseIds(
                rawResponses.values
                    .toList()
            )

        val requestId =
            inferRequestId(
                responseIds
            )

        val ecuIdentifier =
            extractEcuIdentifier(
                rawResponses
            )

        val vin =
            extractVin(
                rawResponses
            )

        val software =
            extractVersion(
                rawResponses,
                true
            )

        val hardware =
            extractVersion(
                rawResponses,
                false
            )

        var confidence =
            20

        confidence +=
            minOf(
                successfulCommands.size * 5,
                30
            )

        confidence +=
            minOf(
                responseIds.size * 5,
                20
            )

        if (
            vin.isNotBlank()
        ) {
            confidence += 15
        }

        if (
            ecuIdentifier.isNotBlank()
        ) {
            confidence += 10
        }

        if (
            successfulCommands.contains(
                "0142"
            )
        ) {
            confidence += 10
        }

        return EcuFingerprint(
            protocol =
                protocol,
            positiveCommands =
                successfulCommands.distinct(),
            responseIds =
                responseIds,
            requestId =
                requestId,
            ecuAddress =
                responseIds.firstOrNull()
                    ?: "",
            ecuIdentifier =
                ecuIdentifier,
            vin =
                vin,
            softwareVersion =
                software,
            hardwareVersion =
                hardware,
            rawResponses =
                rawResponses,
            confidence =
                confidence.coerceAtMost(
                    100
                )
        )
    }

    // ============================================================
    // IDENTIFICATION HELPERS
    // ============================================================

    private fun extractVin(
        responses: Map<String, String>
    ): String {

        val regex =
            Regex(
                "[A-HJ-NPR-Z0-9]{17}"
            )

        for (
        response in responses.values
        ) {

            val match =
                regex.find(
                    response
                        .uppercase(
                            Locale.US
                        )
                )

            if (
                match != null
            ) {
                return match.value
            }
        }

        return ""
    }

    private fun extractVersion(
        responses: Map<String, String>,
        software: Boolean
    ): String {

        for (
        response in responses.values
        ) {

            val upper =
                response.uppercase(
                    Locale.US
                )

            val match =
                if (
                    software
                ) {
                    Regex(
                        "(SW|SOFTWARE)[^\\r\\n]*"
                    ).find(
                        upper
                    )
                } else {
                    Regex(
                        "(HW|HARDWARE)[^\\r\\n]*"
                    ).find(
                        upper
                    )
                }

            if (
                match != null
            ) {
                return match.value
            }
        }

        return ""
    }

    /**
     * This intentionally does NOT claim that arbitrary response bytes
     * are the ECU identifier.
     */
    private fun extractEcuIdentifier(
        responses: Map<String, String>
    ): String {

        val candidates =
            mutableListOf<String>()

        for (
        response in responses.values
        ) {

            val normalized =
                normalizeHex(
                    response
                )

            val tokens =
                normalized.split(
                    Regex("\\s+")
                )

            for (
            token in tokens
            ) {

                if (
                    token.length >= 8 &&
                    token.length <= 24 &&
                    token.all {
                        it in
                                "0123456789ABCDEFabcdef"
                    }
                ) {

                    candidates.add(
                        token.uppercase(
                            Locale.US
                        )
                    )
                }
            }
        }

        /*
         * Only return a candidate when it resembles an identifier,
         * not simply the first arbitrary bytes of a response.
         */
        return candidates
            .firstOrNull {
                it.length >= 8 &&
                        it.length % 2 == 0
            }
            ?: ""
    }

    // ============================================================
    // CAN HELPERS
    // ============================================================

    private fun extractCanResponseIds(
        responses: List<String>
    ): List<String> {

        val ids =
            linkedSetOf<String>()

        for (
        response in responses
        ) {

            val normalized =
                normalizeHex(
                    response
                )

            val tokens =
                normalized.split(
                    Regex("\\s+")
                )

            for (
            token in tokens
            ) {

                if (
                    token.matches(
                        Regex(
                            "18DA[0-9A-Fa-f]{4}"
                        )
                    )
                ) {

                    ids.add(
                        token.uppercase(
                            Locale.US
                        )
                    )
                }

                if (
                    token.matches(
                        Regex(
                            "7E[8-9A-Fa-f]"
                        )
                    )
                ) {

                    ids.add(
                        token.uppercase(
                            Locale.US
                        )
                    )
                }
            }
        }

        return ids.toList()
    }

    private fun inferRequestId(
        responseIds: List<String>
    ): String {

        for (
        id in responseIds
        ) {

            if (
                id.matches(
                    Regex(
                        "7E[8-9A-Fa-f]"
                    )
                )
            ) {

                return String.format(
                    Locale.US,
                    "7E%X",
                    (
                            id
                                .substring(2)
                                .toInt(16) - 8
                            ) and 0xF
                )
            }

            if (
                id.startsWith(
                    "18DA",
                    true
                )
            ) {

                val target =
                    id.substring(
                        4,
                        6
                    )

                val source =
                    id.substring(
                        6,
                        8
                    )

                return "18DA$target$source"
            }
        }

        return "7DF"
    }

    // ============================================================
    // TRANSPORT
    // ============================================================

    private fun isTransportConnected(): Boolean {

        return when (
            ConnectionSource.activeSource
        ) {

            ConnectionSource.Source.WIFI ->
                YadraConnectionManager
                    .elmConnected

            ConnectionSource.Source.BLUETOOTH ->
                BluetoothConnectionManager
                    .elmConnected

            ConnectionSource.Source.NONE ->
                false
        }
    }

    private fun sendSafe(
        command: String,
        maxCommands: Int
    ): String {

        if (
            stopRequested.get()
        ) {
            return ""
        }

        if (
            commandCounter.get() >=
            maxCommands
        ) {
            return ""
        }

        commandCounter.incrementAndGet()

        val response =
            try {

                when (
                    ConnectionSource.activeSource
                ) {

                    ConnectionSource.Source.WIFI -> {

                        YadraConnectionManager
                            .sendCommand(
                                command
                            )
                    }

                    ConnectionSource.Source.BLUETOOTH -> {

                        BluetoothConnectionManager
                            .sendActiveCommand(
                                command
                            )
                    }

                    ConnectionSource.Source.NONE ->
                        ""
                }

            } catch (_: Exception) {
                ""
            }

        val clean =
            response
                .trim()

        rawDatabase
            .getOrPut(
                command.uppercase(
                    Locale.US
                )
            ) {
                mutableListOf()
            }
            .add(
                clean
            )

        return clean
    }

    // ============================================================
    // ELM HELPERS
    // ============================================================

    private fun containsElmError(
        response: String
    ): Boolean {

        val upper =
            response.uppercase(
                Locale.US
            )

        return upper.contains(
            "ERROR"
        ) ||
                upper.contains(
                    "STOPPED"
                ) ||
                upper.contains(
                    "BUS ERROR"
                ) ||
                upper.contains(
                    "CAN ERROR"
                )
    }

    private fun isKwpFastResponse(
        response: String
    ): Boolean {

        val normalized =
            response
                .uppercase(
                    Locale.US
                )

        return normalized.contains(
            "A5"
        ) &&
                (
                        normalized.contains(
                            "ISO 14230"
                        ) ||
                                normalized.contains(
                                    "KWP"
                                ) ||
                                normalized.contains(
                                    "FAST"
                                )
                        )
    }

    // ============================================================
    // PROFILE SAVE
    // ============================================================

    private fun saveSuccessfulLearnedProfile(
        result: ProtocolResult
    ) {

        if (
            !result.success
        ) {
            return
        }

        val fingerprint =
            result.fingerprint
                ?: return

        val profile =
            result.profile

        try {

            val saved =
                EcuConnectionProfile(
                    id =
                        java.util.UUID
                            .randomUUID()
                            .toString(),

                    source =
                        ConnectionSource
                            .activeSource
                            .name,

                    protocolCode =
                        profile.code,

                    protocolName =
                        profile.name,

                    scanType =
                        profile.scanType.name,

                    elmProtocol =
                        profile.elmProtocol,

                    baudRate =
                        profile.baudRate,

                    canExtended =
                        profile.canExtended,

                    requestId =
                        fingerprint.requestId,

                    responseIds =
                        fingerprint.responseIds,

                    ecuAddress =
                        fingerprint.ecuAddress,

                    ecuIdentifier =
                        fingerprint.ecuIdentifier,

                    vin =
                        fingerprint.vin,

                    softwareVersion =
                        fingerprint.softwareVersion,

                    hardwareVersion =
                        fingerprint.hardwareVersion,

                    successfulCommands =
                        fingerprint.positiveCommands,

                    fingerprintResponses =
                        fingerprint.rawResponses,

                    elmVersion =
                        "",

                    confidence =
                        fingerprint.confidence,

                    createdAt =
                        System.currentTimeMillis(),

                    lastSeen =
                        System.currentTimeMillis(),

                    successCount =
                        1,

                    failureCount =
                        0
                )

            appContext?.let {
                EcuProfileStore.saveOrUpdate(
                    it,
                    saved
                )
            }

            /*
             * Update runtime state.
             */
            EcuRuntimeState.setUnknown(
                protocol =
                    profile.code,
                protocolName =
                    profile.name
            )

            EcuRuntimeState.setUnknownDetails(
                ecuAddress =
                    fingerprint.ecuAddress,

                ecuIdentifier =
                    fingerprint.ecuIdentifier,

                vin =
                    fingerprint.vin,

                softwareVersion =
                    fingerprint.softwareVersion,

                hardwareVersion =
                    fingerprint.hardwareVersion,

                confidence =
                    fingerprint.confidence,

                responseIds =
                    fingerprint.responseIds,

                successfulCommands =
                    fingerprint.positiveCommands,

                rawResponse =
                    fingerprint.rawResponses
                        .entries
                        .joinToString(
                            "\n"
                        ) {
                            "${it.key}: ${it.value}"
                        }
            )

            EcuRuntimeState.setConnected(
                true
            )

        } catch (_: Exception) {
            /*
             * Discovery itself must not fail because persistence failed.
             */
        }
    }

    // ============================================================
    // PROFILE LIST
    // ============================================================

    private fun buildScanProfiles():
            List<ScanProfile> {

        return listOf(

            ScanProfile(
                "J1850-PWM",
                "J1850 PWM",
                ScanType.ELM_STANDARD,
                "1",
                protocol =
                    DiscoveryProtocol.OBD2
            ),

            ScanProfile(
                "J1850-VPW",
                "J1850 VPW",
                ScanType.ELM_STANDARD,
                "2",
                protocol =
                    DiscoveryProtocol.OBD2
            ),

            ScanProfile(
                "ISO9141-10400",
                "ISO 9141-2 10400",
                ScanType.ELM_STANDARD,
                "3",
                10400,
                false,
                DiscoveryProtocol.ISO9141
            ),

            ScanProfile(
                "KWP5-10400",
                "ISO 14230 KWP 5-baud",
                ScanType.ELM_STANDARD,
                "4",
                10400,
                false,
                DiscoveryProtocol.KWP2000
            ),

            ScanProfile(
                "KWPFAST-10400",
                "ISO 14230 KWP FAST",
                ScanType.ELM_STANDARD,
                "5",
                10400,
                false,
                DiscoveryProtocol.KWP2000
            ),

            ScanProfile(
                "CAN11-500000",
                "CAN 11-bit 500K",
                ScanType.ELM_STANDARD,
                "6",
                500000,
                false,
                DiscoveryProtocol.UDS_CAN_11
            ),

            ScanProfile(
                "CAN29-500000",
                "CAN 29-bit 500K",
                ScanType.ELM_STANDARD,
                "7",
                500000,
                true,
                DiscoveryProtocol.UDS_CAN_29
            ),

            ScanProfile(
                "CAN11-250000",
                "CAN 11-bit 250K",
                ScanType.ELM_STANDARD,
                "8",
                250000,
                false,
                DiscoveryProtocol.UDS_CAN_11
            ),

            ScanProfile(
                "CAN29-250000",
                "CAN 29-bit 250K",
                ScanType.ELM_STANDARD,
                "9",
                250000,
                true,
                DiscoveryProtocol.UDS_CAN_29
            ),

            ScanProfile(
                "J1939-250000",
                "J1939 29-bit 250K",
                ScanType.ELM_STANDARD,
                "A",
                250000,
                true,
                DiscoveryProtocol.J1939
            ),

            ScanProfile(
                "KWP-9600",
                "KWP 9600",
                ScanType.ELM_STANDARD,
                "5",
                9600,
                false,
                DiscoveryProtocol.KWP2000
            ),

            ScanProfile(
                "KWP-4800",
                "KWP 4800",
                ScanType.ELM_STANDARD,
                "5",
                4800,
                false,
                DiscoveryProtocol.KWP2000
            ),

            ScanProfile(
                "KWP-33",
                "KWP 33 baud",
                ScanType.ELM_STANDARD,
                "4",
                33,
                false,
                DiscoveryProtocol.KWP2000
            ),

            ScanProfile(
                "KWP-13",
                "KWP 13 baud",
                ScanType.ELM_STANDARD,
                "4",
                13,
                false,
                DiscoveryProtocol.KWP2000
            ),

            ScanProfile(
                "KWP-7A",
                "KWP 0x7A",
                ScanType.ELM_STANDARD,
                "5",
                0x7A,
                false,
                DiscoveryProtocol.KWP2000
            ),

            ScanProfile(
                "KWP-81",
                "KWP 0x81",
                ScanType.ELM_STANDARD,
                "5",
                0x81,
                false,
                DiscoveryProtocol.KWP2000
            ),

            ScanProfile(
                "CAN11-33300",
                "CAN 11-bit 33300",
                ScanType.CAN_USER,
                baudRate = 33300,
                protocol =
                    DiscoveryProtocol.UDS_CAN_11
            ),

            ScanProfile(
                "CAN11-50000",
                "CAN 11-bit 50000",
                ScanType.CAN_USER,
                baudRate = 50000,
                protocol =
                    DiscoveryProtocol.UDS_CAN_11
            ),

            ScanProfile(
                "CAN11-83300",
                "CAN 11-bit 83300",
                ScanType.CAN_USER,
                baudRate = 83300,
                protocol =
                    DiscoveryProtocol.UDS_CAN_11
            ),

            ScanProfile(
                "CAN11-100000",
                "CAN 11-bit 100K",
                ScanType.CAN_USER,
                baudRate = 100000,
                protocol =
                    DiscoveryProtocol.UDS_CAN_11
            ),

            ScanProfile(
                "CAN11-125000",
                "CAN 11-bit 125K",
                ScanType.CAN_USER,
                baudRate = 125000,
                protocol =
                    DiscoveryProtocol.UDS_CAN_11
            ),

            ScanProfile(
                "CAN11-250000-U",
                "CAN 11-bit 250K User",
                ScanType.CAN_USER,
                baudRate = 250000,
                protocol =
                    DiscoveryProtocol.UDS_CAN_11
            ),

            ScanProfile(
                "CAN11-500000-U",
                "CAN 11-bit 500K User",
                ScanType.CAN_USER,
                baudRate = 500000,
                protocol =
                    DiscoveryProtocol.UDS_CAN_11
            ),

            ScanProfile(
                "CAN29-33300",
                "CAN 29-bit 33300",
                ScanType.CAN_USER,
                baudRate = 33300,
                canExtended = true,
                protocol =
                    DiscoveryProtocol.UDS_CAN_29
            ),

            ScanProfile(
                "CAN29-50000",
                "CAN 29-bit 50000",
                ScanType.CAN_USER,
                baudRate = 50000,
                canExtended = true,
                protocol =
                    DiscoveryProtocol.UDS_CAN_29
            ),

            ScanProfile(
                "CAN29-83300",
                "CAN 29-bit 83300",
                ScanType.CAN_USER,
                baudRate = 83300,
                canExtended = true,
                protocol =
                    DiscoveryProtocol.UDS_CAN_29
            ),

            ScanProfile(
                "CAN29-100000",
                "CAN 29-bit 100K",
                ScanType.CAN_USER,
                baudRate = 100000,
                canExtended = true,
                protocol =
                    DiscoveryProtocol.UDS_CAN_29
            ),

            ScanProfile(
                "CAN29-125000",
                "CAN 29-bit 125K",
                ScanType.CAN_USER,
                baudRate = 125000,
                canExtended = true,
                protocol =
                    DiscoveryProtocol.UDS_CAN_29
            ),

            ScanProfile(
                "CAN29-250000-U",
                "CAN 29-bit 250K User",
                ScanType.CAN_USER,
                baudRate = 250000,
                canExtended = true,
                protocol =
                    DiscoveryProtocol.UDS_CAN_29
            ),

            ScanProfile(
                "CAN29-500000-U",
                "CAN 29-bit 500K User",
                ScanType.CAN_USER,
                baudRate = 500000,
                canExtended = true,
                protocol =
                    DiscoveryProtocol.UDS_CAN_29
            ),

            ScanProfile(
                "J1939-50000",
                "J1939 50K",
                ScanType.CAN_USER,
                baudRate = 50000,
                canExtended = true,
                protocol =
                    DiscoveryProtocol.J1939
            ),

            ScanProfile(
                "J1939-100000",
                "J1939 100K",
                ScanType.CAN_USER,
                baudRate = 100000,
                canExtended = true,
                protocol =
                    DiscoveryProtocol.J1939
            ),

            ScanProfile(
                "J1939-125000",
                "J1939 125K",
                ScanType.CAN_USER,
                baudRate = 125000,
                canExtended = true,
                protocol =
                    DiscoveryProtocol.J1939
            ),

            ScanProfile(
                "J1939-250000-U",
                "J1939 250K User",
                ScanType.CAN_USER,
                baudRate = 250000,
                canExtended = true,
                protocol =
                    DiscoveryProtocol.J1939
            ),

            ScanProfile(
                "J1939-500000",
                "J1939 500K",
                ScanType.CAN_USER,
                baudRate = 500000,
                canExtended = true,
                protocol =
                    DiscoveryProtocol.J1939
            ),

            /*
             * Additional logical discovery profiles.
             */
            ScanProfile(
                "UDS11",
                "UDS CAN 11-bit",
                ScanType.ELM_STANDARD,
                "6",
                500000,
                false,
                DiscoveryProtocol.UDS_CAN_11
            ),

            ScanProfile(
                "UDS29",
                "UDS CAN 29-bit",
                ScanType.ELM_STANDARD,
                "7",
                500000,
                true,
                DiscoveryProtocol.UDS_CAN_29
            ),

            ScanProfile(
                "UDS-EXT",
                "UDS Extended Addressing",
                ScanType.ELM_STANDARD,
                "6",
                500000,
                false,
                DiscoveryProtocol.UDS_EXTENDED_ADDRESSING
            ),

            ScanProfile(
                "GENERIC-CAN",
                "Generic CAN",
                ScanType.ELM_STANDARD,
                "6",
                500000,
                false,
                DiscoveryProtocol.GENERIC_CAN
            ),

            ScanProfile(
                "ISO9141-9600",
                "ISO 9141 9600",
                ScanType.ELM_STANDARD,
                "3",
                9600,
                false,
                DiscoveryProtocol.ISO9141
            ),

            ScanProfile(
                "ISO9141-4800",
                "ISO 9141 4800",
                ScanType.ELM_STANDARD,
                "3",
                4800,
                false,
                DiscoveryProtocol.ISO9141
            ),

            ScanProfile(
                "KWPFAST-A",
                "KWP FAST Alternate",
                ScanType.ELM_STANDARD,
                "5",
                10400,
                false,
                DiscoveryProtocol.KWP2000
            ),

            ScanProfile(
                "KWP2000-A",
                "KWP2000 Alternate",
                ScanType.ELM_STANDARD,
                "4",
                10400,
                false,
                DiscoveryProtocol.KWP2000
            ),

            ScanProfile(
                "ISO14230",
                "ISO 14230 Generic",
                ScanType.ELM_STANDARD,
                "5",
                10400,
                false,
                DiscoveryProtocol.KWP2000
            ),

            ScanProfile(
                "CAN11-250K",
                "CAN 11-bit 250K",
                ScanType.ELM_STANDARD,
                "8",
                250000,
                false,
                DiscoveryProtocol.UDS_CAN_11
            ),

            ScanProfile(
                "CAN29-250K",
                "CAN 29-bit 250K",
                ScanType.ELM_STANDARD,
                "9",
                250000,
                true,
                DiscoveryProtocol.UDS_CAN_29
            ),

            ScanProfile(
                "CAN11-500K",
                "CAN 11-bit 500K",
                ScanType.ELM_STANDARD,
                "6",
                500000,
                false,
                DiscoveryProtocol.UDS_CAN_11
            ),

            ScanProfile(
                "CAN29-500K",
                "CAN 29-bit 500K",
                ScanType.ELM_STANDARD,
                "7",
                500000,
                true,
                DiscoveryProtocol.UDS_CAN_29
            )
        )
    }

    // ============================================================
    // RAW HEX UTILITIES
    // ============================================================

    private fun normalizeHex(
        input: String
    ): String {

        return input
            .uppercase(
                Locale.US
            )
            .replace(
                Regex(
                    "[^0-9A-F\\s]"
                ),
                " "
            )
            .replace(
                Regex(
                    "\\s+"
                ),
                " "
            )
            .trim()
    }

    private fun hexToBytes(
        input: String
    ): ByteArray {

        val normalized =
            normalizeHex(
                input
            )

        if (
            normalized.isBlank()
        ) {
            return ByteArray(0)
        }

        val tokens =
            normalized.split(
                Regex("\\s+")
            )

        val output =
            ArrayList<Byte>()

        for (
        token in tokens
        ) {

            if (
                token.length == 2 &&
                token.all {
                    it in
                            "0123456789ABCDEF"
                }
            ) {

                try {

                    output.add(
                        token
                            .toInt(16)
                            .toByte()
                    )

                } catch (_: Exception) {
                }
            }
        }

        return output.toByteArray()
    }

    private fun bytesToPrintableAscii(
        bytes: ByteArray
    ): String {

        return bytes
            .map { it.toInt() and 0xFF }
            .filter {
                it in 0x20..0x7E
            }
            .map {
                it.toChar()
            }
            .joinToString("")
            .trim()
    }

    // ============================================================
    // PROGRESS
    // ============================================================

    private fun notifyProgress(
        current: Int,
        total: Int,
        code: String,
        name: String
    ) {

        try {
            progressListener?.invoke(
                current,
                total,
                code,
                name
            )
        } catch (_: Exception) {
        }
    }

    // ============================================================
    // STOP
    // ============================================================

    fun stopScan() {

        stopRequested.set(
            true
        )
    }

    // ============================================================
    // CLEAR
    // ============================================================

    fun clearLearnedProfiles(
        context: Context
    ) {

        /*
         * EcuProfileStore API in the existing application may contain
         * other source-specific profiles. Do not blindly clear them.
         *
         * If the existing store exposes a source-filtered delete,
         * it should be used there.
         */
        try {

            val profiles =
                EcuProfileStore
                    .getAll(
                        context
                    )

            profiles
                .filter {
                    it.source ==
                            ConnectionSource
                                .activeSource
                                .name
                }
                .forEach {

                    try {
                        EcuProfileStore.delete(
                            context,
                            it.id
                        )
                    } catch (_: Exception) {
                    }
                }

        } catch (_: Exception) {
        }
    }

    fun clearRuntimeState() {

        try {
            EcuRuntimeState.clear()
        } catch (_: Exception) {
        }
    }

    // ============================================================
    // SELECTED PROFILE
    // ============================================================

    fun connectSelectedProfile(
        index: Int
    ): ProtocolResult? {

        val profile =
            scanProfiles
                .getOrNull(index)
                ?: return null

        commandCounter.set(0)

        rawDatabase.clear()
        commandResults.clear()

        if (
            !isTransportConnected()
        ) {
            return ProtocolResult(
                success = false,
                profile = profile
            )
        }

        if (
            !configureProfile(
                profile
            )
        ) {
            return ProtocolResult(
                success = false,
                profile = profile
            )
        }

        val result =
            probeProfile(
                profile = profile,
                maxCommands = 500
            )

        if (
            result.success
        ) {

            saveSuccessfulLearnedProfile(
                result
            )
        }

        return result
    }

    // ============================================================
    // RAW DATABASE ACCESS
    // ============================================================

    fun getRawResponses():
            Map<String, List<String>> {

        return rawDatabase
            .mapValues {
                it.value.toList()
            }
    }

    fun getCommandResults():
            List<UnknownEcuCommandResult> {

        return commandResults.toList()
    }
}