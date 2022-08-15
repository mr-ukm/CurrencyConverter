package com.example.currencyconverter.ui

import android.content.SharedPreferences
import app.cash.turbine.test
import com.example.currencyconverter.constant.Constants
import com.example.currencyconverter.di.repository.APIRepository
import com.example.currencyconverter.di.repository.DaoRepository
import com.example.currencyconverter.model.LatestRateResponse
import com.example.currencyconverter.model.Rate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import retrofit2.Response

@RunWith(JUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private lateinit var mainViewModel: MainViewModel

    @Mock
    lateinit var apiRepository: APIRepository

    @Mock
    lateinit var daoRepository: DaoRepository

    @Mock
    lateinit var sharedPreferences: SharedPreferences

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

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `inputCurrencyAmount stateflow test for 2 updates`() {
        runTest {
            mainViewModel.inputCurrencyAmount.test {
                assertEquals(0.0, awaitItem(), 0.0) // default value

                mainViewModel.updateInputCurrencyValue(10.5)
                assertEquals(10.5, awaitItem(), 0.0)

                mainViewModel.updateInputCurrencyValue(20.0)
                assertEquals(20.0, awaitItem(), 0.0)
            }
        }
    }

    @Test
    fun `selectedCurrency stateflow test for 2 updates`() {
        runTest {
            mainViewModel.selectedCurrency.test {
                assertEquals("", awaitItem()) // default value

                mainViewModel.updateSelectedCurrency("INR")
                assertEquals("INR", awaitItem())

                mainViewModel.updateSelectedCurrency("JPY")
                assertEquals("JPY", awaitItem())
            }
        }
    }

    @Test
    fun `updateCurrencyRatesFromAPI for 200 response code with non empty body`() {
        runTest {
            val rateList = listOf(
                Rate("INR", 50.0), Rate("JPY", 10.0),
                Rate("USD", 1.0)
            )
            val latestRateResponse = LatestRateResponse(baseCurrency = "USD", rates = rateList)
            val retrofitLatestRateResponse = Response.success(200, latestRateResponse)

            whenever(apiRepository.getLatestRates(anyString())).thenReturn(
                retrofitLatestRateResponse
            )

            mainViewModel.updateLatestRates.test {
                mainViewModel.updateCurrencyRatesFromAPI()
                val firstEmit = awaitItem()
                val secondEmit = awaitItem()

                assertTrue(firstEmit is com.example.currencyconverter.model.Response.Loading)
                assertTrue(secondEmit is com.example.currencyconverter.model.Response.Success)
                assertEquals(
                    latestRateResponse,
                    (secondEmit as com.example.currencyconverter.model.Response.Success).data
                )
            }
        }
    }

    @Test
    fun `updateCurrencyRatesFromAPI for 200 response code with empty body`() {
        runTest {
            val latestRateResponse: LatestRateResponse? = null
            val retrofitLatestRateResponse = Response.success(200, latestRateResponse)

            whenever(apiRepository.getLatestRates(anyString())).thenReturn(
                retrofitLatestRateResponse
            )

            mainViewModel.updateLatestRates.test {
                mainViewModel.updateCurrencyRatesFromAPI()
                val firstEmit = awaitItem()
                val secondEmit = awaitItem()

                assertTrue(firstEmit is com.example.currencyconverter.model.Response.Loading)
                assertTrue(secondEmit is com.example.currencyconverter.model.Response.Error)
                assertEquals(
                    "Response code 200, but response body is empty",
                    (secondEmit as com.example.currencyconverter.model.Response.Error).errorMessage
                )
            }
        }
    }

    @Test
    fun `updateCurrencyRatesFromAPI for non 200 response code with empty body`() {
        runTest {

            val retrofitLatestRateResponse = Response.error<LatestRateResponse>(
                404,
                ResponseBody.create(MediaType.parse("application/json"), "Error Occurred")
            )

            whenever(apiRepository.getLatestRates(anyString())).thenReturn(
                retrofitLatestRateResponse
            )

            mainViewModel.updateLatestRates.test {
                mainViewModel.updateCurrencyRatesFromAPI()
                val firstEmit = awaitItem()
                val secondEmit = awaitItem()

                assertTrue(firstEmit is com.example.currencyconverter.model.Response.Loading)
                assertTrue(secondEmit is com.example.currencyconverter.model.Response.Error)
                assertEquals(
                    "Response code 404 Response body is empty",
                    (secondEmit as com.example.currencyconverter.model.Response.Error).errorMessage
                )
            }
        }
    }

    @Test
    fun getCurrencyListFromDBTest() {
        runTest {
            val currencyList = listOf("INR", "JPY", "USD")
            whenever(daoRepository.getCurrencyListFromDB()).thenReturn(currencyList)
            assertEquals(currencyList, mainViewModel.getCurrencyListFromDB())
        }
    }

    @Test
    fun getRateListFromDBTest() {
        runTest {
            val rateList = listOf(Rate("INR", 50.0), Rate("JPY", 10.0))
            whenever(daoRepository.getRateListFromDB()).thenReturn(rateList)

            assertEquals(rateList, mainViewModel.getRateListFromDB())
        }
    }

    @Test
    fun `getRateMapFromRateList for list containing 2 values verifying exact mapping`() {
        runTest {
            val rateList = listOf(
                Rate(currencyName = "INR", currentValue = 50.0),
                Rate(currencyName = "JPY", currentValue = 10.0)
            )

            val rateMapFromViewModel = mainViewModel.getRateMapFromRateList(rateList = rateList)

            assertEquals(50.0, rateMapFromViewModel["INR"])
            assertEquals(10.0, rateMapFromViewModel["JPY"])
            assertNotEquals(50.1, rateMapFromViewModel["INR"])
            assertNotEquals(9.9, rateMapFromViewModel["JPY"])
        }
    }

    @Test
    fun `canDataBeRefreshed for lastSyncTimestamp more than 30 min and function should return true`() {
        runTest {
            val mockLastSyncTimestamp = System.currentTimeMillis() - Constants.API_SYNC_THRESHOLD
            whenever(sharedPreferences.getLong(Constants.API_SUCCESS_TIMESTAMP, 0)).thenReturn(
                mockLastSyncTimestamp
            )
            assertTrue(mainViewModel.canDataBeRefreshed())
        }
    }

    @Test
    fun `canDataBeRefreshed for lastSyncTimestamp less than 30 min and function should return false`() {
        runTest {
            val mockLastSyncTimestamp = System.currentTimeMillis()
            whenever(sharedPreferences.getLong(Constants.API_SUCCESS_TIMESTAMP, 0)).thenReturn(
                mockLastSyncTimestamp
            )
            assertFalse(mainViewModel.canDataBeRefreshed())
        }
    }

    @Test
    fun getBaseCurrencyValueFromSharedPreferenceTest() {
        runTest {
            whenever(sharedPreferences.getString(Constants.BASE_CURRENCY, "")).thenReturn("JPY")
            assertEquals("JPY", mainViewModel.getBaseCurrencyValueFromSharedPreference())
            assertNotEquals("JPP", mainViewModel.getBaseCurrencyValueFromSharedPreference())
        }
    }

    @Test
    fun `getDoubleValueFromString for dot, 0dot0, dot0`() {
        runTest {
            assertEquals(0.0, mainViewModel.getDoubleValueFromString(""), 0.0)
            assertEquals(0.0, mainViewModel.getDoubleValueFromString("  "), 0.0)
            assertEquals(0.0, mainViewModel.getDoubleValueFromString("0.0"), 0.0)
            assertEquals(0.0, mainViewModel.getDoubleValueFromString(".000000"), 0.0)
            assertEquals(0.0, mainViewModel.getDoubleValueFromString("."), 0.0)
            assertNotEquals(0.1, mainViewModel.getDoubleValueFromString(".00001"), 0.0)
            assertEquals(50.03, mainViewModel.getDoubleValueFromString("50.03"), 0.0)
            assertEquals(0.23, mainViewModel.getDoubleValueFromString(".23"), 0.0)
        }
    }
}