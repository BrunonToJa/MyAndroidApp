package com.example.mybussines.brokers.xtb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class xXtbApiClient(
    private val sessionStore: XtbSessionStore
) {
    private val client = OkHttpClient()

    private val baseUrl = "https://xstation5api.xtb.com"
    private val userAgent = "Mozilla/5.0"

    suspend fun getInstrumentGroups(): String {
        return getJson("/v1/api/instrument-groups")
    }

    suspend fun getAlerts(): String {
        return getJson("/v1/api/cms/alert/pl")
    }

    private suspend fun getJson(path: String): String = withContext(Dispatchers.IO) {
        val session = sessionStore.getSession()
            ?: error("Brak aktywnej sesji XTB")

        val url = baseUrl.trimEnd('/') + path

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json, text/plain, */*")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Cookie", session.cookies ?: "")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}: ${response.message}")
            }

            response.body?.string() ?: ""
        }
    }

    fun parseOpenPositions(json: String): List<XtbOpenPosition> {
        val result = mutableListOf<XtbOpenPosition>()
        val root = JSONObject(json)

        val arr = root.optJSONArray("data")
            ?: root.optJSONArray("items")
            ?: root.optJSONArray("positions")
            ?: return result

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue

            result.add(
                XtbOpenPosition(
                    id = obj.optString("id", obj.optString("order", i.toString())),
                    symbol = obj.optString("symbol"),
                    side = obj.optString("side").ifBlank {
                        when (obj.optInt("cmd", -1)) {
                            0 -> "BUY"
                            1 -> "SELL"
                            else -> ""
                        }
                    },
                    volume = obj.optDouble("volume", 0.0),
                    openPrice = obj.optNullableDouble("open_price")
                        ?: obj.optNullableDouble("openPrice"),
                    profit = obj.optNullableDouble("profit")
                )
            )
        }

        return result
    }

    fun parseTradeHistory(json: String): List<XtbTradeHistoryItem> {
        val result = mutableListOf<XtbTradeHistoryItem>()
        val root = JSONObject(json)

        val arr = root.optJSONArray("data")
            ?: root.optJSONArray("items")
            ?: root.optJSONArray("history")
            ?: root.optJSONArray("positions")
            ?: return result

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue

            result.add(
                XtbTradeHistoryItem(
                    id = obj.optString("id", obj.optString("order", i.toString())),
                    symbol = obj.optString("symbol"),
                    side = obj.optString("side").ifBlank {
                        when (obj.optInt("cmd", -1)) {
                            0 -> "BUY"
                            1 -> "SELL"
                            else -> ""
                        }
                    },
                    volume = obj.optDouble("volume", 0.0),
                    openPrice = obj.optNullableDouble("open_price")
                        ?: obj.optNullableDouble("openPrice"),
                    closePrice = obj.optNullableDouble("close_price")
                        ?: obj.optNullableDouble("closePrice"),
                    openTime = obj.optString("open_time").ifBlank {
                        obj.optString("openTime").ifBlank { null }
                    },
                    closeTime = obj.optString("close_time").ifBlank {
                        obj.optString("closeTime").ifBlank { null }
                    },
                    profit = obj.optNullableDouble("profit")
                )
            )
        }

        return result
    }

    private fun JSONObject.optNullableDouble(name: String): Double? {
        return if (has(name) && !isNull(name)) optDouble(name) else null
    }
}