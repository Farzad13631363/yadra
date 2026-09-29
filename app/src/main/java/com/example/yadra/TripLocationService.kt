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

        /*
         * ==========================================
         * TRIP SNAPSHOT
         * ==========================================
         *
         * این اطلاعات بین TripActivity های مختلف
         * در همان اجرای برنامه باقی می‌ماند.
         */

        data class TripSnapshot(
            val running: Boolean,
            val speed: Float,
            val distance: Float,
            val maxSpeed: Float,
            val averageSpeed: Float,
            val duration: Long,
            val latitude: Double,
            val longitude: Double
        )

        @Volatile
        private var currentSnapshot =
            TripSnapshot(
                false,
                0f,
                0f,
                0f,
                0f,
                0L,
                Double.NaN,
                Double.NaN
            )

        /*
         * دریافت وضعیت فعلی سفر
         */
        fun getSnapshot(): TripSnapshot {

            return currentSnapshot
        }
    }

    private lateinit var locationManager: LocationManager

    private var tripStarted =
        false

    private var startTime =
        0L

    private var lastLocation:
            Location? = null

    private var totalDistance =
        0f

    private var maxSpeed =
        0f

    private var speedSum =
        0f

    private var speedCount =
        0

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

    /*
     * ==========================================
     * START TRIP
     * ==========================================
     */

    private fun startTripTracking() {

        /*
         * اگر سفر از قبل فعال است،
         * دوباره آن را شروع نکن.
         */
        if (tripStarted) {

            sendCurrentSnapshot()

            return
        }

        if (!hasLocationPermission()) {

            stopSelf()

            return
        }

        tripStarted =
            true

        startTime =
            System.currentTimeMillis()

        lastLocation =
            null

        totalDistance =
            0f

        maxSpeed =
            0f

        speedSum =
            0f

        speedCount =
            0

        /*
         * Snapshot اولیه
         */
        currentSnapshot =
            TripSnapshot(
                true,
                0f,
                0f,
                0f,
                0f,
                0L,
                Double.NaN,
                Double.NaN
            )

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        requestLocationUpdates()
    }

    /*
     * ==========================================
     * REQUEST LOCATION
     * ==========================================
     */

    private fun requestLocationUpdates() {

        if (!hasLocationPermission()) {
            return
        }

        try {

            var lastKnownLocation:
                    Location? = null

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

                lastKnownLocation =
                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )

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

                if (
                    lastKnownLocation == null
                ) {

                    lastKnownLocation =
                        locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER
                        )
                }

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
             */

            if (
                lastKnownLocation != null
            ) {

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

    /*
     * ==========================================
     * STOP TRIP
     * ==========================================
     */

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

        tripStarted =
            false

        /*
         * وضعیت نهایی را نگه می‌داریم،
         * فقط running را false می‌کنیم.
         */
        currentSnapshot =
            currentSnapshot.copy(
                running = false
            )
    }

    /*
     * ==========================================
     * LOCATION CHANGED
     * ==========================================
     */

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

        var speed =
            0f

        if (
            location.hasSpeed()
        ) {

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

        val averageSpeed =
            if (
                speedCount > 0
            ) {

                speedSum /
                        speedCount

            } else {

                0f
            }

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
         * COORDINATES
         * --------------------------------
         */

        val latitude =
            location.latitude

        val longitude =
            location.longitude

        /*
         * ==========================================
         * UPDATE SNAPSHOT
         * ==========================================
         */

        currentSnapshot =
            TripSnapshot(
                true,
                speedKmh,
                totalDistance,
                maxSpeed,
                averageSpeed,
                duration,
                latitude,
                longitude
            )

        /*
         * ==========================================
         * SEND TO ACTIVITY
         * ==========================================
         */

        sendCurrentSnapshot()

        /*
         * ==========================================
         * NOTIFICATION
         * ==========================================
         */

        updateNotification(
            speedKmh,
            totalDistance
        )
    }

    /*
     * ==========================================
     * SEND CURRENT SNAPSHOT
     * ==========================================
     */

    private fun sendCurrentSnapshot() {

        val snapshot =
            currentSnapshot

        val updateIntent =
            Intent(
                ACTION_LOCATION_UPDATE
            ).apply {

                setPackage(
                    packageName
                )

                putExtra(
                    EXTRA_SPEED,
                    snapshot.speed
                )

                putExtra(
                    EXTRA_DISTANCE,
                    snapshot.distance
                )

                putExtra(
                    EXTRA_MAX_SPEED,
                    snapshot.maxSpeed
                )

                putExtra(
                    EXTRA_AVERAGE_SPEED,
                    snapshot.averageSpeed
                )

                putExtra(
                    EXTRA_DURATION,
                    snapshot.duration
                )

                putExtra(
                    EXTRA_LATITUDE,
                    snapshot.latitude
                )

                putExtra(
                    EXTRA_LONGITUDE,
                    snapshot.longitude
                )
            }

        sendBroadcast(
            updateIntent
        )
    }

    /*
     * ==========================================
     * NOTIFICATION
     * ==========================================
     */

    private fun createNotification():
            Notification {

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

    /*
     * ==========================================
     * NOTIFICATION CHANNEL
     * ==========================================
     */

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

    /*
     * ==========================================
     * LOCATION PERMISSION
     * ==========================================
     */

    private fun hasLocationPermission():
            Boolean {

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

    /*
     * ==========================================
     * PROVIDER CALLBACKS
     * ==========================================
     */

    override fun onProviderEnabled(
        provider: String
    ) {
    }

    override fun onProviderDisabled(
        provider: String
    ) {
    }

    @Suppress("DEPRECATION")
    override fun onStatusChanged(
        provider: String?,
        status: Int,
        extras: android.os.Bundle?
    ) {
    }

    /*
     * ==========================================
     * BIND
     * ==========================================
     */

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    /*
     * ==========================================
     * DESTROY
     * ==========================================
     */

    override fun onDestroy() {

        try {

            locationManager.removeUpdates(
                this
            )

        } catch (_: Exception) {
        }

        tripStarted =
            false

        currentSnapshot =
            currentSnapshot.copy(
                running = false
            )

        super.onDestroy()
    }
}