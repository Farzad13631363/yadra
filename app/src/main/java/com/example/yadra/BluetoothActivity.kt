package com.example.yadra

import android.Manifest
import android.animation.ValueAnimator
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class BluetoothActivity : AppCompatActivity(),
    BluetoothConnectionManager.Listener {

    companion object {

        private const val BLUETOOTH_PERMISSION_REQUEST = 200
        private const val LOCATION_PERMISSION_REQUEST = 201
    }

    private lateinit var bluetoothManager: BluetoothConnectionManager

    private lateinit var txtStatus: TextView

    private lateinit var deviceContainer: LinearLayout

    private val displayedAddresses =
        HashSet<String>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        bluetoothManager =
            BluetoothConnectionManager(this)

        bluetoothManager.setListener(this)

        createScreen()

        bluetoothManager.registerReceiver()

        checkBluetoothPermissions()
    }

    override fun onResume() {

        super.onResume()

        if (::bluetoothManager.isInitialized) {
            prepareBluetooth()
        }
    }

    private fun createScreen() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.rgb(5, 12, 25)
                )
            }

        val waveView =
            BluetoothWaveView(this)

        root.addView(
            waveView,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        txtStatus =
            TextView(this).apply {

                text =
                    "Bluetooth آماده است"

                textSize = 16f

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    30,
                    20,
                    30,
                    20
                )
            }

        root.addView(
            txtStatus,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        deviceContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    20,
                    10,
                    20,
                    30
                )
            }

        root.addView(
            deviceContainer,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun checkBluetoothPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val permissions =
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )

            val missing =
                permissions.filter {

                    ContextCompat.checkSelfPermission(
                        this,
                        it
                    ) != PackageManager.PERMISSION_GRANTED
                }

            if (missing.isNotEmpty()) {

                ActivityCompat.requestPermissions(
                    this,
                    missing.toTypedArray(),
                    BLUETOOTH_PERMISSION_REQUEST
                )

                return
            }

        } else {

            val locationGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) ==
                        PackageManager.PERMISSION_GRANTED

            if (!locationGranted) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST
                )

                return
            }
        }

        prepareBluetooth()
    }

    private fun prepareBluetooth() {

        if (!bluetoothManager.hasBluetooth()) {

            txtStatus.text =
                "🔴 Bluetooth در این دستگاه وجود ندارد"

            return
        }

        if (!bluetoothManager.isBluetoothEnabled()) {

            txtStatus.text =
                "🔵 لطفاً Bluetooth را روشن کنید"

            try {

                startActivity(
                    Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE
                    )
                )

            } catch (_: Exception) {
            }

            return
        }

        /*
         * Android 6 تا Android 11
         * برای Bluetooth Discovery باید
         * Location سیستم روشن باشد.
         */
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        ) {

            if (!isLocationEnabled()) {

                txtStatus.text =
                    "📍 Location خاموش است. لطفاً آن را روشن کنید."

                try {

                    startActivity(
                        Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS
                        )
                    )

                } catch (_: Exception) {
                }

                return
            }
        }

        txtStatus.text =
            "🟢 Bluetooth روشن است"

        showPairedDevices()

        startBluetoothDiscovery()
    }

    private fun isLocationEnabled(): Boolean {

        return try {

            val locationManager =
                getSystemService(
                    LOCATION_SERVICE
                ) as LocationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                locationManager.isLocationEnabled

            } else {

                @Suppress("DEPRECATION")
                Settings.Secure.getInt(
                    contentResolver,
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF
                ) != Settings.Secure.LOCATION_MODE_OFF
            }

        } catch (_: Exception) {

            false
        }
    }

    private fun showPairedDevices() {

        deviceContainer.removeAllViews()

        displayedAddresses.clear()

        addSectionTitle(
            "Paired devices"
        )

        val pairedDevices =
            bluetoothManager.getPairedDevices()

        if (pairedDevices.isEmpty()) {

            val text =
                TextView(this).apply {

                    this.text =
                        "No paired devices"

                    textSize = 15f

                    setTextColor(
                        Color.LTGRAY
                    )

                    setPadding(
                        10,
                        10,
                        10,
                        15
                    )
                }

            deviceContainer.addView(
                text
            )

            return
        }

        for (device in pairedDevices) {

            val address =
                bluetoothManager.getDeviceAddress(
                    device
                )

            displayedAddresses.add(
                address
            )

            addDevice(
                device,
                true
            )
        }
    }

    private fun startBluetoothDiscovery() {

        txtStatus.text =
            "🔵 در حال جستجوی Bluetooth..."

        val started =
            bluetoothManager.startDiscovery()

        if (!started) {

            txtStatus.text =
                "🔴 شروع جستجوی Bluetooth ناموفق بود"
        }
    }

    override fun onDeviceFound(
        device: BluetoothDevice
    ) {

        runOnUiThread {

            val address =
                bluetoothManager.getDeviceAddress(
                    device
                )

            if (
                displayedAddresses.contains(
                    address
                )
            ) {
                return@runOnUiThread
            }

            displayedAddresses.add(
                address
            )

            addDevice(
                device,
                false
            )
        }
    }

    override fun onDiscoveryStarted() {

        runOnUiThread {

            txtStatus.text =
                "🔵 در حال جستجوی دستگاه‌ها..."
        }
    }

    override fun onDiscoveryFinished() {

        runOnUiThread {

            txtStatus.text =
                "🟢 جستجو تمام شد"
        }
    }

    override fun onError(
        message: String
    ) {

        runOnUiThread {

            txtStatus.text =
                "🔴 $message"

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addSectionTitle(
        title: String
    ) {

        val text =
            TextView(this).apply {

                this.text = title

                textSize = 18f

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    10,
                    15,
                    10,
                    15
                )
            }

        deviceContainer.addView(
            text
        )
    }

    private fun addDevice(
        device: BluetoothDevice,
        paired: Boolean
    ) {

        val name =
            bluetoothManager.getDeviceName(
                device
            )

        val address =
            bluetoothManager.getDeviceAddress(
                device
            )

        val isElm =
            name.contains(
                "ELM",
                ignoreCase = true
            ) ||
                    name.contains(
                        "OBD",
                        ignoreCase = true
                    )

        val deviceText =
            TextView(this).apply {

                text =
                    if (isElm) {

                        "🔵 $name\n" +
                                "ELM327 / OBD-II\n" +
                                address

                    } else {

                        "🔷 $name\n" +
                                if (paired) {
                                    "Paired\n"
                                } else {
                                    "Available\n"
                                } +
                                address
                    }

                textSize = 16f

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    25,
                    22,
                    25,
                    22
                )

                setBackgroundColor(
                    Color.rgb(
                        15,
                        28,
                        50
                    )
                )

                // -------------------------------------------------
                // اتصال به ELM327
                // -------------------------------------------------

                setOnClickListener {

                    if (!isElm) {

                        Toast.makeText(
                            this@BluetoothActivity,
                            "این دستگاه ELM327 نیست",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    txtStatus.text =
                        "🔵 در حال اتصال به $name..."

                    Toast.makeText(
                        this@BluetoothActivity,
                        "در حال اتصال به ELM327...",
                        Toast.LENGTH_SHORT
                    ).show()

                    Thread {

                        val connected =
                            bluetoothManager.connectToElm327(
                                device
                            )

                        runOnUiThread {

                            if (
                                connected &&
                                BluetoothConnectionManager.elmConnected
                            ) {

                                if (
                                    BluetoothConnectionManager.ecuConnected
                                ) {

                                    txtStatus.text =
                                        "🟢 ELM327 و ECU متصل هستند"

                                    Toast.makeText(
                                        this@BluetoothActivity,
                                        "✅ اتصال ELM327 و ECU موفق بود",
                                        Toast.LENGTH_LONG
                                    ).show()

                                } else {

                                    txtStatus.text =
                                        "🟡 ELM327 متصل است ولی ECU پاسخ نداد"

                                    Toast.makeText(
                                        this@BluetoothActivity,
                                        "⚠️ ELM327 متصل شد، ولی ECU پاسخ نداد",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            } else {

                                txtStatus.text =
                                    "🔴 اتصال به ELM327 ناموفق بود"

                                Toast.makeText(
                                    this@BluetoothActivity,
                                    "❌ اتصال به ELM327 ناموفق بود",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                    }.start()
                }
            }

        val params =
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {

                setMargins(
                    0,
                    6,
                    0,
                    6
                )
            }

        deviceContainer.addView(
            deviceText,
            params
        )
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

        when (requestCode) {

            BLUETOOTH_PERMISSION_REQUEST,
            LOCATION_PERMISSION_REQUEST -> {

                val allGranted =
                    grantResults.isNotEmpty() &&
                            grantResults.all {
                                it ==
                                        PackageManager.PERMISSION_GRANTED
                            }

                if (allGranted) {

                    prepareBluetooth()

                } else {

                    txtStatus.text =
                        "🔴 مجوز Bluetooth داده نشده است"
                }
            }
        }
    }

    override fun onDestroy() {

        bluetoothManager.cancelDiscovery()

        bluetoothManager.unregisterReceiver()

        bluetoothManager.setListener(null)

        super.onDestroy()
    }

    private class BluetoothWaveView(
        context: android.content.Context
    ) : View(context) {

        private val wavePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                style =
                    Paint.Style.STROKE

                strokeWidth = 4f

                color =
                    Color.rgb(
                        30,
                        144,
                        255
                    )
            }

        private val iconPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                style =
                    Paint.Style.STROKE

                strokeWidth = 8f

                strokeCap =
                    Paint.Cap.ROUND

                strokeJoin =
                    Paint.Join.ROUND

                color =
                    Color.rgb(
                        30,
                        144,
                        255
                    )
            }

        private val textPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.WHITE

                textSize = 20f

                textAlign =
                    Paint.Align.CENTER
            }

        private var progress = 0f

        private val animator =
            ValueAnimator
                .ofFloat(
                    0f,
                    1f
                )
                .apply {

                    duration =
                        2200L

                    repeatCount =
                        ValueAnimator.INFINITE

                    addUpdateListener {

                        progress =
                            it.animatedValue
                                    as Float

                        invalidate()
                    }
                }

        init {

            animator.start()
        }

        override fun onDraw(
            canvas: Canvas
        ) {

            super.onDraw(canvas)

            val centerX =
                width / 2f

            val centerY =
                height / 2f - 40f

            val maxRadius =
                width.coerceAtMost(
                    height
                ) * 0.42f

            drawWave(
                canvas,
                centerX,
                centerY,
                maxRadius,
                progress
            )

            drawWave(
                canvas,
                centerX,
                centerY,
                maxRadius,
                (progress + 0.33f) % 1f
            )

            drawWave(
                canvas,
                centerX,
                centerY,
                maxRadius,
                (progress + 0.66f) % 1f
            )

            wavePaint.alpha = 70

            canvas.drawCircle(
                centerX,
                centerY,
                72f,
                wavePaint
            )

            drawBluetoothIcon(
                canvas,
                centerX,
                centerY
            )

            textPaint.textSize = 20f

            canvas.drawText(
                "Bluetooth",
                centerX,
                centerY + 145f,
                textPaint
            )

            textPaint.textSize = 16f

            canvas.drawText(
                "Searching for devices...",
                centerX,
                centerY + 180f,
                textPaint
            )
        }

        private fun drawWave(
            canvas: Canvas,
            centerX: Float,
            centerY: Float,
            maxRadius: Float,
            phase: Float
        ) {

            val radius =
                80f +
                        (
                                maxRadius - 80f
                                ) * phase

            val alpha =
                (
                        (1f - phase) *
                                150f
                        )
                    .toInt()
                    .coerceIn(
                        0,
                        150
                    )

            wavePaint.alpha =
                alpha

            canvas.drawCircle(
                centerX,
                centerY,
                radius,
                wavePaint
            )
        }

        private fun drawBluetoothIcon(
            canvas: Canvas,
            centerX: Float,
            centerY: Float
        ) {

            val top =
                centerY - 55f

            val bottom =
                centerY + 55f

            val middle =
                centerY

            val left =
                centerX - 25f

            val right =
                centerX + 25f

            canvas.drawLine(
                centerX,
                top,
                centerX,
                bottom,
                iconPaint
            )

            canvas.drawLine(
                centerX,
                top,
                right,
                middle,
                iconPaint
            )

            canvas.drawLine(
                right,
                middle,
                centerX,
                top,
                iconPaint
            )

            canvas.drawLine(
                centerX,
                bottom,
                right,
                middle,
                iconPaint
            )

            canvas.drawLine(
                right,
                middle,
                centerX,
                bottom,
                iconPaint
            )

            canvas.drawLine(
                left,
                centerY - 35f,
                centerX + 5f,
                middle + 5f,
                iconPaint
            )

            canvas.drawLine(
                left,
                centerY + 35f,
                centerX + 5f,
                middle - 5f,
                iconPaint
            )
        }

        override fun onDetachedFromWindow() {

            animator.cancel()

            super.onDetachedFromWindow()
        }
    }
}