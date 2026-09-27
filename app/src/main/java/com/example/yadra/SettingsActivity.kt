package com.example.yadra

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "yadra_settings"
        private const val KEY_LOGIN_ANIMATION =
            "login_animation_enabled"
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_settings
        )

        // --------------------------------------------------------
        // تنظیمات ذخیره‌سازی
        // --------------------------------------------------------

        val preferences =
            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )

        // --------------------------------------------------------
        // منوی زبان
        // --------------------------------------------------------

        val languageSpinner =
            findViewById<Spinner>(
                R.id.languageSpinner
            )

        val languages = arrayOf(
            "فارسی",
            "English",
            "العربية"
        )

        val languageAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                languages
            )

        languageAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        languageSpinner.adapter =
            languageAdapter

        // --------------------------------------------------------
        // منوی تم
        // --------------------------------------------------------

        val themeSpinner =
            findViewById<Spinner>(
                R.id.themeSpinner
            )

        val themes = arrayOf(
            "سیستم",
            "روشن",
            "تاریک"
        )

        val themeAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                themes
            )

        themeAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        themeSpinner.adapter =
            themeAdapter

        // --------------------------------------------------------
        // منوی فونت
        // --------------------------------------------------------

        val fontSpinner =
            findViewById<Spinner>(
                R.id.fontSpinner
            )

        val fonts = arrayOf(
            "Vazirmatn",
            "Noto Sans Arabic",
            "Noto Sans",
            "Roboto",
            "Monospace"
        )

        val fontAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                fonts
            )

        fontAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        fontSpinner.adapter =
            fontAdapter

        // --------------------------------------------------------
        // اندازه فونت
        // --------------------------------------------------------

        val fontSizeSpinner =
            findViewById<Spinner>(
                R.id.fontSizeSpinner
            )

        val fontSizes = arrayOf(
            "کوچک",
            "متوسط",
            "بزرگ",
            "خیلی بزرگ"
        )

        val fontSizeAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                fontSizes
            )

        fontSizeAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        fontSizeSpinner.adapter =
            fontSizeAdapter

        // --------------------------------------------------------
        // حالت صفحه اصلی
        // --------------------------------------------------------

        val dashboardModeSpinner =
            findViewById<Spinner>(
                R.id.dashboardModeSpinner
            )

        val dashboardModes = arrayOf(
            "استاندارد",
            "حرفه‌ای"
        )

        val dashboardModeAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                dashboardModes
            )

        dashboardModeAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        dashboardModeSpinner.adapter =
            dashboardModeAdapter

        // --------------------------------------------------------
        // ذخیره حالت صفحه اصلی
        // --------------------------------------------------------

        val savedMode =
            preferences.getString(
                "dashboard_mode",
                "standard"
            )

        dashboardModeSpinner.setSelection(
            if (
                savedMode == "professional"
            ) {
                1
            } else {
                0
            }
        )

        dashboardModeSpinner.setOnItemSelectedListener(
            object :
                android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {

                    val mode =
                        if (
                            position == 1
                        ) {
                            "professional"
                        } else {
                            "standard"
                        }

                    preferences.edit()
                        .putString(
                            "dashboard_mode",
                            mode
                        )
                        .apply()
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {
                }
            }
        )

        // --------------------------------------------------------
        // انیمیشن ورود
        // --------------------------------------------------------

        val loginAnimationSwitch =
            findViewById<Switch>(
                R.id.loginAnimationSwitch
            )

        // مقدار ذخیره‌شده
        val loginAnimationEnabled =
            preferences.getBoolean(
                KEY_LOGIN_ANIMATION,
                true
            )

        // تنظیم وضعیت اولیه
        loginAnimationSwitch.isChecked =
            loginAnimationEnabled

        // تنظیم رنگ اولیه
        updateLoginAnimationSwitchColor(
            loginAnimationSwitch,
            loginAnimationEnabled
        )

        // تغییر وضعیت
        loginAnimationSwitch.setOnCheckedChangeListener {
                _,
                isChecked ->

            // ذخیره وضعیت
            preferences.edit()
                .putBoolean(
                    KEY_LOGIN_ANIMATION,
                    isChecked
                )
                .apply()

            // تغییر رنگ
            updateLoginAnimationSwitchColor(
                loginAnimationSwitch,
                isChecked
            )
        }
    }

    // ------------------------------------------------------------
    // رنگ وضعیت انیمیشن ورود
    // ------------------------------------------------------------

    private fun updateLoginAnimationSwitchColor(
        switch: Switch,
        enabled: Boolean
    ) {

        if (enabled) {

            // ----------------------------------------------------
            // فعال = سبز
            // ----------------------------------------------------

            switch.setTextColor(
                Color.rgb(
                    0,
                    230,
                    118
                )
            )

            switch.thumbTintList =
                ColorStateList.valueOf(
                    Color.rgb(
                        0,
                        230,
                        118
                    )
                )

            switch.trackTintList =
                ColorStateList.valueOf(
                    Color.rgb(
                        0,
                        100,
                        60
                    )
                )

        } else {

            // ----------------------------------------------------
            // غیرفعال = قرمز
            // ----------------------------------------------------

            switch.setTextColor(
                Color.rgb(
                    255,
                    82,
                    82
                )
            )

            switch.thumbTintList =
                ColorStateList.valueOf(
                    Color.rgb(
                        255,
                        82,
                        82
                    )
                )

            switch.trackTintList =
                ColorStateList.valueOf(
                    Color.rgb(
                        120,
                        30,
                        30
                    )
                )
        }
    }
}