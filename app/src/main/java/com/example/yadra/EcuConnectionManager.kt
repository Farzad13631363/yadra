package com.example.yadra

object EcuConnectionManager {

    @Volatile
    var connected: Boolean = false
        private set

    @Volatile
    var protocol: String = ""
        private set

    @Volatile
    var lastResponse: String = ""
        private set

    /**
     * وضعیت فعلی اتصال ELM327 / ECU را با منبع فعال هماهنگ می‌کند.
     */
    @Synchronized
    fun syncCurrentConnectionState() {

        when (ConnectionSource.activeSource) {

            ConnectionSource.Source.WIFI -> {

                connected =
                    YadraConnectionManager.elmConnected &&
                            YadraConnectionManager.ecuConnected

                protocol =
                    YadraConnectionManager.detectedProtocol.trim()

                if (protocol.isBlank()) {
                    protocol = "Unknown"
                }

                lastResponse =
                    if (connected) {
                        "4100"
                    } else {
                        "No ECU response"
                    }
            }

            ConnectionSource.Source.BLUETOOTH -> {

                connected =
                    BluetoothConnectionManager.elmConnected &&
                            BluetoothConnectionManager.ecuConnected

                protocol =
                    BluetoothConnectionManager.protocol.trim()

                if (protocol.isBlank()) {
                    protocol = "Unknown"
                }

                lastResponse =
                    if (connected) {
                        "4100"
                    } else {
                        "No ECU response"
                    }
            }

            ConnectionSource.Source.NONE -> {

                connected = false
                protocol = ""
                lastResponse = ""
            }
        }
    }

    /**
     * اتصال ECU با توجه به منبع فعال.
     *
     * توجه:
     * در حالت Wi-Fi، initializeElm327()
     * خودش ECU را با PID 0100 تست می‌کند.
     *
     * در حالت Bluetooth، فرض می‌شود ECU هنگام
     * اتصال Bluetooth قبلاً تست شده است.
     */
    @Synchronized
    fun connectToEcu(): Boolean {

        connected = false
        protocol = ""
        lastResponse = ""

        return when (ConnectionSource.activeSource) {

            ConnectionSource.Source.WIFI ->
                connectWifi()

            ConnectionSource.Source.BLUETOOTH ->
                connectBluetooth()

            ConnectionSource.Source.NONE -> {

                lastResponse =
                    "ELM327 is not connected"

                false
            }
        }
    }

    /**
     * -------------------------
     * Wi-Fi
     * -------------------------
     */
    private fun connectWifi(): Boolean {

        if (!YadraConnectionManager.elmConnected) {

            connected = false
            protocol = ""

            lastResponse =
                "Wi-Fi ELM327 is not connected"

            return false
        }

        /*
         * ابتدا پروتکل موجود را نگه می‌داریم.
         * اگر خالی باشد، از ELM327 درخواست ATDP می‌کنیم.
         */
        protocol =
            YadraConnectionManager.detectedProtocol.trim()

        if (protocol.isBlank()) {

            protocol =
                YadraConnectionManager
                    .sendCommand("ATDP")
                    .clean()
        }

        if (protocol.isBlank()) {
            protocol = "Unknown"
        }

        /*
         * initializeElm327():
         *
         * ATZ
         * ATE0
         * ATL0
         * ATS0
         * ATH0
         * ATSP0
         * ATDP
         * 0100
         *
         * را مدیریت می‌کند و در نهایت
         * ecuConnected را تنظیم می‌کند.
         */
        val initialized =
            YadraConnectionManager.initializeElm327()

        /*
         * بعد از initialize دوباره پروتکل را
         * از Manager می‌گیریم تا مقدار واقعی
         * آخرین اتصال ثبت شود.
         */
        protocol =
            YadraConnectionManager.detectedProtocol
                .trim()

        if (protocol.isBlank()) {
            protocol = "Unknown"
        }

        connected =
            initialized &&
                    YadraConnectionManager.elmConnected &&
                    YadraConnectionManager.ecuConnected

        lastResponse =
            if (connected) {
                "4100"
            } else {
                YadraConnectionManager.lastConnectError
                    .takeIf { it.isNotBlank() }
                    ?: "No ECU response"
            }

        return connected
    }

    /**
     * -------------------------
     * Bluetooth
     * -------------------------
     *
     * در Bluetooth فرض می‌شود ECU هنگام
     * اتصال ELM327 قبلاً تست شده است.
     *
     * بنابراین در اینجا دوباره ATDP یا 0100
     * ارسال نمی‌کنیم.
     */
    private fun connectBluetooth(): Boolean {

        if (!BluetoothConnectionManager.elmConnected) {

            connected = false
            protocol = ""

            lastResponse =
                "Bluetooth ELM327 is not connected"

            return false
        }

        protocol =
            BluetoothConnectionManager.protocol
                .trim()

        if (protocol.isBlank()) {
            protocol = "Unknown"
        }

        connected =
            BluetoothConnectionManager.elmConnected &&
                    BluetoothConnectionManager.ecuConnected

        lastResponse =
            if (connected) {
                "4100"
            } else {
                "No ECU response"
            }

        return connected
    }

    /**
     * خواندن RPM از ECU.
     *
     * PID:
     * 010C
     *
     * پاسخ استاندارد:
     * 41 0C A B
     *
     * فرمول:
     * ((A * 256) + B) / 4
     */
    @Synchronized
    fun readRpm(): Int? {

        /*
         * ابتدا وضعیت واقعی اتصال را بررسی می‌کنیم.
         */
        syncCurrentConnectionState()

        if (!connected) {

            lastResponse =
                "ECU is not connected"

            return null
        }

        return when (ConnectionSource.activeSource) {

            ConnectionSource.Source.WIFI -> {

                val rpm =
                    YadraConnectionManager.readRpm()

                if (rpm != null) {

                    lastResponse =
                        "RPM: $rpm"

                } else {

                    lastResponse =
                        YadraConnectionManager.lastConnectError
                            .takeIf { it.isNotBlank() }
                            ?: "RPM read failed"
                }

                rpm
            }

            ConnectionSource.Source.BLUETOOTH -> {

                try {

                    val response =
                        BluetoothConnectionManager
                            .sendActiveCommand("010C")

                    lastResponse =
                        response.clean()

                    parseRpm(response)

                } catch (e: Exception) {

                    lastResponse =
                        e.message
                            ?.takeIf { it.isNotBlank() }
                            ?: "RPM read failed"

                    null
                }
            }

            ConnectionSource.Source.NONE -> {

                connected = false
                protocol = ""
                lastResponse =
                    "No active connection source"

                null
            }
        }
    }

    /**
     * بررسی پاسخ مثبت ECU.
     *
     * مثال:
     * 41 00 ...
     */
    private fun isPositiveResponse(
        response: String
    ): Boolean {

        val normalized =
            normalizeHex(response)

        return normalized.contains("4100")
    }

    /**
     * تبدیل پاسخ ELM327 به RPM.
     *
     * نمونه پاسخ:
     *
     * 41 0C 1A F8
     *
     * A = 1A
     * B = F8
     *
     * RPM = ((0x1A * 256) + 0xF8) / 4
     */
    private fun parseRpm(
        response: String
    ): Int? {

        val normalized =
            normalizeHex(response)

        val index =
            normalized.indexOf("410C")

        if (index < 0) {
            return null
        }

        /*
         * 410C + AA + BB
         *
         * حداقل 8 کاراکتر لازم است:
         *
         * 41 0C AA BB
         * بدون فاصله:
         * 410CAABB
         */
        if (normalized.length < index + 8) {
            return null
        }

        return try {

            val a =
                normalized.substring(
                    index + 4,
                    index + 6
                ).toInt(16)

            val b =
                normalized.substring(
                    index + 6,
                    index + 8
                ).toInt(16)

            val rpm =
                ((a * 256) + b) / 4

            /*
             * RPM منفی یا غیرمنطقی را قبول نمی‌کنیم.
             */
            if (rpm < 0) {
                null
            } else {
                rpm
            }

        } catch (_: NumberFormatException) {

            null
        }
    }

    /**
     * فقط اعداد Hex را باقی می‌گذارد.
     *
     * مثال:
     *
     * "41 0C 1A F8\r\n>"
     *
     * تبدیل می‌شود به:
     *
     * "410C1AF8"
     */
    private fun normalizeHex(
        response: String
    ): String {

        return response
            .uppercase()
            .replace(
                Regex("[^0-9A-F]"),
                ""
            )
    }

    /**
     * تمیز کردن پاسخ متنی ELM327.
     */
    private fun String.clean(): String {

        return replace(
            "\r",
            " "
        )
            .replace(
                "\n",
                " "
            )
            .replace(
                ">",
                " "
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    /**
     * قطع وضعیت منطقی اتصال ECU.
     *
     * توجه:
     * این تابع فقط وضعیت EcuConnectionManager
     * را پاک می‌کند و اتصال فیزیکی ELM327 را
     * قطع نمی‌کند.
     */
    @Synchronized
    fun disconnect() {

        connected = false
        protocol = ""
        lastResponse = ""
    }
}