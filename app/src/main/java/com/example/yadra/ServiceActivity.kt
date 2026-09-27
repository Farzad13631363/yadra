package com.example.yadra

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class ServiceActivity : AppCompatActivity() {

    private lateinit var txtConnectionStatus: TextView
    private lateinit var txtCurrentKm: TextView
    private lateinit var serviceContainer: LinearLayout

    private val handler = Handler(Looper.getMainLooper())

    private val serviceItems = listOf(
        ServiceItem("Engine Oil", 10_000),
        ServiceItem("Oil Filter", 10_000),
        ServiceItem("Air Filter", 20_000),
        ServiceItem("Cabin Filter", 15_000),
        ServiceItem("Brake Fluid", 40_000),
        ServiceItem("Spark Plugs", 40_000)
    )

    private val notifiedServices = mutableSetOf<String>()

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateVehicleMileage()
            handler.postDelayed(this, 5_000)
        }
    }

    companion object {
        private const val PREFS_NAME = "vehicle_service_prefs"
        private const val ODOMETER_PID = "01A6"

        private const val NOTIFICATION_CHANNEL_ID = "service_reminders"
        private const val NOTIFICATION_ID_BASE = 5000
        private const val NOTIFICATION_PERMISSION_REQUEST = 9001
    }

    data class ServiceItem(
        val name: String,
        val intervalKm: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service)

        txtConnectionStatus = findViewById(R.id.txtConnectionStatus)
        txtCurrentKm = findViewById(R.id.txtCurrentKm)
        serviceContainer = findViewById(R.id.serviceContainer)

        createNotificationChannel()
        requestNotificationPermissionIfNeeded()

        showInitialServiceStatus()
        buildServiceCards()

        handler.post(updateRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        super.onDestroy()
    }

    private fun showInitialServiceStatus() {
        txtConnectionStatus.setText(R.string.service_connection_checking)
        txtCurrentKm.setText(R.string.service_current_km)
    }

    private fun buildServiceCards() {
        serviceContainer.removeAllViews()

        for (service in serviceItems) {
            val card = layoutInflater.inflate(
                R.layout.item_service,
                serviceContainer,
                false
            )

            val title = card.findViewById<TextView>(
                R.id.txtServiceName
            )

            val interval = card.findViewById<TextView>(
                R.id.txtServiceInterval
            )

            val status = card.findViewById<TextView>(
                R.id.txtServiceStatus
            )

            val registerButton = card.findViewById<Button>(
                R.id.btnRegisterService
            )

            title.text = service.name

            interval.text = getString(
                R.string.service_interval,
                service.intervalKm
            )

            status.setText(
                R.string.service_km_unavailable
            )

            registerButton.setOnClickListener {
                registerService(service)
            }

            serviceContainer.addView(card)
        }
    }

    private fun updateVehicleMileage() {
        Thread {
            try {
                if (!isElmConnected()) {
                    runOnUiThread {
                        txtConnectionStatus.setText(
                            R.string.service_ecu_not_connected
                        )

                        txtCurrentKm.setText(
                            R.string.service_km_unavailable
                        )
                    }
                    return@Thread
                }

                runOnUiThread {
                    txtConnectionStatus.setText(
                        R.string.service_ecu_connected
                    )
                }

                val response = sendOdometerCommand()
                val currentKm = parseOdometer(response)

                if (currentKm == null) {
                    runOnUiThread {
                        txtCurrentKm.setText(
                            R.string.service_km_unavailable
                        )
                    }
                    return@Thread
                }

                runOnUiThread {
                    updateServiceDisplay(currentKm)
                }

            } catch (_: Exception) {
                runOnUiThread {
                    txtCurrentKm.setText(
                        R.string.service_km_unavailable
                    )
                }
            }
        }.start()
    }

    private fun updateServiceDisplay(currentKm: Int) {
        txtCurrentKm.text = getString(
            R.string.service_status_current_only,
            currentKm
        )

        val prefs = getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        for (index in serviceItems.indices) {
            if (index >= serviceContainer.childCount) {
                continue
            }

            val service = serviceItems[index]

            val card = serviceContainer.getChildAt(index)

            val status = card.findViewById<TextView>(
                R.id.txtServiceStatus
            )

            val lastServiceKm = prefs.getInt(
                servicePreferenceKey(service),
                0
            )

            val nextServiceKm = lastServiceKm + service.intervalKm
            val remainingKm = nextServiceKm - currentKm

            if (remainingKm <= 0) {
                val overdueKm = -remainingKm

                status.text = getString(
                    R.string.service_due,
                    nextServiceKm,
                    overdueKm
                )

                if (!notifiedServices.contains(service.name)) {
                    showServiceNotification(
                        service,
                        currentKm,
                        nextServiceKm
                    )

                    notifiedServices.add(service.name)
                }
            } else {
                status.text = getString(
                    R.string.service_status,
                    lastServiceKm,
                    nextServiceKm,
                    remainingKm
                )
            }
        }
    }

    private fun registerService(service: ServiceItem) {
        val currentKm = extractCurrentKm(
            txtCurrentKm.text.toString()
        )

        if (currentKm == null) {
            Toast.makeText(
                this,
                R.string.service_km_unavailable,
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val prefs = getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .putInt(
                servicePreferenceKey(service),
                currentKm
            )
            .apply()

        notifiedServices.remove(service.name)

        Toast.makeText(
            this,
            getString(
                R.string.service_registered,
                service.name,
                currentKm
            ),
            Toast.LENGTH_SHORT
        ).show()

        updateServiceDisplay(currentKm)
    }

    private fun extractCurrentKm(text: String): Int? {
        val regex = Regex("""(\d[\d,]*)""")
        val match = regex.find(text) ?: return null

        return match.value
            .replace(",", "")
            .toIntOrNull()
    }

    private fun servicePreferenceKey(service: ServiceItem): String {
        return "last_service_${
            service.name.lowercase().replace(" ", "_")
        }"
    }

    private fun isElmConnected(): Boolean {
        return when (ConnectionSource.activeSource) {
            ConnectionSource.Source.WIFI ->
                YadraConnectionManager.elmConnected

            ConnectionSource.Source.BLUETOOTH ->
                BluetoothConnectionManager.elmConnected

            ConnectionSource.Source.NONE ->
                false
        }
    }

    private fun sendOdometerCommand(): String {
        return when (ConnectionSource.activeSource) {
            ConnectionSource.Source.WIFI ->
                YadraConnectionManager.sendCommand(ODOMETER_PID)

            ConnectionSource.Source.BLUETOOTH ->
                BluetoothConnectionManager.sendActiveCommand(
                    ODOMETER_PID
                )

            ConnectionSource.Source.NONE ->
                ""
        }
    }

    private fun parseOdometer(response: String): Int? {
        if (response.isBlank()) {
            return null
        }

        val cleaned = response
            .uppercase()
            .replace(" ", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace(">", "")

        if (
            cleaned.contains("NODATA") ||
            cleaned.contains("ERROR") ||
            cleaned.contains("UNABLETOCONNECT")
        ) {
            return null
        }

        val marker = "41A6"
        val markerIndex = cleaned.indexOf(marker)

        if (markerIndex < 0) {
            return null
        }

        val dataStart = markerIndex + marker.length

        if (cleaned.length < dataStart + 8) {
            return null
        }

        val data = cleaned.substring(
            dataStart,
            dataStart + 8
        )

        return try {
            data.chunked(2)
                .map { it.toInt(16) }
                .fold(0L) { result, byte ->
                    (result shl 8) + byte
                }
                .toInt()
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(
                    R.string.service_notification_channel_name
                ),
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.description = getString(
                R.string.service_notification_channel_description
            )

            val manager = getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST
            )
        }
    }

    private fun showServiceNotification(
        service: ServiceItem,
        currentKm: Int,
        nextServiceKm: Int
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_dialog_alert
            )
            .setContentTitle(
                getString(
                    R.string.service_notification_title
                )
            )
            .setContentText(
                getString(
                    R.string.service_notification_text,
                    service.name,
                    currentKm
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    getString(
                        R.string.service_notification_details,
                        service.name,
                        nextServiceKm,
                        currentKm
                    )
                )
            )
            .setPriority(
                NotificationCompat.PRIORITY_HIGH
            )
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat
            .from(this)
            .notify(
                NOTIFICATION_ID_BASE +
                        serviceItems.indexOf(service),
                notification
            )
    }
}