package com.example.yadra

data class EcuConnectionProfile(
    val id: String,
    val source: String,

    val protocolCode: String,
    val protocolName: String,
    val scanType: String,

    val elmProtocol: String = "",
    val baudRate: Int? = null,

    val canExtended: Boolean? = null,

    val requestId: String? = null,
    val responseIds: List<String> = emptyList(),

    val ecuAddress: String? = null,
    val ecuIdentifier: String? = null,

    val vin: String? = null,
    val softwareVersion: String? = null,
    val hardwareVersion: String? = null,

    val successfulCommands: List<String> = emptyList(),

    val fingerprintResponses: Map<String, String> = emptyMap(),

    val elmVersion: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),

    val successCount: Int = 0,
    val failureCount: Int = 0,

    val confidence: Int = 0
)