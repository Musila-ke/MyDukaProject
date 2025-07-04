package com.example.myduka

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myduka.databinding.ActivityNotificationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Notifications : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationAdapter
    private val notifications = mutableListOf<NotificationDC>()
    private val db = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) LayoutManager & Adapter
        binding.recyclerViewNotifications.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter = NotificationAdapter { notif ->
            // Mark as seen
            val updated = notif.copy(seen = true)
            val updatedList = notifications.map {
                if (it.id == notif.id) updated else it
            }
            notifications.clear()
            notifications.addAll(updatedList)
            updateListDisplay()

            uid?.let { user ->
                db.collection("users").document(user)
                    .collection("notifications")
                    .document(notif.id)
                    .update("seen", true)
            }

            if (notif.type == NotificationDC.TYPE_WORKERS) {
                startActivity(Intent(this, Profile::class.java))
            }
        }
        binding.recyclerViewNotifications.adapter = adapter

        // 2) Swipe-to-delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.adapterPosition
                val notif = adapter.currentList.getOrNull(pos) ?: return

                // Remove from local list
                val updatedList = notifications.filter { it.id != notif.id }
                notifications.clear()
                notifications.addAll(updatedList)
                updateListDisplay()

                // Remove from Firestore
                uid?.let { user ->
                    db.collection("users").document(user)
                        .collection("notifications")
                        .document(notif.id)
                        .delete()
                }
            }
        }).attachToRecyclerView(binding.recyclerViewNotifications)

        // 3) Chip filters
        binding.chipGroupFilters.setOnCheckedChangeListener { _, _ ->
            updateListDisplay()
        }
        binding.chipAll.isChecked = true

        // 4) Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // 5) Start listening
        fetchNotificationsFromFirestore()
    }

    private fun fetchNotificationsFromFirestore() {
        val user = uid ?: return
        db.collection("users").document(user)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snaps, err ->
                if (err != null || snaps == null) return@addSnapshotListener

                val newList = snaps.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(NotificationDC::class.java)
                    } catch (e: Exception) {
                        // Fallback: manually construct the object if timestamp is a Long
                        val data = doc.data ?: return@mapNotNull null

                        val rawTimestamp = data["timestamp"]
                        val timestamp: com.google.firebase.Timestamp? = when (rawTimestamp) {
                            is Long -> com.google.firebase.Timestamp(rawTimestamp / 1000, ((rawTimestamp % 1000) * 1_000_000).toInt())
                            is com.google.firebase.Timestamp -> rawTimestamp
                            else -> null
                        }

                        NotificationDC(
                            id = doc.id,
                            type = data["type"] as? String ?: "",
                            message = data["message"] as? String ?: "",
                            seen = data["seen"] as? Boolean ?: false,
                            timestamp = timestamp
                        )
                    }
                }

                notifications.clear()
                notifications.addAll(newList)
                updateListDisplay()
            }
    }

    private fun updateListDisplay() {
        val checked = binding.chipGroupFilters.checkedChipId
        val filtered = when (checked) {
            R.id.chipWorkers -> notifications.filter { it.type == NotificationDC.TYPE_WORKERS }
            R.id.chipExpiry  -> notifications.filter { it.type == NotificationDC.TYPE_EXPIRY }
            R.id.chipSales   -> notifications.filter { it.type == NotificationDC.TYPE_SALES }
            else             -> notifications
        }.sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }

        adapter.submitList(ArrayList(filtered)) // fresh reference avoids bugs
    }
}
