package com.example.yadra.license

data class LicenseInfo(
    val licenseId: String = "",
    val deviceId: String = "",
    val activationToken: String = "",
    val status: LicenseStatus =
        LicenseStatus.INACTIVE,
    val expiresAt: String = ""
)

enum class LicenseStatus {
    INACTIVE,
    ACTIVE,
    EXPIRED,
    DEVICE_MISMATCH
}