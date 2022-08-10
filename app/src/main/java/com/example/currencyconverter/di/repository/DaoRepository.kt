package com.example.currencyconverter.di.repository

import com.example.currencyconverter.di.repository.dao.RateDao
import com.example.currencyconverter.model.Rate
import javax.inject.Inject

class DaoRepository @Inject constructor(private val rateDao: RateDao) {

    suspend fun insertAllRates(rates: List<Rate>) = rateDao.insertAllRates(rates)

    suspend fun getCurrencyListFromDB() = rateDao.getCurrencyList()
}