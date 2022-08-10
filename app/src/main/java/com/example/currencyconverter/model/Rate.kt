package com.example.currencyconverter.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rate")
data class Rate(

    @PrimaryKey
    @ColumnInfo(name = "name")
    val currencyName: String,

    @ColumnInfo(name = "value")
    val currentValue: Double
)
