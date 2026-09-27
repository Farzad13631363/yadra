package com.example.yadra

import java.util.Locale

object ObdSensorParser {

    fun parse(
        sensor: ObdSensor,
        response: String
    ): Float? {

        val bytes =
            extractBytesAfterHeader(
                response,
                sensor.pid
            )

        if (bytes.isEmpty()) {
            return null
        }

        return when (sensor.id) {

            // =================================================
            // ENGINE
            // =================================================

            "engine_load" ->
                byte0(bytes)?.let {
                    it * 100f / 255f
                }

            "coolant_temp" ->
                byte0(bytes)?.let {
                    it - 40f
                }

            "short_fuel_trim_bank1",
            "long_fuel_trim_bank1",
            "short_fuel_trim_bank2",
            "long_fuel_trim_bank2" ->
                byte0(bytes)?.let {
                    (it * 100f / 128f) - 100f
                }

            "fuel_pressure" ->
                byte0(bytes)?.let {
                    it * 3f
                }

            "map" ->
                byte0(bytes)?.toFloat()

            "rpm" ->
                if (bytes.size >= 2) {
                    (bytes[0] * 256f + bytes[1]) / 4f
                } else {
                    null
                }

            "vehicle_speed" ->
                byte0(bytes)?.toFloat()

            "timing_advance" ->
                byte0(bytes)?.let {
                    it / 2f - 64f
                }

            "intake_air_temp" ->
                byte0(bytes)?.let {
                    it - 40f
                }

            "maf" ->
                if (bytes.size >= 2) {
                    (bytes[0] * 256f + bytes[1]) / 100f
                } else {
                    null
                }

            "throttle" ->
                byte0(bytes)?.let {
                    it * 100f / 255f
                }

            // =================================================
            // O2
            // =================================================

            "o2_sensor_1",
            "o2_sensor_2",
            "o2_sensor_3",
            "o2_sensor_4",
            "o2_sensor_5",
            "o2_sensor_6",
            "o2_sensor_7",
            "o2_sensor_8" ->
                byte0(bytes)?.let {
                    it / 200f
                }

            // =================================================
            // GENERAL
            // =================================================

            "obd_standard" ->
                byte0(bytes)?.toFloat()

            "run_time" ->
                if (bytes.size >= 2) {
                    bytes[0] * 256f + bytes[1]
                } else {
                    null
                }

            "distance_mil" ->
                if (bytes.size >= 2) {
                    bytes[0] * 256f + bytes[1]
                } else {
                    null
                }

            "fuel_rail_pressure" ->
                if (bytes.size >= 2) {
                    (bytes[0] * 256f + bytes[1]) * 10f
                } else {
                    null
                }

            "commanded_egr" ->
                byte0(bytes)?.let {
                    it * 100f / 255f
                }

            "egr_error" ->
                byte0(bytes)?.let {
                    it * 100f / 128f - 100f
                }

            "evap_purge" ->
                byte0(bytes)?.let {
                    it * 100f / 255f
                }

            "fuel_level" ->
                byte0(bytes)?.let {
                    it * 100f / 255f
                }

            "barometric_pressure" ->
                byte0(bytes)?.toFloat()

            // =================================================
            // VOLTAGE
            // PID 0142
            // A*256+B / 1000
            // =================================================

            "control_module_voltage",
            "battery_voltage" -> {

                if (bytes.size >= 2) {

                    val raw =
                        bytes[0] * 256 + bytes[1]

                    raw / 1000f

                } else {

                    null
                }
            }

            // =================================================
            // LOAD / THROTTLE
            // =================================================

            "absolute_load" ->
                if (bytes.size >= 2) {
                    (bytes[0] * 256f + bytes[1]) *
                            100f / 255f
                } else {
                    null
                }

            "commanded_equivalence_ratio" ->
                if (bytes.size >= 2) {
                    (bytes[0] * 256f + bytes[1]) /
                            32768f
                } else {
                    null
                }

            "relative_throttle" ->
                byte0(bytes)?.let {
                    it * 100f / 255f
                }

            "ambient_temp" ->
                byte0(bytes)?.let {
                    it - 40f
                }

            "accelerator_pedal_d",
            "accelerator_pedal_e",
            "accelerator_pedal_f",
            "commanded_throttle",
            "throttle_actuator_control" ->
                byte0(bytes)?.let {
                    it * 100f / 255f
                }

            // =================================================
            // TEMPERATURE
            // =================================================

            "engine_oil_temp" ->
                byte0(bytes)?.let {
                    it - 40f
                }

            "coolant_temp_sensor_2",
            "intake_air_temp_sensor_2",
            "egr_temperature" ->
                byte0(bytes)?.let {
                    it - 40f
                }

            // =================================================
            // FUEL
            // =================================================

            "fuel_injection_timing" ->
                if (bytes.size >= 2) {

                    (
                            (bytes[0] * 256f + bytes[1]) /
                                    128f
                            ) - 210f

                } else {
                    null
                }

            "engine_fuel_rate" ->
                if (bytes.size >= 2) {

                    (
                            bytes[0] * 256f +
                                    bytes[1]
                            ) / 20f

                } else {
                    null
                }

            // =================================================
            // TORQUE
            // =================================================

            "driver_demand_torque",
            "engine_percent_torque" ->
                byte0(bytes)?.let {
                    it - 125f
                }

            "engine_reference_torque" ->
                if (bytes.size >= 2) {

                    bytes[0] * 256f +
                            bytes[1].toFloat()

                } else {
                    null
                }

            // =================================================
            // OTHER
            // =================================================

            "mass_air_flow_b" ->
                if (bytes.size >= 2) {

                    (
                            bytes[0] * 256f +
                                    bytes[1]
                            ) / 32f

                } else {
                    null
                }

            "fuel_system_status",
            "auxiliary_input" ->
                byte0(bytes)?.toFloat()

            else ->
                null
        }
    }

    // =========================================================
    // FIRST BYTE
    // =========================================================

    private fun byte0(
        bytes: List<Int>
    ): Int? {

        return bytes.getOrNull(0)
    }

    // =========================================================
    // EXTRACT PID DATA
    // =========================================================

    private fun extractBytesAfterHeader(
        response: String,
        pid: String
    ): List<Int> {

        if (response.isBlank()) {
            return emptyList()
        }

        val upper =
            response.uppercase(
                Locale.US
            )

        // -----------------------------------------------------
        // INVALID RESPONSES
        // -----------------------------------------------------

        if (
            upper.contains("NO DATA") ||
            upper.contains("NODATA") ||
            upper.contains("ERROR") ||
            upper.contains("UNABLE TO CONNECT") ||
            upper.contains("UNABLETOCONNECT") ||
            upper.contains("STOPPED")
        ) {
            return emptyList()
        }

        val cleaned =
            upper
                .replace(
                    "SEARCHING...",
                    " "
                )
                .replace(
                    "SEARCHING",
                    " "
                )
                .replace(
                    ">",
                    " "
                )
                .replace(
                    "\r",
                    " "
                )
                .replace(
                    "\n",
                    " "
                )
                .trim()

        // -----------------------------------------------------
        // PID
        // -----------------------------------------------------

        val pidHex =
            pid
                .replace(
                    Regex("[^0-9A-F]"),
                    ""
                )
                .uppercase(
                    Locale.US
                )

        if (pidHex.length < 4) {
            return emptyList()
        }

        val service =
            pidHex.substring(
                0,
                2
            )

        val pidByte =
            pidHex.substring(
                2,
                4
            )

        val serviceNumber =
            service.toIntOrNull(16)
                ?: return emptyList()

        val responseService =
            String.format(
                Locale.US,
                "%02X",
                serviceNumber + 0x40
            )

        // -----------------------------------------------------
        // TOKEN MODE
        // Handles:
        //
        // 41 42 31 20
        // 41 42 31 20 3E
        // -----------------------------------------------------

        val tokens =
            Regex(
                "(?i)[0-9A-F]{2}"
            )
                .findAll(cleaned)
                .map {
                    it.value.uppercase(
                        Locale.US
                    )
                }
                .toList()

        if (tokens.size >= 2) {

            for (
            index in 0 until tokens.size - 1
            ) {

                if (
                    tokens[index] ==
                    responseService &&
                    tokens[index + 1] ==
                    pidByte
                ) {

                    val data =
                        tokens
                            .drop(index + 2)
                            .mapNotNull {
                                it.toIntOrNull(16)
                            }

                    if (data.isNotEmpty()) {
                        return data
                    }
                }
            }
        }

        // -----------------------------------------------------
        // HEX-ONLY MODE
        //
        // Handles responses such as:
        //
        // 41423120
        // 41420F10
        // -----------------------------------------------------

        val hexOnly =
            cleaned.replace(
                Regex("[^0-9A-F]"),
                ""
            )

        val header =
            responseService +
                    pidByte

        val headerPosition =
            hexOnly.indexOf(
                header
            )

        if (headerPosition < 0) {
            return emptyList()
        }

        val dataStart =
            headerPosition +
                    header.length

        if (
            dataStart >=
            hexOnly.length
        ) {
            return emptyList()
        }

        val result =
            ArrayList<Int>()

        var index =
            dataStart

        while (
            index + 2 <=
            hexOnly.length
        ) {

            val token =
                hexOnly.substring(
                    index,
                    index + 2
                )

            val value =
                token.toIntOrNull(16)

            if (value != null) {
                result.add(value)
            }

            index += 2
        }

        return result
    }
}