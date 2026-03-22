package com.example.district.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.RequestBody
import okio.Buffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HmacInterceptor(
    private val appSecret: String,
    private val clientId: String = "android-app"
) : Interceptor {

    private val secureRandom = SecureRandom()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val timestamp = System.currentTimeMillis() / 1000

        val nonceBytes = ByteArray(16)
        secureRandom.nextBytes(nonceBytes)
        val nonce = bytesToHex(nonceBytes)

        val bodyHash = calculateBodyHash(request.body)

        val message = "${request.method}\n${request.url.encodedPath}\n$timestamp\n$nonce\n$bodyHash"

        val signature = calculateHmac(message, appSecret)

        val signedRequest = request.newBuilder()
            .header("X-Client-ID", clientId)
            .header("X-Timestamp", timestamp.toString())
            .header("X-Nonce", nonce)
            .header("X-Signature", signature)
            .build()

        return chain.proceed(signedRequest)
    }

    private fun calculateBodyHash(body: RequestBody?): String {
        if (body == null || body.contentLength() == 0L) return ""

        val buffer = Buffer()
        body.writeTo(buffer)
        val bytes = buffer.readByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        return bytesToHex(hash)
    }

    private fun calculateHmac(data: String, key: String): String {
        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        return bytesToHex(hmac.doFinal(data.toByteArray()))
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}