package com.example.yadra

object ObdCodeDatabase {

    private fun info(
        code: String,
        typeRes: Int,
        descriptionRes: Int,
        categoryRes: Int,
        isCngRelated: Boolean = false
    ): ObdCodeInfo {
        return ObdCodeInfo(
            code = code,
            typeRes = typeRes,
            descriptionRes = descriptionRes,
            categoryRes = categoryRes,
            isCngRelated = isCngRelated
        )
    }

    private val codes = listOf(

        // -------------------------------------------------
        // P0300 - P0304
        // -------------------------------------------------

        info(
            "P0300",
            R.string.error_type_powertrain,
            R.string.obd_p0300_description,
            R.string.category_misfire
        ),

        info(
            "P0301",
            R.string.error_type_powertrain,
            R.string.obd_p0301_description,
            R.string.category_misfire
        ),

        info(
            "P0302",
            R.string.error_type_powertrain,
            R.string.obd_p0302_description,
            R.string.category_misfire
        ),

        info(
            "P0303",
            R.string.error_type_powertrain,
            R.string.obd_p0303_description,
            R.string.category_misfire
        ),

        info(
            "P0304",
            R.string.error_type_powertrain,
            R.string.obd_p0304_description,
            R.string.category_misfire
        ),

        // -------------------------------------------------
        // Fuel / Air
        // -------------------------------------------------

        info(
            "P0170",
            R.string.error_type_powertrain,
            R.string.obd_p0170_description,
            R.string.category_fuel_air
        ),

        info(
            "P0171",
            R.string.error_type_powertrain,
            R.string.obd_p0171_description,
            R.string.category_fuel_air,
            true
        ),

        info(
            "P0172",
            R.string.error_type_powertrain,
            R.string.obd_p0172_description,
            R.string.category_fuel_air,
            true
        ),

        info(
            "P0174",
            R.string.error_type_powertrain,
            R.string.obd_p0174_description,
            R.string.category_fuel_air,
            true
        ),

        info(
            "P0175",
            R.string.error_type_powertrain,
            R.string.obd_p0175_description,
            R.string.category_fuel_air,
            true
        ),

        // -------------------------------------------------
        // Oxygen sensors
        // -------------------------------------------------

        info(
            "P0130",
            R.string.error_type_powertrain,
            R.string.obd_p0130_description,
            R.string.category_oxygen_sensor
        ),

        info(
            "P0131",
            R.string.error_type_powertrain,
            R.string.obd_p0131_description,
            R.string.category_oxygen_sensor
        ),

        info(
            "P0132",
            R.string.error_type_powertrain,
            R.string.obd_p0132_description,
            R.string.category_oxygen_sensor
        ),

        info(
            "P0133",
            R.string.error_type_powertrain,
            R.string.obd_p0133_description,
            R.string.category_oxygen_sensor
        ),

        info(
            "P0134",
            R.string.error_type_powertrain,
            R.string.obd_p0134_description,
            R.string.category_oxygen_sensor
        ),

        info(
            "P0135",
            R.string.error_type_powertrain,
            R.string.obd_p0135_description,
            R.string.category_oxygen_sensor
        ),

        info(
            "P0140",
            R.string.error_type_powertrain,
            R.string.obd_p0140_description,
            R.string.category_oxygen_sensor
        ),

        // -------------------------------------------------
        // Catalyst
        // -------------------------------------------------

        info(
            "P0420",
            R.string.error_type_powertrain,
            R.string.obd_p0420_description,
            R.string.category_emissions
        ),

        // -------------------------------------------------
        // EGR
        // -------------------------------------------------

        info(
            "P0400",
            R.string.error_type_powertrain,
            R.string.obd_p0400_description,
            R.string.category_egr
        ),

        info(
            "P0401",
            R.string.error_type_powertrain,
            R.string.obd_p0401_description,
            R.string.category_egr
        ),

        info(
            "P0402",
            R.string.error_type_powertrain,
            R.string.obd_p0402_description,
            R.string.category_egr
        ),

        // -------------------------------------------------
        // MAF
        // -------------------------------------------------

        info(
            "P0100",
            R.string.error_type_powertrain,
            R.string.obd_p0100_description,
            R.string.category_air_flow
        ),

        info(
            "P0101",
            R.string.error_type_powertrain,
            R.string.obd_p0101_description,
            R.string.category_air_flow
        ),

        info(
            "P0102",
            R.string.error_type_powertrain,
            R.string.obd_p0102_description,
            R.string.category_air_flow
        ),

        info(
            "P0103",
            R.string.error_type_powertrain,
            R.string.obd_p0103_description,
            R.string.category_air_flow
        ),

        // -------------------------------------------------
        // MAP
        // -------------------------------------------------

        info(
            "P0105",
            R.string.error_type_powertrain,
            R.string.obd_p0105_description,
            R.string.category_map_sensor
        ),

        info(
            "P0106",
            R.string.error_type_powertrain,
            R.string.obd_p0106_description,
            R.string.category_map_sensor
        ),

        info(
            "P0107",
            R.string.error_type_powertrain,
            R.string.obd_p0107_description,
            R.string.category_map_sensor
        ),

        info(
            "P0108",
            R.string.error_type_powertrain,
            R.string.obd_p0108_description,
            R.string.category_map_sensor
        ),

        // -------------------------------------------------
        // Throttle
        // -------------------------------------------------

        info(
            "P0120",
            R.string.error_type_powertrain,
            R.string.obd_p0120_description,
            R.string.category_throttle
        ),

        info(
            "P0121",
            R.string.error_type_powertrain,
            R.string.obd_p0121_description,
            R.string.category_throttle
        ),

        info(
            "P0122",
            R.string.error_type_powertrain,
            R.string.obd_p0122_description,
            R.string.category_throttle
        ),

        info(
            "P0123",
            R.string.error_type_powertrain,
            R.string.obd_p0123_description,
            R.string.category_throttle
        ),

        // -------------------------------------------------
        // Coolant temperature
        // -------------------------------------------------

        info(
            "P0115",
            R.string.error_type_powertrain,
            R.string.obd_p0115_description,
            R.string.category_temperature
        ),

        info(
            "P0116",
            R.string.error_type_powertrain,
            R.string.obd_p0116_description,
            R.string.category_temperature
        ),

        info(
            "P0117",
            R.string.error_type_powertrain,
            R.string.obd_p0117_description,
            R.string.category_temperature
        ),

        info(
            "P0118",
            R.string.error_type_powertrain,
            R.string.obd_p0118_description,
            R.string.category_temperature
        ),

        // -------------------------------------------------
        // Ignition coils
        // -------------------------------------------------

        info(
            "P0351",
            R.string.error_type_powertrain,
            R.string.obd_p0351_description,
            R.string.category_ignition
        ),

        info(
            "P0352",
            R.string.error_type_powertrain,
            R.string.obd_p0352_description,
            R.string.category_ignition
        ),

        info(
            "P0353",
            R.string.error_type_powertrain,
            R.string.obd_p0353_description,
            R.string.category_ignition
        ),

        info(
            "P0354",
            R.string.error_type_powertrain,
            R.string.obd_p0354_description,
            R.string.category_ignition
        ),

        // -------------------------------------------------
        // Fuel pressure
        // -------------------------------------------------

        info(
            "P0087",
            R.string.error_type_powertrain,
            R.string.obd_p0087_description,
            R.string.category_fuel_pressure,
            true
        ),

        info(
            "P0088",
            R.string.error_type_powertrain,
            R.string.obd_p0088_description,
            R.string.category_fuel_pressure,
            true
        ),

        // -------------------------------------------------
        // Network
        // -------------------------------------------------

        info(
            "U0100",
            R.string.error_type_network,
            R.string.obd_u0100_description,
            R.string.category_communication
        ),

        info(
            "U0121",
            R.string.error_type_network,
            R.string.obd_u0121_description,
            R.string.category_communication
        ),

        // -------------------------------------------------
        // Body
        // -------------------------------------------------

        info(
            "B0001",
            R.string.error_type_body,
            R.string.obd_b0001_description,
            R.string.category_body
        ),

        // -------------------------------------------------
        // Chassis
        // -------------------------------------------------

        info(
            "C0035",
            R.string.error_type_chassis,
            R.string.obd_c0035_description,
            R.string.category_chassis
        )
    )

    fun find(code: String): ObdCodeInfo? {
        return codes.firstOrNull {
            it.code.equals(code.trim(), ignoreCase = true)
        }
    }
}