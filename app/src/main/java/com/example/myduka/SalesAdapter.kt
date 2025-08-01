package com.example.mydukaworker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myduka.databinding.ItemSalesHistoryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class SalesAdapter : ListAdapter<SaleItem, SalesAdapter.VH>(DIFF_SALE) {
    companion object {
        private val DIFF_SALE = object : DiffUtil.ItemCallback<SaleItem>() {
            override fun areItemsTheSame(old: SaleItem, new: SaleItem) = old.id == new.id
            override fun areContentsTheSame(old: SaleItem, new: SaleItem) = old == new
        }
    }

    inner class VH(private val binding: ItemSalesHistoryBinding)
        : RecyclerView.ViewHolder(binding.root) {
        private val lineAdapter = SaleLineAdapter()
        private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

        init {
            binding.rvSaleItems.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = lineAdapter
                isNestedScrollingEnabled = false
            }
        }

        fun bind(sale: SaleItem) = with(binding) {
            tvSaleTimestamp.text  = dateFormat.format(sale.timestamp)
            tvSaleWorker.text     = sale.workerName
            lineAdapter.submitList(sale.items)
            tvSaleGrandTotal.text = String.format(Locale.getDefault(), "Grand Total: %.2f", sale.total)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemSalesHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
