package com.example.mybussines.brokers.xtb

data class XtbClosedPosition(
    val symbol: String,
    val title: String,
    val accountCurrency: String?,
    val instrumentCurrency: String?,
    val logoUrl: String?,
    val numericCandidates: List<Double> = emptyList(),
    val profit: Double? = null,
    val volume: Double? = null,
    val openPrice: Double? = null,
    val closePrice: Double? = null
)