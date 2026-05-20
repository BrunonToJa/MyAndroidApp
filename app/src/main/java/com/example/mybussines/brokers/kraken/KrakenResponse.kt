package com.example.mybussines.brokers.kraken

data class KrakenResponse<T>(
    val error: List<String>,
    val result: T?
)
