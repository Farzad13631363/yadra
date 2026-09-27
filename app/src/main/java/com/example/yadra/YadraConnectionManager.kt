package com.example.yadra

import android.net.Network
import android.os.Build
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

object YadraConnectionManager {

    private const val TAG = "YadraConnectionManager"

    private const val ELM_IP = "192.168.0.10"
    private const val ELM_PORT = 35000

    private const val CONNECT_TIMEOUT = 5000
    private const val READ_TIMEOUT = 5000

    private const val DEFAULT_POST_PROMPT_QUIET_MS = 0L

    @Volatile
    private var network: Network? = null

    @Volatile
    var elmConnected: Boolean = false
        private set

    @Volatile
    var connecting: Boolean = false
        private set

    @Volatile
    private var wifiConnected: Boolean = false

    @Volatile
    var lastConnectError: String = ""
        private set

    @Volatile
    var elmVersion: String = ""
        private set

    @Volatile
    var detectedProtocol: String = ""
        private set

    @Volatile
    var ecuConnected: Boolean = false
        private set

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var writer: PrintWriter? = null


    // =========================================================
    // NETWORK
    // =========================================================

    @Synchronized
    fun setNetwork(network: Network?) {

        this.network = network

        Log.d(
            TAG,
            "Network set: $network"
        )
    }

    fun getNetwork(): Network? {
        return network
    }

    @Synchronized
    fun setWifiConnected(value: Boolean) {

        wifiConnected = value

        if (!value) {
            ecuConnected = false
        }

        Log.d(
            TAG,
            "Wi-Fi connected = $value"
        )
    }

    fun isWifiConnected(): Boolean {
        return wifiConnected
    }


    // =========================================================
    // CONNECT
    // =========================================================

    @Synchronized
    fun connectToElm327(): Boolean {

        if (
            elmConnected &&
            socket?.isConnected == true &&
            socket?.isClosed == false
        ) {
            return true
        }

        if (connecting) {
            return false
        }

        connecting = true
        lastConnectError = ""

        disconnectInternal()

        try {

            /*
             * Android 10+:
             *
             * WifiScanActivity receives the fresh Network
             * from WifiNetworkSpecifier and passes it here.
             *
             * Therefore we use Network.socketFactory.
             */
            val newSocket: Socket =
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    network != null
                ) {

                    Log.d(
                        TAG,
                        "Using Network.socketFactory: $network"
                    )

                    network!!.socketFactory.createSocket()

                } else {

                    /*
                     * Android 6 - Android 9:
                     *
                     * WifiScanActivity already calls:
                     *
                     * connectivityManager.bindProcessToNetwork(network)
                     *
                     * Therefore NORMAL Socket() is intentional.
                     */
                    Log.d(
                        TAG,
                        "Using normal Socket() on API ${Build.VERSION.SDK_INT}"
                    )

                    Socket()
                }

            newSocket.soTimeout = READ_TIMEOUT
            newSocket.tcpNoDelay = true
            newSocket.keepAlive = true

            Log.d(
                TAG,
                "Connecting to $ELM_IP:$ELM_PORT"
            )

            newSocket.connect(
                InetSocketAddress(
                    ELM_IP,
                    ELM_PORT
                ),
                CONNECT_TIMEOUT
            )

            socket = newSocket

            inputStream =
                newSocket.getInputStream()

            writer =
                PrintWriter(
                    newSocket.getOutputStream(),
                    false
                )

            elmConnected = true

            Log.d(
                TAG,
                "ELM327 TCP connected"
            )

            return true

        } catch (e: SocketTimeoutException) {

            lastConnectError =
                "Connection timeout"

            Log.e(
                TAG,
                "ELM connection timeout",
                e
            )

            disconnectInternal()

            return false

        } catch (e: IOException) {

            lastConnectError =
                e.message ?: "I/O connection error"

            Log.e(
                TAG,
                "ELM connection I/O error",
                e
            )

            disconnectInternal()

            return false

        } catch (e: Exception) {

            lastConnectError =
                e.message ?: e.javaClass.simpleName

            Log.e(
                TAG,
                "ELM connection failed",
                e
            )

            disconnectInternal()

            return false

        } finally {

            connecting = false
        }
    }


    /*
     * Compatibility method.
     *
     * KwpTestActivity and other activities can call:
     *
     * YadraConnectionManager.connectToElm327Transport()
     *
     * without changing the actual Wi-Fi connection logic.
     */
    fun connectToElm327Transport(): Boolean {
        return connectToElm327()
    }


    fun isConnected(): Boolean {

        return elmConnected &&
                socket?.isConnected == true &&
                socket?.isClosed == false
    }


    fun isElmConnected(): Boolean {
        return isConnected()
    }


    // =========================================================
    // DISCONNECT
    // =========================================================

    @Synchronized
    fun disconnectElm() {

        Log.d(
            TAG,
            "Disconnecting ELM327"
        )

        disconnectInternal()
    }


    /*
     * Compatibility method.
     *
     * KwpTestActivity can call:
     *
     * YadraConnectionManager.disconnect()
     */
    fun disconnect() {
        disconnectElm()
    }


    @Synchronized
    private fun disconnectInternal() {

        elmConnected = false
        ecuConnected = false

        try {
            writer?.close()
        } catch (_: Exception) {
        }

        try {
            inputStream?.close()
        } catch (_: Exception) {
        }

        try {
            socket?.close()
        } catch (_: Exception) {
        }

        writer = null
        inputStream = null
        socket = null
    }


    // =========================================================
    // COMMAND
    // =========================================================

    @Synchronized
    fun sendCommand(
        command: String
    ): String {

        return sendCommand(
            command,
            DEFAULT_POST_PROMPT_QUIET_MS
        )
    }


    @Synchronized
    fun sendCommand(
        command: String,
        postPromptQuietMs: Long
    ): String {

        if (!isConnected()) {

            Log.w(
                TAG,
                "sendCommand: ELM not connected"
            )

            return ""
        }

        return try {

            sendRawCommand(
                command,
                postPromptQuietMs
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Command failed: $command",
                e
            )

            ""
        }
    }


    // =========================================================
    // RAW COMMAND
    // =========================================================

    @Synchronized
    private fun sendRawCommand(
        command: String,
        postPromptQuietMs: Long
    ): String {

        val currentWriter =
            writer ?: return ""

        val currentInput =
            inputStream ?: return ""

        val cleanCommand =
            command
                .trim()
                .replace("\r", "")
                .replace("\n", "")

        if (cleanCommand.isEmpty()) {
            return ""
        }

        Log.d(
            TAG,
            "TX: $cleanCommand"
        )

        currentWriter.print(cleanCommand)
        currentWriter.print("\r")
        currentWriter.flush()

        return readResponse(
            currentInput,
            postPromptQuietMs
        )
    }


    // =========================================================
    // RESPONSE
    // =========================================================

    private fun readResponse(
        input: InputStream,
        postPromptQuietMs: Long
    ): String {

        val result =
            StringBuilder()

        var promptSeen = false

        val startTime =
            System.currentTimeMillis()

        while (
            System.currentTimeMillis() - startTime <
            READ_TIMEOUT
        ) {

            try {

                val value =
                    input.read()

                if (value == -1) {
                    break
                }

                val ch =
                    value.toChar()

                if (ch == '>') {

                    promptSeen = true
                    break
                }

                result.append(ch)

            } catch (_: SocketTimeoutException) {

                break

            } catch (e: IOException) {

                Log.e(
                    TAG,
                    "Read failed",
                    e
                )

                break
            }
        }


        /*
         * Some KWP responses can arrive immediately
         * after the ELM prompt.
         */
        if (
            promptSeen &&
            postPromptQuietMs > 0
        ) {

            var lastDataTime =
                System.currentTimeMillis()

            while (
                System.currentTimeMillis() - lastDataTime <
                postPromptQuietMs
            ) {

                var receivedSomething = false

                try {

                    while (input.available() > 0) {

                        val value =
                            input.read()

                        if (value == -1) {
                            break
                        }

                        val ch =
                            value.toChar()

                        if (ch != '>') {
                            result.append(ch)
                        }

                        receivedSomething = true
                    }

                } catch (_: IOException) {

                    break
                }

                if (receivedSomething) {

                    lastDataTime =
                        System.currentTimeMillis()

                } else {

                    try {

                        Thread.sleep(10)

                    } catch (_: InterruptedException) {

                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
        }

        return result
            .toString()
            .trim()
    }


    // =========================================================
    // INITIALIZE
    // =========================================================

    @Synchronized
    fun initializeElm327(): Boolean {

        if (!isConnected()) {

            lastConnectError =
                "ELM327 is not connected"

            return false
        }

        try {

            /*
             * Reset ELM.
             */
            sendCommand("ATZ")

            try {

                Thread.sleep(1200)

            } catch (_: InterruptedException) {

                Thread.currentThread().interrupt()
            }


            /*
             * Basic ELM configuration.
             */
            sendCommand("ATE0")
            sendCommand("ATL0")
            sendCommand("ATS0")
            sendCommand("ATH1")


            /*
             * ELM version.
             */
            val version =
                sendCommand("ATI").trim()

            if (
                version.isNotBlank() &&
                !version.contains(
                    "ERROR",
                    ignoreCase = true
                )
            ) {

                elmVersion = version

            } else {

                elmVersion = ""
            }


            /*
             * Keep automatic protocol selection.
             */
            val protocolResult =
                sendCommand("ATSP0")

            Log.d(
                TAG,
                "ATSP0: $protocolResult"
            )


            /*
             * Detect protocol.
             */
            val protocol =
                sendCommand("ATDP").trim()

            detectedProtocol =
                if (
                    protocol.isNotBlank() &&
                    !protocol.contains(
                        "ERROR",
                        ignoreCase = true
                    )
                ) {

                    protocol

                } else {

                    ""
                }


            Log.d(
                TAG,
                "ELM version: $elmVersion"
            )

            Log.d(
                TAG,
                "Detected protocol: $detectedProtocol"
            )


            /*
             * Test ECU.
             */
            ecuConnected =
                testEcuConnection()

            if (!ecuConnected) {

                lastConnectError =
                    "ECU did not respond"

                return false
            }

            return true

        } catch (e: Exception) {

            ecuConnected = false

            lastConnectError =
                e.message ?: e.javaClass.simpleName

            Log.e(
                TAG,
                "ELM initialization failed",
                e
            )

            return false
        }
    }


    // =========================================================
    // PREPARE SAVED PROTOCOL
    // =========================================================

    fun prepareElm327ForSavedProtocol(): Boolean {

        if (!isConnected()) {
            return false
        }

        return try {

            sendCommand("ATE0")
            sendCommand("ATL0")
            sendCommand("ATS0")
            sendCommand("ATH1")

            val protocol =
                sendCommand("ATDP").trim()

            detectedProtocol =
                protocol

            Log.d(
                TAG,
                "Current protocol: $protocol"
            )

            protocol.isNotBlank()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "prepareElm327ForSavedProtocol failed",
                e
            )

            false
        }
    }


    // =========================================================
    // ECU TEST
    // =========================================================

    fun testEcuConnection(): Boolean {

        if (!isConnected()) {
            return false
        }

        return try {

            val response =
                sendCommand("0100")

            val normalized =
                response
                    .replace(" ", "")
                    .replace("\r", "")
                    .replace("\n", "")
                    .uppercase()

            Log.d(
                TAG,
                "0100 RX: $normalized"
            )

            if (normalized.isBlank()) {
                return false
            }

            if (
                normalized.contains("NODATA") ||
                normalized.contains("ERROR") ||
                normalized.contains("UNABLETOCONNECT")
            ) {
                return false
            }


            /*
             * ECU negative response still proves
             * communication with the ECU.
             */
            if (normalized.contains("7F")) {
                return true
            }


            /*
             * Normal OBD response.
             */
            if (normalized.contains("4100")) {
                return true
            }

            return true

        } catch (e: Exception) {

            Log.e(
                TAG,
                "ECU test failed",
                e
            )

            false
        }
    }


    // =========================================================
    // REGISTER DETECTED ECU
    // =========================================================

    fun registerDetectedEcu(protocol: String) {

        ecuConnected = true

        if (protocol.isNotBlank()) {

            detectedProtocol =
                protocol.trim()
        }

        Log.d(
            TAG,
            "ECU registered. Protocol: $detectedProtocol"
        )
    }


    // =========================================================
    // RPM
    // =========================================================

    fun readRpm(): Int? {

        if (!isConnected()) {
            return null
        }

        val response =
            sendCommand("010C")

        val clean =
            response
                .replace(" ", "")
                .replace("\r", "")
                .replace("\n", "")
                .uppercase()

        val index =
            clean.indexOf("410C")

        if (index < 0) {
            return null
        }

        if (index + 8 > clean.length) {
            return null
        }

        return try {

            val a =
                clean.substring(
                    index + 4,
                    index + 6
                ).toInt(16)

            val b =
                clean.substring(
                    index + 6,
                    index + 8
                ).toInt(16)

            ((a * 256) + b) / 4

        } catch (_: Exception) {

            null
        }
    }
}