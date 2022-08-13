package com.example.currencyconverter.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.currencyconverter.R
import com.example.currencyconverter.constant.Constants
import com.example.currencyconverter.databinding.ActivityMainBinding
import com.example.currencyconverter.model.Response
import com.example.currencyconverter.ui.adapter.RateListAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var currencyDropDownAdapter: ArrayAdapter<String>

    private lateinit var rateListAdapter: RateListAdapter

    private val amountTextWatcher: TextWatcher = object : TextWatcher {

        private var timer: Timer = Timer()
        private val DELAY: Long = 1000 // For debounce logic
        private var oldInputCurrencyValue = ""

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            timer.cancel()
            timer = Timer()
            binding.progressBar.visibility = View.VISIBLE
            timer.schedule(object : TimerTask() {
                override fun run() {
                    if (oldInputCurrencyValue != s.toString().trim()) {
                        oldInputCurrencyValue = s.toString().trim()
                        mainViewModel.updateInputCurrencyValue(if (oldInputCurrencyValue.isNotEmpty()) oldInputCurrencyValue.toDouble() else 0.0)
                    }
                }
            }, DELAY)
        }
    }

    private val selectedCurrencyTextWatcher: TextWatcher = object : TextWatcher {
        private var oldSelectedCurrency = ""

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (oldSelectedCurrency != s.toString()) {
                binding.progressBar.visibility = View.VISIBLE
                mainViewModel.updateSelectedCurrency(s.toString())
                oldSelectedCurrency = s.toString()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupCurrencyListAdapter()
        setupRateListAdapter()
        updateCurrencyRates(isCalledFromOnCreate = true)
        observeSelectedCurrencyAndInputAmountChanges()
    }

    private fun observeSelectedCurrencyAndInputAmountChanges() {
        observeInputAmountChanges()
        observeSelectedCurrencyChanges()
    }

    override fun onResume() {
        super.onResume()
        binding.amountEt.addTextChangedListener(amountTextWatcher)
        binding.currencyAutoCompleteTv.addTextChangedListener(selectedCurrencyTextWatcher)
    }

    override fun onPause() {
        super.onPause()
        binding.amountEt.removeTextChangedListener(amountTextWatcher)
        binding.currencyAutoCompleteTv.removeTextChangedListener(selectedCurrencyTextWatcher)
    }

    private fun setupCurrencyListAdapter() {
        currencyDropDownAdapter =
            ArrayAdapter(this, R.layout.item_drop_down, mainViewModel.getCurrencyList())
        binding.currencyAutoCompleteTv.setAdapter(currencyDropDownAdapter)
    }

    private fun setupRateListAdapter() {
        rateListAdapter = RateListAdapter()
        binding.rateListRv.adapter = rateListAdapter
        binding.rateListRv.layoutManager = GridLayoutManager(this, 3)
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
                if (isCalledFromOnCreate) { // Just load the adapter data
                    updateCurrencyList()
                } else {
                    Snackbar.make(
                        binding.bottomViewSnackBar,
                        "TimeDifference between last API call & next API call needs to be atleast 30 mins",
                        Snackbar.LENGTH_INDEFINITE
                    ).apply {
                        setAction("DISMISS") {
                            dismiss()
                        }
                        show()
                    }
                    return@launch
                }
            }
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
            mainViewModel.updateCurrencyList(currencyList = updatedCurrencyList)
            currencyDropDownAdapter =
                ArrayAdapter(
                    this@MainActivity,
                    R.layout.item_drop_down,
                    mainViewModel.getCurrencyList()
                )

            binding.currencyAutoCompleteTv.setAdapter(currencyDropDownAdapter)

            if (mainViewModel.getCurrencyList().isNotEmpty()) {
                binding.currencyAutoCompleteTv.setText(mainViewModel.getCurrencyList()[0], false)
            }
            updateRateListAdapterAndRefresh()
        }
    }

    private fun updateRateListAdapterAndRefresh() {
        lifecycleScope.launch {
            val rateList = mainViewModel.getRateListFromDB()
            val baseCurrency = getBaseCurrencyValueFromSharedPreference()
            val rateMap = mainViewModel.getRateMapFromRateList(rateList = rateList)

            withContext(Dispatchers.Default) {
                rateListAdapter.updateAdapterData(
                    rateList = rateList,
                    baseCurrency = baseCurrency,
                    rateMap = rateMap,
                )
            }
            rateListAdapter.notifyDataSetChanged()
        }
    }

    private fun getBaseCurrencyValueFromSharedPreference(): String {
        val sharedPreferences =
            this.getSharedPreferences(Constants.SHARED_PREFERENCE_FILE, Context.MODE_PRIVATE)
        return sharedPreferences.getString(Constants.BASE_CURRENCY, "")!!
    }

    private fun observeInputAmountChanges() {
        lifecycleScope.launch(Dispatchers.Main) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.inputCurrencyAmount.collectLatest {
                    rateListAdapter.updateCurrentAmountValue(currencyAmount = it)
                    rateListAdapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun observeSelectedCurrencyChanges() {
        lifecycleScope.launch(Dispatchers.Main) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.selectedCurrency.collectLatest {
                    rateListAdapter.updateSelectedCurrency(selectedCurrency = it)
                    rateListAdapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
}