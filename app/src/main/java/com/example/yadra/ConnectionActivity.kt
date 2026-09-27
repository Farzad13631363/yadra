package com.example.yadra

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class ConnectionActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_connection)

        requestWifiPermissions()
        setupButtons()
    }

    private fun requestWifiPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            val fineLocation =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )

            val coarseLocation =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )

            if (
                fineLocation != PackageManager.PERMISSION_GRANTED ||
                coarseLocation != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST
                )
            }
        }
    }

    private fun setupButtons() {

        val btnWifi =
            findViewById<MaterialCardView>(R.id.btnWifi)

        val btnBluetooth =
            findViewById<MaterialCardView>(R.id.btnBluetooth)

        val btnUsb =
            findViewById<MaterialCardView>(R.id.btnUsb)

        setupPressAnimation(btnWifi)
        setupPressAnimation(btnBluetooth)
        setupPressAnimation(btnUsb)

        // Wi-Fi
        btnWifi.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    WifiScanActivity::class.java
                )
            )
        }

        // Bluetooth
        btnBluetooth.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    BluetoothActivity::class.java
                )
            )
        }

        // USB
        btnUsb.setOnClickListener {

            Toast.makeText(
                this,
                "اتصال USB انتخاب شد",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupPressAnimation(
        view: View
    ) {

        view.setOnTouchListener { v, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    v.animate()
                        .scaleX(1.08f)
                        .scaleY(1.08f)
                        .setDuration(120)
                        .start()

                    false
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(120)
                        .start()

                    false
                }

                else -> false
            }
        }
    }
}