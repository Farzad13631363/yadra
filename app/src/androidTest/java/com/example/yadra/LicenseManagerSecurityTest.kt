package com.example.yadra

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import com.example.yadra.license.*
import org.junit.Assert.*
import org.junit.Test

class LicenseManagerSecurityTest {
    @Test fun persistedActiveNeverRestoresServerProof() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        // Isolate test preferences; never touch the user's saved license identity.
        val context = object : ContextWrapper(target) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                target.getSharedPreferences("license_policy_test_$name", mode)
        }
        val prefs = context.getSharedPreferences("yadra_license", Context.MODE_PRIVATE)
        try {
            LicenseManager.invalidateVerification()
            prefs.edit().putString("status", "ACTIVE").putString("softwareCode", "diag")
                .putString("license_id", "test-license")
                .putString("device_id", DeviceIdentity.getAndroidIdHash(context)).commit()
            assertFalse(LicenseManager.isActive(context))
            assertEquals(LicenseStatus.INACTIVE, LicenseManager.loadLicense(context).status)
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
