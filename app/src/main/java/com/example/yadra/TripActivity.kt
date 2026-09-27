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
import android.location.LocationManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
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

                /*
                 * Save current trip values
                 */

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

                /*
                 * Update UI
                 */

                updateTripUI(
                    speed,
                    distance,
                    maxSpeed,
                    averageSpeed,
                    duration
                )

                /*
                 * Update map
                 */

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
            15.0
        )

        /*
         * Temporary starting point
         */

        val startPoint =
            GeoPoint(
                35.6892,
                51.3890
            )

        tripMap.controller.setCenter(
            startPoint
        )

        /*
         * Vehicle marker
         */

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

        /*
         * فعلاً مخفی
         */

        vehicleMarker.isEnabled =
            false

        /*
         * Trip route
         */

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

            followVehicle =
                true

            if (
                !lastLatitude.isNaN() &&
                !lastLongitude.isNaN()
            ) {

                val point =
                    GeoPoint(
                        lastLatitude,
                        lastLongitude
                    )

                tripMap.controller.animateTo(
                    point
                )

                tripMap.controller.setZoom(
                    17.0
                )

            } else {

                Toast.makeText(
                    this,
                    "هنوز موقعیت GPS دریافت نشده است",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        tripMap.setOnTouchListener { _, _ ->

            followVehicle =
                false

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

        resetTripUI()

        clearTripRoute()

        lastLatitude =
            Double.NaN

        lastLongitude =
            Double.NaN

        vehicleMarker.isEnabled =
            false

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

        /*
         * اگر هیچ GPS دریافت نشده،
         * ذخیره نکن
         */

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

        val dialog =
            AlertDialog.Builder(
                this
            )
                .setTitle(
                    "ذخیره سفر"
                )
                .setMessage(
                    "یک نام برای این سفر وارد کنید"
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

                if (name.isEmpty()) {

                    editText.error =
                        "نام سفر را وارد کنید"

                    return@setOnClickListener
                }

                saveTrip(
                    name
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
        name: String
    ) {

        val trips =
            getTrips()

        val date =
            SimpleDateFormat(
                "yyyy/MM/dd HH:mm",
                Locale.US
            ).format(
                Date()
            )

        val trip =
            JSONObject()

        trip.put(
            "name",
            name
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

        /*
         * سفر جدید اول لیست
         */

        trips.add(
            0,
            trip
        )

        /*
         * فقط 50 سفر آخر
         */

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

        /*
         * آماده سفر بعدی
         */

        resetTripUI()

        clearTripRoute()

        lastLatitude =
            Double.NaN

        lastLongitude =
            Double.NaN

        vehicleMarker.isEnabled =
            false
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

        /*
         * Main row
         */

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

        /*
         * Name
         */

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

        /*
         * نام سفر فضای اصلی را بگیرد
         */

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

        /*
         * ==========================================
         * DELETE BUTTON
         * ==========================================
         */

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

        /*
         * نارنجی YADRA
         */

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

        /*
         * اندازه و فاصله دکمه
         */

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

        /*
         * ==========================================
         * CLICK ON NAME / ROW
         * ==========================================
         */

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

        /*
         * ==========================================
         * DELETE
         * ==========================================
         */

        deleteButton.setOnClickListener {

            showDeleteConfirmation(
                trip,
                index
            )
        }

        /*
         * ==========================================
         * ADD ROW
         * ==========================================
         */

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
                "-"
            )

        val details =
            """
            مسافت
            %.2f km
            
            مدت سفر
            %s
            
            حداکثر سرعت
            %.1f km/h
            
            میانگین سرعت
            %.1f km/h
            
            تاریخ
            %s
            """.trimIndent().format(
                Locale.US,
                distance,
                formatDuration(
                    duration
                ),
                maxSpeed,
                averageSpeed,
                date
            )

        val dialog =
            AlertDialog.Builder(
                this
            )
                .setTitle(
                    name
                )
                .setMessage(
                    details
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

            /*
             * دکمه حذف هم نارنجی
             */

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

        /*
         * GPS پیدا شد
         */

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