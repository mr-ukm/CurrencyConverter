package com.example.currencyconverter.di.repository.retrofit

import com.example.currencyconverter.model.LatestRateResponse
import com.google.gson.GsonBuilder
import retrofit2.converter.gson.GsonConverterFactory

class CustomGsonConverterFactory {
    companion object {
        fun getGsonConverterFactory(latestRateDeserializer: LatestRateDeserializer): GsonConverterFactory {
            val gson = GsonBuilder().registerTypeAdapter(
                LatestRateResponse::class.java,
                latestRateDeserializer
            ).create()

            return GsonConverterFactory.create(gson)
        }
    }
}