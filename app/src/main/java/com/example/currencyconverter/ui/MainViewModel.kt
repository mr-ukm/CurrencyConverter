package com.example.currencyconverter.ui

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
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

    private val TAG = MainViewModel::class.simpleName

    private val _inputCurrencyAmount: MutableStateFlow<Double> = MutableStateFlow(0.0)
    val inputCurrencyAmount: StateFlow<Double> =
        _inputCurrencyAmount.asStateFlow()

    private val _selectedCurrency: MutableStateFlow<String> = MutableStateFlow("")
    val selectedCurrency = _selectedCurrency.asStateFlow()

    private val _updateLatestRates: MutableSharedFlow<Response<LatestRateResponse>> =
        MutableSharedFlow()

    val updateLatestRates: SharedFlow<Response<LatestRateResponse>> =
        _updateLatestRates.asSharedFlow()

    fun updateInputCurrencyValue(amount: Double) {
        _inputCurrencyAmount.value = amount
    }

    fun updateSelectedCurrency(selectedCurrency: String) {
        _selectedCurrency.value = selectedCurrency
    }

    suspend fun updateCurrencyRatesFromAPI() {
        viewModelScope.launch(ioDispatcher) {
            _updateLatestRates.emit(Response.Loading())
            val latestRateResponse = apiRepository.getLatestRates(Constants.GOLUKEY)
            Log.d(TAG, "API Called")
            val responseBody = latestRateResponse.body()

            if (latestRateResponse.code() == 200) {
                responseBody?.let {
                    _updateLatestRates.emit(Response.Success(it))
                } ?: kotlin.run {
                    _updateLatestRates.emit(Response.Error("Response code 200, but response body is empty"))
                }
            } else {
                responseBody?.let {
                    _updateLatestRates.emit(Response.Error(it.errorMessage))
                } ?: kotlin.run {
                    _updateLatestRates.emit(Response.Error("Response code ${latestRateResponse.code()} Response body is empty"))
                }
            }
        }
    }

    suspend fun addRateListInDB(rates: List<Rate>) {
        withContext(ioDispatcher) {
            daoRepository.insertAllRates(rates = rates)
        }
    }

    suspend fun getCurrencyListFromDB(): List<String> =
        withContext(ioDispatcher) {
            daoRepository.getCurrencyListFromDB()
        }

    suspend fun getRateListFromDB(): List<Rate> =
        withContext(ioDispatcher) {
            daoRepository.getRateListFromDB()
        }

    suspend fun getRateMapFromRateList(rateList: List<Rate>): Map<String, Double> =
        withContext(defaultDispatcher) {
            val rateMap: MutableMap<String, Double> = mutableMapOf()
            rateList.forEach {
                rateMap[it.currencyName] = it.currentValue
            }
            rateMap
        }

    suspend fun canDataBeRefreshed(): Boolean = withContext(defaultDispatcher) {
        val lastSyncTimestamp = sharedPreferences.getLong(Constants.API_SUCCESS_TIMESTAMP, 0)
        (System.currentTimeMillis() - lastSyncTimestamp > Constants.API_SYNC_THRESHOLD)
    }

    suspend fun getBaseCurrencyValueFromSharedPreference(): String =
        withContext(defaultDispatcher) {
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

    fun checkIfTwoStringsAreSameDoubleValues(oldInput: String, newInput: String): Boolean {
        val doubleValueFromOldInput =
            if (oldInput.trim().isNotEmpty()) oldInput.trim().toDouble() else 0.0
        val doubleValueFromNewInput =
            if (newInput.trim().isNotEmpty()) newInput.trim().toDouble() else 0.0
        return doubleValueFromOldInput == doubleValueFromNewInput
    }
}