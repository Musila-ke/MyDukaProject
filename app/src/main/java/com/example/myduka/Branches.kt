package com.example.myduka

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myduka.databinding.ActivityBranchesBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase

/**
 * Activity displaying branches with swipe-to-delete.
 */
class Branches : AppCompatActivity(), Branches_dialog_fragment.BranchAddListener {

    private lateinit var binding: ActivityBranchesBinding
    private lateinit var adapter: BranchAdapter
    private var subscription: ListenerRegistration? = null

    private val db   = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBranchesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Insets handling
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // RecyclerView setup
        adapter = BranchAdapter(emptyList())
        binding.recyclerViewBranches.apply {
            layoutManager = LinearLayoutManager(this@Branches)
            adapter       = this@Branches.adapter
        }
        attachSwipeToDelete()

        // FAB opens add-dialog
        binding.floatingActionButton.setOnClickListener {
            Branches_dialog_fragment().apply {
                listener = this@Branches
                show(supportFragmentManager, "add_branch")
            }
        }

        subscribeToUserBranches()
    }

    private fun attachSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos      = viewHolder.adapterPosition
                val snapshot = adapter.getSnapshot(pos)
                snapshot.reference.delete()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this@Branches,
                            "Branch deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this@Branches,
                            "Delete failed: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                        adapter.notifyItemChanged(pos)
                    }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerViewBranches)
    }

    private fun subscribeToUserBranches() {
        val uid = auth.currentUser?.uid ?: return
        subscription = db.collection("users")
            .document(uid)
            .collection("branches")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(this, err.localizedMessage, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                adapter.updateSnapshots(snap?.documents ?: emptyList())
            }
    }

    override fun onBranchAdded() {
        // The snapshot listener updates automatically
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription?.remove()
    }
}