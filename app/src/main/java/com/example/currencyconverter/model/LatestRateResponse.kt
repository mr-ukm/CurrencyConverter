package com.example.currencyconverter.model

import com.google.gson.annotations.SerializedName

data class LatestRateResponse(
    val timestamp: Long,
    @SerializedName("base")
    val baseCurrency: String,
    @SerializedName("rates")
    val rateList: List<Pair<String, Double>>
)
