package com.example.yadra.license

import android.content.Context

object LicenseManager {

    private const val PREFS_NAME =
        "yadra_license"

    private const val KEY_LICENSE_ID =
        "license_id"

    private const val KEY_DEVICE_ID =
        "device_id"

    private const val KEY_TOKEN =
        "activation_token"

    private const val KEY_STATUS =
        "status"

    private const val KEY_EXPIRES_AT =
        "expires_at"

    fun saveLicense(
        context: Context,
        licenseInfo: LicenseInfo
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_LICENSE_ID,
                licenseInfo.licenseId
            )
            .putString(
                KEY_DEVICE_ID,
                licenseInfo.deviceId
            )
            .putString(
                KEY_TOKEN,
                licenseInfo.activationToken
            )
            .putString(
                KEY_STATUS,
                licenseInfo.status.name
            )
            .putString(
                KEY_EXPIRES_AT,
                licenseInfo.expiresAt
            )
            .apply()
    }

    fun loadLicense(
        context: Context
    ): LicenseInfo {

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val statusName =
            preferences.getString(
                KEY_STATUS,
                LicenseStatus.INACTIVE.name
            )

        val status =
            try {
                LicenseStatus.valueOf(
                    statusName
                        ?: LicenseStatus.INACTIVE.name
                )
            } catch (_: Exception) {
                LicenseStatus.INACTIVE
            }

        return LicenseInfo(
            licenseId =
                preferences.getString(
                    KEY_LICENSE_ID,
                    ""
                ) ?: "",

            deviceId =
                preferences.getString(
                    KEY_DEVICE_ID,
                    ""
                ) ?: "",

            activationToken =
                preferences.getString(
                    KEY_TOKEN,
                    ""
                ) ?: "",

            status = status,

            expiresAt =
                preferences.getString(
                    KEY_EXPIRES_AT,
                    ""
                ) ?: ""
        )
    }

    fun clearLicense(
        context: Context
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .clear()
            .apply()
    }

    fun isActive(
        context: Context
    ): Boolean {

        val license =
            loadLicense(context)

        return license.status ==
                LicenseStatus.ACTIVE
    }
}