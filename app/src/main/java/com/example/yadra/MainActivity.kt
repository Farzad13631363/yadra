package com.example.yadra

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var rotatingDashboard: RotatingDashboardView
    private lateinit var txtDashboardStatus: TextView
    private lateinit var txtDashboardTitle: TextView
    private lateinit var dropdownVehicleProfile: AutoCompleteTextView

    // =====================================================
    // CONNECTION STATUS LOGOS
    // =====================================================

    private lateinit var statusGps: ImageView
    private lateinit var statusBluetooth: ImageView
    private lateinit var statusWifi: ImageView
    private lateinit var statusEcu: ImageView

    private var statusBlinkAnimation: AlphaAnimation? = null

    // =====================================================
    // LOCATION PERMISSION
    // =====================================================

    companion object {

        private const val PREFS_NAME =
            "yadra_settings"

        private const val KEY_DASHBOARD_MODE =
            "dashboard_mode"

        private const val REQUEST_LOCATION_PERMISSION =
            1002
    }

    private var locationPermissionDialogShowing = false

    // =====================================================
    // RPM HANDLER
    // =====================================================

    private val rpmHandler =
        Handler(Looper.getMainLooper())

    private val rpmRunnable =
        object : Runnable {

            override fun run() {

                updateDashboardRpm()

                updateConnectionLogos()

                rpmHandler.postDelayed(
                    this,
                    1000
                )
            }
        }

    // =====================================================
    // ON CREATE
    // =====================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        // =================================================
        // ECU RUNTIME STATE INITIALIZATION
        // =================================================

        EcuRuntimeState.initialize(
            applicationContext
        )

        setContentView(
            R.layout.activity_main
        )

        rotatingDashboard =
            findViewById(
                R.id.rotatingDashboard
            )

        txtDashboardStatus =
            findViewById(
                R.id.txtDashboardStatus
            )

        txtDashboardTitle =
            findViewById(
                R.id.txtDashboardTitle
            )

        dropdownVehicleProfile =
            findViewById(
                R.id.dropdownVehicleProfile
            )

        // =================================================
        // STATUS LOGOS
        // =================================================

        statusGps =
            findViewById(
                R.id.statusGps
            )

        statusBluetooth =
            findViewById(
                R.id.statusBluetooth
            )

        statusWifi =
            findViewById(
                R.id.statusWifi
            )

        statusEcu =
            findViewById(
                R.id.statusEcu
            )

        setupStatusLogos()

        // =================================================
        // VEHICLE PROFILE DROPDOWN
        // =================================================

        setupVehicleProfileDropdown()

        // =================================================
        // DASHBOARD
        // =================================================

        setupRotatingDashboard()

        applyDashboardMode()

        setupBackButton()

        // =================================================
        // LOCATION / GPS
        //
        // اینجا نقطه اصلی درخواست مجوز است.
        // دیگر WifiScanActivity مسئول Location نیست.
        // =================================================

        checkLocationForAppStart()

        // =================================================
        // RPM
        // =================================================

        rpmHandler.post(
            rpmRunnable
        )
    }

    // =====================================================
    // LOCATION / GPS INITIAL CHECK
    // =====================================================

    private fun checkLocationForAppStart() {

        // Android 6.0 / API 23 به بعد
        if (android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.M
        ) {

            val permissionGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {

                requestLocationPermission()

                return
            }
        }

        // مجوز وجود دارد
        checkGpsEnabled()
    }

    // =====================================================
    // REQUEST LOCATION PERMISSION
    // =====================================================

    private fun requestLocationPermission() {

        if (locationPermissionDialogShowing) {
            return
        }

        locationPermissionDialogShowing = true

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUEST_LOCATION_PERMISSION
        )
    }

    // =====================================================
    // PERMISSION RESULT
    // =====================================================

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

        if (
            requestCode ==
            REQUEST_LOCATION_PERMISSION
        ) {

            locationPermissionDialogShowing = false

            var granted = false

            for (result in grantResults) {

                if (
                    result ==
                    PackageManager.PERMISSION_GRANTED
                ) {

                    granted = true

                    break
                }
            }

            if (granted) {

                // مجوز داده شد
                checkGpsEnabled()

            } else {

                // مجوز داده نشده
                updateConnectionLogos()
            }
        }
    }

    // =====================================================
    // CHECK GPS
    // =====================================================

    private fun checkGpsEnabled() {

        val locationManager =
            getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

        val gpsEnabled =
            try {

                if (
                    android.os.Build.VERSION.SDK_INT >=
                    android.os.Build.VERSION_CODES.P
                ) {

                    locationManager.isLocationEnabled

                } else {

                    locationManager.isProviderEnabled(
                        LocationManager.GPS_PROVIDER
                    )
                }

            } catch (_: Exception) {

                false
            }

        if (!gpsEnabled) {

            showGpsDisabledDialog()
        }
    }

    // =====================================================
    // GPS DISABLED DIALOG
    // =====================================================

    private fun showGpsDisabledDialog() {

        if (isFinishing) {
            return
        }

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "GPS خاموش است"
            )
            .setMessage(
                "برای استفاده از قابلیت‌های موقعیت مکانی و سفر، لطفاً GPS را روشن کنید."
            )
            .setPositiveButton(
                "روشن کردن GPS"
            ) { _, _ ->

                try {

                    startActivity(
                        Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS
                        )
                    )

                } catch (_: Exception) {

                    // در صورت عدم وجود صفحه تنظیمات
                }
            }
            .setNegativeButton(
                "بعداً"
            ) { dialog, _ ->

                dialog.dismiss()
            }
            .setCancelable(
                true
            )
            .show()
    }

    // =====================================================
    // STATUS LOGOS
    // =====================================================

    private fun setupStatusLogos() {

        statusGps.setImageResource(
            R.drawable.ic_status_gps
        )

        statusBluetooth.setImageResource(
            R.drawable.ic_status_bluetooth
        )

        statusWifi.setImageResource(
            R.drawable.ic_status_wifi
        )

        statusEcu.setImageResource(
            R.drawable.ic_status_ecu
        )

        statusGps.scaleType =
            ImageView.ScaleType.CENTER_INSIDE

        statusBluetooth.scaleType =
            ImageView.ScaleType.CENTER_INSIDE

        statusWifi.scaleType =
            ImageView.ScaleType.CENTER_INSIDE

        statusEcu.scaleType =
            ImageView.ScaleType.CENTER_INSIDE

        updateConnectionLogos()
    }

    // =====================================================
    // UPDATE CONNECTION LOGOS
    // =====================================================

    private fun updateConnectionLogos() {

        runOnUiThread {

            // =================================================
            // GPS
            // =================================================

            val locationManager =
                getSystemService(
                    Context.LOCATION_SERVICE
                ) as LocationManager

            val gpsEnabled =
                try {

                    if (
                        android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.P
                    ) {

                        locationManager.isLocationEnabled

                    } else {

                        locationManager.isProviderEnabled(
                            LocationManager.GPS_PROVIDER
                        )
                    }

                } catch (_: Exception) {

                    false
                }

            if (gpsEnabled) {

                setLogoConnected(
                    statusGps
                )

            } else {

                setLogoDisconnected(
                    statusGps
                )
            }

            // =================================================
            // BLUETOOTH
            // =================================================

            val bluetoothConnected =
                try {

                    BluetoothConnectionManager
                        .elmConnected

                } catch (_: Exception) {

                    false
                }

            // =================================================
            // WIFI
            // =================================================

            val wifiConnected =
                try {

                    YadraConnectionManager
                        .elmConnected

                } catch (_: Exception) {

                    false
                }

            // =================================================
            // BLUETOOTH / WIFI MUTUAL STATUS
            // =================================================

            when {

                bluetoothConnected -> {

                    setLogoConnected(
                        statusBluetooth
                    )

                    setLogoStandby(
                        statusWifi
                    )
                }

                wifiConnected -> {

                    setLogoStandby(
                        statusBluetooth
                    )

                    setLogoConnected(
                        statusWifi
                    )
                }

                else -> {

                    setLogoDisconnected(
                        statusBluetooth
                    )

                    setLogoDisconnected(
                        statusWifi
                    )
                }
            }

            // =================================================
            // ECU
            // =================================================

            val ecuConnected =
                when {

                    EcuRuntimeState.mode ==
                            EcuMode.UNKNOWN &&
                            EcuRuntimeState.connected -> {

                        true
                    }

                    ConnectionSource.activeSource ==
                            ConnectionSource.Source.WIFI -> {

                        try {

                            YadraConnectionManager
                                .ecuConnected

                        } catch (_: Exception) {

                            false
                        }
                    }

                    ConnectionSource.activeSource ==
                            ConnectionSource.Source.BLUETOOTH -> {

                        try {

                            BluetoothConnectionManager
                                .ecuConnected

                        } catch (_: Exception) {

                            false
                        }
                    }

                    else -> {

                        false
                    }
                }

            if (ecuConnected) {

                setLogoConnected(
                    statusEcu
                )

            } else {

                setLogoDisconnected(
                    statusEcu
                )
            }
        }
    }

    // =====================================================
    // LOGO CONNECTED
    // =====================================================

    private fun setLogoConnected(
        imageView: ImageView
    ) {

        stopBlink(
            imageView
        )

        imageView.clearColorFilter()

        imageView.setColorFilter(
            Color.rgb(
                0,
                220,
                90
            )
        )

        imageView.alpha =
            1.0f
    }

    // =====================================================
    // LOGO DISCONNECTED
    // =====================================================

    private fun setLogoDisconnected(
        imageView: ImageView
    ) {

        imageView.setColorFilter(
            Color.rgb(
                220,
                35,
                35
            )
        )

        startBlink(
            imageView
        )
    }

    // =====================================================
    // LOGO STANDBY
    // =====================================================

    private fun setLogoStandby(
        imageView: ImageView
    ) {

        stopBlink(
            imageView
        )

        imageView.setColorFilter(
            Color.rgb(
                255,
                190,
                40
            )
        )

        imageView.alpha =
            1.0f
    }

    // =====================================================
    // BLINK
    // =====================================================

    private fun startBlink(
        imageView: ImageView
    ) {

        if (imageView.animation != null) {
            return
        }

        val animation =
            AlphaAnimation(
                1.0f,
                0.25f
            )

        animation.duration =
            500

        animation.repeatMode =
            AlphaAnimation.REVERSE

        animation.repeatCount =
            AlphaAnimation.INFINITE

        imageView.startAnimation(
            animation
        )
    }

    // =====================================================
    // STOP BLINK
    // =====================================================

    private fun stopBlink(
        imageView: ImageView
    ) {

        imageView.clearAnimation()

        imageView.alpha =
            1.0f
    }

    // =====================================================
    // VEHICLE PROFILE DROPDOWN
    // =====================================================

    private fun setupVehicleProfileDropdown() {

        dropdownVehicleProfile.setOnClickListener {

            dropdownVehicleProfile.showDropDown()
        }

        dropdownVehicleProfile.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            VehicleProfileManager.setActiveProfile(
                this,
                position
            )

            updateHomeVehicleProfile()
        }

        updateHomeVehicleProfile()
    }

    // =====================================================
    // UPDATE VEHICLE PROFILE
    // =====================================================

    private fun updateHomeVehicleProfile() {

        val profiles =
            VehicleProfileManager.loadAll(
                this
            )

        if (profiles.isEmpty()) {

            txtDashboardTitle.text =
                "YADRA"

            dropdownVehicleProfile.setText(
                "",
                false
            )

            dropdownVehicleProfile.hint =
                "خودرو انتخاب نشده"

            dropdownVehicleProfile.setAdapter(
                ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    emptyList<String>()
                )
            )

            return
        }

        val profileNames =
            profiles.map { profile ->

                val vehicleType =
                    profile.vehicleType
                        .trim()

                val model =
                    profile.model
                        .trim()

                when {

                    vehicleType.isNotBlank() &&
                            model.isNotBlank() ->
                        "$vehicleType $model"

                    vehicleType.isNotBlank() ->
                        vehicleType

                    model.isNotBlank() ->
                        model

                    else ->
                        "پروفایل خودرو"
                }
            }

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                profileNames
            )

        dropdownVehicleProfile.setAdapter(
            adapter
        )

        val activeIndex =
            VehicleProfileManager.getActiveIndex(
                this
            )

        val safeIndex =
            if (
                activeIndex in profiles.indices
            ) {
                activeIndex
            } else {
                0
            }

        dropdownVehicleProfile.setText(
            profileNames[safeIndex],
            false
        )

        dropdownVehicleProfile.hint =
            "خودرو را انتخاب کنید"

        txtDashboardTitle.text =
            "YADRA"
    }

    // =====================================================
    // BACK BUTTON
    // =====================================================

    private fun setupBackButton() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    AlertDialog.Builder(
                        this@MainActivity
                    )
                        .setTitle(
                            "Exit"
                        )
                        .setMessage(
                            "Are you sure you want to exit the app?"
                        )
                        .setPositiveButton(
                            "YES"
                        ) { _, _ ->

                            finishAffinity()
                        }
                        .setNegativeButton(
                            "NO",
                            null
                        )
                        .show()
                }
            }
        )
    }

    // =====================================================
    // DASHBOARD MODE
    // =====================================================

    private fun applyDashboardMode() {

        val preferences =
            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )

        val savedMode =
            preferences.getString(
                KEY_DASHBOARD_MODE,
                "standard"
            )

        val professionalMode =
            savedMode == "professional"

        rotatingDashboard.setProfessionalMode(
            professionalMode
        )
    }

    // =====================================================
    // DASHBOARD
    // =====================================================

    private fun setupRotatingDashboard() {

        rotatingDashboard.setItems(
            listOf(

                RotatingDashboardView.DashboardItem(
                    title = "Profile",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("Profile")
                ) {

                    startActivity(
                        Intent(
                            this,
                            VehicleProfileActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "Buy",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("Buy")
                ) {

                    startActivity(
                        Intent(
                            this,
                            BuyActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "Connection",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("Connection")
                ) {

                    startActivity(
                        Intent(
                            this,
                            ConnectionActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "Warning",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("Warning")
                ) {

                    startActivity(
                        Intent(
                            this,
                            WarningsActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "Sensors",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("Sensors")
                ) {

                    startActivity(
                        Intent(
                            this,
                            SensorsActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "Errors",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("Errors")
                ) {

                    startActivity(
                        Intent(
                            this,
                            ErrorsActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "ECU",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("ECU")
                ) {

                    startActivity(
                        Intent(
                            this,
                            EcuTestActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "Unknown ECU",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("ECU")
                ) {

                    startActivity(
                        Intent(
                            this,
                            UnknownEcuScannerActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "KWP TEST",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("KWP TEST")
                ) {

                    startActivity(
                        Intent(
                            this,
                            KwpTestActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "Actuators",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("Actuators")
                ) {

                    startActivity(
                        Intent(
                            this,
                            ActuatorTestActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "AI",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("AI")
                ) {

                    startActivity(
                        Intent(
                            this,
                            AIActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "Settings",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("Settings")
                ) {

                    startActivity(
                        Intent(
                            this,
                            SettingsActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "Trip",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("Trip")
                ) {

                    startActivity(
                        Intent(
                            this,
                            TripActivity::class.java
                        )
                    )
                },

                RotatingDashboardView.DashboardItem(
                    title = "Charts",
                    iconRes =
                        RotatingDashboardView
                            .iconFor("Charts")
                ) {

                    startActivity(
                        Intent(
                            this,
                            ChartsActivity::class.java
                        )
                    )
                }
            )
        )
    }

    // =====================================================
    // UPDATE DASHBOARD RPM
    // =====================================================

    private fun updateDashboardRpm() {

        Thread {

            try {

                // =================================================
                // UNKNOWN ECU
                // =================================================

                if (
                    EcuRuntimeState.mode ==
                    EcuMode.UNKNOWN &&
                    EcuRuntimeState.connected
                ) {

                    runOnUiThread {

                        rotatingDashboard
                            .setRpm(null)

                        txtDashboardStatus.text =
                            "● Unknown ECU Connected"
                    }

                    return@Thread
                }

                // =================================================
                // STANDARD ECU
                // =================================================

                EcuConnectionManager
                    .syncCurrentConnectionState()

                when (
                    ConnectionSource.activeSource
                ) {

                    // =============================================
                    // WIFI
                    // =============================================

                    ConnectionSource.Source.WIFI -> {

                        val elmConnected =
                            YadraConnectionManager
                                .elmConnected

                        val ecuConnected =
                            YadraConnectionManager
                                .ecuConnected

                        if (
                            elmConnected &&
                            ecuConnected
                        ) {

                            val currentRpm =
                                EcuDataGateway
                                    .readRpm()

                            runOnUiThread {

                                rotatingDashboard
                                    .setRpm(
                                        currentRpm
                                    )

                                txtDashboardStatus.text =
                                    "● Wi-Fi / ELM327 + ECU Connected"
                            }

                        } else if (
                            elmConnected
                        ) {

                            runOnUiThread {

                                rotatingDashboard
                                    .setRpm(null)

                                txtDashboardStatus.text =
                                    "● Wi-Fi / ELM327 Connected / ECU Not Tested"
                            }

                        } else {

                            runOnUiThread {

                                rotatingDashboard
                                    .setRpm(null)

                                txtDashboardStatus.text =
                                    "● Wi-Fi / ELM327 Disconnected"
                            }
                        }
                    }

                    // =============================================
                    // BLUETOOTH
                    // =============================================

                    ConnectionSource.Source.BLUETOOTH -> {

                        val elmConnected =
                            BluetoothConnectionManager
                                .elmConnected

                        val ecuConnected =
                            BluetoothConnectionManager
                                .ecuConnected

                        if (
                            elmConnected &&
                            ecuConnected
                        ) {

                            val currentRpm =
                                EcuDataGateway
                                    .readRpm()

                            runOnUiThread {

                                rotatingDashboard
                                    .setRpm(
                                        currentRpm
                                    )

                                txtDashboardStatus.text =
                                    "● Bluetooth / ELM327 + ECU Connected"
                            }

                        } else if (
                            elmConnected
                        ) {

                            runOnUiThread {

                                rotatingDashboard
                                    .setRpm(null)

                                txtDashboardStatus.text =
                                    "● Bluetooth / ELM327 Connected / ECU Not Tested"
                            }

                        } else {

                            runOnUiThread {

                                rotatingDashboard
                                    .setRpm(null)

                                txtDashboardStatus.text =
                                    "● Bluetooth / ELM327 Disconnected"
                            }
                        }
                    }

                    // =============================================
                    // NONE
                    // =============================================

                    ConnectionSource.Source.NONE -> {

                        runOnUiThread {

                            rotatingDashboard
                                .setRpm(null)

                            txtDashboardStatus.text =
                                "● ELM327 Disconnected"
                        }
                    }
                }

            } catch (_: Exception) {

                runOnUiThread {

                    rotatingDashboard
                        .setRpm(null)

                    txtDashboardStatus.text =
                        "● Connection Error"
                }
            }
        }.start()
    }

    // =====================================================
    // RESUME
    // =====================================================

    override fun onResume() {

        super.onResume()

        updateHomeVehicleProfile()

        applyDashboardMode()

        updateConnectionLogos()

        // =================================================
        // بعد از برگشت از صفحه تنظیمات GPS
        // =================================================

        if (!locationPermissionDialogShowing) {

            checkLocationForAppStart()
        }

        rpmHandler.removeCallbacks(
            rpmRunnable
        )

        rpmHandler.post(
            rpmRunnable
        )
    }

    // =====================================================
    // DESTROY
    // =====================================================

    override fun onDestroy() {

        rpmHandler.removeCallbacks(
            rpmRunnable
        )

        stopBlink(
            statusGps
        )

        stopBlink(
            statusBluetooth
        )

        stopBlink(
            statusWifi
        )

        stopBlink(
            statusEcu
        )

        super.onDestroy()
    }
}