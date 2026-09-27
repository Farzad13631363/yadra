package com.example.yadra

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView

    companion object {
        private const val PREFS_NAME = "yadra_settings"
        private const val KEY_LOGIN_ANIMATION = "login_animation_enabled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_splash)

        /*
         * وضعیت انیمیشن ورود
         *
         * مقدار پیش‌فرض true است تا رفتار فعلی برنامه
         * بدون تغییر باقی بماند.
         */
        val preferences =
            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )

        val animationEnabled =
            preferences.getBoolean(
                KEY_LOGIN_ANIMATION,
                true
            )

        /*
         * اگر انیمیشن خاموش باشد،
         * مستقیماً وارد صفحه اصلی می‌شویم.
         */
        if (!animationEnabled) {

            openMainActivity()

            return
        }

        /*
         * اگر انیمیشن روشن باشد،
         * ویدیوی فعلی اجرا می‌شود.
         */
        videoView =
            findViewById(
                R.id.splashVideo
            )

        val videoUri =
            Uri.parse(
                "android.resource://$packageName/${R.raw.login_animation}"
            )

        videoView.setVideoURI(
            videoUri
        )

        videoView.setOnPreparedListener { mediaPlayer ->

            mediaPlayer.isLooping =
                false

            mediaPlayer.setVolume(
                1f,
                1f
            )
        }

        videoView.setOnCompletionListener {

            openMainActivity()
        }

        videoView.start()
    }

    private fun openMainActivity() {

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )

        startActivity(intent)

        finish()
    }
}