package com.example.currencyconverter.model

import com.google.gson.annotations.SerializedName

data class LatestRateResponse(
    @SerializedName("base")
    var baseCurrency: String = "",
    @SerializedName("rates")
    var rates: List<Rate> = emptyList(),
    @SerializedName("message")
    var errorMessage: String = ""
)
