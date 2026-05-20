package com.example.mybussines.brokers.xtb

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import org.json.JSONObject

class XtbWebBridge(context: Context) {

    private val sessionStore = XtbSessionStore(context)

    @JavascriptInterface
    fun capture(
        method: String,
        url: String,
        body: String?,
        response: String?
    ) {
        save(method, url, body, response, null)
    }

    @JavascriptInterface
    fun captureWithHeaders(
        method: String,
        url: String,
        body: String?,
        response: String?,
        headersJson: String?
    ) {
        save(method, url, body, response, headersJson)
    }

    private fun save(
        method: String,
        url: String,
        body: String?,
        response: String?,
        headersJson: String?
    ) {
        val headers = mutableMapOf<String, String>()

        try {
            if (!headersJson.isNullOrBlank()) {
                val obj = JSONObject(headersJson)
                val keys = obj.keys()

                while (keys.hasNext()) {
                    val key = keys.next()
                    headers[key] = obj.optString(key)
                }
            }
        } catch (_: Exception) {}

        Log.d("XTB_BRIDGE", "Captured: $method $url")
        Log.d("XTB_BRIDGE_HEADERS", headers.toString())

        sessionStore.addCapturedRequest(
            XtbCapturedRequest(
                method = method,
                url = url,
                body = body,
                responsePreview = response,
                headers = headers
            )
        )
    }
}