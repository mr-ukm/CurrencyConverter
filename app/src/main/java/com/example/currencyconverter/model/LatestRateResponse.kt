package com.example.currencyconverter.model

import com.google.gson.annotations.SerializedName

/* baseCurrency: to store the baseCurrency returned by API call response. All other values are with respect to base currency
   rates: to store all currency name & its value (with respect to base currency). Each rate will have currencyName & currencyValue
   errorMessage: to store the error message returned by API call in case there is any error
*/

data class LatestRateResponse(
    @SerializedName("base")
    var baseCurrency: String = "",
    @SerializedName("rates")
    var rates: List<Rate> = emptyList(),
    @SerializedName("message")
    var errorMessage: String = ""
)
