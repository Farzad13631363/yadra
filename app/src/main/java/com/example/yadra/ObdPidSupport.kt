package com.example.yadra

import java.util.Locale

object ObdPidSupport {

    // =====================================================
    // MAIN
    // =====================================================

    fun getSupportedPids(): Set<String>? {

        if (!isElmConnected()) {
            return null
        }

        val supported =
            LinkedHashSet<String>()

        // -------------------------------------------------
        // STEP 1
        // Standard OBD-II bitmap detection
        // -------------------------------------------------

        detectSupportedPids(
            supported
        )

        // -------------------------------------------------
        // STEP 2
        // Directly test every PID that exists in the
        // sensor catalog but was not reported by bitmap.
        //
        // This is important because some ECUs do not
        // correctly advertise every available PID.
        // -------------------------------------------------

        detectCatalogPidsDirectly(
            supported
        )

        return supported
    }

    // =====================================================
    // DETECT ECU SUPPORTED PIDS USING BITMAP
    // =====================================================

    private fun detectSupportedPids(
        supported: MutableSet<String>
    ): Boolean {

        var foundAny = false

        val ranges =
            listOf(
                "0100" to 0x00,
                "0120" to 0x20,
                "0140" to 0x40,
                "0160" to 0x60,
                "0180" to 0x80,
                "01A0" to 0xA0
            )

        for (
        (command, basePid) in ranges
        ) {

            if (!isElmConnected()) {
                break
            }

            val response =
                sendCommand(
                    command
                )

            if (response.isBlank()) {
                continue
            }

            val bytes =
                extractBytesAfterResponse(
                    response,
                    command
                )

            if (bytes.size < 4) {
                continue
            }

            for (byteIndex in 0 until 4) {

                val value =
                    bytes[byteIndex]

                for (bit in 0..7) {

                    val mask =
                        1 shl (7 - bit)

                    val isSupported =
                        (
                                value and mask
                                ) != 0

                    if (!isSupported) {
                        continue
                    }

                    val pidNumber =
                        basePid +
                                byteIndex * 8 +
                                bit +
                                1

                    if (
                        pidNumber < 1 ||
                        pidNumber > 0xFF
                    ) {
                        continue
                    }

                    val pid =
                        String.format(
                            Locale.US,
                            "01%02X",
                            pidNumber
                        )

                    supported.add(
                        pid
                    )

                    foundAny = true
                }
            }
        }

        return foundAny
    }

    // =====================================================
    // DIRECT CATALOG PID DETECTION
    // =====================================================
    //
    // Some ECUs have useful PIDs that are not correctly
    // advertised by the standard bitmap.
    //
    // We therefore test every PID already present in
    // ObdSensorCatalog.
    //
    // No new / guessed PID is generated here.
    //
    // =====================================================

    private fun detectCatalogPidsDirectly(
        supported: MutableSet<String>
    ) {

        if (!isElmConnected()) {
            return
        }

        val catalogPids =
            ObdSensorCatalog.sensors
                .map {
                    it.pid.trim().uppercase(
                        Locale.US
                    )
                }
                .filter {
                    isValidObdPid(it)
                }
                .distinct()

        for (pid in catalogPids) {

            // ارتباط را قبل از هر درخواست بررسی کن.
            if (!isElmConnected()) {
                break
            }

            // اگر Bitmap قبلاً این PID را پیدا کرده،
            // دوباره درخواست نده.
            if (
                supported.contains(pid)
            ) {
                continue
            }

            val response =
                sendCommand(
                    pid
                )

            if (response.isBlank()) {
                continue
            }

            // پاسخ باید واقعاً متعلق به همین PID باشد.
            val bytes =
                extractBytesAfterResponse(
                    response,
                    pid
                )

            if (bytes.isNotEmpty()) {

                supported.add(
                    pid
                )
            }
        }
    }

    // =====================================================
    // VALIDATE STANDARD OBD COMMAND
    // =====================================================

    private fun isValidObdPid(
        pid: String
    ): Boolean {

        if (pid.length != 4) {
            return false
        }

        if (
            !pid.all {
                it in "0123456789ABCDEF"
            }
        ) {
            return false
        }

        val service =
            pid.substring(
                0,
                2
            )

        val pidByte =
            pid.substring(
                2,
                4
            )

        // فعلاً فقط Service 01 را مستقیماً تست می‌کنیم.
        //
        // PIDهای اختصاصی CNG در مرحله بعد می‌توانند
        // با ساختار فرمان جداگانه اضافه شوند.
        if (service != "01") {
            return false
        }

        return pidByte != "00"
    }

    // =====================================================
    // SEND COMMAND
    // =====================================================

    private fun sendCommand(
        command: String
    ): String {

        return when (
            ConnectionSource.activeSource
        ) {

            ConnectionSource.Source.WIFI -> {

                if (
                    !YadraConnectionManager.elmConnected
                ) {
                    ""
                } else {

                    try {

                        YadraConnectionManager
                            .sendCommand(
                                command
                            )

                    } catch (
                        _: Exception
                    ) {

                        ""
                    }
                }
            }

            ConnectionSource.Source.BLUETOOTH -> {

                if (
                    !BluetoothConnectionManager.elmConnected
                ) {
                    ""
                } else {

                    try {

                        BluetoothConnectionManager
                            .sendActiveCommand(
                                command
                            )

                    } catch (
                        _: Exception
                    ) {

                        ""
                    }
                }
            }

            ConnectionSource.Source.NONE -> {
                ""
            }
        }
    }

    // =====================================================
    // CHECK ELM CONNECTION
    // =====================================================

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

    // =====================================================
    // EXTRACT RESPONSE DATA
    // =====================================================

    private fun extractBytesAfterResponse(
        response: String,
        command: String
    ): List<Int> {

        if (
            response.isBlank()
        ) {
            return emptyList()
        }

        val upper =
            response.uppercase(
                Locale.US
            )

        // -------------------------------------------------
        // INVALID RESPONSES
        // -------------------------------------------------

        if (
            upper.contains("NO DATA") ||
            upper.contains("NODATA") ||
            upper.contains("ERROR") ||
            upper.contains("UNABLE TO CONNECT") ||
            upper.contains("UNABLETOCONNECT") ||
            upper.contains("STOPPED") ||
            upper.contains("?")
        ) {
            return emptyList()
        }

        val cleaned =
            upper
                .replace(
                    "SEARCHING...",
                    " "
                )
                .replace(
                    "SEARCHING",
                    " "
                )
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

        // -------------------------------------------------
        // COMMAND
        // -------------------------------------------------

        val commandHex =
            command
                .replace(
                    Regex(
                        "[^0-9A-F]"
                    ),
                    ""
                )
                .uppercase(
                    Locale.US
                )

        if (
            commandHex.length < 4
        ) {
            return emptyList()
        }

        val service =
            commandHex.substring(
                0,
                2
            )

        val pid =
            commandHex.substring(
                2,
                4
            )

        val serviceValue =
            service.toIntOrNull(
                16
            )
                ?: return emptyList()

        val responseService =
            String.format(
                Locale.US,
                "%02X",
                serviceValue + 0x40
            )

        // =================================================
        // TOKEN PARSING
        // =================================================

        val tokens =
            Regex(
                """(?i)[0-9A-F]{2}"""
            )
                .findAll(
                    cleaned
                )
                .map {
                    it.value.uppercase(
                        Locale.US
                    )
                }
                .toList()

        val headerIndex =
            findHeader(
                tokens,
                responseService,
                pid
            )

        if (
            headerIndex >= 0
        ) {

            val data =
                tokens
                    .drop(
                        headerIndex + 2
                    )
                    .mapNotNull {
                        it.toIntOrNull(
                            16
                        )
                    }

            if (
                data.isNotEmpty()
            ) {
                return data
            }
        }

        // =================================================
        // FALLBACK HEX PARSING
        // =================================================

        val hexOnly =
            cleaned.replace(
                Regex(
                    "[^0-9A-F]"
                ),
                ""
            )

        val header =
            responseService +
                    pid

        val headerPosition =
            hexOnly.indexOf(
                header
            )

        if (
            headerPosition < 0
        ) {
            return emptyList()
        }

        val dataStart =
            headerPosition +
                    header.length

        if (
            dataStart >=
            hexOnly.length
        ) {
            return emptyList()
        }

        val result =
            ArrayList<Int>()

        var index =
            dataStart

        while (
            index + 2 <=
            hexOnly.length
        ) {

            val token =
                hexOnly.substring(
                    index,
                    index + 2
                )

            val value =
                token.toIntOrNull(
                    16
                )

            if (
                value != null
            ) {

                result.add(
                    value
                )
            }

            index += 2
        }

        return result
    }

    // =====================================================
    // FIND RESPONSE HEADER
    // =====================================================

    private fun findHeader(
        tokens: List<String>,
        service: String,
        pid: String
    ): Int {

        if (
            tokens.size < 2
        ) {
            return -1
        }

        for (
        i in 0 until tokens.size - 1
        ) {

            if (
                tokens[i].equals(
                    service,
                    ignoreCase = true
                ) &&
                tokens[i + 1].equals(
                    pid,
                    ignoreCase = true
                )
            ) {

                return i
            }
        }

        return -1
    }
}