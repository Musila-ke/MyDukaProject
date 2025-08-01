package com.example.myduka

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProductAdapter(
    private val branchId: String,
    private val onAddUnitClicked: (productId: String) -> Unit
) : ListAdapter<Product, ProductAdapter.VH>(DIFF) {

    private val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val db = FirebaseFirestore.getInstance()
    private var fullList: List<Product> = emptyList()
    private val holders = mutableListOf<VH>()

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
            override fun areContentsTheSame(a: Product, b: Product) = a == b
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.product_units_recyclerview, parent, false)
        val vh = VH(view)
        holders.add(vh)
        return vh
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateData(newList: List<Product>) {
        fullList = newList
        submitList(fullList)
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) fullList else fullList.filter {
            it.name.lowercase().contains(q)
        }
        submitList(filtered)
    }

    fun detach() {
        holders.forEach { it.detach() }
        holders.clear()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val thumb: ShapeableImageView = view.findViewById(R.id.imageView10)
        private val nameTv: TextView = view.findViewById(R.id.namePoS)
        private val descTv: TextView = view.findViewById(R.id.descriptionPoS)
        private val priceTv: TextView = view.findViewById(R.id.pricePoS)
        private val quantityTv: TextView = view.findViewById(R.id.quantity)
        private val lowStockTv: TextView = view.findViewById(R.id.textViewLowStock)
        private val addUnitIv: ImageView = view.findViewById(R.id.addUnit)
        private val discountEt: EditText = view.findViewById(R.id.editTextDiscount)
        private val submitDiscIv: ImageView = view.findViewById(R.id.submitDiscount)
        private val discLabelTv: TextView = view.findViewById(R.id.textViewDiscountedPriceDisplay)
        private val discPriceTv: TextView = view.findViewById(R.id.textViewDiscountedPrice)
        private val textView30: TextView = view.findViewById(R.id.textView30)
        private val addUnit: ImageView = view.findViewById(R.id.addUnit)

        private lateinit var currentProductId: String
        private var listener: ListenerRegistration? = null

        fun bind(p: Product) {
            currentProductId = p.id
            nameTv.text = p.name
            descTv.text = p.description
            priceTv.text = String.format("%.2f", p.price)
            Glide.with(thumb).load(p.imageUrl).centerCrop().into(thumb)

            if (p.type == "Service") {
                quantityTv.visibility = View.GONE
                lowStockTv.visibility = View.GONE
                textView30.visibility = View.GONE
                addUnit.visibility = View.GONE

            } else {
                quantityTv.visibility = View.VISIBLE
                lowStockTv.visibility = View.VISIBLE
                textView30.visibility = View.VISIBLE
                addUnit.visibility = View.VISIBLE

                listener?.remove()
                listener = db.collection("users").document(uid)
                    .collection("branches").document(branchId)
                    .collection("branchproducts").document(currentProductId)
                    .collection("units")
                    .addSnapshotListener { snap, _ ->
                        val count = snap?.size() ?: 0
                        quantityTv.text = count.toString()
                        lowStockTv.visibility = if (count <= 5) View.VISIBLE else View.INVISIBLE
                    }
            }


            // Always show discount UI if saved
            discountEt.setText(p.discountPercent?.toInt()?.toString() ?: "")
            discLabelTv.visibility = View.VISIBLE
            discPriceTv.visibility = View.VISIBLE
            p.discountedPrice?.let {
                discPriceTv.text = String.format("%.2f", it)
            }

            // Submit discount action
            submitDiscIv.setOnClickListener {
                val d = discountEt.text.toString().toDoubleOrNull()
                if (d == null || d < 0 || d > 100) {
                    Toast.makeText(discountEt.context, "Enter 0–100% discount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val discounted = p.price * (100 - d) / 100
                discPriceTv.text = String.format("%.2f", discounted)
                

                // Persist discount
                db.collection("users").document(uid)
                    .collection("branches").document(branchId)
                    .collection("branchproducts").document(currentProductId)
                    .update(
                        mapOf(
                            "discountPercent" to d,
                            "discountedPrice" to discounted
                        ) as Map<String, Any>
                    )
            }

            // Add unit button
            addUnitIv.setOnClickListener { onAddUnitClicked(p.id) }
        }

        fun detach() { listener?.remove() }
    }
}