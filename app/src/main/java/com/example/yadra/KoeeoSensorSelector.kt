package com.example.yadra

object KoeeoSensorSelector {

    fun getSupportedSensors(): List<ObdSensor> {

        val supportedPids =
            ObdPidSupport.getSupportedPids()
                ?: return emptyList()

        return ObdSensorCatalog.sensors.filter { sensor ->
            supportedPids.contains(sensor.pid)
        }
    }
}