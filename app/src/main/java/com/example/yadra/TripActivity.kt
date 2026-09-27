package com.example.yadra

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TripActivity : AppCompatActivity() {

    companion object {

        private const val PREFS_NAME =
            "yadra_trip_history"

        private const val PREFS_TRIPS =
            "trips"

        private const val MAX_TRIPS =
            50

        private const val ORANGE =
            "#FF9800"

        private const val DARK =
            "#121212"

        private const val CARD_DARK =
            "#1E1E1E"

        private const val TEXT_WHITE =
            "#FFFFFF"

        private const val TEXT_GRAY =
            "#BDBDBD"
    }

    private lateinit var tvTripStatus: TextView
    private lateinit var tvCurrentSpeed: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvMaxSpeed: TextView
    private lateinit var tvAverageSpeed: TextView

    private lateinit var btnStartStopTrip: Button
    private lateinit var btnMapLocation: ImageButton

    private lateinit var tripMap: MapView

    private lateinit var tripHistoryContainer: LinearLayout

    private lateinit var vehicleMarker: Marker
    private lateinit var tripPolyline: Polyline

    private lateinit var tripPreferences: SharedPreferences

    private val tripPoints =
        ArrayList<GeoPoint>()

    private var lastLatitude =
        Double.NaN

    private var lastLongitude =
        Double.NaN

    private var tripRunning =
        false

    private var followVehicle =
        true

    /*
     * ==========================================
     * CURRENT LOCATION REQUEST
     * ==========================================
     */

    private var waitingForCurrentLocation =
        false

    private val locationHandler =
        Handler(Looper.getMainLooper())

    private val locationTimeoutRunnable =
        Runnable {

            if (waitingForCurrentLocation) {

                waitingForCurrentLocation =
                    false

                stopTemporaryLocationUpdates()

                Toast.makeText(
                    this,
                    "موقعیت GPS دریافت نشد",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val temporaryLocationListener =
        object : LocationListener {

            override fun onLocationChanged(
                location: Location
            ) {

                if (!waitingForCurrentLocation) {
                    return
                }

                waitingForCurrentLocation =
                    false

                locationHandler.removeCallbacks(
                    locationTimeoutRunnable
                )

                stopTemporaryLocationUpdates()

                updateVehicleLocation(
                    location.latitude,
                    location.longitude
                )

                followVehicle =
                    true

                val point =
                    GeoPoint(
                        location.latitude,
                        location.longitude
                    )

                tripMap.controller.animateTo(
                    point
                )

                tripMap.controller.setZoom(
                    17.0
                )

                tripMap.invalidate()

                if (!tripRunning) {

                    tvTripStatus.text =
                        "موقعیت فعلی دریافت شد"

                    tvTripStatus.setTextColor(
                        Color.parseColor(
                            ORANGE
                        )
                    )
                }
            }

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
                extras: Bundle?
            ) {
            }
        }

    /*
     * ==========================================
     * CURRENT TRIP DATA
     * ==========================================
     */

    private var currentSpeed =
        0f

    private var currentDistance =
        0f

    private var currentDuration =
        0L

    private var currentMaxSpeed =
        0f

    private var currentAverageSpeed =
        0f

    /*
     * ==========================================
     * BROADCAST RECEIVER
     * ==========================================
     */

    private val tripReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                if (
                    intent?.action !=
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

                val latitude =
                    intent.getDoubleExtra(
                        TripLocationService.EXTRA_LATITUDE,
                        Double.NaN
                    )

                val longitude =
                    intent.getDoubleExtra(
                        TripLocationService.EXTRA_LONGITUDE,
                        Double.NaN
                    )

                currentSpeed =
                    speed

                currentDistance =
                    distance

                currentDuration =
                    duration

                currentMaxSpeed =
                    maxSpeed

                currentAverageSpeed =
                    averageSpeed

                updateTripUI(
                    speed,
                    distance,
                    maxSpeed,
                    averageSpeed,
                    duration
                )

                if (
                    !latitude.isNaN() &&
                    !longitude.isNaN()
                ) {

                    updateVehicleLocation(
                        latitude,
                        longitude
                    )
                }
            }
        }

    /*
     * ==========================================
     * ON CREATE
     * ==========================================
     */

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_trip
        )

        tripPreferences =
            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )

        initializeViews()

        initializeMap()

        initializeMapControls()

        resetTripUI()

        loadTripHistory()
    }

    /*
     * ==========================================
     * VIEWS
     * ==========================================
     */

    private fun initializeViews() {

        tvTripStatus =
            findViewById(
                R.id.tvTripStatus
            )

        tvCurrentSpeed =
            findViewById(
                R.id.tvCurrentSpeed
            )

        tvDistance =
            findViewById(
                R.id.tvDistance
            )

        tvDuration =
            findViewById(
                R.id.tvDuration
            )

        tvMaxSpeed =
            findViewById(
                R.id.tvMaxSpeed
            )

        tvAverageSpeed =
            findViewById(
                R.id.tvAverageSpeed
            )

        btnStartStopTrip =
            findViewById(
                R.id.btnStartStopTrip
            )

        btnMapLocation =
            findViewById(
                R.id.btnMapLocation
            )

        tripMap =
            findViewById(
                R.id.tripMap
            )

        tripHistoryContainer =
            findViewById(
                R.id.tripHistoryContainer
            )
    }

    /*
     * ==========================================
     * MAP
     * ==========================================
     */

    private fun initializeMap() {

        Configuration
            .getInstance()
            .userAgentValue =
            "YADRA/1.0 (Android)"

        tripMap.setTileSource(
            TileSourceFactory.MAPNIK
        )

        tripMap.setMultiTouchControls(
            true
        )

        tripMap.setBuiltInZoomControls(
            true
        )

        tripMap.controller.setZoom(
            16.0
        )

        val startPoint =
            GeoPoint(
                35.6892,
                51.3890
            )

        tripMap.controller.setCenter(
            startPoint
        )

        vehicleMarker =
            Marker(
                tripMap
            )

        vehicleMarker.title =
            "YADRA Vehicle"

        vehicleMarker.snippet =
            "موقعیت فعلی خودرو"

        vehicleMarker.position =
            startPoint

        vehicleMarker.setAnchor(
            Marker.ANCHOR_CENTER,
            Marker.ANCHOR_BOTTOM
        )

        vehicleMarker.isEnabled =
            false

        tripPolyline =
            Polyline()

        tripPolyline.title =
            "YADRA Trip Route"

        tripPolyline.width =
            8f

        tripMap.overlays.add(
            tripPolyline
        )

        tripMap.overlays.add(
            vehicleMarker
        )

        tripMap.invalidate()
    }

    /*
     * ==========================================
     * MAP CONTROLS
     * ==========================================
     */

    private fun initializeMapControls() {

        btnMapLocation.setOnClickListener {

            getCurrentLocation()
        }

        tripMap.setOnTouchListener { _, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN,
                MotionEvent.ACTION_MOVE -> {

                    followVehicle =
                        false
                }
            }

            false
        }

        btnStartStopTrip.setOnClickListener {

            if (tripRunning) {

                stopTrip()

            } else {

                startTrip()
            }
        }
    }

    /*
     * ==========================================
     * GET CURRENT LOCATION
     * ==========================================
     */

    private fun getCurrentLocation() {

        if (!hasLocationPermission()) {

            Toast.makeText(
                this,
                "دسترسی GPS برای دریافت مکان فعلی لازم است",
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

        val networkEnabled =
            try {

                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )

            } catch (_: Exception) {

                false
            }

        if (
            !gpsEnabled &&
            !networkEnabled
        ) {

            Toast.makeText(
                this,
                "لطفاً GPS یا Location دستگاه را روشن کنید",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        var lastKnownLocation:
                Location? = null

        try {

            if (gpsEnabled) {

                lastKnownLocation =
                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )
            }

            if (
                lastKnownLocation == null &&
                networkEnabled
            ) {

                lastKnownLocation =
                    locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )
            }

        } catch (_: SecurityException) {
        }

        if (lastKnownLocation != null) {

            val latitude =
                lastKnownLocation.latitude

            val longitude =
                lastKnownLocation.longitude

            updateVehicleLocation(
                latitude,
                longitude
            )

            followVehicle =
                true

            val point =
                GeoPoint(
                    latitude,
                    longitude
                )

            tripMap.controller.animateTo(
                point
            )

            tripMap.controller.setZoom(
                17.0
            )

            tripMap.invalidate()

            if (!tripRunning) {

                tvTripStatus.text =
                    "موقعیت فعلی دریافت شد"

                tvTripStatus.setTextColor(
                    Color.parseColor(
                        ORANGE
                    )
                )
            }

            requestFreshLocation()

            return
        }

        requestFreshLocation()
    }

    /*
     * ==========================================
     * REQUEST FRESH LOCATION
     * ==========================================
     */

    private fun requestFreshLocation() {

        if (!hasLocationPermission()) {
            return
        }

        if (waitingForCurrentLocation) {
            return
        }

        val locationManager =
            getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

        waitingForCurrentLocation =
            true

        tvTripStatus.text =
            "در حال دریافت موقعیت GPS..."

        tvTripStatus.setTextColor(
            Color.parseColor(
                ORANGE
            )
        )

        try {

            if (
                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )
            ) {

                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0f,
                    temporaryLocationListener
                )
            }

            if (
                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )
            ) {

                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0f,
                    temporaryLocationListener
                )
            }

            locationHandler.postDelayed(
                locationTimeoutRunnable,
                10000L
            )

        } catch (_: SecurityException) {

            waitingForCurrentLocation =
                false

            Toast.makeText(
                this,
                "دسترسی GPS در دسترس نیست",
                Toast.LENGTH_SHORT
            ).show()

        } catch (_: Exception) {

            waitingForCurrentLocation =
                false

            Toast.makeText(
                this,
                "دریافت موقعیت GPS ناموفق بود",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /*
     * ==========================================
     * STOP TEMPORARY LOCATION
     * ==========================================
     */

    private fun stopTemporaryLocationUpdates() {

        try {

            val locationManager =
                getSystemService(
                    Context.LOCATION_SERVICE
                ) as LocationManager

            locationManager.removeUpdates(
                temporaryLocationListener
            )

        } catch (_: Exception) {
        }
    }

    /*
     * ==========================================
     * START TRIP
     * ==========================================
     */

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

        waitingForCurrentLocation =
            false

        locationHandler.removeCallbacks(
            locationTimeoutRunnable
        )

        stopTemporaryLocationUpdates()

        resetTripUI()

        clearTripRoute()

        followVehicle =
            true

        currentSpeed =
            0f

        currentDistance =
            0f

        currentDuration =
            0L

        currentMaxSpeed =
            0f

        currentAverageSpeed =
            0f

        tripRunning =
            true

        tvTripStatus.text =
            "در حال دریافت موقعیت GPS..."

        tvTripStatus.setTextColor(
            Color.parseColor(
                ORANGE
            )
        )

        btnStartStopTrip.text =
            "پایان سفر"

        btnStartStopTrip.setTextColor(
            Color.WHITE
        )

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

    /*
     * ==========================================
     * STOP TRIP
     * ==========================================
     */

    private fun stopTrip() {

        tripRunning =
            false

        val serviceIntent =
            Intent(
                this,
                TripLocationService::class.java
            ).apply {

                action =
                    TripLocationService.ACTION_STOP
            }

        startService(
            serviceIntent
        )

        tvTripStatus.text =
            "سفر پایان یافت"

        tvTripStatus.setTextColor(
            Color.parseColor(
                ORANGE
            )
        )

        btnStartStopTrip.text =
            "شروع سفر"

        if (
            currentDuration <= 0L &&
            currentDistance <= 0f &&
            tripPoints.isEmpty()
        ) {

            Toast.makeText(
                this,
                "اطلاعاتی برای ذخیره سفر وجود ندارد",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        showSaveTripDialog()
    }

    /*
     * ==========================================
     * SAVE TRIP DIALOG
     * ==========================================
     */

    private fun showSaveTripDialog() {

        val editText =
            EditText(this)

        editText.setSingleLine(
            true
        )

        editText.hint =
            "مثلاً سفر شمال"

        editText.setTextColor(
            Color.WHITE
        )

        editText.setHintTextColor(
            Color.GRAY
        )

        editText.setPadding(
            20,
            10,
            20,
            10
        )

        val commentEditText =
            EditText(this)

        commentEditText.setSingleLine(
            false
        )

        commentEditText.setMinLines(
            3
        )

        commentEditText.gravity =
            Gravity.TOP

        commentEditText.hint =
            "کامنت یا توضیحات سفر"

        commentEditText.setTextColor(
            Color.WHITE
        )

        commentEditText.setHintTextColor(
            Color.GRAY
        )

        commentEditText.setPadding(
            20,
            10,
            20,
            10
        )

        val container =
            LinearLayout(this)

        container.orientation =
            LinearLayout.VERTICAL

        container.setPadding(
            30,
            10,
            30,
            0
        )

        container.addView(
            editText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val commentParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        commentParams.topMargin =
            12.dp()

        container.addView(
            commentEditText,
            commentParams
        )

        val dialog =
            AlertDialog.Builder(
                this
            )
                .setTitle(
                    "ذخیره سفر"
                )
                .setMessage(
                    "نام و توضیحات سفر را وارد کنید"
                )
                .setView(
                    container
                )
                .setNegativeButton(
                    "انصراف",
                    null
                )
                .setPositiveButton(
                    "ذخیره",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.window?.setBackgroundDrawableResource(
                android.R.color.transparent
            )

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setTextColor(
                Color.parseColor(
                    ORANGE
                )
            )

            dialog.getButton(
                AlertDialog.BUTTON_NEGATIVE
            ).setTextColor(
                Color.parseColor(
                    ORANGE
                )
            )

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val name =
                    editText.text
                        .toString()
                        .trim()

                val comment =
                    commentEditText.text
                        .toString()
                        .trim()

                if (name.isEmpty()) {

                    editText.error =
                        "نام سفر را وارد کنید"

                    return@setOnClickListener
                }

                saveTrip(
                    name,
                    comment
                )

                dialog.dismiss()
            }
        }

        dialog.show()

        dialog.window?.decorView?.post {

            dialog.window
                ?.decorView
                ?.setBackgroundColor(
                    Color.parseColor(
                        CARD_DARK
                    )
                )
        }
    }

    /*
     * ==========================================
     * SAVE TRIP
     * ==========================================
     */

    private fun saveTrip(
        name: String,
        comment: String
    ) {

        val trips =
            getTrips()

        val date =
            getDateTimeBoth()

        val trip =
            JSONObject()

        trip.put(
            "name",
            name
        )

        trip.put(
            "comment",
            comment
        )

        trip.put(
            "distance",
            currentDistance
        )

        trip.put(
            "duration",
            currentDuration
        )

        trip.put(
            "maxSpeed",
            currentMaxSpeed
        )

        trip.put(
            "averageSpeed",
            currentAverageSpeed
        )

        trip.put(
            "date",
            date
        )

        trips.add(
            0,
            trip
        )

        while (
            trips.size > MAX_TRIPS
        ) {

            trips.removeAt(
                trips.lastIndex
            )
        }

        saveTrips(
            trips
        )

        loadTripHistory()

        Toast.makeText(
            this,
            "سفر «$name» ذخیره شد",
            Toast.LENGTH_SHORT
        ).show()

        resetTripUI()

        clearTripRoute()

        followVehicle =
            false
    }

    /*
     * ==========================================
     * DATE
     * ==========================================
     */

    private fun getDateTimeBoth(): String {

        val now =
            Date()

        val gregorianDate =
            SimpleDateFormat(
                "yyyy/MM/dd HH:mm",
                Locale.US
            ).format(
                now
            )

        val calendar =
            Calendar.getInstance()

        calendar.time =
            now

        val gy =
            calendar.get(Calendar.YEAR)

        val gm =
            calendar.get(Calendar.MONTH) + 1

        val gd =
            calendar.get(Calendar.DAY_OF_MONTH)

        val hour =
            calendar.get(Calendar.HOUR_OF_DAY)

        val minute =
            calendar.get(Calendar.MINUTE)

        val (jy, jm, jd) =
            gregorianToPersian(
                gy,
                gm,
                gd
            )

        val persianDate =
            "\u202A%04d/%02d/%02d %02d:%02d\u202C".format(
                Locale.US,
                jy,
                jm,
                jd,
                hour,
                minute
            )

        val gregorianDateLeft =
            "\u202Aمیلادی: %s\u202C".format(
                Locale.US,
                gregorianDate
            )

        return "$persianDate\n$gregorianDateLeft"
    }

    /*
     * ==========================================
     * GREGORIAN TO PERSIAN
     * ==========================================
     */

    private fun gregorianToPersian(
        gy: Int,
        gm: Int,
        gd: Int
    ): Triple<Int, Int, Int> {

        val gDaysInMonth =
            intArrayOf(
                31, 28, 31, 30, 31, 30,
                31, 31, 30, 31, 30, 31
            )

        var gyTemp =
            gy

        var jy =
            0

        if (gyTemp >= 1600) {

            jy =
                979

            gyTemp -=
                1600

        } else {

            jy =
                0

            gyTemp -=
                621
        }

        val gy2 =
            if (gm > 2) {
                gyTemp + 1
            } else {
                gyTemp
            }

        var days =
            365 * gyTemp +
                    ((gy2 + 3) / 4) -
                    ((gy2 + 99) / 100) +
                    ((gy2 + 399) / 400)

        for (i in 0 until gm - 1) {

            days +=
                gDaysInMonth[i]
        }

        days +=
            gd - 80

        val jyTemp =
            jy +
                    33 * (days / 12053)

        var daysRemaining =
            days % 12053

        var jyFinal =
            jyTemp +
                    4 * (daysRemaining / 1461)

        daysRemaining %=
            1461

        if (daysRemaining > 365) {

            jyFinal +=
                (daysRemaining - 1) / 365

            daysRemaining =
                (daysRemaining - 1) % 365
        }

        val jm: Int
        val jd: Int

        if (daysRemaining < 186) {

            jm =
                1 + daysRemaining / 31

            jd =
                1 + daysRemaining % 31

        } else {

            jm =
                7 + (daysRemaining - 186) / 30

            jd =
                1 + (daysRemaining - 186) % 30
        }

        return Triple(
            jyFinal,
            jm,
            jd
        )
    }

    /*
     * ==========================================
     * GET TRIPS
     * ==========================================
     */

    private fun getTrips():
            MutableList<JSONObject> {

        val result =
            mutableListOf<JSONObject>()

        val jsonString =
            tripPreferences.getString(
                PREFS_TRIPS,
                "[]"
            )

        try {

            val array =
                JSONArray(
                    jsonString
                )

            for (
            i in 0 until array.length()
            ) {

                result.add(
                    array.getJSONObject(i)
                )
            }

        } catch (_: Exception) {
        }

        return result
    }

    /*
     * ==========================================
     * SAVE TRIPS
     * ==========================================
     */

    private fun saveTrips(
        trips: List<JSONObject>
    ) {

        val array =
            JSONArray()

        for (
        trip in trips
        ) {

            array.put(
                trip
            )
        }

        tripPreferences
            .edit()
            .putString(
                PREFS_TRIPS,
                array.toString()
            )
            .apply()
    }

    /*
     * ==========================================
     * LOAD HISTORY
     * ==========================================
     */

    private fun loadTripHistory() {

        tripHistoryContainer.removeAllViews()

        val trips =
            getTrips()

        if (trips.isEmpty()) {

            val emptyText =
                TextView(this)

            emptyText.text =
                "هنوز سفری ذخیره نشده است"

            emptyText.textSize =
                15f

            emptyText.setTextColor(
                Color.parseColor(
                    TEXT_GRAY
                )
            )

            emptyText.gravity =
                Gravity.CENTER

            emptyText.setPadding(
                20,
                30,
                20,
                30
            )

            tripHistoryContainer.addView(
                emptyText
            )

            return
        }

        for (
        index in trips.indices
        ) {

            addTripHistoryItem(
                trips[index],
                index
            )
        }
    }

    /*
     * ==========================================
     * HISTORY ITEM
     * ==========================================
     */

    private fun addTripHistoryItem(
        trip: JSONObject,
        index: Int
    ) {

        val row =
            LinearLayout(this)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        row.setPadding(
            16.dp(),
            10.dp(),
            10.dp(),
            10.dp()
        )

        row.setBackgroundColor(
            Color.parseColor(
                CARD_DARK
            )
        )

        val nameText =
            TextView(this)

        nameText.text =
            trip.optString(
                "name",
                "سفر بدون نام"
            )

        nameText.textSize =
            17f

        nameText.setTextColor(
            Color.WHITE
        )

        nameText.gravity =
            Gravity.CENTER_VERTICAL

        nameText.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        nameText.setPadding(
            5.dp(),
            5.dp(),
            5.dp(),
            5.dp()
        )

        val nameParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

        nameParams.weight =
            1f

        row.addView(
            nameText,
            nameParams
        )

        val deleteButton =
            Button(this)

        deleteButton.text =
            "حذف"

        deleteButton.textSize =
            14f

        deleteButton.setTextColor(
            Color.BLACK
        )

        deleteButton.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        deleteButton.isAllCaps =
            false

        deleteButton.setPadding(
            0,
            0,
            0,
            0
        )

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.LOLLIPOP
        ) {

            deleteButton.backgroundTintList =
                ColorStateList.valueOf(
                    Color.parseColor(
                        ORANGE
                    )
                )

        } else {

            deleteButton.setBackgroundColor(
                Color.parseColor(
                    ORANGE
                )
            )
        }

        val deleteParams =
            LinearLayout.LayoutParams(
                80.dp(),
                42.dp()
            )

        deleteParams.gravity =
            Gravity.CENTER_VERTICAL

        deleteParams.setMargins(
            8.dp(),
            0,
            0,
            0
        )

        row.addView(
            deleteButton,
            deleteParams
        )

        nameText.setOnClickListener {

            showTripDetails(
                trip
            )
        }

        row.setOnClickListener {

            showTripDetails(
                trip
            )
        }

        deleteButton.setOnClickListener {

            showDeleteConfirmation(
                trip,
                index
            )
        }

        val rowParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                62.dp()
            )

        rowParams.setMargins(
            0,
            0,
            0,
            8.dp()
        )

        tripHistoryContainer.addView(
            row,
            rowParams
        )
    }

    /*
     * ==========================================
     * TRIP DETAILS
     * ==========================================
     */

    private fun showTripDetails(
        trip: JSONObject
    ) {

        val name =
            trip.optString(
                "name",
                "سفر بدون نام"
            )

        val comment =
            trip.optString(
                "comment",
                ""
            ).trim()

        val distance =
            trip.optDouble(
                "distance",
                0.0
            ) / 1000.0

        val duration =
            trip.optLong(
                "duration",
                0L
            )

        val maxSpeed =
            trip.optDouble(
                "maxSpeed",
                0.0
            )

        val averageSpeed =
            trip.optDouble(
                "averageSpeed",
                0.0
            )

        val date =
            trip.optString(
                "date",
                ""
            ).trim()

        /*
         * ==========================================
         * DETAILS TEXT
         * ==========================================
         */

        val details =
            """
            مسافت:
            %.2f km
            
            مدت سفر:
            %s
            
            حداکثر سرعت:
            %.1f km/h
            
            میانگین سرعت:
            %.1f km/h
            """.trimIndent().format(
                Locale.US,
                distance,
                formatDuration(
                    duration
                ),
                maxSpeed,
                averageSpeed
            )

        /*
         * ==========================================
         * DATE CONTAINER
         * ==========================================
         */

        val mainContainer =
            LinearLayout(this)

        mainContainer.orientation =
            LinearLayout.VERTICAL

        mainContainer.setPadding(
            30.dp(),
            10.dp(),
            30.dp(),
            10.dp()
        )

        /*
         * Details
         */

        val detailsText =
            TextView(this)

        detailsText.text =
            details

        detailsText.textSize =
            15f

        detailsText.setTextColor(
            Color.LTGRAY
        )

        detailsText.gravity =
            Gravity.RIGHT

        mainContainer.addView(
            detailsText
        )

        /*
         * ==========================================
         * DATE TITLE
         * ==========================================
         */

        val dateTitle =
            TextView(this)

        dateTitle.text =
            "تاریخ:"

        dateTitle.textSize =
            15f

        dateTitle.setTextColor(
            Color.WHITE
        )

        dateTitle.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        dateTitle.gravity =
            Gravity.RIGHT

        val dateTitleParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        dateTitleParams.topMargin =
            12.dp()

        dateTitleParams.bottomMargin =
            6.dp()

        mainContainer.addView(
            dateTitle,
            dateTitleParams
        )

        /*
         * ==========================================
         * DATE LINES
         * ==========================================
         */

        val dateLines =
            date.split("\n")

        var shamsiDate =
            ""

        var gregorianDate =
            ""

        if (
            dateLines.isNotEmpty()
        ) {

            shamsiDate =
                dateLines[0]
                    .replace(
                        "شمسی:",
                        ""
                    )
                    .replace(
                        "میلادی:",
                        ""
                    )
                    .trim()
        }

        if (
            dateLines.size > 1
        ) {

            gregorianDate =
                dateLines[1]
                    .replace(
                        "میلادی:",
                        ""
                    )
                    .replace(
                        "شمسی:",
                        ""
                    )
                    .trim()
        }

        val shamsiText =
            TextView(this)

        shamsiText.text =
            if (
                shamsiDate.isNotEmpty()
            ) {

                "شمسی:    $shamsiDate"

            } else {

                "شمسی:    ندارد"
            }

        shamsiText.textSize =
            15f

        shamsiText.setTextColor(
            Color.LTGRAY
        )

        shamsiText.textDirection =
            android.view.View.TEXT_DIRECTION_LTR

        shamsiText.gravity =
            Gravity.LEFT

        mainContainer.addView(
            shamsiText
        )

        val gregorianText =
            TextView(this)

        gregorianText.text =
            if (
                gregorianDate.isNotEmpty()
            ) {

                "میلادی:  $gregorianDate"

            } else {

                "میلادی:  ندارد"
            }

        gregorianText.textSize =
            15f

        gregorianText.setTextColor(
            Color.LTGRAY
        )

        gregorianText.textDirection =
            android.view.View.TEXT_DIRECTION_LTR

        gregorianText.gravity =
            Gravity.LEFT

        mainContainer.addView(
            gregorianText
        )

        /*
         * ==========================================
         * COMMENT
         * ==========================================
         */

        val commentTitle =
            TextView(this)

        commentTitle.text =
            "کامنت:"

        commentTitle.textSize =
            15f

        commentTitle.setTextColor(
            Color.WHITE
        )

        commentTitle.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        commentTitle.gravity =
            Gravity.RIGHT

        val commentTitleParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        commentTitleParams.topMargin =
            12.dp()

        commentTitleParams.bottomMargin =
            4.dp()

        mainContainer.addView(
            commentTitle,
            commentTitleParams
        )

        val commentText =
            TextView(this)

        commentText.text =
            if (
                comment.isNotEmpty()
            ) {

                comment

            } else {

                "ندارد"
            }

        commentText.textSize =
            15f

        commentText.setTextColor(
            Color.LTGRAY
        )

        commentText.gravity =
            Gravity.RIGHT

        mainContainer.addView(
            commentText
        )

        /*
         * ==========================================
         * DIALOG
         * ==========================================
         */

        val dialog =
            AlertDialog.Builder(
                this
            )
                .setTitle(
                    name
                )
                .setView(
                    mainContainer
                )
                .setPositiveButton(
                    "بستن",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setTextColor(
                Color.parseColor(
                    ORANGE
                )
            )
        }

        dialog.show()
    }

    /*
     * ==========================================
     * DELETE CONFIRMATION
     * ==========================================
     */

    private fun showDeleteConfirmation(
        trip: JSONObject,
        index: Int
    ) {

        val name =
            trip.optString(
                "name",
                "این سفر"
            )

        val dialog =
            AlertDialog.Builder(
                this
            )
                .setTitle(
                    "حذف سفر"
                )
                .setMessage(
                    "آیا از حذف «$name» اطمینان دارید؟"
                )
                .setNegativeButton(
                    "انصراف",
                    null
                )
                .setPositiveButton(
                    "حذف"
                ) { _, _ ->

                    deleteTrip(
                        index
                    )
                }
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setTextColor(
                Color.parseColor(
                    ORANGE
                )
            )

            dialog.getButton(
                AlertDialog.BUTTON_NEGATIVE
            ).setTextColor(
                Color.parseColor(
                    ORANGE
                )
            )
        }

        dialog.show()
    }

    /*
     * ==========================================
     * DELETE TRIP
     * ==========================================
     */

    private fun deleteTrip(
        index: Int
    ) {

        val trips =
            getTrips()

        if (
            index < 0 ||
            index >= trips.size
        ) {

            return
        }

        val name =
            trips[index].optString(
                "name",
                "سفر"
            )

        trips.removeAt(
            index
        )

        saveTrips(
            trips
        )

        loadTripHistory()

        Toast.makeText(
            this,
            "سفر «$name» حذف شد",
            Toast.LENGTH_SHORT
        ).show()
    }

    /*
     * ==========================================
     * UPDATE VEHICLE LOCATION
     * ==========================================
     */

    private fun updateVehicleLocation(
        latitude: Double,
        longitude: Double
    ) {

        val currentPoint =
            GeoPoint(
                latitude,
                longitude
            )

        lastLatitude =
            latitude

        lastLongitude =
            longitude

        vehicleMarker.position =
            currentPoint

        vehicleMarker.isEnabled =
            true

        if (tripRunning) {

            addTripPoint(
                currentPoint
            )
        }

        if (followVehicle) {

            tripMap.controller.animateTo(
                currentPoint
            )
        }

        tripMap.invalidate()

        if (tripRunning) {

            tvTripStatus.text =
                "سفر در حال ثبت است"

            tvTripStatus.setTextColor(
                Color.parseColor(
                    ORANGE
                )
            )
        }
    }

    /*
     * ==========================================
     * ADD POINT
     * ==========================================
     */

    private fun addTripPoint(
        point: GeoPoint
    ) {

        if (
            tripPoints.isNotEmpty()
        ) {

            val lastPoint =
                tripPoints.last()

            if (
                lastPoint.latitude ==
                point.latitude &&
                lastPoint.longitude ==
                point.longitude
            ) {

                return
            }
        }

        tripPoints.add(
            point
        )

        tripPolyline.setPoints(
            tripPoints
        )
    }

    /*
     * ==========================================
     * CLEAR ROUTE
     * ==========================================
     */

    private fun clearTripRoute() {

        tripPoints.clear()

        tripPolyline.setPoints(
            emptyList()
        )

        tripMap.invalidate()
    }

    /*
     * ==========================================
     * PERMISSION
     * ==========================================
     */

    private fun hasLocationPermission():
            Boolean {

        val fineLocation =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        val coarseLocation =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        return fineLocation ||
                coarseLocation
    }

    /*
     * ==========================================
     * RESET UI
     * ==========================================
     */

    private fun resetTripUI() {

        tvTripStatus.text =
            "آماده شروع سفر"

        tvTripStatus.setTextColor(
            Color.parseColor(
                TEXT_GRAY
            )
        )

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

        btnStartStopTrip.setTextColor(
            Color.WHITE
        )
    }

    /*
     * ==========================================
     * UPDATE UI
     * ==========================================
     */

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

    /*
     * ==========================================
     * FORMAT DURATION
     * ==========================================
     */

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

    /*
     * ==========================================
     * RECEIVER REGISTER
     * ==========================================
     */

    override fun onStart() {

        super.onStart()

        val filter =
            IntentFilter().apply {

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

    /*
     * ==========================================
     * RECEIVER UNREGISTER
     * ==========================================
     */

    override fun onStop() {

        try {

            unregisterReceiver(
                tripReceiver
            )

        } catch (_: Exception) {
        }

        super.onStop()
    }

    /*
     * ==========================================
     * MAP RESUME
     * ==========================================
     */

    override fun onResume() {

        super.onResume()

        tripMap.onResume()
    }

    /*
     * ==========================================
     * MAP PAUSE
     * ==========================================
     */

    override fun onPause() {

        tripMap.onPause()

        super.onPause()
    }

    /*
     * ==========================================
     * DESTROY
     * ==========================================
     */

    override fun onDestroy() {

        waitingForCurrentLocation =
            false

        locationHandler.removeCallbacks(
            locationTimeoutRunnable
        )

        stopTemporaryLocationUpdates()

        super.onDestroy()
    }

    /*
     * ==========================================
     * DP HELPER
     * ==========================================
     */

    private fun Int.dp(): Int {

        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }
}