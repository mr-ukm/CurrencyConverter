package com.example.currencyconverter.di.repository

import com.example.currencyconverter.di.repository.retrofit.APIService
import javax.inject.Inject

class APIRepository @Inject constructor(private val apiService: APIService) {

    suspend fun getLatestRates(appId: String) = apiService.getLatestRates(appId = appId)
}