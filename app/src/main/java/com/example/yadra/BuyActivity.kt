package com.example.yadra

import android.app.AlertDialog
import android.widget.EditText
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yadra.license.DeviceIdentity
import com.example.yadra.license.LicenseManager
import com.example.yadra.license.LicenseStatus

class BuyActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_buy
        )

        val txtDeviceId =
            findViewById<TextView>(
                R.id.txtDeviceId
            )

        val txtLicenseStatus =
            findViewById<TextView>(
                R.id.txtLicenseStatus
            )

        val txtLicenseId =
            findViewById<TextView>(
                R.id.txtLicenseId
            )

        val txtExpiresAt =
            findViewById<TextView>(
                R.id.txtExpiresAt
            )

        val btnBuy =
            findViewById<Button>(
                R.id.btnBuy
            )
        val btnActivate =
            findViewById<Button>(
                R.id.btnActivate
            )

        btnActivate.setOnClickListener {

            val input =
                EditText(this)

            input.hint =
                "کد فعال‌سازی"

            AlertDialog.Builder(this)
                .setTitle("فعال‌سازی لایسنس")
                .setView(input)
                .setPositiveButton("فعال‌سازی") { _, _ ->

                    val activationCode =
                        input.text
                            .toString()
                            .trim()

                    if (activationCode.isBlank()) {

                        Toast.makeText(
                            this,
                            "کد فعال‌سازی را وارد کنید",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "اتصال به سرور فعال‌سازی بعداً اضافه می‌شود",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .setNegativeButton(
                    "انصراف",
                    null
                )
                .show()
        }
        val deviceId =
            DeviceIdentity.getDeviceId(
                this
            )

        txtDeviceId.text =
            deviceId

        val license =
            LicenseManager.loadLicense(
                this
            )

        txtLicenseStatus.text =
            when (license.status) {

                LicenseStatus.ACTIVE ->
                    "فعال"

                LicenseStatus.EXPIRED ->
                    "منقضی شده"

                LicenseStatus.DEVICE_MISMATCH ->
                    "این لایسنس برای دستگاه دیگری است"

                LicenseStatus.INACTIVE ->
                    "فعال نیست"
            }

        txtLicenseId.text =
            if (license.licenseId.isBlank()) {
                "License ID: —"
            } else {
                "License ID: ${license.licenseId}"
            }

        txtExpiresAt.text =
            if (license.expiresAt.isBlank()) {
                "تاریخ انقضا: —"
            } else {
                "تاریخ انقضا: ${license.expiresAt}"
            }

        btnBuy.setOnClickListener {

            Toast.makeText(
                this,
                "سایت خرید YADRA هنوز آماده نشده است",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}