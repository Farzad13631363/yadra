package com.example.yadra.license

data class LicenseInfo(
    val licenseId: String = "",
    val deviceId: String = "",
    val activationToken: String = "",
    val status: LicenseStatus =
        LicenseStatus.INACTIVE,
    val expiresAt: String = "",
    val softwareCode: String = "",
    val isPerpetual: Boolean? = null,
    val durationDays: Int? = null
)

enum class LicenseStatus {
    INACTIVE,
    ACTIVE,
    EXPIRED,
    DEVICE_MISMATCH
}
// Null expiry alone is never evidence of a perpetual plan.
fun LicenseInfo.durationText(): String = when {
    expiresAt.isNotBlank() -> "تاریخ انقضا: $expiresAt"
    isPerpetual == true -> "مدت اعتبار: مادام‌العمر"
    else -> "مدت اعتبار: نامشخص؛ اطلاعات طرح از سرور لازم است"
}
