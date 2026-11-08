package com.example.yadra.license

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceIdentity {

    private const val PREFIX = "YD"

    fun getAndroidId(
        context: Context
    ): String {

        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"
    }

    fun getDeviceId(
        context: Context
    ): String {

        val androidId = getAndroidId(context)

        val hash = sha256(androidId)

        return "$PREFIX-${hash.take(16).uppercase()}"
    }

    // شناسه هش‌شده کامل برای ارسال به سرور
    fun getAndroidIdHash(
        context: Context
    ): String {

        val androidId = getAndroidId(context)

        require(
            androidId.isNotBlank() &&
                    androidId != "UNKNOWN"
        ) {
            "Android device ID is unavailable"
        }

        return sha256(androidId)
    }

    private fun sha256(
        value: String
    ): String {

        val digest = MessageDigest.getInstance(
            "SHA-256"
        )

        val bytes = digest.digest(
            value.toByteArray(
                Charsets.UTF_8
            )
        )

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}