
package com.example.yadra.license

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

object DeviceKeyStore {

    private const val KEY_ALIAS =
        "com.example.yadra.license.device.ec.v1"

    private const val ANDROID_KEYSTORE =
        "AndroidKeyStore"

    @Synchronized
    fun getOrCreateKeyPair(): KeyPair {

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {

            val privateKey =
                keyStore.getKey(KEY_ALIAS, null) as? PrivateKey

            val publicKey =
                keyStore.getCertificate(KEY_ALIAS)?.publicKey

            if (privateKey != null && publicKey != null) {
                return KeyPair(publicKey, privateKey)
            }

            throw IllegalStateException(
                "Existing device key is unavailable"
            )
        }

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )

        val specification = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN
        )
            .setAlgorithmParameterSpec(
                ECGenParameterSpec("secp256r1")
            )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .build()

        generator.initialize(specification)

        return generator.generateKeyPair()
    }

    fun getPublicKeyBase64(): String {

        return Base64.encodeToString(
            getOrCreateKeyPair().public.encoded,
            Base64.NO_WRAP
        )
    }

    fun getPublicKeyFingerprint(): String {

        val digest = java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(getOrCreateKeyPair().public.encoded)

        return digest.joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }

    fun signPayload(payload: ByteArray): String {

        val privateKey = getOrCreateKeyPair().private

        val signer = Signature.getInstance("SHA256withECDSA")

        signer.initSign(privateKey)
        signer.update(payload)

        return Base64.encodeToString(
            signer.sign(),
            Base64.NO_WRAP
        )
    }

    @Synchronized
    fun deleteKey() {

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }
}
