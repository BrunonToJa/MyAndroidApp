package com.example.mybussines.brokers.kraken

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object KrakenAuth {
    fun signRequest(apiPath: String, nonce: String, postData: String, apiSecret: String): String {
        try {
            // 1. SHA-256 z (nonce + postData)
            val message = nonce + postData
            val md = MessageDigest.getInstance("SHA-256")
            val sha256Hash = md.digest(message.toByteArray(Charsets.UTF_8))

            // 2. HMAC-SHA512 z (URI path + sha256Hash) używając zdekodowanego sekretu
            val secretKeyBytes = Base64.decode(apiSecret, Base64.DEFAULT)
            val hmacKey = SecretKeySpec(secretKeyBytes, "HmacSHA512")
            val mac = Mac.getInstance("HmacSHA512")
            mac.init(hmacKey)

            val pathBytes = apiPath.toByteArray(Charsets.UTF_8)
            val hmacData = ByteArray(pathBytes.size + sha256Hash.size)
            System.arraycopy(pathBytes, 0, hmacData, 0, pathBytes.size)
            System.arraycopy(sha256Hash, 0, hmacData, pathBytes.size, sha256Hash.size)

            val hmacHash = mac.doFinal(hmacData)

            // 3. Zwracamy wynik zakodowany w Base64
            return Base64.encodeToString(hmacHash, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}