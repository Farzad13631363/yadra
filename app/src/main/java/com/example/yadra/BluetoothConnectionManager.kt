package com.example.yadra

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.UUID

class BluetoothConnectionManager(
    context: Context
) {

    interface Listener {

        fun onDeviceFound(
            device: BluetoothDevice
        )

        fun onDiscoveryStarted() {
        }

        fun onDiscoveryFinished() {
        }

        fun onError(
            message: String
        )
    }

    companion object {

        private val SPP_UUID =
            UUID.fromString(
                "00001101-0000-1000-8000-00805F9B34FB"
            )

        private const val RESPONSE_TIMEOUT = 5000L
        private const val IDLE_TIMEOUT = 1500L

        private const val PREFS_NAME =
            "YADRA_BLUETOOTH"

        private const val KEY_ELM_ADDRESS =
            "saved_elm_address"

        @Volatile
        var elmConnected: Boolean = false
            private set

        @Volatile
        var ecuConnected: Boolean = false
            private set

        @Volatile
        var protocol: String = ""
            private set

        @Volatile
        var elmVersion: String = ""
            private set

        private var activeDeviceAddress:
                String? = null

        private var activeSocket:
                BluetoothSocket? = null

        private var activeWriter:
                PrintWriter? = null

        private var activeReader:
                BufferedReader? = null

        private val commandLock =
            Any()

        @JvmStatic
        fun sendActiveCommand(
            command: String
        ): String {

            synchronized(commandLock) {

                val writer =
                    activeWriter
                        ?: return ""

                val reader =
                    activeReader
                        ?: return ""

                if (!elmConnected) {
                    return ""
                }

                return try {

                    val cleanCommand =
                        command
                            .trim()
                            .replace(
                                "\r",
                                ""
                            )
                            .replace(
                                "\n",
                                ""
                            )

                    writer.print(
                        "$cleanCommand\r"
                    )

                    writer.flush()

                    val response =
                        StringBuilder()

                    val startTime =
                        System.currentTimeMillis()

                    var lastDataTime =
                        startTime

                    while (true) {

                        if (reader.ready()) {

                            val ch =
                                reader.read()

                            if (ch == -1) {
                                break
                            }

                            response.append(
                                ch.toChar()
                            )

                            lastDataTime =
                                System.currentTimeMillis()

                            if (
                                ch.toChar() == '>'
                            ) {
                                break
                            }

                        } else {

                            val now =
                                System.currentTimeMillis()

                            if (
                                now -
                                lastDataTime >=
                                IDLE_TIMEOUT
                            ) {
                                break
                            }

                            if (
                                now -
                                startTime >=
                                RESPONSE_TIMEOUT
                            ) {
                                break
                            }

                            Thread.sleep(50)
                        }
                    }

                    response
                        .toString()
                        .trim()

                } catch (_: Exception) {

                    elmConnected = false
                    ecuConnected = false

                    closeSocket()

                    activeDeviceAddress = null

                    if (
                        ConnectionSource.activeSource ==
                        ConnectionSource.Source.BLUETOOTH
                    ) {
                        ConnectionSource.clear()
                    }

                    ""
                }
            }
        }

        @JvmStatic
        fun disconnectActive() {

            synchronized(commandLock) {

                closeSocket()

                elmConnected = false
                ecuConnected = false
                protocol = ""
                elmVersion = ""
                activeDeviceAddress = null

                if (
                    ConnectionSource.activeSource ==
                    ConnectionSource.Source.BLUETOOTH
                ) {
                    ConnectionSource.clear()
                }
            }
        }

        fun getSavedElmAddress(
            context: Context
        ): String? {

            return context
                .applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .getString(
                    KEY_ELM_ADDRESS,
                    null
                )
        }

        private fun saveElmAddress(
            context: Context,
            address: String
        ) {

            context
                .applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .edit()
                .putString(
                    KEY_ELM_ADDRESS,
                    address
                )
                .apply()
        }

        fun clearSavedElmAddress(
            context: Context
        ) {

            context
                .applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .edit()
                .remove(
                    KEY_ELM_ADDRESS
                )
                .apply()
        }

        private fun closeSocket() {

            try {
                activeWriter?.close()
            } catch (_: Exception) {
            }

            try {
                activeReader?.close()
            } catch (_: Exception) {
            }

            try {
                activeSocket?.close()
            } catch (_: Exception) {
            }

            activeWriter = null
            activeReader = null
            activeSocket = null
        }
    }

    private val appContext =
        context.applicationContext

    private val bluetoothAdapter:
            BluetoothAdapter? by lazy {

        val bluetoothManager =
            appContext.getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as android.bluetooth.BluetoothManager

        bluetoothManager.adapter
    }

    private var listener:
            Listener? = null

    private var discoveryReceiver:
            BroadcastReceiver? = null

    var onDeviceFound:
            ((BluetoothDevice) -> Unit)? = null

    fun setListener(
        listener: Listener?
    ) {
        this.listener = listener
    }

    private fun hasConnectPermission(): Boolean {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.S
        ) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_CONNECT
        ) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun hasScanPermission(): Boolean {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.S
        ) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_SCAN
        ) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun hasBluetooth(): Boolean {
        return bluetoothAdapter != null
    }

    fun isBluetoothEnabled(): Boolean {

        if (!hasConnectPermission()) {
            return false
        }

        return try {

            bluetoothAdapter?.isEnabled == true

        } catch (_: SecurityException) {

            false
        }
    }

    @SuppressLint("MissingPermission")
    fun getDeviceName(
        device: BluetoothDevice
    ): String {

        if (!hasConnectPermission()) {
            return "Unknown Bluetooth Device"
        }

        return try {

            device.name
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Unknown Bluetooth Device"

        } catch (_: Exception) {

            "Unknown Bluetooth Device"
        }
    }

    @SuppressLint("MissingPermission")
    fun getDeviceAddress(
        device: BluetoothDevice
    ): String {

        if (!hasConnectPermission()) {
            return "Unknown address"
        }

        return try {

            device.address

        } catch (_: Exception) {

            "Unknown address"
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices():
            Set<BluetoothDevice> {

        if (!hasConnectPermission()) {
            return emptySet()
        }

        return try {

            bluetoothAdapter?.bondedDevices
                ?: emptySet()

        } catch (_: Exception) {

            emptySet()
        }
    }

    @SuppressLint("MissingPermission")
    fun findSavedElmDevice():
            BluetoothDevice? {

        if (!hasConnectPermission()) {
            return null
        }

        val savedAddress =
            getSavedElmAddress(
                appContext
            )

        if (
            savedAddress.isNullOrBlank()
        ) {
            return null
        }

        for (
        device in getPairedDevices()
        ) {

            if (
                getDeviceAddress(device) ==
                savedAddress
            ) {

                return device
            }
        }

        return null
    }

    fun hasSavedElmDevice(): Boolean {

        return !getSavedElmAddress(
            appContext
        ).isNullOrBlank()
    }

    fun registerReceiver() {

        if (discoveryReceiver != null) {
            return
        }

        val receiver =
            object : BroadcastReceiver() {

                override fun onReceive(
                    context: Context?,
                    intent: Intent?
                ) {

                    if (intent == null) {
                        return
                    }

                    when (intent.action) {

                        BluetoothDevice.ACTION_FOUND -> {

                            if (!hasConnectPermission()) {
                                return
                            }

                            val device:
                                    BluetoothDevice?

                            if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.TIRAMISU
                            ) {

                                device =
                                    intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE,
                                        BluetoothDevice::class.java
                                    )

                            } else {

                                @Suppress("DEPRECATION")

                                device =
                                    intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE
                                    )
                            }

                            if (device != null) {

                                onDeviceFound?.invoke(
                                    device
                                )

                                listener?.onDeviceFound(
                                    device
                                )
                            }
                        }

                        BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {

                            listener?.onDiscoveryStarted()
                        }

                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {

                            listener?.onDiscoveryFinished()
                        }
                    }
                }
            }

        discoveryReceiver =
            receiver

        val filter =
            IntentFilter().apply {

                addAction(
                    BluetoothDevice.ACTION_FOUND
                )

                addAction(
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED
                )

                addAction(
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                )
            }

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                appContext.registerReceiver(
                    receiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )

            } else {

                @Suppress("DEPRECATION")

                appContext.registerReceiver(
                    receiver,
                    filter
                )
            }

        } catch (_: Exception) {

            discoveryReceiver = null

            listener?.onError(
                "Bluetooth receiver registration failed"
            )
        }
    }

    fun unregisterReceiver() {

        val receiver =
            discoveryReceiver
                ?: return

        try {

            appContext.unregisterReceiver(
                receiver
            )

        } catch (_: Exception) {
        }

        discoveryReceiver = null
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {

        if (!hasScanPermission()) {

            listener?.onError(
                "Bluetooth scan permission not granted"
            )

            return false
        }

        if (!hasConnectPermission()) {

            listener?.onError(
                "Bluetooth connect permission not granted"
            )

            return false
        }

        val adapter =
            bluetoothAdapter
                ?: return false

        return try {

            registerReceiver()

            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }

            val started =
                adapter.startDiscovery()

            if (!started) {

                listener?.onError(
                    "Bluetooth discovery could not start"
                )
            }

            started

        } catch (_: Exception) {

            listener?.onError(
                "Bluetooth discovery failed"
            )

            false
        }
    }

    @SuppressLint("MissingPermission")
    fun cancelDiscovery() {

        if (!hasScanPermission()) {
            return
        }

        try {

            bluetoothAdapter?.cancelDiscovery()

        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun connectToElm327(
        device: BluetoothDevice
    ): Boolean {

        if (!hasConnectPermission()) {

            listener?.onError(
                "Bluetooth connect permission not granted"
            )

            return false
        }

        cancelDiscovery()

        val address =
            getDeviceAddress(device)

        /*
         * اگر اتصال همین دستگاه هنوز سالم است،
         * دوباره Socket را باز نمی‌کنیم.
         */
        if (
            elmConnected &&
            activeSocket != null &&
            activeWriter != null &&
            activeReader != null &&
            activeDeviceAddress == address
        ) {

            ConnectionSource.setBluetooth()

            return true
        }

        /*
         * اتصال قبلی را فقط در صورت وجود
         * اتصال جدید می‌بندیم.
         */
        synchronized(commandLock) {

            closeSocket()

            elmConnected = false
            ecuConnected = false
            protocol = ""
            elmVersion = ""
            activeDeviceAddress = null
        }

        var socket:
                BluetoothSocket? = null

        try {

            /*
             * روش اصلی SPP
             */
            try {

                socket =
                    device.createRfcommSocketToServiceRecord(
                        SPP_UUID
                    )

                socket.connect()

            } catch (_: Exception) {

                try {
                    socket?.close()
                } catch (_: Exception) {
                }

                socket = null

                /*
                 * روش fallback برای بعضی ELM327ها
                 */
                val method =
                    device.javaClass.getMethod(
                        "createRfcommSocket",
                        Int::class.javaPrimitiveType
                    )

                socket =
                    method.invoke(
                        device,
                        1
                    ) as BluetoothSocket

                socket.connect()
            }

            /*
             * Socket با موفقیت متصل شد.
             */
            activeSocket =
                socket

            activeWriter =
                PrintWriter(
                    OutputStreamWriter(
                        socket.outputStream
                    ),
                    true
                )

            activeReader =
                BufferedReader(
                    InputStreamReader(
                        socket.inputStream
                    )
                )

            activeDeviceAddress =
                address

            elmConnected = true
            ecuConnected = false
            protocol = ""
            elmVersion = ""

            /*
             * ذخیره آدرس برای اتصال خودکار بعدی
             */
            saveElmAddress(
                appContext,
                address
            )

            ConnectionSource.setBluetooth()

            /*
             * راه‌اندازی ELM327
             */
            sendActiveCommand(
                "ATZ"
            )

            Thread.sleep(1000)

            sendActiveCommand(
                "ATE0"
            )

            sendActiveCommand(
                "ATL0"
            )

            sendActiveCommand(
                "ATS0"
            )

            sendActiveCommand(
                "ATH0"
            )

            sendActiveCommand(
                "ATSP0"
            )

            /*
             * نسخه ELM
             */
            elmVersion =
                extractElmVersion(
                    sendActiveCommand(
                        "ATI"
                    )
                )

            /*
             * پروتکل
             */
            protocol =
                extractProtocol(
                    sendActiveCommand(
                        "ATDP"
                    )
                )

            /*
             * تست ECU
             */
            val response =
                sendActiveCommand(
                    "0100"
                )

            val normalized =
                response
                    .uppercase()
                    .replace(
                        Regex("[^0-9A-F]"),
                        ""
                    )

            ecuConnected =
                normalized.contains(
                    "4100"
                )

            /*
             * حتی اگر ECU جواب ندهد،
             * خود ELM327 همچنان Connected است.
             */
            return true

        } catch (e: Exception) {

            synchronized(commandLock) {

                closeSocket()

                elmConnected = false
                ecuConnected = false
                protocol = ""
                elmVersion = ""
                activeDeviceAddress = null
            }

            if (
                ConnectionSource.activeSource ==
                ConnectionSource.Source.BLUETOOTH
            ) {

                ConnectionSource.clear()
            }

            listener?.onError(
                "ELM327 connection failed: ${
                    e.message ?: "Unknown error"
                }"
            )

            return false
        }
    }

    @Synchronized
    fun sendCommand(
        command: String
    ): String {

        return sendActiveCommand(
            command
        )
    }

    fun readRpm(): Int? {

        if (!elmConnected) {
            return null
        }

        val response =
            sendActiveCommand(
                "010C"
            )

        val normalized =
            response
                .uppercase()
                .replace(
                    Regex("[^0-9A-F]"),
                    ""
                )

        val index =
            normalized.indexOf(
                "410C"
            )

        if (index < 0) {
            return null
        }

        if (
            normalized.length <
            index + 8
        ) {
            return null
        }

        return try {

            val a =
                normalized
                    .substring(
                        index + 4,
                        index + 6
                    )
                    .toInt(16)

            val b =
                normalized
                    .substring(
                        index + 6,
                        index + 8
                    )
                    .toInt(16)

            ((a * 256) + b) / 4

        } catch (_: Exception) {

            null
        }
    }

    fun disconnect() {

        disconnectActive()
    }

    private fun extractElmVersion(
        response: String
    ): String {

        val lines =
            response
                .replace(
                    "\r",
                    "\n"
                )
                .split("\n")
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }

        for (line in lines) {

            if (
                line.contains(
                    "ELM",
                    ignoreCase = true
                )
            ) {

                return line
            }
        }

        return ""
    }

    private fun extractProtocol(
        response: String
    ): String {

        return response
            .replace(
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
}