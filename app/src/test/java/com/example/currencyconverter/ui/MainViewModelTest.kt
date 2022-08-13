package com.example.currencyconverter.ui

import android.content.SharedPreferences
import com.example.currencyconverter.di.repository.APIRepository
import com.example.currencyconverter.di.repository.DaoRepository
import com.example.currencyconverter.model.Rate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(JUnit4::class)
class MainViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private lateinit var mainViewModel: MainViewModel

    @Mock
    lateinit var apiRepository: APIRepository

    @Mock
    lateinit var daoRepository: DaoRepository

    @Mock
    lateinit var sharedPreferences: SharedPreferences


    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        mainViewModel = MainViewModel(
            apiRepository = apiRepository,
            daoRepository = daoRepository,
            sharedPreferences = sharedPreferences,
            defaultDispatcher = testDispatcher,
            ioDispatcher = testDispatcher
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getRateMapFromRateList for list containing 2 values`() {
        runTest {
            val rateList = listOf(
                Rate(currencyName = "INR", currentValue = 50.0),
                Rate(currencyName = "JPY", currentValue = 10.0)
            )

            val rateMapFromViewModel = mainViewModel.getRateMapFromRateList(rateList = rateList)

            assertEquals(50.0, rateMapFromViewModel["INR"])
            assertEquals(10.0, rateMapFromViewModel["JPY"])
        }
    }
}