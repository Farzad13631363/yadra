package com.example.yadra

import com.example.yadra.license.*
import org.junit.Assert.*
import org.junit.Test

class LicensePolicyTest {
    @Test fun emptyExpiryDoesNotMeanLifetime() {
        assertFalse(LicenseInfo().durationText().contains("مادام‌العمر"))
        assertFalse(LicenseInfo(isPerpetual = false).durationText().contains("مادام‌العمر"))
    }

    @Test fun explicitPerpetualPlanDisplaysPersianLifetime() {
        assertEquals("مدت اعتبار: مادام‌العمر", LicenseInfo(isPerpetual = true).durationText())
    }

    @Test fun realExpiryTakesPrecedenceOverPerpetualFlag() {
        val expiry = "2030-01-01T00:00:00.000Z"
        assertEquals("تاریخ انقضا: $expiry",
            LicenseInfo(expiresAt = expiry, isPerpetual = true).durationText())
    }

    private val professional = listOf(
        "Professional dashboard", "ECU", "Unknown ECU", "Errors", "Sensors",
        "Charts", "Trip", "AI", "Actuators", "KWP TEST", "ECU CODE TEST"
    )
    private val proof = LicenseInfo(
        licenseId = "test-license", deviceId = "test-device",
        softwareCode = "diag", status = LicenseStatus.ACTIVE
    )

    @Test fun verifiedActiveDiagGrantsEveryProfessionalFeature() {
        val active = DiagEntitlement.isVerifiedDiagActive(proof, "test-device")
        assertTrue(active)
        professional.forEach { assertTrue(it, DiagEntitlement.canAccess(it, active)) }
    }

    @Test fun inactiveAndExpiredLicensesDenyProfessionalFeatures() {
        listOf(LicenseStatus.INACTIVE, LicenseStatus.EXPIRED, LicenseStatus.DEVICE_MISMATCH).forEach { status ->
            val active = DiagEntitlement.isVerifiedDiagActive(proof.copy(status = status), "test-device")
            assertFalse(active)
            professional.forEach { assertFalse(it, DiagEntitlement.canAccess(it, active)) }
        }
    }

    @Test fun mismatchedSoftwareOrDeviceAndMissingIdentityDenyAccess() {
        listOf(proof.copy(softwareCode = "other"), proof.copy(deviceId = "other"),
            proof.copy(licenseId = ""), proof.copy(softwareCode = "")).forEach {
            assertFalse(DiagEntitlement.isVerifiedDiagActive(it, "test-device"))
        }
    }

    @Test fun absentProcessProofCannotRestoreAccessAfterRestart() {
        assertFalse(DiagEntitlement.isVerifiedDiagActive(null, "test-device"))
        // Identity hints and a persisted ACTIVE alone have no verified software proof.
        assertFalse(DiagEntitlement.isVerifiedDiagActive(
            LicenseInfo(licenseId = "test-license", deviceId = "test-device", status = LicenseStatus.ACTIVE),
            "test-device"))
    }

    @Test fun freeFeaturesStayAccessibleAndUnknownFeaturesFailClosed() {
        listOf("Buy", "Profile", "Connection", "Warning", "Settings", "Support").forEach {
            assertTrue(it, DiagEntitlement.canAccess(it, false))
        }
        assertFalse(DiagEntitlement.canAccess("unknown", true))
        assertFalse(DiagEntitlement.canAccess("unknown", false))
    }
}
