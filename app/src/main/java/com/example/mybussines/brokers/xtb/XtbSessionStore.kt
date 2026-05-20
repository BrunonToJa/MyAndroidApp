package com.example.mybussines.brokers.xtb

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class XtbSessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("xtb_session_store", Context.MODE_PRIVATE)

    fun saveSession(session: XtbSession) {
        prefs.edit()
            .putBoolean("isLoggedIn", session.isLoggedIn)
            .putString("lastUrl", session.lastUrl)
            .putString("cookies", session.cookies)
            .apply()
    }

    fun getSession(): XtbSession? {
        return XtbSession(
            isLoggedIn = prefs.getBoolean("isLoggedIn", false),
            lastUrl = prefs.getString("lastUrl", null),
            cookies = prefs.getString("cookies", null)
        )
    }

    fun addCapturedRequest(request: XtbCapturedRequest) {
        val current = getCapturedRequests().toMutableList()
        current.add(request)

        val arr = JSONArray()

        current.takeLast(300).forEach { req ->
            val headersObj = JSONObject()
            req.headers.forEach { (key, value) ->
                headersObj.put(key, value)
            }

            arr.put(
                JSONObject()
                    .put("method", req.method)
                    .put("url", req.url)
                    .put("body", req.body)
                    .put("responsePreview", req.responsePreview)
                    .put("headers", headersObj)
            )
        }

        prefs.edit()
            .putString("capturedRequests", arr.toString())
            .apply()
    }

    fun getCapturedRequests(): List<XtbCapturedRequest> {
        val raw = prefs.getString("capturedRequests", null) ?: return emptyList()
        val arr = JSONArray(raw)
        val result = mutableListOf<XtbCapturedRequest>()

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val headersObj = obj.optJSONObject("headers")
            val headers = mutableMapOf<String, String>()

            if (headersObj != null) {
                val keys = headersObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    headers[key] = headersObj.optString(key)
                }
            }

            result.add(
                XtbCapturedRequest(
                    method = obj.optString("method"),
                    url = obj.optString("url"),
                    body = obj.optString("body").takeIf { it != "null" },
                    responsePreview = obj.optString("responsePreview").takeIf { it != "null" },
                    headers = headers
                )
            )
        }

        return result
    }

    fun clearCapturedRequests() {
        prefs.edit().remove("capturedRequests").apply()
    }
}