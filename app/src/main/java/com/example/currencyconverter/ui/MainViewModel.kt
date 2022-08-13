package com.example.currencyconverter.ui

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.currencyconverter.constant.Constants
import com.example.currencyconverter.di.DefaultDispatcher
import com.example.currencyconverter.di.IODispatcher
import com.example.currencyconverter.di.repository.APIRepository
import com.example.currencyconverter.di.repository.DaoRepository
import com.example.currencyconverter.model.LatestRateResponse
import com.example.currencyconverter.model.Rate
import com.example.currencyconverter.model.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val apiRepository: APIRepository,
    private val daoRepository: DaoRepository,
    private val sharedPreferences: SharedPreferences,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _inputCurrencyAmount: MutableStateFlow<Double> = MutableStateFlow(0.0)
    val inputCurrencyAmount: StateFlow<Double> =
        _inputCurrencyAmount.asStateFlow()

    private val _selectedCurrency: MutableStateFlow<String> = MutableStateFlow("")
    val selectedCurrency = _selectedCurrency.asStateFlow()

    private val currencyList: MutableList<String> = mutableListOf()

    fun updateInputCurrencyValue(amount: Double) {
        _inputCurrencyAmount.value = amount
    }

    fun updateSelectedCurrency(selectedCurrency: String) {
        _selectedCurrency.value = selectedCurrency
    }

    suspend fun updateCurrencyList(currencyList: List<String>) = withContext(defaultDispatcher) {
        this@MainViewModel.currencyList.clear()
        this@MainViewModel.currencyList.addAll(currencyList)
    }

    fun getCurrencyList(): List<String> = this.currencyList

    suspend fun getLatestRates() = flow<Response<LatestRateResponse>> {
        if (!canDataBeRefreshed()) {
            return@flow
        }
        emit(Response.Loading())
        val latestRateResponse = apiRepository.getLatestRates(Constants.GOLUKEY)
        Log.d("customTag", "API Called")
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
    }.flowOn(ioDispatcher)

    suspend fun addRateListInDB(rates: List<Rate>) {
        withContext(ioDispatcher) {
            daoRepository.insertAllRates(rates = rates)
        }
    }

    suspend fun getCurrencyListFromDB() =
        withContext(ioDispatcher) {
            return@withContext daoRepository.getCurrencyListFromDB()
        }

    suspend fun getRateListFromDB() =
        withContext(ioDispatcher) {
            return@withContext daoRepository.getRateListFromDB()
        }

    suspend fun getRateMapFromRateList(rateList: List<Rate>): Map<String, Double> =
        withContext(defaultDispatcher) {
            val rateMap: MutableMap<String, Double> = mutableMapOf()
            rateList.forEach {
                rateMap[it.currencyName] = it.currentValue
            }
            return@withContext rateMap
        }

    suspend fun canDataBeRefreshed() = withContext(defaultDispatcher) {
        val lastSyncTimestamp = sharedPreferences.getLong(Constants.API_SUCCESS_TIMESTAMP, 0)
        (System.currentTimeMillis() - lastSyncTimestamp > Constants.API_SYNC_THRESHOLD)
    }

    suspend fun getBaseCurrencyValueFromSharedPreference() = withContext(defaultDispatcher) {
        sharedPreferences.getString(Constants.BASE_CURRENCY, "")!!
    }

    fun updateSharedPreferenceForTimestampAndBaseCurrencyAndStatus(
        baseCurrency: String
    ) {
        with(sharedPreferences.edit()) {
            putLong(Constants.API_SUCCESS_TIMESTAMP, System.currentTimeMillis())
            putString(Constants.BASE_CURRENCY, baseCurrency)
            apply()
        }
    }
}