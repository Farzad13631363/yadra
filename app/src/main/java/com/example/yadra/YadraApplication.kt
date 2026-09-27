package com.example.yadra

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager

class YadraApplication : Application(),
    Application.ActivityLifecycleCallbacks {

    private var startedActivities = 0

    private val handler =
        Handler(Looper.getMainLooper())

    private var currentActivity: Activity? = null

    private val screenTimeoutRunnable =
        Runnable {
            currentActivity?.window?.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

    private val disconnectRunnable =
        Runnable {
            if (startedActivities == 0) {
                disconnectAll()
            }
        }

    override fun onCreate() {
        super.onCreate()

        // قطع اتصال‌های باقی‌مانده از اجرای قبلی
        disconnectAll()

        // ثبت وضعیت Activityهای برنامه
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(
        activity: Activity
    ) {
        startedActivities++

        // اگر دوباره وارد برنامه شدیم،
        // قطع اتصال زمان‌بندی‌شده را لغو می‌کنیم
        handler.removeCallbacks(
            disconnectRunnable
        )
    }

    override fun onActivityResumed(
        activity: Activity
    ) {
        currentActivity = activity

        // صفحه حداقل 5 دقیقه خاموش نشود
        activity.window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // تایمر قبلی را حذف کن
        handler.removeCallbacks(
            screenTimeoutRunnable
        )

        // بعد از 5 دقیقه اجازه خاموش شدن صفحه داده شود
        handler.postDelayed(
            screenTimeoutRunnable,
            5 * 60 * 1000L
        )
    }

    override fun onActivityPaused(
        activity: Activity
    ) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    override fun onActivityStopped(
        activity: Activity
    ) {
        startedActivities--

        if (startedActivities < 0) {
            startedActivities = 0
        }

        // وقتی هیچ Activityای باقی نمانده،
        // اتصال‌ها را بعد از 1 ثانیه قطع کن
        if (startedActivities == 0) {
            handler.postDelayed(
                disconnectRunnable,
                1000L
            )
        }
    }

    private fun disconnectAll() {

        // قطع اتصال Wi-Fi ELM327
        try {
            YadraConnectionManager.disconnect()
        } catch (_: Exception) {
        }

        // قطع اتصال Bluetooth ELM327
        try {
            BluetoothConnectionManager.disconnectActive()
        } catch (_: Exception) {
        }

        // پاک کردن منبع اتصال فعال
        ConnectionSource.clear()
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) {
    }

    override fun onActivityDestroyed(
        activity: Activity
    ) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }
}