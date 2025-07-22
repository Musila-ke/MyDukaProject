package com.example.myduka

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myduka.databinding.WorkerrecyclerviewBinding
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Locale

class WorkerAdapter(
    private var workerSnapshots: List<DocumentSnapshot>
) : RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder>() {

    inner class WorkerViewHolder(val binding: WorkerrecyclerviewBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val binding = WorkerrecyclerviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WorkerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        val doc = workerSnapshots[position]

        // Basic info
        holder.binding.workerEmail.text    = doc.getString("workerEmail") ?: "No Email"
        holder.binding.workerName.text     = doc.getString("workerName")  ?: "No Name"
        holder.binding.branchPosition.text = doc.getString("branchName")  ?: "No Branch"

        // Pull both timestamps
        val inDate  = doc.getTimestamp("lastCheckInTime")?.toDate()
        val outDate = doc.getTimestamp("lastCheckOutTime")?.toDate()
        val fmt     = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault())

        // Decide which to show: if outDate is null or inDate is newer, show check‑in; else check‑out
        val displayText: String
        val isCheckedIn: Boolean
        when {
            inDate == null && outDate == null -> {
                displayText  = "No check‑in/out yet"
                isCheckedIn  = false
            }
            outDate == null -> {
                displayText  = "Last check‑in: ${fmt.format(inDate)}"
                isCheckedIn  = true
            }
            inDate == null -> {
                displayText  = "Last check‑out: ${fmt.format(outDate)}"
                isCheckedIn  = false
            }
            inDate.after(outDate) -> {
                displayText  = "Last check‑in: ${fmt.format(inDate)}"
                isCheckedIn  = true
            }
            else -> {
                displayText  = "Last check‑out: ${fmt.format(outDate)}"
                isCheckedIn  = false
            }
        }

        // Set text + color
        holder.binding.workerStatus.text = displayText
        val colorRes = if (isCheckedIn) R.color.green else R.color.red
        holder.binding.workerStatus.setTextColor(
            ContextCompat.getColor(holder.binding.root.context, colorRes)
        )
    }

    override fun getItemCount(): Int = workerSnapshots.size

    fun getSnapshot(position: Int): DocumentSnapshot = workerSnapshots[position]

    fun updateSnapshots(newSnapshots: List<DocumentSnapshot>) {
        workerSnapshots = newSnapshots
        notifyDataSetChanged()
    }
}
