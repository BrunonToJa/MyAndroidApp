package com.example.mybussines.brokers.kraken

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface KrakenApi {
    @FormUrlEncoded
    @POST("/0/private/Balance")
    suspend fun getBalance(
        @Header("API-Key") apiKey: String,
        @Header("API-Sign") apiSign: String,
        @Field("nonce") nonce: String
    ): Response<KrakenResponse<Map<String, String>>>
}