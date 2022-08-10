package com.example.currencyconverter.model

sealed class Response<T> {
    class Error<T>(val errorMessage: String) : Response<T>()
    class Success<T>(val data: T) : Response<T>()
    class Loading<T> : Response<T>()
}
