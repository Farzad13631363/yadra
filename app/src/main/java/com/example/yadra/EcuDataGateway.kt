package com.example.yadra

object EcuDataGateway {

    // =========================================================
    // SENSOR
    // =========================================================

    fun readSensor(
        sensor: ObdSensor
    ): Float? {

        return when (EcuRuntimeState.mode) {

            EcuMode.STANDARD -> {

                ObdSensorReader.readSensor(
                    sensor
                )
            }

            EcuMode.UNKNOWN -> {

                readUnknownSensor(
                    sensor
                )
            }
        }
    }

    // =========================================================
    // UNKNOWN SENSOR
    // =========================================================

    private fun readUnknownSensor(
        sensor: ObdSensor
    ): Float? {

        if (
            EcuRuntimeState.mode !=
            EcuMode.UNKNOWN
        ) {
            return null
        }

        if (
            !EcuRuntimeState.connected
        ) {
            return null
        }

        val response =
            when (
                ConnectionSource.activeSource
            ) {

                ConnectionSource.Source.WIFI -> {

                    if (
                        !YadraConnectionManager.elmConnected
                    ) {
                        return null
                    }

                    YadraConnectionManager
                        .sendCommand(
                            sensor.pid
                        )
                }

                ConnectionSource.Source.BLUETOOTH -> {

                    if (
                        !BluetoothConnectionManager.elmConnected
                    ) {
                        return null
                    }

                    BluetoothConnectionManager
                        .sendActiveCommand(
                            sensor.pid
                        )
                }

                ConnectionSource.Source.NONE -> {
                    return null
                }
            }

        if (
            response.isBlank()
        ) {
            return null
        }

        return ObdSensorParser.parse(
            sensor,
            response
        )
    }

    // =========================================================
    // RPM
    // =========================================================

    fun readRpm(): Int? {

        return when (
            EcuRuntimeState.mode
        ) {

            EcuMode.STANDARD -> {

                EcuConnectionManager
                    .readRpm()
            }

            EcuMode.UNKNOWN -> {

                val rpmSensor =
                    ObdSensor(
                        id = "rpm",
                        name = "RPM",
                        pid = "010C",
                        unit = "rpm",
                        minimum = 0f,
                        maximum = 12000f
                    )

                readUnknownSensor(
                    rpmSensor
                )?.toInt()
            }
        }
    }

    // =========================================================
    // READ ERRORS
    // =========================================================

    fun readErrors(): String? {

        return when (
            EcuRuntimeState.mode
        ) {

            // -------------------------------------------------
            // STANDARD ECU
            // -------------------------------------------------

            EcuMode.STANDARD -> {

                when (
                    ConnectionSource.activeSource
                ) {

                    ConnectionSource.Source.WIFI -> {

                        if (
                            !YadraConnectionManager.elmConnected ||
                            !YadraConnectionManager.ecuConnected
                        ) {
                            return null
                        }

                        YadraConnectionManager
                            .sendCommand(
                                "03"
                            )
                    }

                    ConnectionSource.Source.BLUETOOTH -> {

                        if (
                            !BluetoothConnectionManager.elmConnected ||
                            !BluetoothConnectionManager.ecuConnected
                        ) {
                            return null
                        }

                        BluetoothConnectionManager
                            .sendActiveCommand(
                                "03"
                            )
                    }

                    ConnectionSource.Source.NONE -> {
                        null
                    }
                }
            }

            // -------------------------------------------------
            // UNKNOWN ECU
            // -------------------------------------------------

            EcuMode.UNKNOWN -> {

                if (
                    !EcuRuntimeState.connected
                ) {
                    return null
                }

                when (
                    ConnectionSource.activeSource
                ) {

                    ConnectionSource.Source.WIFI -> {

                        if (
                            !YadraConnectionManager.elmConnected
                        ) {
                            return null
                        }

                        /*
                         * ECU detected in Unknown mode.
                         *
                         * The ECU was observed responding to
                         * Mode 07 during discovery.
                         *
                         * Do NOT use standard Mode 03 here.
                         */

                        YadraConnectionManager
                            .sendCommand(
                                "07"
                            )
                    }

                    ConnectionSource.Source.BLUETOOTH -> {

                        if (
                            !BluetoothConnectionManager.elmConnected
                        ) {
                            return null
                        }

                        /*
                         * Unknown ECU:
                         * use the command that was actually
                         * observed during discovery.
                         */

                        BluetoothConnectionManager
                            .sendActiveCommand(
                                "07"
                            )
                    }

                    ConnectionSource.Source.NONE -> {
                        null
                    }
                }
            }
        }
    }

    // =========================================================
    // CLEAR ERRORS
    // =========================================================

    fun clearErrors(): String? {

        return when (
            EcuRuntimeState.mode
        ) {

            // -------------------------------------------------
            // STANDARD ECU
            // -------------------------------------------------

            EcuMode.STANDARD -> {

                when (
                    ConnectionSource.activeSource
                ) {

                    ConnectionSource.Source.WIFI -> {

                        if (
                            !YadraConnectionManager.elmConnected ||
                            !YadraConnectionManager.ecuConnected
                        ) {
                            return null
                        }

                        YadraConnectionManager
                            .sendCommand(
                                "04"
                            )
                    }

                    ConnectionSource.Source.BLUETOOTH -> {

                        if (
                            !BluetoothConnectionManager.elmConnected ||
                            !BluetoothConnectionManager.ecuConnected
                        ) {
                            return null
                        }

                        BluetoothConnectionManager
                            .sendActiveCommand(
                                "04"
                            )
                    }

                    ConnectionSource.Source.NONE -> {
                        null
                    }
                }
            }

            // -------------------------------------------------
            // UNKNOWN ECU
            // -------------------------------------------------

            EcuMode.UNKNOWN -> {

                /*
                 * We do NOT know yet which command safely
                 * clears DTCs on this ECU.
                 *
                 * Therefore never send standard Mode 04.
                 */

                null
            }
        }
    }
}