package com.example.currencyconverter.di.repository.retrofit

import com.example.currencyconverter.model.LatestRateResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface APIService {
    @GET("latest.json")
    suspend fun getLatestRates(@Query("app_id") appId: String): LatestRateResponse
}