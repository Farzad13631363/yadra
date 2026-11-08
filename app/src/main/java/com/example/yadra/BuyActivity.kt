package com.example.yadra

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yadra.license.*
import org.json.JSONTokener
import java.util.concurrent.Executors

class BuyActivity : AppCompatActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var login: WebView? = null
    private var token: String? = null
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy)
        render()
        findViewById<Button>(R.id.btnBuy).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://yadran.ir/license/diag")))
        }
        findViewById<Button>(R.id.btnActivate).setOnClickListener {
            if (!busy) token?.let { loadLicenses(it) } ?: signIn()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun signIn() {
        val web = WebView(this)
        login = web
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        web.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        val dialog = AlertDialog.Builder(this).setTitle("ورود به حساب مالک لایسنس")
            .setView(web).setNegativeButton("انصراف", null).create()
        fun trusted(url: String?): Boolean {
            val uri = url?.let(Uri::parse) ?: return false
            return uri.scheme == "https" && uri.host == "yadran.ir" && (uri.port == -1 || uri.port == 443)
        }
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val poll = object : Runnable {
            override fun run() {
                if (!dialog.isShowing) return
                if (trusted(web.url)) {
                    web.evaluateJavascript("(function(){return location.origin === 'https://yadran.ir' ? localStorage.getItem('yadra_customer_token') : null;})()") { result ->
                        if (trusted(web.url) && dialog.isShowing) {
                            val value = runCatching { JSONTokener(result).nextValue() as? String }.getOrNull()
                            if (!value.isNullOrBlank()) {
                                token = value
                                web.evaluateJavascript("localStorage.removeItem('yadra_customer_token')", null)
                                dialog.dismiss()
                                loadLicenses(value)
                            }
                        }
                    }
                }
                handler.postDelayed(this, 1000)
            }
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean =
                !trusted(request.url.toString())
        }
        dialog.setOnDismissListener { handler.removeCallbacks(poll); web.stopLoading(); web.destroy(); login = null }
        dialog.show()
        handler.postDelayed(poll, 1000)
        web.loadUrl("https://yadran.ir/login")
    }

    private fun loadLicenses(bearer: String) {
        busy = true
        worker.execute {
            try {
                val rows = YadraLicenseApi.licenses(applicationContext, bearer)
                runOnUiThread {
                    busy = false
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (rows.isEmpty()) message("لایسنس diag برای این حساب یافت نشد")
                    else AlertDialog.Builder(this).setTitle("انتخاب لایسنس؛ تأیید، دستگاه را متصل می‌کند")
                        .setItems(rows.map { "لایسنس ${it.id}" }.toTypedArray()) { _, index -> activate(bearer, rows[index]) }
                        .setNegativeButton("انصراف", null).show()
                }
            } catch (error: Exception) { failed(error) }
        }
    }

    private fun activate(bearer: String, row: YadraLicenseApi.AccountLicense) {
        busy = true
        worker.execute {
            try {
                // Clear any cached grant before attempting online proof.
                LicenseManager.invalidateVerification()
                val result = YadraLicenseApi.activateOrVerify(applicationContext, bearer, row)
                runOnUiThread { busy = false; if (!isDestroyed) { render(); message("وضعیت: ${result.status}") } }
            } catch (error: Exception) { failed(error) }
        }
    }

    private fun failed(error: Exception) {
        runOnUiThread {
            busy = false
            if (isDestroyed) return@runOnUiThread
            if (error is YadraLicenseApi.ApiException && error.httpStatus == 401) token = null
            render()
            message(if (error is YadraLicenseApi.ApiException) error.message ?: "خطای سرور"
                else "ارتباط یا اعتبارسنجی کامل نشد؛ اینترنت را بررسی و دوباره تلاش کنید")
        }
    }
    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    private fun render() {
        val license = LicenseManager.loadLicense(this)
        findViewById<TextView>(R.id.txtDeviceId).text = DeviceIdentity.getDeviceId(this)
        findViewById<TextView>(R.id.txtLicenseStatus).text = license.status.name
        findViewById<TextView>(R.id.txtLicenseId).text = "License ID: ${license.licenseId.ifBlank { "—" }}"
        findViewById<TextView>(R.id.txtExpiresAt).text = license.durationText()
    }
    override fun onDestroy() {
        token = null
        login?.destroy()
        login = null
        worker.shutdown()
        super.onDestroy()
    }
}
