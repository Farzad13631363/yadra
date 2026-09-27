package com.example.yadra

import android.net.Network
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class ObdConnectionManager {

    companion object {
        private const val ELM327_IP = "192.168.0.10"
        private const val ELM327_PORT = 35000

        private const val CONNECT_TIMEOUT = 5000
        private const val RESPONSE_TIMEOUT = 5000
    }

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    private var network: Network? = null

    fun setNetwork(network: Network?) {
        this.network = network
    }

    fun isConnected(): Boolean {
        return socket?.isConnected == true &&
                socket?.isClosed == false
    }

    /**
     * اتصال به ELM327
     */
    fun connect(): Boolean {

        disconnect()

        return try {

            socket =
                if (network != null) {
                    network!!.socketFactory.createSocket()
                } else {
                    Socket()
                }

            socket!!.connect(
                InetSocketAddress(
                    ELM327_IP,
                    ELM327_PORT
                ),
                CONNECT_TIMEOUT
            )

            socket!!.soTimeout =
                RESPONSE_TIMEOUT

            writer =
                PrintWriter(
                    OutputStreamWriter(
                        socket!!.getOutputStream()
                    ),
                    true
                )

            reader =
                BufferedReader(
                    InputStreamReader(
                        socket!!.getInputStream()
                    )
                )

            true

        } catch (_: Exception) {

            disconnect()

            false
        }
    }

    /**
     * راه‌اندازی ELM327
     */
    fun initialize(): String {

        if (!isConnected()) {
            if (!connect()) {
                return ""
            }
        }

        sendCommand("ATZ")

        Thread.sleep(1000)

        sendCommand("ATE0")
        sendCommand("ATL0")
        sendCommand("ATS0")
        sendCommand("ATH0")
        sendCommand("ATSP0")

        return sendCommand("ATDP")
    }

    /**
     * ارسال فرمان خام به ELM327
     */
    fun sendCommand(command: String): String {

        if (!isConnected()) {

            if (!connect()) {
                return ""
            }
        }

        return try {

            writer?.print(
                command.trim() + "\r"
            )

            writer?.flush()

            readResponse()

        } catch (_: Exception) {

            ""
        }
    }

    /**
     * تشخیص پروتکل OBD
     */
    fun detectProtocol(): String {

        return sendCommand("ATDP")
            .replace("\r", " ")
            .replace("\n", " ")
            .replace(">", " ")
            .trim()
    }

    /**
     * خواندن RPM
     *
     * PID 010C
     */
    fun readRpm(): Int? {

        val response =
            sendCommand("010C")

        return parseRpm(response)
    }

    /**
     * خواندن سرعت خودرو
     *
     * PID 010D
     */
    fun readVehicleSpeed(): Int? {

        val response =
            sendCommand("010D")

        return parseSingleBytePid(
            response,
            "41 0D"
        )
    }

    /**
     * خواندن دمای مایع خنک‌کننده
     *
     * PID 0105
     */
    fun readCoolantTemperature(): Int? {

        val response =
            sendCommand("0105")

        val value =
            parseSingleBytePid(
                response,
                "41 05"
            )

        return value?.minus(40)
    }

    /**
     * خواندن موقعیت دریچه گاز
     *
     * PID 0111
     */
    fun readThrottlePosition(): Double? {

        val response =
            sendCommand("0111")

        val value =
            parseSingleBytePid(
                response,
                "41 11"
            )
                ?: return null

        return value * 100.0 / 255.0
    }

    /**
     * خواندن فشار منیفولد
     *
     * PID 010B
     */
    fun readMap(): Int? {

        val response =
            sendCommand("010B")

        return parseSingleBytePid(
            response,
            "41 0B"
        )
    }

    /**
     * خواندن MAF
     *
     * PID 0110
     *
     * فرمول:
     * ((A * 256) + B) / 100
     */
    fun readMaf(): Double? {

        val response =
            sendCommand("0110")

        val bytes =
            extractResponseBytes(
                response,
                "41 10"
            )

        if (bytes.size < 2) {
            return null
        }

        val value =
            bytes[0] * 256 + bytes[1]

        return value / 100.0
    }

    /**
     * خواندن Fuel Trim کوتاه‌مدت
     *
     * PID 0106
     */
    fun readShortTermFuelTrim(): Double? {

        val response =
            sendCommand("0106")

        val value =
            parseSingleBytePid(
                response,
                "41 06"
            )
                ?: return null

        return ((value - 128) * 100.0) / 128.0
    }

    /**
     * خواندن Fuel Trim بلندمدت
     *
     * PID 0107
     */
    fun readLongTermFuelTrim(): Double? {

        val response =
            sendCommand("0107")

        val value =
            parseSingleBytePid(
                response,
                "41 07"
            )
                ?: return null

        return ((value - 128) * 100.0) / 128.0
    }

    /**
     * خواندن DTC
     *
     * Mode 03
     */
    fun readTroubleCodes(): String {

        return sendCommand("03")
    }

    /**
     * پاک کردن DTC
     *
     * Mode 04
     */
    fun clearTroubleCodes(): String {

        return sendCommand("04")
    }

    /**
     * خواندن ولتاژ ECU
     */
    fun readVoltage(): Double? {

        val response =
            sendCommand("ATRV")

        val regex =
            Regex("""(\d+(?:\.\d+)?)\s*V""")

        val match =
            regex.find(response.uppercase())

        return match
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()
    }

    /**
     * تبدیل RPM
     *
     * 41 0C A B
     *
     * RPM = ((A * 256) + B) / 4
     */
    private fun parseRpm(
        response: String
    ): Int? {

        val bytes =
            extractResponseBytes(
                response,
                "41 0C"
            )

        if (bytes.size < 2) {
            return null
        }

        val value =
            bytes[0] * 256 + bytes[1]

        return value / 4
    }

    /**
     * PIDهایی که فقط یک Byte دارند
     */
    private fun parseSingleBytePid(
        response: String,
        header: String
    ): Int? {

        val bytes =
            extractResponseBytes(
                response,
                header
            )

        if (bytes.isEmpty()) {
            return null
        }

        return bytes[0]
    }

    /**
     * استخراج Byteهای پاسخ ELM327
     */
    private fun extractResponseBytes(
        response: String,
        header: String
    ): List<Int> {

        val normalized =
            response
                .uppercase()
                .replace("\r", " ")
                .replace("\n", " ")
                .replace(">", " ")

        val headerBytes =
            header
                .uppercase()
                .split(" ")

        val tokens =
            Regex("""\b[0-9A-F]{2}\b""")
                .findAll(normalized)
                .map {
                    it.value
                }
                .toList()

        if (tokens.isEmpty()) {
            return emptyList()
        }

        val start =
            findHeaderPosition(
                tokens,
                headerBytes
            )

        if (start == -1) {
            return emptyList()
        }

        return tokens
            .drop(
                start + headerBytes.size
            )
            .mapNotNull {
                it.toIntOrNull(16)
            }
    }

    private fun findHeaderPosition(
        tokens: List<String>,
        header: List<String>
    ): Int {

        if (header.isEmpty()) {
            return -1
        }

        for (
        i in 0..tokens.size - header.size
        ) {

            var matches = true

            for (
            j in header.indices
            ) {

                if (
                    tokens[i + j] !=
                    header[j]
                ) {
                    matches = false
                    break
                }
            }

            if (matches) {
                return i
            }
        }

        return -1
    }

    /**
     * دریافت پاسخ تا رسیدن به >
     */
    private fun readResponse(): String {

        val response =
            StringBuilder()

        val buffer =
            CharArray(1024)

        val startTime =
            System.currentTimeMillis()

        while (
            System.currentTimeMillis() -
            startTime <
            RESPONSE_TIMEOUT
        ) {

            try {

                if (
                    reader?.ready() == true
                ) {

                    val count =
                        reader?.read(buffer)
                            ?: -1

                    if (count > 0) {

                        response.append(
                            String(
                                buffer,
                                0,
                                count
                            )
                        )

                        if (
                            response.contains(">")
                        ) {
                            break
                        }
                    }

                } else {

                    Thread.sleep(50)
                }

            } catch (_: Exception) {

                break
            }
        }

        return response.toString()
    }

    /**
     * قطع اتصال
     */
    fun disconnect() {

        try {
            reader?.close()
        } catch (_: Exception) {
        }

        try {
            writer?.close()
        } catch (_: Exception) {
        }

        try {
            socket?.close()
        } catch (_: Exception) {
        }

        reader = null
        writer = null
        socket = null
    }
}