package com.example.mybussines.brokers.xtb

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class XtbCookieJar(
    private val cookieHeader: String
) : CookieJar {

    private val cookiesByHost = mutableMapOf<String, List<Cookie>>()

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookiesByHost[url.host] ?: parseCookieHeader(url, cookieHeader)
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookiesByHost[url.host] = cookies
    }

    private fun parseCookieHeader(url: HttpUrl, header: String): List<Cookie> {
        return header.split(";")
            .mapNotNull { raw ->
                val parts = raw.trim().split("=", limit = 2)
                if (parts.size != 2) return@mapNotNull null
                Cookie.Builder()
                    .name(parts[0].trim())
                    .value(parts[1].trim())
                    .domain(url.host)
                    .path("/")
                    .build()
            }
    }
}