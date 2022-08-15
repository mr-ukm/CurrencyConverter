package com.example.currencyconverter.constant

import com.example.currencyconverter.BuildConfig

class Constants {
    companion object {
        val API_KEY = BuildConfig.API_KEY
        val SHARED_PREFERENCE_FILE = "app_shared_preference"
        val API_SUCCESS_TIMESTAMP = "api_success_timestamp"
        val BASE_CURRENCY = "base_currency"
        val API_SYNC_THRESHOLD: Long = 30 * 60 * 1000  // 30 min
    }
}