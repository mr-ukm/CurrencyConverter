package com.example.currencyconverter.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.currencyconverter.di.repository.APIRepository
import com.example.currencyconverter.model.LatestRateResponse
import com.example.currencyconverter.model.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val apiRepository: APIRepository) : ViewModel() {

    fun getLatestRates() = flow<Response<LatestRateResponse>> {
        emit(Response.Loading())
        val latestRateResponse = apiRepository.getLatestRates("00cb8d8a423b4f478731da5e163e9597")

        val responseBody = latestRateResponse.body()
        Log.d("customUjjwal", "ResponseBody: $responseBody")

        if (latestRateResponse.code() == 200) {
            responseBody?.let {
                emit(Response.Success(it))
            } ?: kotlin.run {
                emit(Response.Error("Response code 200, but response body is empty"))
            }
        } else {
            responseBody?.let {
                emit(Response.Error(it.errorMessage))
            } ?: kotlin.run {
                emit(Response.Error("Response code ${latestRateResponse.code()} Response body is empty"))
            }
        }
    }.catch {
        emit(Response.Error(it.message.toString()))
    }.flowOn(Dispatchers.IO)
}