package com.example.yadra

object ObdSensorReader {

    fun readSensor(
        sensor: ObdSensor
    ): Float? {

        val response =
            when (ConnectionSource.activeSource) {

                ConnectionSource.Source.WIFI -> {

                    if (!YadraConnectionManager.ecuConnected) {
                        return null
                    }

                    YadraConnectionManager.sendCommand(
                        sensor.pid
                    )
                }

                ConnectionSource.Source.BLUETOOTH -> {

                    if (!BluetoothConnectionManager.elmConnected) {
                        return null
                    }

                    BluetoothConnectionManager.sendActiveCommand(
                        sensor.pid
                    )
                }

                ConnectionSource.Source.NONE -> {
                    return null
                }
            }

        if (response.isBlank()) {
            return null
        }

        return ObdSensorParser.parse(
            sensor,
            response
        )
    }
}