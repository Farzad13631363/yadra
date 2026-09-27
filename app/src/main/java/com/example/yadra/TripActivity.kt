package com.example.yadra

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class TripActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 5001
    }

    private lateinit var tvTripStatus: TextView
    private lateinit var tvCurrentSpeed: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvMaxSpeed: TextView
    private lateinit var tvAverageSpeed: TextView
    private lateinit var btnStartStopTrip: Button

    private var tripRunning = false

    private val tripReceiver = object : BroadcastReceiver() {

        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {

            if (intent?.action !=
                TripLocationService.ACTION_LOCATION_UPDATE
            ) {
                return
            }

            val speed =
                intent.getFloatExtra(
                    TripLocationService.EXTRA_SPEED,
                    0f
                )

            val distance =
                intent.getFloatExtra(
                    TripLocationService.EXTRA_DISTANCE,
                    0f
                )

            val maxSpeed =
                intent.getFloatExtra(
                    TripLocationService.EXTRA_MAX_SPEED,
                    0f
                )

            val averageSpeed =
                intent.getFloatExtra(
                    TripLocationService.EXTRA_AVERAGE_SPEED,
                    0f
                )

            val duration =
                intent.getLongExtra(
                    TripLocationService.EXTRA_DURATION,
                    0L
                )

            updateTripUI(
                speed,
                distance,
                maxSpeed,
                averageSpeed,
                duration
            )
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_trip)

        initializeViews()

        btnStartStopTrip.setOnClickListener {

            if (tripRunning) {
                stopTrip()
            } else {
                startTrip()
            }
        }

        resetTripUI()
    }

    private fun initializeViews() {

        tvTripStatus =
            findViewById(R.id.tvTripStatus)

        tvCurrentSpeed =
            findViewById(R.id.tvCurrentSpeed)

        tvDistance =
            findViewById(R.id.tvDistance)

        tvDuration =
            findViewById(R.id.tvDuration)

        tvMaxSpeed =
            findViewById(R.id.tvMaxSpeed)

        tvAverageSpeed =
            findViewById(R.id.tvAverageSpeed)

        btnStartStopTrip =
            findViewById(R.id.btnStartStopTrip)
    }

    private fun startTrip() {

        if (!hasLocationPermission()) {

            Toast.makeText(
                this,
                "دسترسی GPS برای ثبت سفر لازم است",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val locationManager =
            getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

        val gpsEnabled =
            try {

                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )

            } catch (_: Exception) {

                false
            }

        if (!gpsEnabled) {

            Toast.makeText(
                this,
                "لطفاً GPS دستگاه را روشن کنید",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        resetTripUI()

        tripRunning = true

        tvTripStatus.text =
            "سفر در حال ثبت است"

        btnStartStopTrip.text =
            "پایان سفر"

        val serviceIntent =
            Intent(
                this,
                TripLocationService::class.java
            ).apply {

                action =
                    TripLocationService.ACTION_START
            }

        ContextCompat.startForegroundService(
            this,
            serviceIntent
        )
    }

    private fun stopTrip() {

        tripRunning = false

        val serviceIntent =
            Intent(
                this,
                TripLocationService::class.java
            ).apply {

                action =
                    TripLocationService.ACTION_STOP
            }

        startService(serviceIntent)

        tvTripStatus.text =
            "سفر پایان یافت"

        btnStartStopTrip.text =
            "شروع سفر"
    }

    private fun hasLocationPermission(): Boolean {

        val fineLocation =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    private fun resetTripUI() {

        tvTripStatus.text =
            "آماده شروع سفر"

        tvCurrentSpeed.text =
            "0.0 km/h"

        tvDistance.text =
            "0.00 km"

        tvDuration.text =
            "00:00:00"

        tvMaxSpeed.text =
            "0.0 km/h"

        tvAverageSpeed.text =
            "0.0 km/h"

        btnStartStopTrip.text =
            "شروع سفر"
    }

    private fun updateTripUI(
        speed: Float,
        distanceMeters: Float,
        maxSpeed: Float,
        averageSpeed: Float,
        durationMillis: Long
    ) {

        val distanceKm =
            distanceMeters / 1000f

        tvCurrentSpeed.text =
            String.format(
                Locale.US,
                "%.1f km/h",
                speed
            )

        tvDistance.text =
            String.format(
                Locale.US,
                "%.2f km",
                distanceKm
            )

        tvMaxSpeed.text =
            String.format(
                Locale.US,
                "%.1f km/h",
                maxSpeed
            )

        tvAverageSpeed.text =
            String.format(
                Locale.US,
                "%.1f km/h",
                averageSpeed
            )

        tvDuration.text =
            formatDuration(
                durationMillis
            )
    }

    private fun formatDuration(
        durationMillis: Long
    ): String {

        val totalSeconds =
            durationMillis / 1000

        val hours =
            totalSeconds / 3600

        val minutes =
            (totalSeconds % 3600) / 60

        val seconds =
            totalSeconds % 60

        return String.format(
            Locale.US,
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
    }

    override fun onStart() {

        super.onStart()

        val filter = IntentFilter().apply {
            addAction(
                TripLocationService.ACTION_LOCATION_UPDATE
            )
        }

        ContextCompat.registerReceiver(
            this,
            tripReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {

        try {

            unregisterReceiver(
                tripReceiver
            )

        } catch (_: Exception) {
        }

        super.onStop()
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

        if (requestCode ==
            LOCATION_PERMISSION_REQUEST
        ) {

            if (hasLocationPermission()) {

                startTrip()

            } else {

                Toast.makeText(
                    this,
                    "دسترسی GPS برای ثبت سفر لازم است",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}