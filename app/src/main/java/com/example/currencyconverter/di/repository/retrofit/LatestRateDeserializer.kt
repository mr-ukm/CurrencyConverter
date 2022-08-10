package com.example.currencyconverter.di.repository.retrofit

import com.example.currencyconverter.model.LatestRateResponse
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class LatestRateDeserializer : JsonDeserializer<LatestRateResponse> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LatestRateResponse {

        val latestRateResponse = LatestRateResponse()
        val jsonObject = json?.asJsonObject

        jsonObject?.let {
            if (it.has("timestamp")) {
                latestRateResponse.timestamp = it["timestamp"].asLong
            }
            if (it.has("base")) {
                latestRateResponse.baseCurrency = it["base"].asString
            }
            if (it.has("message")) {
                latestRateResponse.errorMessage = it["message"].asString
            }

            if (it.has("rates")) {
                val rateJsonObject = it["rates"].asJsonObject
                val iterator = rateJsonObject.keySet().iterator()
                val rateMapList: MutableList<Pair<String, Double>> = mutableListOf()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    val value = rateJsonObject.get(key).asDouble
                    rateMapList.add(Pair(key, value))
                }
                latestRateResponse.rates = rateMapList
            }
        }
        return latestRateResponse
    }
}