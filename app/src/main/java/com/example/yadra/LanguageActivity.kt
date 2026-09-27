package com.example.yadra

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class LanguageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_language)

        val btnPersian = findViewById<Button>(R.id.btnPersian)
        val btnEnglish = findViewById<Button>(R.id.btnEnglish)
        val btnArabic = findViewById<Button>(R.id.btnArabic)

        btnPersian.setOnClickListener {
            changeLanguage("fa")
        }

        btnEnglish.setOnClickListener {
            changeLanguage("en")
        }

        btnArabic.setOnClickListener {
            changeLanguage("ar")
        }
    }

    private fun changeLanguage(languageCode: String) {

        val locale = LocaleListCompat.forLanguageTags(languageCode)

        AppCompatDelegate.setApplicationLocales(locale)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}