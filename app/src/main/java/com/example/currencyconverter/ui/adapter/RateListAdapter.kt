package com.example.currencyconverter.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.currencyconverter.databinding.ItemRateListBinding
import com.example.currencyconverter.model.Rate

class RateListAdapter :
    RecyclerView.Adapter<RateListAdapter.RateListViewHolder>() {

    private val rateList: MutableList<Rate> = mutableListOf()
    private var baseCurrency: String = ""
    private val rateMap: MutableMap<String, Double> = mutableMapOf()
    private var currentAmount: Double = 0.0
    private var selectedCurrency: String = ""

    inner class RateListViewHolder(private val binding: ItemRateListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindData(rate: Rate) {
            binding.currencyNameTv.text = rate.currencyName

            val convertedCurrencyValue = getConvertedAmount(rate.currentValue)
            binding.currencyValueTv.text = convertedCurrencyValue.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RateListViewHolder {
        val view = ItemRateListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RateListViewHolder(view)
    }

    override fun onBindViewHolder(holder: RateListViewHolder, position: Int) {
        if (position < 0 || position >= rateList.size) {
            return
        }
        val rate = rateList[position]
        holder.bindData(rate)
    }

    override fun getItemCount(): Int {
        return rateList.size
    }

    suspend fun updateAdapterData(
        rateList: List<Rate>,
        baseCurrency: String,
        rateMap: Map<String, Double>
    ) {
        this.rateList.clear()
        this.rateList.addAll(rateList)
        this.baseCurrency = baseCurrency
        this.rateMap.clear()
        this.rateMap.putAll(rateMap)
    }

    private fun getConvertedAmount(outputCurrencyRate: Double): Double {
        val selectedCurrencyRate: Double = rateMap.getOrDefault(selectedCurrency, 1.0)
        return (currentAmount / selectedCurrencyRate) * outputCurrencyRate
    }
}