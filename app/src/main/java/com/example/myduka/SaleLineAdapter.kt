package com.example.mydukaworker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myduka.databinding.ItemCheckoutSummaryBinding
import java.util.Date
import java.util.Locale

/** A single line-item in a sale */
data class SaleLineItem(
    val productId: String,
    val name: String,
    val quantity: Int,
    val lineTotal: Double
)

/** A sale record */
data class SaleItem(
    val id: String,
    val timestamp: Date,
    val workerName: String,
    val items: List<SaleLineItem>,
    val total: Double
)

/** Adapter for sale line-items */
class SaleLineAdapter : ListAdapter<SaleLineItem, SaleLineAdapter.VH>(DIFF_LINE) {
    companion object {
        private val DIFF_LINE = object : DiffUtil.ItemCallback<SaleLineItem>() {
            override fun areItemsTheSame(old: SaleLineItem, new: SaleLineItem) = old.productId == new.productId
            override fun areContentsTheSame(old: SaleLineItem, new: SaleLineItem) = old == new
        }
    }

    inner class VH(private val binding: ItemCheckoutSummaryBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SaleLineItem) = with(binding) {
            tvSummaryName.text      = item.name
            tvSummaryQty.text  = item.quantity.toString()
            tvSummaryLineTotal.text = String.format(Locale.getDefault(), "%.2f", item.lineTotal)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemCheckoutSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
