package com.example.myduka

import android.os.Bundle
import android.util.TypedValue
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myduka.databinding.ActivitySalesHistoryBinding
import com.example.mydukaworker.SaleItem
import com.example.mydukaworker.SaleLineItem
import com.google.android.gms.tasks.Task
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.android.gms.tasks.Tasks
import java.util.*

class SalesHistory : AppCompatActivity() {

    private lateinit var binding: ActivitySalesHistoryBinding
    private val db = FirebaseFirestore.getInstance()
    private val adapter = SalesAdapter()

    // Track current filters
    private var selectedBranchId: String? = null
    private var currentFilter: Filter = Filter.ALL

    // Listener for branch‐chips
    private var branchListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySalesHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Setup RecyclerView
        binding.rvSalesHistory.apply {
            layoutManager = LinearLayoutManager(this@SalesHistory)
            adapter = this@SalesHistory.adapter
        }

        // Load dynamic branch chips with a snapshot listener
        loadBranchChips()

        // Setup time-filter chips
        binding.chipGroupFilter.apply {
            check(R.id.chipAll)
            setOnCheckedStateChangeListener { _, ids ->
                currentFilter = when (ids.firstOrNull()) {
                    R.id.chipToday -> Filter.TODAY
                    R.id.chipWeek  -> Filter.WEEK
                    R.id.chipMonth -> Filter.MONTH
                    else           -> Filter.ALL
                }
                loadSales(currentFilter)
            }
        }

        // Initial data load
        loadSales(currentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up the branch‐chips listener
        branchListener?.remove()
    }

    /**
     * Fetches branches and builds dynamic chips for selection,
     * updating live whenever branches change in Firestore.
     */
    private fun loadBranchChips() {
        val adminUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Remove any previous listener (if re-calling)
        branchListener?.remove()

        branchListener = db.collection("users")
            .document(adminUid)
            .collection("branches")
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener

                binding.chipgroupbranches.removeAllViews()

                // "All Branches" chip
                binding.chipgroupbranches.addView(
                    makeBranchChip("All", null).apply { isChecked = true }
                )

                // One chip per branch document
                for (doc in snap.documents) {
                    val id = doc.id
                    val name = doc.getString("name").orEmpty()
                    binding.chipgroupbranches.addView(
                        makeBranchChip(name, id)
                    )
                }

                // When user picks a chip, update selectedBranchId + reload sales
                binding.chipgroupbranches.setOnCheckedStateChangeListener { _, ids ->
                    val chip = binding.chipgroupbranches.findViewById<Chip>(ids.first())
                    selectedBranchId = chip.tag as? String
                    loadSales(currentFilter)
                }
            }
    }

    /**
     * Helper to create a styled Chip for branches.
     */
    private fun makeBranchChip(label: String, branchId: String?): Chip {
        return Chip(this).apply {
            text = label
            tag = branchId  // null -> All branches
            isCheckable = true
            chipCornerRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics
            )
            setChipBackgroundColorResource(
                com.google.android.material.R.color.mtrl_chip_background_color
            )
        }
    }

    /**
     * Loads sales from Firestore based on selected branch and time filter.
     * (Still using get() here—but you can swap in snapshot listeners similarly if
     * you want live updates for sales as well.)
     */
    private fun loadSales(filter: Filter) {
        val adminUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Compute start date for time filter
        val cal = Calendar.getInstance()
        val startDate: Date? = when (filter) {
            Filter.TODAY -> cal.apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time
            Filter.WEEK  -> cal.apply { add(Calendar.DAY_OF_YEAR, -7) }.time
            Filter.MONTH -> cal.apply { add(Calendar.MONTH, -1) }.time
            Filter.ALL   -> null
        }

        if (selectedBranchId != null) {
            // Single-branch query
            val bid = selectedBranchId!!
            var query = db.collection("users")
                .document(adminUid)
                .collection("branches")
                .document(bid)
                .collection("sales")
                .orderBy("timestamp", Query.Direction.DESCENDING)

            startDate?.let { query = query.whereGreaterThanOrEqualTo("timestamp", Timestamp(it)) }

            query.get().addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { mapSale(it) }
                adapter.submitList(list)
            }

        } else {
            // All branches: fetch then merge
            db.collection("users")
                .document(adminUid)
                .collection("branches")
                .get()
                .addOnSuccessListener { branchSnap ->
                    val tasks = mutableListOf<Task<QuerySnapshot>>()
                    val allSales = mutableListOf<SaleItem>()

                    for (br in branchSnap.documents) {
                        val bid = br.id
                        var q = db.collection("users")
                            .document(adminUid)
                            .collection("branches")
                            .document(bid)
                            .collection("sales")
                            .orderBy("timestamp", Query.Direction.DESCENDING)

                        startDate?.let { q = q.whereGreaterThanOrEqualTo("timestamp", Timestamp(it)) }

                        tasks += q.get().addOnSuccessListener { snap ->
                            snap.documents.mapNotNullTo(allSales) { mapSale(it) }
                        }
                    }

                    Tasks.whenAllComplete(tasks)
                        .addOnSuccessListener {
                            val sorted = allSales.sortedByDescending { it.timestamp }
                            adapter.submitList(sorted)
                        }
                }
        }
    }

    /**
     * Maps a Firestore document to a SaleItem.
     */
    private fun mapSale(doc: DocumentSnapshot): SaleItem? {
        val ts = doc.getTimestamp("timestamp") ?: return null
        val workerName = doc.getString("workerName").orEmpty()

        val items = (doc.get("items") as? List<*>)?.mapNotNull { raw ->
            (raw as? Map<*, *>)?.let { m ->
                SaleLineItem(
                    productId = (m["productId"] as? String).orEmpty(),
                    name      = (m["productName"] as? String).orEmpty(),
                    quantity  = (m["quantity"] as? Long)?.toInt() ?: 0,
                    lineTotal = m["lineTotal"] as? Double ?: 0.0
                )
            }
        } ?: emptyList()

        val total = doc.getDouble("grandTotal") ?: 0.0
        return SaleItem(doc.id, ts.toDate(), workerName, items, total)
    }

    private enum class Filter { TODAY, WEEK, MONTH, ALL }
}

