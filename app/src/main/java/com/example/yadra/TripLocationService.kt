package com.example.yadra

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.util.Locale

class TripLocationService : Service(), LocationListener {

    companion object {

        const val ACTION_START =
            "com.example.yadra.TRIP_START"

        const val ACTION_STOP =
            "com.example.yadra.TRIP_STOP"

        const val ACTION_LOCATION_UPDATE =
            "com.example.yadra.TRIP_LOCATION_UPDATE"

        const val EXTRA_SPEED =
            "speed"

        const val EXTRA_DISTANCE =
            "distance"

        const val EXTRA_MAX_SPEED =
            "max_speed"

        const val EXTRA_AVERAGE_SPEED =
            "average_speed"

        const val EXTRA_DURATION =
            "duration"

        const val EXTRA_LATITUDE =
            "latitude"

        const val EXTRA_LONGITUDE =
            "longitude"

        private const val CHANNEL_ID =
            "trip_location_channel"

        private const val NOTIFICATION_ID =
            2001

        private const val LOCATION_INTERVAL =
            1000L

        private const val MIN_DISTANCE =
            1f
    }

    private lateinit var locationManager: LocationManager

    private var tripStarted = false

    private var startTime = 0L

    private var lastLocation: Location? = null

    private var totalDistance = 0f

    private var maxSpeed = 0f

    private var speedSum = 0f

    private var speedCount = 0

    override fun onCreate() {

        super.onCreate()

        locationManager =
            getSystemService(
                LOCATION_SERVICE
            ) as LocationManager

        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {

                startTripTracking()
            }

            ACTION_STOP -> {

                stopTripTracking()

                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startTripTracking() {

        if (tripStarted) {
            return
        }

        if (!hasLocationPermission()) {

            stopSelf()

            return
        }

        tripStarted = true

        startTime =
            System.currentTimeMillis()

        lastLocation = null

        totalDistance = 0f

        maxSpeed = 0f

        speedSum = 0f

        speedCount = 0

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        requestLocationUpdates()
    }

    private fun requestLocationUpdates() {

        if (!hasLocationPermission()) {
            return
        }

        try {

            var lastKnownLocation: Location? = null

            /*
             * --------------------------------
             * GPS PROVIDER
             * --------------------------------
             */

            if (
                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )
            ) {

                /*
                 * آخرین موقعیت GPS
                 */
                lastKnownLocation =
                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )

                /*
                 * درخواست موقعیت جدید GPS
                 */
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_INTERVAL,
                    MIN_DISTANCE,
                    this
                )
            }

            /*
             * --------------------------------
             * NETWORK PROVIDER
             * --------------------------------
             */

            if (
                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )
            ) {

                /*
                 * اگر GPS موقعیت قبلی نداشت،
                 * Network را امتحان کن.
                 */
                if (lastKnownLocation == null) {

                    lastKnownLocation =
                        locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                        )
                }

                /*
                 * درخواست موقعیت Network
                 */
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_INTERVAL,
                    MIN_DISTANCE,
                    this
                )
            }

            /*
             * --------------------------------
             * LAST KNOWN LOCATION
             * --------------------------------
             *
             * اگر موقعیت قبلی موجود باشد،
             * بدون منتظر ماندن برای GPS
             * آن را به Activity ارسال می‌کنیم.
             */

            if (lastKnownLocation != null) {

                onLocationChanged(
                    lastKnownLocation
                )
            }

        } catch (
            e: SecurityException
        ) {

            stopSelf()
        } catch (
            e: Exception
        ) {

            stopSelf()
        }
    }

    private fun stopTripTracking() {

        if (!tripStarted) {
            return
        }

        try {

            locationManager.removeUpdates(
                this
            )

        } catch (_: Exception) {
        }

        tripStarted = false
    }

    override fun onLocationChanged(
        location: Location
    ) {

        if (!tripStarted) {
            return
        }

        val currentTime =
            System.currentTimeMillis()

        /*
         * --------------------------------
         * SPEED
         * --------------------------------
         */

        var speed = 0f

        if (location.hasSpeed()) {

            speed =
                location.speed
        }

        val speedKmh =
            speed * 3.6f

        /*
         * --------------------------------
         * DISTANCE
         * --------------------------------
         */

        val previous =
            lastLocation

        if (previous != null) {

            val distance =
                previous.distanceTo(
                    location
                )

            /*
             * جلوگیری از GPS Jump
             */
            if (
                distance >= 0f &&
                distance < 500f
            ) {

                totalDistance +=
                    distance
            }
        }

        /*
         * --------------------------------
         * MAX SPEED
         * --------------------------------
         */

        if (
            speedKmh > maxSpeed
        ) {

            maxSpeed =
                speedKmh
        }

        /*
         * --------------------------------
         * AVERAGE SPEED
         * --------------------------------
         */

        speedSum +=
            speedKmh

        speedCount++

        /*
         * --------------------------------
         * SAVE LOCATION
         * --------------------------------
         */

        lastLocation =
            Location(location)

        /*
         * --------------------------------
         * DURATION
         * --------------------------------
         */

        val duration =
            currentTime -
                    startTime

        /*
         * --------------------------------
         * AVERAGE SPEED
         * --------------------------------
         */

        val averageSpeed =
            if (speedCount > 0) {

                speedSum /
                        speedCount

            } else {

                0f
            }

        /*
         * --------------------------------
         * GPS COORDINATES
         * --------------------------------
         */

        val latitude =
            location.latitude

        val longitude =
            location.longitude

        /*
         * --------------------------------
         * SEND DATA TO TRIP ACTIVITY
         * --------------------------------
         */

        val updateIntent =
            Intent(
                ACTION_LOCATION_UPDATE
            ).apply {

                setPackage(
                    packageName
                )

                putExtra(
                    EXTRA_SPEED,
                    speedKmh
                )

                putExtra(
                    EXTRA_DISTANCE,
                    totalDistance
                )

                putExtra(
                    EXTRA_MAX_SPEED,
                    maxSpeed
                )

                putExtra(
                    EXTRA_AVERAGE_SPEED,
                    averageSpeed
                )

                putExtra(
                    EXTRA_DURATION,
                    duration
                )

                putExtra(
                    EXTRA_LATITUDE,
                    latitude
                )

                putExtra(
                    EXTRA_LONGITUDE,
                    longitude
                )
            }

        sendBroadcast(
            updateIntent
        )

        /*
         * --------------------------------
         * NOTIFICATION
         * --------------------------------
         */

        updateNotification(
            speedKmh,
            totalDistance
        )
    }

    private fun createNotification(): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle(
                "YADRA Trip"
            )
            .setContentText(
                "در حال دریافت موقعیت GPS..."
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_mylocation
            )
            .setOngoing(true)
            .setCategory(
                NotificationCompat.CATEGORY_SERVICE
            )
            .build()
    }

    private fun updateNotification(
        speed: Float,
        distanceMeters: Float
    ) {

        val distanceKm =
            distanceMeters / 1000f

        val text =
            String.format(
                Locale.US,
                "Speed: %.1f km/h  •  Distance: %.2f km",
                speed,
                distanceKm
            )

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    "YADRA Trip"
                )
                .setContentText(
                    text
                )
                .setSmallIcon(
                    android.R.drawable.ic_menu_mylocation
                )
                .setOngoing(true)
                .setCategory(
                    NotificationCompat.CATEGORY_SERVICE
                )
                .build()

        val manager =
            getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.notify(
            NOTIFICATION_ID,
            notification
        )
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Trip Location",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "YADRA trip GPS tracking"

            val manager =
                getSystemService(
                    NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun hasLocationPermission(): Boolean {

        val fineLocation =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        val coarseLocation =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        return fineLocation ||
                coarseLocation
    }

    override fun onProviderEnabled(
        provider: String
    ) {
    }

    override fun onProviderDisabled(
        provider: String
    ) {
    }

    override fun onStatusChanged(
        provider: String?,
        status: Int,
        extras: android.os.Bundle?
    ) {
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    override fun onDestroy() {

        try {

            locationManager.removeUpdates(
                this
            )

        } catch (_: Exception) {
        }

        tripStarted = false

        super.onDestroy()
    }
}