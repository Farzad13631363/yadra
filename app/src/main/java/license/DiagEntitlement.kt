package com.example.yadra.license

/** Product policy: a server-verified diag license includes every professional feature. */
enum class DashboardItemId(val title: String, val free: Boolean = false) {
    PROFILE("Profile", true), BUY("Buy", true), CONNECTION("Connection", true),
    WARNING("Warning", true), SENSORS("Sensors"), ERRORS("Errors"), ECU("ECU"),
    UNKNOWN_ECU("Unknown ECU"), KWP_TEST("KWP TEST"), ECU_CODE_TEST("ECU CODE TEST"),
    ACTUATORS("Actuators"), AI("AI"), SETTINGS("Settings", true), TRIP("Trip"), CHARTS("Charts")
}

object DiagEntitlement {
    // Every dashboard ID belongs to diag, including the features that are also free.
    fun canAccess(item: DashboardItemId, verifiedDiag: Boolean): Boolean = item.free || verifiedDiag

    // Compatibility for callers outside the dashboard; dashboard access uses stable IDs.
    fun includes(feature: String): Boolean =
        DashboardItemId.values().any { it.title == feature } || feature == "Professional dashboard"

    // active must come from LicenseManager.isActive(context), never a saved preference.
    fun canAccess(feature: String, active: Boolean): Boolean {
        DashboardItemId.values().firstOrNull { it.title == feature }?.let {
            return canAccess(it, active)
        }
        return feature == "Support" || (active && feature == "Professional dashboard")
    }

    // Validates only an already server-verified proof. LicenseManager owns that proof.
    internal fun isVerifiedDiagActive(proof: LicenseInfo?, deviceId: String): Boolean =
        proof != null && proof.status == LicenseStatus.ACTIVE &&
            proof.softwareCode == "diag" && proof.licenseId.isNotBlank() &&
            deviceId.isNotBlank() && proof.deviceId == deviceId
}
