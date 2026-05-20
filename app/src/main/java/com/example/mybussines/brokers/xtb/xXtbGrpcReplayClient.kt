package com.example.mybussines.brokers.xtb

import android.content.Context
import android.webkit.CookieManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class xXtbGrpcReplayClient(private val context: Context) {

    private val client = OkHttpClient()

    fun replayCapturedPost(
        request: XtbCapturedRequest,
        callback: (success: Boolean, response: String?) -> Unit
    ) {
        val bodyRaw = request.body ?: "AAAAAAA="

        val body = bodyRaw.toRequestBody(
            "application/grpc-web-text".toMediaType()
        )

        val cookies = CookieManager.getInstance()
            .getCookie("https://xstation5.xtb.com")
            ?: ""

        val builder = Request.Builder()
            .url(request.url)
            .post(body)

        request.headers.forEach { (key, value) ->
            val lower = key.lowercase()

            if (
                lower != "host" &&
                lower != "content-length" &&
                lower != "accept-encoding" &&
                lower != "connection"
            ) {
                builder.addHeader(key, value)
            }
        }

        builder
            .header("Content-Type", "application/grpc-web-text")
            .header("Accept", "application/grpc-web-text")
            .header("x-grpc-web", "1")
            .header("Cookie", cookies)
            .header("Origin", "https://xstation5.xtb.com")
            .header("Referer", "https://xstation5.xtb.com/")

        val okRequest = builder.build()

        client.newCall(okRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val text = response.body?.string()
                callback(response.isSuccessful, text)
            }
        })
    }
}