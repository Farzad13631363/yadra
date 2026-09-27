package com.example.yadra

object ObdSensorCatalog {

    val sensors = listOf(

        // =========================================================
        // Engine / Fuel / Air
        // =========================================================

        ObdSensor(
            id = "engine_load",
            name = "Calculated Engine Load",
            pid = "0104",
            unit = "%",
            minimum = 0f,
            maximum = 100f
        ),

        ObdSensor(
            id = "coolant_temp",
            name = "Engine Coolant Temperature",
            pid = "0105",
            unit = "°C",
            minimum = -40f,
            maximum = 215f
        ),

        ObdSensor(
            id = "short_fuel_trim_bank1",
            name = "Short Term Fuel Trim Bank 1",
            pid = "0106",
            unit = "%",
            minimum = -100f,
            maximum = 99.22f
        ),

        ObdSensor(
            id = "long_fuel_trim_bank1",
            name = "Long Term Fuel Trim Bank 1",
            pid = "0107",
            unit = "%",
            minimum = -100f,
            maximum = 99.22f
        ),

        ObdSensor(
            id = "short_fuel_trim_bank2",
            name = "Short Term Fuel Trim Bank 2",
            pid = "0108",
            unit = "%",
            minimum = -100f,
            maximum = 99.22f
        ),

        ObdSensor(
            id = "long_fuel_trim_bank2",
            name = "Long Term Fuel Trim Bank 2",
            pid = "0109",
            unit = "%",
            minimum = -100f,
            maximum = 99.22f
        ),

        ObdSensor(
            id = "fuel_pressure",
            name = "Fuel Pressure",
            pid = "010A",
            unit = "kPa",
            minimum = 0f,
            maximum = 765f
        ),

        ObdSensor(
            id = "map",
            name = "Intake Manifold Pressure (MAP)",
            pid = "010B",
            unit = "kPa",
            minimum = 0f,
            maximum = 255f
        ),

        ObdSensor(
            id = "rpm",
            name = "Engine RPM",
            pid = "010C",
            unit = "rpm",
            minimum = 0f,
            maximum = 16383.75f
        ),

        ObdSensor(
            id = "vehicle_speed",
            name = "Vehicle Speed",
            pid = "010D",
            unit = "km/h",
            minimum = 0f,
            maximum = 255f
        ),

        ObdSensor(
            id = "timing_advance",
            name = "Timing Advance",
            pid = "010E",
            unit = "°",
            minimum = -64f,
            maximum = 63.5f
        ),

        ObdSensor(
            id = "intake_air_temp",
            name = "Intake Air Temperature",
            pid = "010F",
            unit = "°C",
            minimum = -40f,
            maximum = 215f
        ),

        ObdSensor(
            id = "maf",
            name = "Mass Air Flow",
            pid = "0110",
            unit = "g/s",
            minimum = 0f,
            maximum = 655.35f
        ),

        ObdSensor(
            id = "throttle",
            name = "Throttle Position",
            pid = "0111",
            unit = "%",
            minimum = 0f,
            maximum = 100f
        ),

        // =========================================================
        // Oxygen / Fuel System
        // =========================================================

        ObdSensor(
            id = "o2_sensor_1",
            name = "O2 Sensor 1",
            pid = "0114",
            unit = "V",
            minimum = 0f,
            maximum = 1.275f
        ),

        ObdSensor(
            id = "o2_sensor_2",
            name = "O2 Sensor 2",
            pid = "0115",
            unit = "V",
            minimum = 0f,
            maximum = 1.275f
        ),

        ObdSensor(
            id = "o2_sensor_3",
            name = "O2 Sensor 3",
            pid = "0116",
            unit = "V",
            minimum = 0f,
            maximum = 1.275f
        ),

        ObdSensor(
            id = "o2_sensor_4",
            name = "O2 Sensor 4",
            pid = "0117",
            unit = "V",
            minimum = 0f,
            maximum = 1.275f
        ),

        ObdSensor(
            id = "o2_sensor_5",
            name = "O2 Sensor 5",
            pid = "0118",
            unit = "V",
            minimum = 0f,
            maximum = 1.275f
        ),

        ObdSensor(
            id = "o2_sensor_6",
            name = "O2 Sensor 6",
            pid = "0119",
            unit = "V",
            minimum = 0f,
            maximum = 1.275f
        ),

        ObdSensor(
            id = "o2_sensor_7",
            name = "O2 Sensor 7",
            pid = "011A",
            unit = "V",
            minimum = 0f,
            maximum = 1.275f
        ),

        ObdSensor(
            id = "o2_sensor_8",
            name = "O2 Sensor 8",
            pid = "011B",
            unit = "V",
            minimum = 0f,
            maximum = 1.275f
        ),

        // =========================================================
        // Vehicle / ECU Information
        // =========================================================

        ObdSensor(
            id = "obd_standard",
            name = "OBD Standard",
            pid = "011C",
            unit = "",
            minimum = 0f,
            maximum = 255f
        ),

        ObdSensor(
            id = "run_time",
            name = "Engine Run Time",
            pid = "011F",
            unit = "s",
            minimum = 0f,
            maximum = 65535f
        ),

        // =========================================================
        // Distance / Fuel / Ambient
        // =========================================================

        ObdSensor(
            id = "fuel_rail_pressure",
            name = "Fuel Rail Pressure",
            pid = "0123",
            unit = "kPa",
            minimum = 0f,
            maximum = 655350f
        ),

        ObdSensor(
            id = "commanded_egr",
            name = "Commanded EGR",
            pid = "012C",
            unit = "%",
            minimum = 0f,
            maximum = 100f
        ),

        ObdSensor(
            id = "egr_error",
            name = "EGR Error",
            pid = "012D",
            unit = "%",
            minimum = -100f,
            maximum = 99.22f
        ),

        ObdSensor(
            id = "evap_purge",
            name = "Evaporative Purge",
            pid = "012E",
            unit = "%",
            minimum = 0f,
            maximum = 100f
        ),

        ObdSensor(
            id = "fuel_level",
            name = "Fuel Level",
            pid = "012F",
            unit = "%",
            minimum = 0f,
            maximum = 100f
        ),

        ObdSensor(
            id = "distance_mil",
            name = "Distance With MIL On",
            pid = "0121",
            unit = "km",
            minimum = 0f,
            maximum = 65535f
        ),

        ObdSensor(
            id = "barometric_pressure",
            name = "Barometric Pressure",
            pid = "0133",
            unit = "kPa",
            minimum = 0f,
            maximum = 255f
        ),

        // =========================================================
        // Control / Voltage / Temperature
        // =========================================================

        ObdSensor(
            id = "control_module_voltage",
            name = "Control Module Voltage",
            pid = "0142",
            unit = "V",
            minimum = 0f,
            maximum = 65.535f
        ),

        ObdSensor(
            id = "absolute_load",
            name = "Absolute Load",
            pid = "0143",
            unit = "%",
            minimum = 0f,
            maximum = 25700f
        ),

        ObdSensor(
            id = "commanded_equivalence_ratio",
            name = "Commanded Equivalence Ratio",
            pid = "0144",
            unit = "",
            minimum = 0f,
            maximum = 2f
        ),

        ObdSensor(
            id = "relative_throttle",
            name = "Relative Throttle Position",
            pid = "0145",
            unit = "%",
            minimum = 0f,
            maximum = 100f
        ),

        ObdSensor(
            id = "ambient_temp",
            name = "Ambient Air Temperature",
            pid = "0146",
            unit = "°C",
            minimum = -40f,
            maximum = 215f
        ),

        ObdSensor(
            id = "accelerator_pedal_d",
            name = "Accelerator Pedal Position D",
            pid = "0149",
            unit = "%",
            minimum = 0f,
            maximum = 100f
        ),

        ObdSensor(
            id = "accelerator_pedal_e",
            name = "Accelerator Pedal Position E",
            pid = "014A",
            unit = "%",
            minimum = 0f,
            maximum = 100f
        ),

        ObdSensor(
            id = "accelerator_pedal_f",
            name = "Accelerator Pedal Position F",
            pid = "014B",
            unit = "%",
            minimum = 0f,
            maximum = 100f
        ),

        ObdSensor(
            id = "commanded_throttle",
            name = "Commanded Throttle Actuator",
            pid = "014C",
            unit = "%",
            minimum = 0f,
            maximum = 100f
        ),

        // =========================================================
        // Engine Oil / Fuel / System
        // =========================================================

        ObdSensor(
            id = "engine_oil_temp",
            name = "Engine Oil Temperature",
            pid = "015C",
            unit = "°C",
            minimum = -40f,
            maximum = 210f
        ),

        ObdSensor(
            id = "fuel_injection_timing",
            name = "Fuel Injection Timing",
            pid = "015D",
            unit = "°",
            minimum = -210f,
            maximum = 301f
        ),

        ObdSensor(
            id = "engine_fuel_rate",
            name = "Engine Fuel Rate",
            pid = "015E",
            unit = "L/h",
            minimum = 0f,
            maximum = 3212.75f
        ),

        ObdSensor(
            id = "engine_percent_torque",
            name = "Engine Percent Torque",
            pid = "0162",
            unit = "%",
            minimum = -125f,
            maximum = 130f
        ),

        ObdSensor(
            id = "engine_reference_torque",
            name = "Engine Reference Torque",
            pid = "0163",
            unit = "Nm",
            minimum = 0f,
            maximum = 65535f
        ),

        // =========================================================
        // Advanced / Newer Standard PIDs
        // =========================================================

        ObdSensor(
            id = "driver_demand_torque",
            name = "Driver Demand Engine Torque",
            pid = "0161",
            unit = "%",
            minimum = -125f,
            maximum = 130f
        ),

        ObdSensor(
            id = "mass_air_flow_b",
            name = "Mass Air Flow Sensor B",
            pid = "0166",
            unit = "g/s",
            minimum = 0f,
            maximum = 2047.96875f
        ),

        ObdSensor(
            id = "coolant_temp_sensor_2",
            name = "Engine Coolant Temperature Sensor 2",
            pid = "0167",
            unit = "°C",
            minimum = -40f,
            maximum = 215f
        ),

        ObdSensor(
            id = "intake_air_temp_sensor_2",
            name = "Intake Air Temperature Sensor 2",
            pid = "0168",
            unit = "°C",
            minimum = -40f,
            maximum = 215f
        ),

        ObdSensor(
            id = "egr_temperature",
            name = "EGR Temperature",
            pid = "0169",
            unit = "°C",
            minimum = -40f,
            maximum = 215f
        ),

        ObdSensor(
            id = "throttle_actuator_control",
            name = "Throttle Actuator Control",
            pid = "0170",
            unit = "%",
            minimum = 0f,
            maximum = 100f
        ),

        ObdSensor(
            id = "fuel_system_status",
            name = "Fuel System Status",
            pid = "0103",
            unit = "",
            minimum = 0f,
            maximum = 255f
        ),

        // =========================================================
        // Hybrid / Electric / Battery related standard data
        // =========================================================

        ObdSensor(
            id = "auxiliary_input",
            name = "Auxiliary Input Status",
            pid = "0165",
            unit = "",
            minimum = 0f,
            maximum = 255f
        ),

        ObdSensor(
            id = "battery_voltage",
            name = "Control Module / Battery Voltage",
            pid = "0142",
            unit = "V",
            minimum = 0f,
            maximum = 65.535f
        )
    )
}