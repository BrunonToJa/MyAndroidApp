package com.example.mybussines.brokers.xtb

class xXtbRepository(
    private val sessionStore: XtbSessionStore
) {
    private val apiClient = xXtbApiClient(sessionStore)

    suspend fun getOpenPositions(): List<XtbOpenPosition> {
        // Na razie brak stabilnego endpointu REST dla otwartych pozycji.
        return emptyList()
    }

    suspend fun getTradeHistory(): List<XtbTradeHistoryItem> {
        // Na razie historia idzie przez gRPC replay, nie przez ten repository.
        return emptyList()
    }

    suspend fun getAccountSummary(): XtbAccountSummary {
        return XtbAccountSummary(
            balance = 0.0,
            equity = 0.0,
            margin = 0.0,
            currency = "PLN"
        )
    }
}