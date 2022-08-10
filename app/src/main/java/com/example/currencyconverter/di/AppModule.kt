package com.example.currencyconverter.di

import com.example.currencyconverter.di.repository.APIRepository
import com.example.currencyconverter.di.repository.retrofit.APIService
import com.example.currencyconverter.di.repository.retrofit.CustomGsonConverterFactory
import com.example.currencyconverter.di.repository.retrofit.LatestRateDeserializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLatestRateDeserializer() = LatestRateDeserializer()

    @Provides
    @Singleton
    fun provideCustomGsonConverterFactory(latestRateDeserializer: LatestRateDeserializer): GsonConverterFactory {
        return CustomGsonConverterFactory.getGsonConverterFactory(latestRateDeserializer = latestRateDeserializer)
    }

    @Provides
    @Singleton
    fun provideRetrofit(gsonConverterFactory: GsonConverterFactory): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://openexchangerates.org/api/")
            .addConverterFactory(gsonConverterFactory)
            .build()
    }

    @Provides
    @Singleton
    fun provideAPIService(retrofit: Retrofit): APIService =
        retrofit.create(APIService::class.java)

    @Provides
    @Singleton
    fun provideAPIRepository(apiService: APIService) = APIRepository(apiService = apiService)
}
