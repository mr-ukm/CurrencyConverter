package com.example.currencyconverter.ui

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.currencyconverter.R
import com.example.currencyconverter.constant.Constants
import com.example.currencyconverter.databinding.ActivityMainBinding
import com.example.currencyconverter.model.Response
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var currencyDropDownAdapter: ArrayAdapter<String>
    private val currencyList: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupCurrencyListAdapter()
        updateCurrencyRates(isCalledFromOnCreate = true)
    }

    private fun setupCurrencyListAdapter() {
        currencyDropDownAdapter = ArrayAdapter(this, R.layout.item_drop_down, currencyList)
        binding.currencyAutoCompleteTv.setAdapter(currencyDropDownAdapter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh_menu -> {
                updateCurrencyRates()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun updateCurrencyRates(isCalledFromOnCreate: Boolean = false) {
        lifecycleScope.launch {
            if (!canDataBeRefreshed()) {
                if (!isCalledFromOnCreate) { // Don't show toast if function called by app launch
                    Toast.makeText(
                        this@MainActivity,
                        "TimeDifference between last API call & next API call needs to be atleast 30 mins",
                        Toast.LENGTH_LONG
                    ).show()
                }
                updateCurrencyList()
                return@launch
            }
            mainViewModel.getLatestRates().collectLatest { response ->
                when (response) {
                    is Response.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is Response.Success -> {
                        mainViewModel.addRateListInDB(response.data.rates)
                        binding.progressBar.visibility = View.GONE
                        updateSharedPreferenceForTimestampAndBaseCurrencyAndStatus(
                            response.data.baseCurrency
                        )
                        updateCurrencyList()
                    }
                    is Response.Error -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun updateSharedPreferenceForTimestampAndBaseCurrencyAndStatus(
        baseCurrency: String
    ) {
        val sharedPreferences =
            this.getSharedPreferences(Constants.SHARED_PREFERENCE_FILE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong(Constants.API_SUCCESS_TIMESTAMP, System.currentTimeMillis())
            putString(Constants.BASE_CURRENCY, baseCurrency)
            apply()
        }
    }

    private fun canDataBeRefreshed(): Boolean {
        val sharedPreferences =
            this.getSharedPreferences(Constants.SHARED_PREFERENCE_FILE, Context.MODE_PRIVATE)
        val lastSyncTimestamp = sharedPreferences.getLong(Constants.API_SUCCESS_TIMESTAMP, 0)
        return (System.currentTimeMillis() - lastSyncTimestamp > Constants.API_SYNC_THRESHOLD)
    }

    private fun updateCurrencyList() {
        lifecycleScope.launch {
            val updatedCurrencyList = mainViewModel.getCurrencyListFromDB()
            currencyList.clear()
            currencyList.addAll(updatedCurrencyList)
            currencyDropDownAdapter =
                ArrayAdapter(this@MainActivity, R.layout.item_drop_down, currencyList)

            binding.currencyAutoCompleteTv.setAdapter(currencyDropDownAdapter)

            if (currencyList.size > 0) {
                binding.currencyAutoCompleteTv.setText(currencyList[0], false)
            }
        }
    }
}