package com.example.myduka

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myduka.databinding.ExpiredProductItemBinding

class ExpiredProductsAdapter(
    private var items: List<ExpiredProduct>
) : RecyclerView.Adapter<ExpiredProductsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ExpiredProductItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ExpiredProductItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ep = items[position]
        with(holder.binding) {
            // Product name + image
            textViewProductName.text = ep.name
            Glide.with(root.context)
                .load(ep.imageUrl)
                .placeholder(R.drawable.gallery)
                .into(imageViewProduct)

            // Do any expired unit have a barcode?
            val hasBarcodes = ep.expiredUnits.any { it.barcode != null }
            if (hasBarcodes) {
                textViewBarcodesLabel.visibility = View.VISIBLE
                layoutBarcodes.visibility = View.VISIBLE
                layoutBarcodes.removeAllViews()

                // Add each barcode as a bullet
                ep.expiredUnits.forEach { unit ->
                    unit.barcode?.let { code ->
                        val tv = TextView(root.context).apply {
                            text = "• $code"
                            textSize = 14f
                            setTextColor(Color.BLACK)
                            setPadding(8, 4, 8, 4)
                        }
                        layoutBarcodes.addView(tv)
                    }
                }
            } else {
                textViewBarcodesLabel.visibility = View.GONE
                layoutBarcodes.visibility = View.GONE
            }
        }
    }

    fun updateList(newList: List<ExpiredProduct>) {
        items = newList
        notifyDataSetChanged()
    }
}
