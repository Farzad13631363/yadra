package com.example.yadra.license

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object YadraLicenseApi {
    private const val BASE_URL = "https://yadran.ir/api/v1/customer"
    data class AccountLicense(val id: String, val code: String, val software: String, val activated: Boolean,
        val expiresAt: String, val isPerpetual: Boolean?, val durationDays: Int?)
    class ApiException(val httpStatus: Int, val errorCode: String) : IOException("HTTP $httpStatus: $errorCode")

    private fun request(context: Context, token: String, path: String, body: JSONObject? = null): JSONObject {
        require(token.isNotBlank() && !token.contains('\n') && !token.contains('\r'))
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.allNetworks.firstOrNull {
            val caps = connectivity.getNetworkCapabilities(it)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: throw IOException("اتصال اینترنت معتبر در دسترس نیست؛ اینترنت همراه یا Wi-Fi اینترنت‌دار را فعال کنید")
        val client = OkHttpClient.Builder().socketFactory(network.socketFactory)
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> =
                    network.getAllByName(hostname).toList()
            })
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS).retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false).build()
        val builder = Request.Builder().url(BASE_URL + path)
            .header("Authorization", "Bearer $token").header("Accept", "application/json")
        if (body != null) builder.post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        client.newCall(builder.build()).execute().use { response ->
            val json = try { JSONObject(response.body?.string() ?: "") } catch (_: Exception) {
                throw ApiException(response.code, "INVALID_RESPONSE")
            }
            if (!response.isSuccessful) throw ApiException(response.code,
                json.optString("error").takeIf { it.matches(Regex("[A-Z_]{1,80}")) } ?: "REQUEST_FAILED")
            return json
        }
    }

    fun licenses(context: Context, token: String): List<AccountLicense> {
        val json = request(context, token, "/licenses")
        check(json.getBoolean("success"))
        val rows = json.getJSONObject("data").getJSONArray("licenses")
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            AccountLicense(row.getString("id"), if (row.isNull("code")) "" else row.getString("code"),
                row.getString("softwareCode"), row.getJSONObject("activation").getBoolean("activated"),
                if (row.isNull("expiresAt")) "" else row.getString("expiresAt"),
                if (row.opt("isPerpetual") is Boolean) row.getBoolean("isPerpetual") else null,
                if (row.isNull("durationDays")) null else row.getInt("durationDays"))
        }.filter { it.software == "diag" }
    }

    fun activateOrVerify(context: Context, token: String, license: AccountLicense): LicenseInfo {
        LicenseManager.invalidateVerification()
        require(license.software == "diag")
        val hash = DeviceIdentity.getAndroidIdHash(context)
        val key = DeviceKeyStore.getPublicKeyBase64()
        val fingerprint = DeviceKeyStore.getPublicKeyFingerprint()
        fun identity() = JSONObject().put("softwareCode", license.software).put("androidIdHash", hash)
            .put("publicKeyBase64", key).put("fingerprint", fingerprint)
        val verify = license.activated
        val route = "/license-activation" + if (verify) "/verify" else ""
        val beginBody = identity()
        if (verify) beginBody.put("licenseId", license.id) else {
            require(license.code.isNotBlank()) { "کد لایسنس در پاسخ سرور موجود نیست" }
            beginBody.put("licenseCode", license.code)
        }
        val begin = request(context, token, "$route/begin", beginBody)
        if (verify && begin.optString("status") != "needs_online") {
            return LicenseInfo(licenseId = license.id, deviceId = hash, status = status(begin.optString("status")))
        }
        val challengeId = begin.getString("challengeId")
        val challenge = begin.getString("challenge")
        require(challengeId.matches(Regex("[1-9][0-9]{0,18}")) && challenge.matches(Regex("[A-Za-z0-9_-]{43}")))
        val payload = listOf(if (verify) "YADRA-LICENSE-VERIFY" else "YADRA-LICENSE-ACTIVATE", "v1",
            challengeId, challenge, license.id, license.software, hash, fingerprint).joinToString("\n")
        val complete = request(context, token, "$route/complete", identity().put("challengeId", challengeId)
            .put("challenge", challenge).put("signature", DeviceKeyStore.signPayload(payload.toByteArray(Charsets.UTF_8))))
        val state = status(complete.getString("status"))
        if (!verify) check(complete.getBoolean("success") && complete.getString("licenseId") == license.id)
        val result = LicenseInfo(licenseId = license.id, deviceId = hash, status = state,
            softwareCode = license.software, isPerpetual = license.isPerpetual,
            durationDays = license.durationDays,
            // verify/complete has no expiry; begin expiry belongs to the challenge.
            expiresAt = if (verify) license.expiresAt
                else if (complete.isNull("expiresAt")) "" else complete.getString("expiresAt"))
        if (verify) LicenseManager.saveVerifiedLicense(context, result)
        else if (state == LicenseStatus.ACTIVE) {
            // Activation is followed by device-bound verification, never a second activation.
            return activateOrVerify(context, token, license.copy(activated = true,
                expiresAt = result.expiresAt))
        }
        return result
    }

    private fun status(value: String) = when (value) {
        "active" -> LicenseStatus.ACTIVE
        "expired" -> LicenseStatus.EXPIRED
        "device_invalid" -> LicenseStatus.DEVICE_MISMATCH
        else -> LicenseStatus.INACTIVE
    }
}
