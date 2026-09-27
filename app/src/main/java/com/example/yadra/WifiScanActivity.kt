package com.example.yadra

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WifiScanActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "YADRA_WIFI"
        private const val TARGET_SSID = "WIFI_OBDII"

        private const val REQUEST_WIFI_PERMISSIONS = 1001
        private const val REQUEST_LOCATION_PERMISSION = 1002

        private const val LEGACY_WIFI_WAIT_ATTEMPTS = 30
        private const val LEGACY_NETWORK_WAIT_ATTEMPTS = 15
    }

    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager

    private lateinit var txtScanStatus: TextView
    private lateinit var networkContainer: LinearLayout
    private lateinit var btnScanAgain: Button

    @Volatile
    private var wifiNetwork: Network? = null

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var scanReceiver: BroadcastReceiver? = null
    private var networkReceiver: BroadcastReceiver? = null

    private var receiverRegistered = false
    private var networkReceiverRegistered = false

    @Volatile
    private var activityDestroyed = false

    @Volatile
    private var elmConnectionInProgress = false

    @Volatile
    private var allowAutoConnect = true

    @Volatile
    private var processBoundToWifi = false

    // جلوگیری از نمایش چندباره Dialog
    private var locationDialogShowing = false

    // =========================================================
    // CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityDestroyed = false

        try {

            setContentView(R.layout.activity_wifi_scan)

            wifiManager =
                applicationContext.getSystemService(
                    Context.WIFI_SERVICE
                ) as WifiManager

            connectivityManager =
                getSystemService(
                    Context.CONNECTIVITY_SERVICE
                ) as ConnectivityManager

            txtScanStatus =
                findViewById(R.id.txtScanStatus)

            networkContainer =
                findViewById(R.id.networkContainer)

            btnScanAgain =
                findViewById(R.id.btnScanAgain)

            btnScanAgain.setOnClickListener {

                if (activityDestroyed) {
                    return@setOnClickListener
                }

                try {

                    if (
                        YadraConnectionManager.elmConnected &&
                        ConnectionSource.activeSource ==
                        ConnectionSource.Source.WIFI
                    ) {

                        txtScanStatus.text =
                            getString(
                                R.string.wifi_connected,
                                TARGET_SSID
                            )

                        return@setOnClickListener
                    }

                    allowAutoConnect = true
                    elmConnectionInProgress = false

                    unregisterNetworkRequest()

                    if (!YadraConnectionManager.elmConnected) {

                        wifiNetwork = null

                        YadraConnectionManager
                            .setNetwork(null)

                        YadraConnectionManager
                            .setWifiConnected(false)

                        if (
                            ConnectionSource.activeSource ==
                            ConnectionSource.Source.WIFI
                        ) {
                            ConnectionSource.clear()
                        }
                    }

                    startWifiScan()

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Scan button error",
                        e
                    )
                }
            }

            registerNetworkReceiver()

            if (
                YadraConnectionManager.elmConnected &&
                ConnectionSource.activeSource ==
                ConnectionSource.Source.WIFI
            ) {

                txtScanStatus.text =
                    getString(
                        R.string.wifi_connected,
                        TARGET_SSID
                    )

                restoreLegacyWifiNetwork()

            } else {

                startWifiScan()
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "onCreate failed",
                e
            )

            if (::txtScanStatus.isInitialized) {

                txtScanStatus.setText(
                    R.string.wifi_initialization_error
                )
            }
        }
    }

    // =========================================================
    // SCAN
    // =========================================================

    private fun startWifiScan() {

        if (activityDestroyed) {
            return
        }

        try {

            if (
                YadraConnectionManager.elmConnected &&
                ConnectionSource.activeSource ==
                ConnectionSource.Source.WIFI
            ) {

                txtScanStatus.text =
                    getString(
                        R.string.wifi_connected,
                        TARGET_SSID
                    )

                return
            }

            // -----------------------------------------------
            // Permission
            // -----------------------------------------------

            if (!hasRequiredPermissions()) {

                requestRequiredPermissions()
                return
            }

            // -----------------------------------------------
            // Android 6 تا 12
            // Location باید روشن باشد
            // -----------------------------------------------

            if (
                Build.VERSION.SDK_INT <
                Build.VERSION_CODES.TIRAMISU
            ) {

                if (!isLocationEnabled()) {

                    txtScanStatus.setText(
                        R.string.wifi_location_disabled
                    )

                    showLocationDisabledDialog()

                    return
                }
            }

            // -----------------------------------------------
            // Wi-Fi
            // -----------------------------------------------

            if (!isWifiEnabledSafely()) {

                txtScanStatus.setText(
                    R.string.wifi_disabled
                )

                try {

                    startActivity(
                        Intent(
                            Settings.ACTION_WIFI_SETTINGS
                        )
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Unable to open Wi-Fi settings",
                        e
                    )
                }

                return
            }

            networkContainer.removeAllViews()

            txtScanStatus.setText(
                R.string.wifi_scanning
            )

            registerScanReceiver()

            @Suppress("DEPRECATION")
            val started =
                wifiManager.startScan()

            if (!started) {

                Log.w(
                    TAG,
                    "wifiManager.startScan() returned false"
                )

                showScanResults()

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q &&
                    allowAutoConnect &&
                    !YadraConnectionManager.elmConnected &&
                    !elmConnectionInProgress &&
                    networkCallback == null
                ) {

                    connectToTargetNetwork()
                }
            }

        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "Wi-Fi scan permission denied",
                e
            )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q &&
                hasRequiredPermissions()
            ) {

                connectToTargetNetwork()

            } else {

                txtScanStatus.setText(
                    R.string.wifi_permission_required
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "startWifiScan failed",
                e
            )

            txtScanStatus.setText(
                R.string.wifi_scan_failed
            )
        }
    }

    // =========================================================
    // LOCATION DIALOG
    // =========================================================

    private fun showLocationDisabledDialog() {

        if (activityDestroyed) {
            return
        }

        if (locationDialogShowing) {
            return
        }

        locationDialogShowing = true

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("GPS خاموش است")
                .setMessage(
                    "برای اسکن Wi-Fi و اتصال به ELM327، Location/GPS دستگاه باید روشن باشد."
                )
                .setCancelable(false)
                .setNegativeButton("لغو") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("روشن کردن GPS") { _, _ ->

                    try {

                        startActivity(
                            Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS
                            )
                        )

                    } catch (e: Exception) {

                        Log.e(
                            TAG,
                            "Unable to open Location settings",
                            e
                        )
                    }
                }
                .create()

        dialog.setOnDismissListener {

            locationDialogShowing = false

        }

        dialog.show()
    }

    // =========================================================
    // WIFI ENABLED
    // =========================================================

    private fun isWifiEnabledSafely(): Boolean {

        return try {

            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled

        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "Cannot read Wi-Fi state",
                e
            )

            false
        }
    }

    // =========================================================
    // PERMISSIONS
    // =========================================================

    private fun hasRequiredPermissions(): Boolean {

        return try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) ==
                        PackageManager.PERMISSION_GRANTED

            } else {

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) ==
                        PackageManager.PERMISSION_GRANTED
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Permission check failed",
                e
            )

            false
        }
    }

    private fun requestRequiredPermissions() {

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    ),
                    REQUEST_WIFI_PERMISSIONS
                )

            } else {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    REQUEST_LOCATION_PERMISSION
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Permission request failed",
                e
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (activityDestroyed) {
            return
        }

        if (
            requestCode ==
            REQUEST_WIFI_PERMISSIONS ||
            requestCode ==
            REQUEST_LOCATION_PERMISSION
        ) {

            if (hasRequiredPermissions()) {

                allowAutoConnect = true

                startWifiScan()

            } else {

                txtScanStatus.setText(
                    R.string.wifi_permission_denied
                )
            }
        }
    }

    // =========================================================
    // LOCATION
    // =========================================================

    private fun isLocationEnabled(): Boolean {

        return try {

            val manager =
                getSystemService(
                    Context.LOCATION_SERVICE
                ) as LocationManager

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                manager.isLocationEnabled

            } else {

                @Suppress("DEPRECATION")
                Settings.Secure.getInt(
                    contentResolver,
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF
                ) !=
                        Settings.Secure.LOCATION_MODE_OFF
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Location state check failed",
                e
            )

            false
        }
    }

    // =========================================================
    // SCAN RECEIVER
    // =========================================================

    private fun registerScanReceiver() {

        if (receiverRegistered) {
            return
        }

        try {

            scanReceiver =
                object : BroadcastReceiver() {

                    override fun onReceive(
                        context: Context?,
                        intent: Intent?
                    ) {

                        if (activityDestroyed) {
                            return
                        }

                        try {

                            val updated =
                                intent?.getBooleanExtra(
                                    WifiManager.EXTRA_RESULTS_UPDATED,
                                    true
                                ) ?: true

                            Log.d(
                                TAG,
                                "Wi-Fi scan completed. updated=$updated"
                            )

                            showScanResults()

                        } catch (e: Exception) {

                            Log.e(
                                TAG,
                                "Scan receiver error",
                                e
                            )
                        }
                    }
                }

            val filter =
                IntentFilter(
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
                )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                registerReceiver(
                    scanReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )

            } else {

                @Suppress("DEPRECATION")
                registerReceiver(
                    scanReceiver,
                    filter
                )
            }

            receiverRegistered = true

        } catch (e: Exception) {

            Log.e(
                TAG,
                "registerScanReceiver failed",
                e
            )
        }
    }

    // =========================================================
    // SHOW RESULTS
    // =========================================================

    @Suppress("DEPRECATION")
    private fun showScanResults() {

        if (activityDestroyed) {
            return
        }

        try {

            val results =
                try {

                    wifiManager.scanResults
                        .sortedByDescending {
                            it.level
                        }

                } catch (e: SecurityException) {

                    Log.w(
                        TAG,
                        "scanResults unavailable: ${e.message}"
                    )

                    emptyList()
                }

            networkContainer.removeAllViews()

            if (results.isEmpty()) {

                txtScanStatus.setText(
                    R.string.wifi_no_networks
                )

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q &&
                    allowAutoConnect &&
                    !YadraConnectionManager.elmConnected &&
                    !elmConnectionInProgress &&
                    networkCallback == null
                ) {

                    connectToTargetNetwork()
                }

                return
            }

            val networks =
                LinkedHashMap<String, ScanResult>()

            for (result in results) {

                val ssid =
                    normalizeSsid(
                        result.SSID
                    )

                if (
                    ssid.isNotBlank() &&
                    ssid != "<unknown ssid>"
                ) {

                    if (!networks.containsKey(ssid)) {

                        networks[ssid] = result
                    }
                }
            }

            txtScanStatus.text =
                getString(
                    R.string.wifi_networks_found_count,
                    networks.size
                )

            var targetFound = false

            for (result in networks.values) {

                addNetworkItem(result)

                val ssid =
                    normalizeSsid(
                        result.SSID
                    )

                if (
                    ssid.equals(
                        TARGET_SSID,
                        ignoreCase = true
                    )
                ) {

                    targetFound = true
                }
            }

            if (
                targetFound &&
                allowAutoConnect &&
                !YadraConnectionManager.elmConnected &&
                !elmConnectionInProgress &&
                !YadraConnectionManager.connecting &&
                networkCallback == null
            ) {

                Log.d(
                    TAG,
                    "Target SSID found: $TARGET_SSID"
                )

                connectToTargetNetwork()

            } else if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q &&
                allowAutoConnect &&
                !YadraConnectionManager.elmConnected &&
                !elmConnectionInProgress &&
                !YadraConnectionManager.connecting &&
                networkCallback == null
            ) {

                connectToTargetNetwork()
            }

        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "Cannot read Wi-Fi scan results",
                e
            )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q &&
                hasRequiredPermissions()
            ) {

                connectToTargetNetwork()

            } else {

                txtScanStatus.setText(
                    R.string.wifi_permission_required
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "showScanResults failed",
                e
            )
        }
    }

    // =========================================================
    // NETWORK ITEM
    // =========================================================

    private fun addNetworkItem(
        result: ScanResult
    ) {

        try {

            val ssid =
                normalizeSsid(
                    result.SSID
                )

            val isTarget =
                ssid.equals(
                    TARGET_SSID,
                    ignoreCase = true
                )

            val signal =
                when {

                    result.level >= -55 ->
                        "▰▰▰▰"

                    result.level >= -67 ->
                        "▰▰▰▫"

                    result.level >= -75 ->
                        "▰▰▫▫"

                    result.level >= -85 ->
                        "▰▫▫▫"

                    else ->
                        "▫▫▫▫"
                }

            val item =
                TextView(this)

            item.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    setMargins(
                        0,
                        4,
                        0,
                        4
                    )
                }

            item.setPadding(
                18,
                16,
                18,
                16
            )

            item.gravity =
                Gravity.CENTER_VERTICAL

            item.textSize = 16f

            item.setTextColor(
                if (isTarget) {

                    Color.rgb(
                        0,
                        255,
                        80
                    )

                } else {

                    Color.rgb(
                        180,
                        255,
                        200
                    )
                }
            )

            item.setBackgroundColor(
                if (isTarget) {

                    Color.rgb(
                        8,
                        45,
                        20
                    )

                } else {

                    Color.rgb(
                        6,
                        25,
                        14
                    )
                }
            )

            item.text =
                if (isTarget) {

                    getString(
                        R.string.wifi_target_item,
                        ssid,
                        signal
                    )

                } else {

                    getString(
                        R.string.wifi_normal_item,
                        ssid,
                        signal
                    )
                }

            if (isTarget) {

                item.setOnClickListener {

                    if (activityDestroyed) {
                        return@setOnClickListener
                    }

                    allowAutoConnect = true

                    if (
                        !elmConnectionInProgress &&
                        !YadraConnectionManager.elmConnected
                    ) {

                        connectToTargetNetwork()
                    }
                }
            }

            networkContainer.addView(item)

        } catch (e: Exception) {

            Log.e(
                TAG,
                "addNetworkItem failed",
                e
            )
        }
    }

    // =========================================================
    // CONNECT TARGET
    // =========================================================

    private fun connectToTargetNetwork() {

        if (activityDestroyed) {
            return
        }

        if (YadraConnectionManager.elmConnected) {
            return
        }

        if (
            elmConnectionInProgress ||
            YadraConnectionManager.connecting
        ) {
            return
        }

        try {

            if (!hasRequiredPermissions()) {

                requestRequiredPermissions()
                return
            }

            txtScanStatus.text =
                getString(
                    R.string.wifi_connecting,
                    TARGET_SSID
                )

            Log.d(
                TAG,
                "Connecting to $TARGET_SSID"
            )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                connectUsingNetworkSpecifier()

            } else {

                connectUsingLegacyWifi()
            }

        } catch (e: SecurityException) {

            allowAutoConnect = false

            Log.e(
                TAG,
                "Wi-Fi connection permission error",
                e
            )

            txtScanStatus.setText(
                R.string.wifi_permission_required
            )

        } catch (e: Exception) {

            allowAutoConnect = false

            Log.e(
                TAG,
                "connectToTargetNetwork failed",
                e
            )

            txtScanStatus.setText(
                R.string.wifi_connection_failed
            )
        }
    }

    // =========================================================
    // ANDROID 10+
    // =========================================================

    private fun connectUsingNetworkSpecifier() {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.Q
        ) {
            return
        }

        if (networkCallback != null) {

            Log.d(
                TAG,
                "WIFI_OBDII request already active"
            )

            return
        }

        try {

            Log.d(
                TAG,
                "REQUESTING WIFI_OBDII"
            )

            val specifier =
                WifiNetworkSpecifier.Builder()
                    .setSsid(TARGET_SSID)
                    .build()

            val request =
                NetworkRequest.Builder()
                    .addTransportType(
                        NetworkCapabilities.TRANSPORT_WIFI
                    )
                    .removeCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    )
                    .setNetworkSpecifier(specifier)
                    .build()

            val callback =
                object :
                    ConnectivityManager.NetworkCallback() {

                    override fun onAvailable(
                        network: Network
                    ) {

                        Log.d(
                            TAG,
                            "WIFI_OBDII CONNECTED: $network"
                        )

                        wifiNetwork = network

                        YadraConnectionManager
                            .setNetwork(network)

                        YadraConnectionManager
                            .setWifiConnected(true)

                        ConnectionSource.setWifi()

                        postToUi {

                            txtScanStatus.text =
                                getString(
                                    R.string.wifi_connected,
                                    TARGET_SSID
                                )
                        }

                        connectToElm327()
                    }

                    override fun onUnavailable() {

                        Log.e(
                            TAG,
                            "WIFI_OBDII CONNECTION UNAVAILABLE"
                        )

                        networkCallback = null
                        wifiNetwork = null
                        allowAutoConnect = false

                        YadraConnectionManager
                            .setNetwork(null)

                        YadraConnectionManager
                            .setWifiConnected(false)

                        ConnectionSource.clear()

                        postToUi {

                            txtScanStatus.setText(
                                R.string.wifi_connection_failed
                            )
                        }
                    }

                    override fun onLost(
                        network: Network
                    ) {

                        Log.w(
                            TAG,
                            "WIFI_OBDII LOST: $network"
                        )

                        if (wifiNetwork != network) {
                            return
                        }

                        wifiNetwork = null
                        allowAutoConnect = false

                        try {

                            if (
                                YadraConnectionManager.elmConnected ||
                                elmConnectionInProgress
                            ) {

                                YadraConnectionManager
                                    .disconnectElm()
                            }

                        } catch (e: Exception) {

                            Log.e(
                                TAG,
                                "ELM disconnect after Wi-Fi loss failed",
                                e
                            )
                        }

                        YadraConnectionManager
                            .setNetwork(null)

                        YadraConnectionManager
                            .setWifiConnected(false)

                        ConnectionSource.clear()

                        postToUi {

                            txtScanStatus.setText(
                                R.string.wifi_disconnected
                            )
                        }
                    }
                }

            networkCallback = callback

            connectivityManager.requestNetwork(
                request,
                callback
            )

            Log.d(
                TAG,
                "Waiting for WIFI_OBDII..."
            )

        } catch (e: SecurityException) {

            networkCallback = null
            allowAutoConnect = false

            Log.e(
                TAG,
                "WIFI_OBDII SECURITY EXCEPTION",
                e
            )

            txtScanStatus.setText(
                R.string.wifi_permission_required
            )

        } catch (e: Exception) {

            networkCallback = null
            allowAutoConnect = false

            Log.e(
                TAG,
                "WIFI_OBDII REQUEST FAILED",
                e
            )

            txtScanStatus.text =
                "Wi-Fi connection error: ${e.message}"
        }
    }

    // =========================================================
    // ANDROID 5-9
    // =========================================================

    @Suppress("DEPRECATION")
    private fun connectUsingLegacyWifi() {

        try {

            val configured =
                wifiManager.configuredNetworks

            val existing =
                configured?.firstOrNull { config ->

                    normalizeSsid(config.SSID)
                        .equals(
                            TARGET_SSID,
                            ignoreCase = true
                        )
                }

            val networkId: Int

            if (existing != null) {

                networkId =
                    existing.networkId

            } else {

                val config =
                    WifiConfiguration()

                config.SSID =
                    "\"$TARGET_SSID\""

                config.allowedKeyManagement.set(
                    WifiConfiguration.KeyMgmt.NONE
                )

                networkId =
                    wifiManager.addNetwork(config)
            }

            if (networkId < 0) {

                allowAutoConnect = false

                txtScanStatus.setText(
                    R.string.wifi_configure_failed
                )

                return
            }

            wifiManager.disconnect()

            if (
                !wifiManager.enableNetwork(
                    networkId,
                    true
                )
            ) {

                allowAutoConnect = false

                txtScanStatus.setText(
                    R.string.wifi_enable_failed
                )

                return
            }

            wifiManager.reconnect()

            txtScanStatus.text =
                getString(
                    R.string.wifi_connecting,
                    TARGET_SSID
                )

            waitForLegacyConnection()

        } catch (e: SecurityException) {

            allowAutoConnect = false

            Log.e(
                TAG,
                "Legacy Wi-Fi permission error",
                e
            )

            txtScanStatus.setText(
                R.string.wifi_permission_required
            )

        } catch (e: Exception) {

            allowAutoConnect = false

            Log.e(
                TAG,
                "Legacy Wi-Fi connection failed",
                e
            )

            txtScanStatus.setText(
                R.string.wifi_connection_failed
            )
        }
    }

    // =========================================================
    // WAIT LEGACY
    // =========================================================

    @Suppress("DEPRECATION")
    private fun waitForLegacyConnection() {

        Thread {

            var connected = false

            for (
            attempt in
            0 until LEGACY_WIFI_WAIT_ATTEMPTS
            ) {

                if (activityDestroyed) {
                    return@Thread
                }

                try {

                    val info =
                        wifiManager.connectionInfo

                    val ssid =
                        normalizeSsid(
                            info?.ssid ?: ""
                        )

                    Log.d(
                        TAG,
                        "Legacy Wi-Fi attempt=$attempt ssid=$ssid"
                    )

                    if (
                        ssid.equals(
                            TARGET_SSID,
                            ignoreCase = true
                        )
                    ) {

                        connected = true
                        break
                    }

                } catch (e: SecurityException) {

                    Log.e(
                        TAG,
                        "Legacy connectionInfo permission error",
                        e
                    )

                    allowAutoConnect = false

                    postToUi {

                        txtScanStatus.setText(
                            R.string.wifi_permission_required
                        )
                    }

                    return@Thread

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "connectionInfo error",
                        e
                    )
                }

                try {

                    Thread.sleep(500)

                } catch (_: InterruptedException) {

                    return@Thread
                }
            }

            if (!connected) {

                allowAutoConnect = false

                postToUi {

                    txtScanStatus.setText(
                        R.string.wifi_connection_timeout
                    )
                }

                return@Thread
            }

            var network: Network? = null

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M
            ) {

                for (
                attempt in
                0 until LEGACY_NETWORK_WAIT_ATTEMPTS
                ) {

                    if (activityDestroyed) {
                        return@Thread
                    }

                    network =
                        findWifiNetwork()

                    if (network != null) {
                        break
                    }

                    try {

                        Thread.sleep(300)

                    } catch (_: InterruptedException) {

                        return@Thread
                    }
                }

                if (network == null) {

                    allowAutoConnect = false

                    postToUi {

                        txtScanStatus.setText(
                            R.string.wifi_network_not_ready
                        )
                    }

                    return@Thread
                }

                wifiNetwork = network

                Log.d(
                    TAG,
                    "Legacy Wi-Fi Network selected: $network"
                )

                YadraConnectionManager
                    .setNetwork(network)

            } else {

                network =
                    findWifiNetworkApi21()

                if (network == null) {

                    allowAutoConnect = false

                    postToUi {

                        txtScanStatus.setText(
                            R.string.wifi_network_not_ready
                        )
                    }

                    return@Thread
                }

                wifiNetwork = network

                if (!bindProcessToWifiForApi21()) {

                    allowAutoConnect = false

                    postToUi {

                        txtScanStatus.setText(
                            R.string.wifi_network_not_ready
                        )
                    }

                    return@Thread
                }
            }

            YadraConnectionManager
                .setWifiConnected(true)

            ConnectionSource.setWifi()

            postToUi {

                txtScanStatus.text =
                    getString(
                        R.string.wifi_connected,
                        TARGET_SSID
                    )

                connectToElm327()
            }

        }.start()
    }

    // =========================================================
    // FIND WIFI NETWORK API 23+
    // =========================================================

    @Suppress("DEPRECATION")
    private fun findWifiNetwork(): Network? {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.M
        ) {
            return findWifiNetworkApi21()
        }

        try {

            val currentSsid =
                normalizeSsid(
                    wifiManager
                        .connectionInfo
                        ?.ssid ?: ""
                )

            Log.d(
                TAG,
                "Current Wi-Fi SSID = $currentSsid"
            )

            if (
                !currentSsid.equals(
                    TARGET_SSID,
                    ignoreCase = true
                )
            ) {

                Log.d(
                    TAG,
                    "Current SSID is not $TARGET_SSID"
                )

                return null
            }

            val activeNetwork =
                connectivityManager.activeNetwork

            if (activeNetwork != null) {

                val capabilities =
                    connectivityManager
                        .getNetworkCapabilities(
                            activeNetwork
                        )

                val networkInfo =
                    connectivityManager
                        .getNetworkInfo(
                            activeNetwork
                        )

                Log.d(
                    TAG,
                    "Active Network = $activeNetwork"
                )

                if (
                    capabilities?.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI
                    ) == true &&
                    networkInfo?.isConnected == true
                ) {

                    Log.d(
                        TAG,
                        "Active Network is connected Wi-Fi: $activeNetwork"
                    )

                    return activeNetwork
                }
            }

            for (
            network in
            connectivityManager.allNetworks
            ) {

                val capabilities =
                    connectivityManager
                        .getNetworkCapabilities(
                            network
                        )
                        ?: continue

                if (
                    !capabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI
                    )
                ) {
                    continue
                }

                val networkInfo =
                    connectivityManager
                        .getNetworkInfo(network)

                Log.d(
                    TAG,
                    "Checking Wi-Fi Network=$network connected=${networkInfo?.isConnected}"
                )

                if (networkInfo?.isConnected == true) {

                    Log.d(
                        TAG,
                        "Found connected Wi-Fi Network: $network"
                    )

                    return network
                }
            }

        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "findWifiNetwork permission error",
                e
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "findWifiNetwork failed",
                e
            )
        }

        return null
    }

    // =========================================================
    // FIND WIFI NETWORK API 21-22
    // =========================================================

    @Suppress("DEPRECATION")
    private fun findWifiNetworkApi21(): Network? {

        try {

            val currentSsid =
                normalizeSsid(
                    wifiManager
                        .connectionInfo
                        ?.ssid ?: ""
                )

            if (
                !currentSsid.equals(
                    TARGET_SSID,
                    ignoreCase = true
                )
            ) {
                return null
            }

            for (
            network in
            connectivityManager.allNetworks
            ) {

                val info =
                    connectivityManager
                        .getNetworkInfo(network)
                        ?: continue

                if (
                    info.type ==
                    ConnectivityManager.TYPE_WIFI &&
                    info.isConnected
                ) {

                    return network
                }
            }

        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "findWifiNetworkApi21 permission error",
                e
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "findWifiNetworkApi21 failed",
                e
            )
        }

        return null
    }

    // =========================================================
    // RESTORE NETWORK
    // =========================================================

    private fun restoreLegacyWifiNetwork() {

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                val savedNetwork =
                    YadraConnectionManager
                        .getNetwork()

                if (savedNetwork != null) {

                    wifiNetwork =
                        savedNetwork

                    return
                }
            }

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M
            ) {

                if (wifiNetwork != null) {

                    YadraConnectionManager
                        .setNetwork(wifiNetwork)

                    return
                }

                val network =
                    findWifiNetwork()

                if (network != null) {

                    wifiNetwork =
                        network

                    YadraConnectionManager
                        .setNetwork(network)
                }

            } else {

                val network =
                    findWifiNetworkApi21()

                if (network != null) {

                    wifiNetwork =
                        network

                    bindProcessToWifiForApi21()

                    YadraConnectionManager
                        .setNetwork(network)
                }
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "restoreLegacyWifiNetwork failed",
                e
            )
        }
    }

    // =========================================================
    // BIND PROCESS TO WIFI - ANDROID 6+
    // =========================================================

    private fun bindProcessToWifi(): Boolean {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.M
        ) {
            return true
        }

        if (processBoundToWifi) {

            Log.d(
                TAG,
                "Process is already bound to Wi-Fi"
            )

            return true
        }

        val network =
            wifiNetwork
                ?: findWifiNetwork()

        if (network == null) {

            Log.e(
                TAG,
                "Cannot bind process: Wi-Fi Network is null"
            )

            return false
        }

        return try {

            Log.d(
                TAG,
                "Binding process to Wi-Fi Network: $network"
            )

            val result =
                connectivityManager
                    .bindProcessToNetwork(network)

            Log.d(
                TAG,
                "bindProcessToNetwork result = $result"
            )

            if (result) {

                processBoundToWifi = true

                Log.d(
                    TAG,
                    "Process successfully bound to Wi-Fi: $network"
                )

            } else {

                processBoundToWifi = false

                Log.e(
                    TAG,
                    "Process Wi-Fi binding failed: $network"
                )
            }

            result

        } catch (e: Exception) {

            processBoundToWifi = false

            Log.e(
                TAG,
                "bindProcessToNetwork failed",
                e
            )

            false
        }
    }

    // =========================================================
    // API 21-22 WIFI BIND
    // =========================================================

    @Suppress("DEPRECATION")
    private fun bindProcessToWifiForApi21(): Boolean {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {
            return true
        }

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.LOLLIPOP
        ) {
            return true
        }

        if (processBoundToWifi) {
            return true
        }

        val network =
            wifiNetwork
                ?: findWifiNetworkApi21()
                ?: return false

        return try {

            val method =
                ConnectivityManager::class.java
                    .getDeclaredMethod(
                        "setProcessDefaultNetwork",
                        Network::class.java
                    )

            val result =
                method.invoke(
                    null,
                    network
                ) as? Boolean ?: false

            processBoundToWifi = result

            Log.d(
                TAG,
                "API21/22 process bound to Wi-Fi: $result network=$network"
            )

            result

        } catch (e: Exception) {

            Log.e(
                TAG,
                "API21/22 Wi-Fi process binding failed",
                e
            )

            processBoundToWifi = false

            false
        }
    }

    // =========================================================
    // CLEAR PROCESS NETWORK BINDING
    // =========================================================

    @Suppress("DEPRECATION")
    private fun clearProcessNetworkBinding() {

        if (!processBoundToWifi) {
            return
        }

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M
            ) {

                connectivityManager
                    .bindProcessToNetwork(null)

            } else if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.LOLLIPOP
            ) {

                val method =
                    ConnectivityManager::class.java
                        .getDeclaredMethod(
                            "setProcessDefaultNetwork",
                            Network::class.java
                        )

                method.invoke(
                    null,
                    null
                )
            }

            Log.d(
                TAG,
                "Process Wi-Fi binding cleared"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "clearProcessNetworkBinding failed",
                e
            )
        }

        processBoundToWifi = false
    }

    // =========================================================
    // CONNECT ELM327
    // =========================================================

    private fun connectToElm327() {

        if (activityDestroyed) {
            return
        }

        if (YadraConnectionManager.elmConnected) {

            try {

                EcuConnectionManager
                    .syncCurrentConnectionState()

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "ECU sync failed",
                    e
                )
            }

            return
        }

        if (
            elmConnectionInProgress ||
            YadraConnectionManager.connecting
        ) {
            return
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M &&
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.Q
        ) {

            val network =
                wifiNetwork
                    ?: findWifiNetwork()

            if (network == null) {

                allowAutoConnect = false

                Log.e(
                    TAG,
                    "ELM connection aborted: connected Wi-Fi Network not found"
                )

                txtScanStatus.setText(
                    R.string.wifi_network_not_found
                )

                return
            }

            wifiNetwork = network

            Log.d(
                TAG,
                "Selected Wi-Fi Network = $network"
            )

            if (!bindProcessToWifi()) {

                allowAutoConnect = false

                Log.e(
                    TAG,
                    "ELM connection aborted: unable to bind process to Wi-Fi"
                )

                txtScanStatus.setText(
                    R.string.wifi_network_not_ready
                )

                return
            }

            YadraConnectionManager
                .setNetwork(network)
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            if (wifiNetwork == null) {

                wifiNetwork =
                    YadraConnectionManager
                        .getNetwork()
            }

            if (wifiNetwork == null) {

                allowAutoConnect = false

                Log.e(
                    TAG,
                    "ELM connection aborted: WIFI_OBDII Network is null"
                )

                txtScanStatus.setText(
                    R.string.wifi_network_not_found
                )

                return
            }

            Log.d(
                TAG,
                "Using WIFI_OBDII Network for ELM: $wifiNetwork"
            )

            YadraConnectionManager
                .setNetwork(wifiNetwork)
        }

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.M
        ) {

            if (
                !processBoundToWifi &&
                !bindProcessToWifiForApi21()
            ) {

                allowAutoConnect = false

                txtScanStatus.setText(
                    R.string.wifi_network_not_ready
                )

                return
            }
        }

        elmConnectionInProgress = true

        postToUi {

            txtScanStatus.setText(
                R.string.elm327_connecting
            )
        }

        Thread {

            try {

                Log.d(
                    TAG,
                    "================================"
                )

                Log.d(
                    TAG,
                    "Starting ELM327 connection"
                )

                Log.d(
                    TAG,
                    "Android API = ${Build.VERSION.SDK_INT}"
                )

                Log.d(
                    TAG,
                    "Wi-Fi Network = $wifiNetwork"
                )

                val connected =
                    YadraConnectionManager
                        .connectToElm327()

                if (!connected) {

                    val error =
                        YadraConnectionManager
                            .lastConnectError

                    Log.e(
                        TAG,
                        "ELM CONNECT FAILED: $error"
                    )

                    allowAutoConnect = false

                    postToUi {

                        txtScanStatus.text =
                            getString(
                                R.string.elm327_connection_failed,
                                error
                            )
                    }

                    return@Thread
                }

                Log.d(
                    TAG,
                    "TCP connection successful"
                )

                postToUi {

                    txtScanStatus.setText(
                        R.string.elm327_initializing
                    )
                }

                val initialized =
                    YadraConnectionManager
                        .initializeElm327()

                if (!initialized) {

                    val error =
                        YadraConnectionManager
                            .lastConnectError

                    Log.e(
                        TAG,
                        "ELM initialization failed: $error"
                    )

                    allowAutoConnect = false

                    YadraConnectionManager
                        .disconnectElm()

                    postToUi {

                        txtScanStatus.text =
                            getString(
                                R.string.elm327_initialization_failed,
                                error
                            )
                    }

                    return@Thread
                }

                try {

                    EcuConnectionManager
                        .syncCurrentConnectionState()

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "ECU synchronization failed",
                        e
                    )
                }

                postToUi {

                    val version =
                        YadraConnectionManager
                            .elmVersion

                    val protocol =
                        YadraConnectionManager
                            .detectedProtocol

                    val ecu =
                        YadraConnectionManager
                            .ecuConnected

                    val status =
                        getString(
                            if (ecu) {
                                R.string.ecu_connected
                            } else {
                                R.string.ecu_not_detected
                            }
                        )

                    val builder =
                        StringBuilder()

                    builder.append(
                        getString(
                            R.string.full_wifi_connected
                        )
                    )

                    builder.append("\n")

                    builder.append(
                        getString(
                            R.string.full_elm_connected
                        )
                    )

                    if (version.isNotBlank()) {

                        builder.append("\n")

                        builder.append(
                            getString(
                                R.string.full_elm_version,
                                version
                            )
                        )
                    }

                    if (protocol.isNotBlank()) {

                        builder.append("\n")

                        builder.append(
                            getString(
                                R.string.full_protocol,
                                protocol
                            )
                        )
                    }

                    builder.append("\n")
                    builder.append(status)

                    txtScanStatus.text =
                        builder.toString()
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "ELM connection thread failed",
                    e
                )

                allowAutoConnect = false

                postToUi {

                    txtScanStatus.text =
                        getString(
                            R.string.elm327_error,
                            e.message
                                ?: e.javaClass.simpleName
                        )
                }

            } finally {

                elmConnectionInProgress = false

                Log.d(
                    TAG,
                    "ELM connection attempt finished"
                )

                Log.d(
                    TAG,
                    "================================"
                )
            }

        }.start()
    }

    // =========================================================
    // SAFE UI
    // =========================================================

    private fun postToUi(
        action: () -> Unit
    ) {

        if (activityDestroyed) {
            return
        }

        runOnUiThread {

            if (activityDestroyed) {
                return@runOnUiThread
            }

            try {

                action()

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "UI update failed",
                    e
                )
            }
        }
    }

    // =========================================================
    // DISCONNECT
    // =========================================================

    private fun setWifiDisconnected(
        messageResId: Int
    ) {

        try {

            YadraConnectionManager
                .disconnectElm()

            try {

                EcuConnectionManager
                    .disconnect()

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "ECU disconnect failed",
                    e
                )
            }

            clearProcessNetworkBinding()

            wifiNetwork = null

            YadraConnectionManager
                .setNetwork(null)

            YadraConnectionManager
                .setWifiConnected(false)

            if (
                ConnectionSource.activeSource ==
                ConnectionSource.Source.WIFI
            ) {

                ConnectionSource.clear()
            }

            if (!activityDestroyed) {

                txtScanStatus.setText(
                    messageResId
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "setWifiDisconnected failed",
                e
            )
        }
    }

    // =========================================================
    // NETWORK RECEIVER
    // =========================================================

    private fun registerNetworkReceiver() {

        if (networkReceiverRegistered) {
            return
        }

        try {

            networkReceiver =
                object : BroadcastReceiver() {

                    override fun onReceive(
                        context: Context?,
                        intent: Intent?
                    ) {

                        if (activityDestroyed) {
                            return
                        }

                        try {

                            if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.Q
                            ) {
                                return
                            }

                            if (
                                !YadraConnectionManager
                                    .elmConnected
                            ) {

                                checkRealWifiConnection()
                            }

                        } catch (e: Exception) {

                            Log.e(
                                TAG,
                                "Network receiver error",
                                e
                            )
                        }
                    }
                }

            val filter =
                IntentFilter().apply {

                    addAction(
                        WifiManager
                            .NETWORK_STATE_CHANGED_ACTION
                    )

                    addAction(
                        WifiManager
                            .WIFI_STATE_CHANGED_ACTION
                    )

                    @Suppress("DEPRECATION")
                    addAction(
                        ConnectivityManager
                            .CONNECTIVITY_ACTION
                    )
                }

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                registerReceiver(
                    networkReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )

            } else {

                @Suppress("DEPRECATION")
                registerReceiver(
                    networkReceiver,
                    filter
                )
            }

            networkReceiverRegistered = true

        } catch (e: Exception) {

            Log.e(
                TAG,
                "registerNetworkReceiver failed",
                e
            )
        }
    }

    // =========================================================
    // CHECK WIFI
    // =========================================================

    @Suppress("DEPRECATION")
    private fun checkRealWifiConnection() {

        if (activityDestroyed) {
            return
        }

        if (!hasRequiredPermissions()) {
            return
        }

        try {

            if (YadraConnectionManager.elmConnected) {

                txtScanStatus.text =
                    getString(
                        R.string.wifi_connected,
                        TARGET_SSID
                    )

                return
            }

            if (!isWifiEnabledSafely()) {
                return
            }

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                return
            }

            val ssid =
                normalizeSsid(
                    wifiManager
                        .connectionInfo
                        ?.ssid ?: ""
                )

            if (
                !ssid.equals(
                    TARGET_SSID,
                    ignoreCase = true
                )
            ) {
                return
            }

            if (
                Build.VERSION.SDK_INT <
                Build.VERSION_CODES.M
            ) {

                val network =
                    findWifiNetworkApi21()
                        ?: return

                wifiNetwork = network

                if (!bindProcessToWifiForApi21()) {
                    return
                }

            } else {

                val network =
                    findWifiNetwork()
                        ?: return

                wifiNetwork = network

                if (!bindProcessToWifi()) {
                    return
                }

                YadraConnectionManager
                    .setNetwork(network)
            }

            allowAutoConnect = true

            YadraConnectionManager
                .setWifiConnected(true)

            ConnectionSource
                .setWifi()

            txtScanStatus.text =
                getString(
                    R.string.wifi_connected,
                    TARGET_SSID
                )

            connectToElm327()

        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "checkRealWifiConnection permission error",
                e
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "checkRealWifiConnection failed",
                e
            )
        }
    }

    // =========================================================
    // RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()

        if (!::wifiManager.isInitialized) {
            return
        }

        if (activityDestroyed) {
            activityDestroyed = false
        }

        try {

            if (
                YadraConnectionManager.elmConnected &&
                ConnectionSource.activeSource ==
                ConnectionSource.Source.WIFI
            ) {

                txtScanStatus.text =
                    getString(
                        R.string.wifi_connected,
                        TARGET_SSID
                    )

                return
            }

            if (!allowAutoConnect) {
                return
            }

            // -----------------------------------------------
            // مهم برای Android 7
            //
            // بعد از برگشت از Settings دوباره Location
            // بررسی می‌شود.
            // -----------------------------------------------

            if (
                Build.VERSION.SDK_INT <
                Build.VERSION_CODES.TIRAMISU
            ) {

                if (!isLocationEnabled()) {

                    txtScanStatus.setText(
                        R.string.wifi_location_disabled
                    )

                    showLocationDisabledDialog()

                    return
                }
            }

            // -----------------------------------------------
            // اگر GPS روشن شده، اسکن را دوباره شروع کن
            // -----------------------------------------------

            startWifiScan()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "onResume failed",
                e
            )
        }
    }

    // =========================================================
    // NORMALIZE SSID
    // =========================================================

    private fun normalizeSsid(
        ssid: String
    ): String {

        return ssid
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
    }

    // =========================================================
    // NETWORK REQUEST CLEANUP
    // =========================================================

    private fun unregisterNetworkRequest() {

        val callback =
            networkCallback

        networkCallback = null

        if (callback == null) {
            return
        }

        try {

            connectivityManager
                .unregisterNetworkCallback(
                    callback
                )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "unregisterNetworkRequest failed",
                e
            )
        }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        val keepElmConnection =
            YadraConnectionManager.elmConnected &&
                    ConnectionSource.activeSource ==
                    ConnectionSource.Source.WIFI

        activityDestroyed = true

        unregisterScanReceiver()
        unregisterNetworkReceiver()

        if (!keepElmConnection) {

            unregisterNetworkRequest()

            clearProcessNetworkBinding()

            wifiNetwork = null

            YadraConnectionManager
                .setNetwork(null)

        } else {

            Log.d(
                TAG,
                "Keeping WIFI_OBDII network request"
            )
        }

        super.onDestroy()
    }

    // =========================================================
    // UNREGISTER SCAN
    // =========================================================

    private fun unregisterScanReceiver() {

        if (!receiverRegistered) {
            return
        }

        try {

            val receiver =
                scanReceiver

            scanReceiver = null

            if (receiver != null) {
                unregisterReceiver(receiver)
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "unregisterScanReceiver failed",
                e
            )
        }

        receiverRegistered = false
    }

    // =========================================================
    // UNREGISTER NETWORK
    // =========================================================

    private fun unregisterNetworkReceiver() {

        if (!networkReceiverRegistered) {
            return
        }

        try {

            val receiver =
                networkReceiver

            networkReceiver = null

            if (receiver != null) {
                unregisterReceiver(receiver)
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "unregisterNetworkReceiver failed",
                e
            )
        }

        networkReceiverRegistered = false
    }
}