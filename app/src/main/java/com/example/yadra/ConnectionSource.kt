package com.example.yadra

object ConnectionSource {

    enum class Source {
        NONE,
        WIFI,
        BLUETOOTH
    }

    @Volatile
    var activeSource: Source = Source.NONE
        private set

    fun setWifi() {
        activeSource = Source.WIFI
    }

    fun setBluetooth() {
        activeSource = Source.BLUETOOTH
    }

    fun clear() {
        activeSource = Source.NONE
    }
}