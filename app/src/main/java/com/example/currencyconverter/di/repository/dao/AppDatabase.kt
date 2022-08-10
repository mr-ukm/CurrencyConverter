package com.example.currencyconverter.di.repository.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.currencyconverter.model.Rate

@Database(entities = [Rate::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rateDao(): RateDao
}