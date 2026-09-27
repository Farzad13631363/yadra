package com.example.yadra.license

import android.content.Context

object YadraLicenseApi {

    /*
     * آدرس سایت YADRA را بعداً اینجا قرار می‌دهیم.
     *
     * فعلاً سایت ساخته نشده، بنابراین هیچ
     * درخواست اینترنتی ارسال نمی‌شود.
     */
    private const val BASE_URL =
        "https://YOUR-YADRA-SITE/api/"

    fun registerDevice(
        context: Context
    ) {

        val deviceId =
            DeviceIdentity.getDeviceId(
                context
            )

        // فعلاً فقط برای تست
        println(
            "YADRA Device: $deviceId"
        )
    }

    fun checkLicense(
        context: Context
    ) {

        // بعداً:
        // POST /license/check
    }

    fun activateLicense(
        context: Context,
        activationCode: String
    ) {

        // بعداً:
        // POST /license/activate
    }

    fun createPurchase(
        context: Context
    ) {

        // بعداً:
        // POST /purchase/create
    }
}