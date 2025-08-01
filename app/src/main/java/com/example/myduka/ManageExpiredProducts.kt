// ManageExpiredProductsActivity.kt
package com.example.myduka

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myduka.databinding.ActivityManageExpiredProductsBinding
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ManageExpiredProducts : AppCompatActivity() {

    private lateinit var binding: ActivityManageExpiredProductsBinding
    private lateinit var adapter: ExpiredProductsAdapter
    private val db = FirebaseFirestore.getInstance()
    private val uid by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    // to drive the Delete button
    private var currentExpiredList = listOf<ExpiredProduct>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageExpiredProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ExpiredProductsAdapter(emptyList())
        binding.recyclerViewExpiredProducts.apply {
            layoutManager = LinearLayoutManager(this@ManageExpiredProducts)
            adapter = this@ManageExpiredProducts.adapter
        }

        binding.buttonDeleteAllExpired.setOnClickListener {
            deleteAllExpiredUnits()
        }

        loadBranches()
    }

    private fun loadBranches() {
        db.collection("users")
            .document(uid)
            .collection("branches")
            .get()
            .addOnSuccessListener { snap ->
                snap.forEach { doc ->
                    val branchName = doc.getString("name") ?: "Unnamed"
                    val branchId = doc.id
                    val chip = Chip(this).apply {
                        text = branchName
                        tag = branchId
                        isCheckable = true
                    }
                    binding.chipGroupBranches.addView(chip)
                }

                binding.chipGroupBranches.setOnCheckedChangeListener { group, checkedId ->
                    if (checkedId != View.NO_ID) {
                        val chip = group.findViewById<Chip>(checkedId)
                        loadExpiredProducts(chip.tag as String)
                    } else {
                        adapter.updateList(emptyList())
                        binding.buttonDeleteAllExpired.visibility = View.GONE
                    }
                }
            }
    }

    private fun loadExpiredProducts(branchId: String) {
        val productsRef = db.collection("users")
            .document(uid)
            .collection("branches")
            .document(branchId)
            .collection("branchproducts")

        productsRef
            .whereEqualTo("hasExpiredUnits", true)
            .get()
            .addOnSuccessListener { prodSnap ->
                val total = prodSnap.size()
                if (total == 0) {
                    adapter.updateList(emptyList())
                    binding.buttonDeleteAllExpired.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val tempList = mutableListOf<ExpiredProduct>()
                var doneCount = 0
                val now = Timestamp.now()

                prodSnap.forEach { pDoc ->
                    val pid = pDoc.id
                    val name = pDoc.getString("name") ?: ""
                    val img = pDoc.getString("imageUrl")

                    // fetch *all* units, then pick expired by expiryDate
                    productsRef
                        .document(pid)
                        .collection("units")
                        .get()
                        .addOnSuccessListener { uSnap ->
                            val expiredUnits = uSnap.mapNotNull { uDoc ->
                                uDoc.getTimestamp("expiryDate")?.takeIf { it < now }?.let {
                                    ExpiredUnit(
                                        id = uDoc.id,
                                        barcode = uDoc.getString("barcode")
                                    )
                                }
                            }

                            if (expiredUnits.isNotEmpty()) {
                                tempList += ExpiredProduct(
                                    branchId = branchId,
                                    productId = pid,
                                    name = name,
                                    imageUrl = img,
                                    expiredUnits = expiredUnits
                                )
                            }
                        }
                        .addOnCompleteListener {
                            doneCount++
                            if (doneCount == total) {
                                currentExpiredList = tempList
                                adapter.updateList(tempList)
                                binding.buttonDeleteAllExpired.visibility =
                                    if (tempList.isEmpty()) View.GONE else View.VISIBLE
                            }
                        }
                }
            }
    }

    private fun deleteAllExpiredUnits() {
        val batch = db.batch()
        val postDeleteTasks = mutableListOf<Task<*>>()
        val now = com.google.firebase.Timestamp.now()

        currentExpiredList.forEach { product ->
            val unitsRef = db.collection("users")
                .document(uid)
                .collection("branches")
                .document(product.branchId)
                .collection("branchproducts")
                .document(product.productId)
                .collection("units")

            // Delete each expired unit document
            product.expiredUnits.forEach { unit ->
                batch.delete(unitsRef.document(unit.id))
            }

            // After deletion, check if any expired units still remain
            val cleanupTask = unitsRef
                .get()
                .continueWithTask { snapTask ->
                    val stillHasExpired = snapTask.result
                        ?.documents
                        ?.mapNotNull { it.getTimestamp("expiryDate") }
                        ?.any { it < now } ?: false

                    if (!stillHasExpired) {
                        // No expired units left → clear flag
                        val productRef = db.collection("users")
                            .document(uid)
                            .collection("branches")
                            .document(product.branchId)
                            .collection("branchproducts")
                            .document(product.productId)

                        productRef.update("hasExpiredUnits", false)
                    } else {
                        Tasks.forResult(null)
                    }
                }

            postDeleteTasks += cleanupTask
        }

        // Commit all deletes
        batch.commit()
            .addOnSuccessListener {
                // Then wait for cleanup tasks
                Tasks.whenAllSuccess<Any>(postDeleteTasks)
                    .addOnSuccessListener {
                        adapter.updateList(emptyList())
                        binding.buttonDeleteAllExpired.visibility = View.GONE
                        Snackbar.make(
                            binding.root,
                            "Ensure you've removed these items from the shelves!",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(
                            binding.root,
                            "Cleanup update failed: ${e.message}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Snackbar.make(
                    binding.root,
                    "Failed to delete expired units: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }
}
