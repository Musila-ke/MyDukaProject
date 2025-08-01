package com.example.myduka

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myduka.databinding.ActivityAddProductUnitsBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


// Activity to list products and allow adding units or discount
class AddProductUnits : AppCompatActivity() {
    private lateinit var binding: ActivityAddProductUnitsBinding
    private lateinit var adapter: ProductAdapter
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private var selectedBranchId: String? = null
    private var productListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductUnitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Adapter requires branchId, set up after branches
        binding.rvProducts.layoutManager = LinearLayoutManager(this)

        setupSearch()
        loadBranches()

        binding.warning.setOnClickListener {
            startActivity(Intent(Intent(this,ManageExpiredProducts::class.java)))
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true.also {
                adapter.filter(query.orEmpty())
            }
            override fun onQueryTextChange(newText: String?) = true.also {
                adapter.filter(newText.orEmpty())
            }
        })
    }

    private fun loadBranches() {
        db.collection("users").document(uid)
            .collection("branches")
            .get()
            .addOnSuccessListener { result ->
                binding.branchChipGroup.removeAllViews()
                for (doc in result) {
                    val branchName = doc.getString("name") ?: continue
                    val branchId = doc.id
                    val chip = Chip(this).apply {
                        text = branchName
                        isCheckable = true
                        tag = branchId
                        setOnClickListener {
                            selectedBranchId = branchId
                            binding.searchView.setQuery("", false)
                            setupAdapterAndLoad(branchId)
                        }
                    }
                    binding.branchChipGroup.addView(chip)
                }
                // Select first branch
                binding.branchChipGroup.getChildAt(0)?.let { c ->
                    (c as Chip).apply {
                        isChecked = true
                        selectedBranchId = tag as String
                        setupAdapterAndLoad(selectedBranchId!!)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load branches", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupAdapterAndLoad(branchId: String) {
        productListener?.remove()
        adapter = ProductAdapter(branchId) { productId ->
            Intent(this, AddUnits::class.java).apply {
                putExtra("branchId", branchId)
                putExtra("productId", productId)
                startActivity(this)
            }
        }
        binding.rvProducts.adapter = adapter
        loadProductsForBranch(branchId)
    }

    private fun loadProductsForBranch(branchId: String) {
        productListener = db.collection("users").document(uid)
            .collection("branches").document(branchId)
            .collection("branchproducts")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("ProductLoad", "Error loading products", err)
                    Toast.makeText(this, "Could not load products", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }.orEmpty()
                val hasExpired = list.any { it.hasExpiredUnits }
                binding.warning.visibility = if (hasExpired) View.VISIBLE else View.GONE
                adapter.updateData(list)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        productListener?.remove()
        if (::adapter.isInitialized) adapter.detach()
    }
}