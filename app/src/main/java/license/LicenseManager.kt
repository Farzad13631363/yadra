package com.example.yadra.license

import android.content.Context

object LicenseManager {
    // A process-local server proof, never restored from SharedPreferences.
    @Volatile private var verified: LicenseInfo? = null

    private val listeners = java.util.concurrent.CopyOnWriteArraySet<() -> Unit>()
    fun addVerificationListener(listener: () -> Unit) { listeners.add(listener) }
    fun removeVerificationListener(listener: () -> Unit) { listeners.remove(listener) }
    private fun notifyVerificationChanged() { listeners.forEach { it() } }

    internal fun saveVerifiedLicense(context: Context, license: LicenseInfo) {
        verified = license.takeIf {
            DiagEntitlement.isVerifiedDiagActive(it, DeviceIdentity.getAndroidIdHash(context))
        }
        context.getSharedPreferences("yadra_license", Context.MODE_PRIVATE).edit()
            .putString("license_id", license.licenseId)
            .putString("device_id", license.deviceId)
            .putString("status", license.status.name)
            .putString("activation_token", "")
            .apply()
        notifyVerificationChanged()
    }

    fun invalidateVerification() { verified = null; notifyVerificationChanged() }

    fun loadLicense(context: Context): LicenseInfo {
        verified?.let { return it }
        val prefs = context.getSharedPreferences("yadra_license", Context.MODE_PRIVATE)
        return LicenseInfo(licenseId = prefs.getString("license_id", "") ?: "",
            deviceId = prefs.getString("device_id", "") ?: "")
    }

    fun isActive(context: Context): Boolean = DiagEntitlement.isVerifiedDiagActive(
        verified, DeviceIdentity.getAndroidIdHash(context)
    )
}
