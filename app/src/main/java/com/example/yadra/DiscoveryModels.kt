package com.example.yadra

enum class DiscoveryProtocol {
    OBD2,
    UDS_CAN_11,
    UDS_CAN_29,
    UDS_EXTENDED_ADDRESSING,
    KWP2000,
    ISO9141,
    J1939,
    GENERIC_CAN,
    GENERIC_ISOTP
}

data class EcuFingerprint(
    val protocol: DiscoveryProtocol,
    val positiveCommands: List<String> = emptyList(),
    val responseIds: List<String> = emptyList(),
    val requestId: String? = null,
    val ecuAddress: String? = null,
    val ecuIdentifier: String? = null,
    val vin: String? = null,
    val softwareVersion: String? = null,
    val hardwareVersion: String? = null,
    val rawResponses: Map<String, String> = emptyMap(),
    val confidence: Int = 0
)