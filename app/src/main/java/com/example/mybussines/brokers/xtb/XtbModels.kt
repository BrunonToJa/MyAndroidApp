package com.example.mybussines.brokers.xtb

data class XtbSession(
    val isLoggedIn: Boolean,
    val lastUrl: String?,
    val cookies: String?
)

data class XtbCapturedRequest(
    val method: String,
    val url: String,
    val body: String?,
    val responsePreview: String?,
    val headers: Map<String, String> = emptyMap()
)

data class XtbOpenPosition(
    val id: String,
    val symbol: String,
    val side: String,
    val volume: Double,
    val openPrice: Double?,
    val profit: Double?
)

data class XtbTradeHistoryItem(
    val id: String,
    val symbol: String,
    val side: String,
    val volume: Double,
    val openPrice: Double?,
    val closePrice: Double?,
    val openTime: String?,
    val closeTime: String?,
    val profit: Double?
)

data class XtbAccountSummary(
    val balance: Double,
    val equity: Double,
    val margin: Double,
    val currency: String
)