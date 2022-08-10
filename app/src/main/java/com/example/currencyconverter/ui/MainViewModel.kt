package com.example.currencyconverter.ui

import androidx.lifecycle.ViewModel
import com.example.currencyconverter.constant.Constants
import com.example.currencyconverter.di.repository.APIRepository
import com.example.currencyconverter.di.repository.DaoRepository
import com.example.currencyconverter.model.LatestRateResponse
import com.example.currencyconverter.model.Rate
import com.example.currencyconverter.model.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val apiRepository: APIRepository,
    private val daoRepository: DaoRepository
) : ViewModel() {

    suspend fun getLatestRates() = flow<Response<LatestRateResponse>> {
        emit(Response.Loading())
        val latestRateResponse = apiRepository.getLatestRates(Constants.GOLUKEY)

        val responseBody = latestRateResponse.body()

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

    suspend fun addRateListInDB(rates: List<Rate>) {
        withContext(Dispatchers.IO) {
            daoRepository.insertAllRates(rates = rates)
        }
    }

    suspend fun getCurrencyListFromDB() =
        withContext(Dispatchers.IO) {
            return@withContext daoRepository.getCurrencyListFromDB()
        }
}