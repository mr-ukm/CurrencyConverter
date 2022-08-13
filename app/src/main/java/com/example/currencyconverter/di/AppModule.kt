package com.example.currencyconverter.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.currencyconverter.constant.Constants
import com.example.currencyconverter.di.repository.APIRepository
import com.example.currencyconverter.di.repository.DaoRepository
import com.example.currencyconverter.di.repository.dao.AppDatabase
import com.example.currencyconverter.di.repository.dao.RateDao
import com.example.currencyconverter.di.repository.retrofit.APIService
import com.example.currencyconverter.di.repository.retrofit.CustomGsonConverterFactory
import com.example.currencyconverter.di.repository.retrofit.LatestRateDeserializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @Singleton
    fun provideRoomDatabase(@ApplicationContext applicationContext: Context) =
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database").build()

    @Provides
    @Singleton
    fun provideRateDAO(appDatabase: AppDatabase) = appDatabase.rateDao()

    @Provides
    @Singleton
    fun provideDaoRepository(rateDao: RateDao) = DaoRepository(rateDao = rateDao)

    @Provides
    @Singleton
    fun provideSharedPreference(@ApplicationContext applicationContext: Context): SharedPreferences =
        applicationContext.getSharedPreferences(
            Constants.SHARED_PREFERENCE_FILE,
            Context.MODE_PRIVATE
        )
}
