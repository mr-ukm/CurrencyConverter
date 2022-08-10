package com.example.currencyconverter.di.repository.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.currencyconverter.model.Rate

@Dao
interface RateDao {

    @Insert
    suspend fun insertAllRates(rates: List<Rate>)

    @Query("SELECT * FROM rate")
    suspend fun getAllRates(): List<Rate>
}