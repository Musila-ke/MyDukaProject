package com.example.myduka

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myduka.databinding.WorkerrecyclerviewBinding
import com.google.firebase.firestore.DocumentSnapshot

class WorkerAdapter(
    private var workerSnapshots: List<DocumentSnapshot>
) : RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder>() {

    inner class WorkerViewHolder(val binding: WorkerrecyclerviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val binding = WorkerrecyclerviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WorkerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        val doc    = workerSnapshots[position]
        val email  = doc.getString("workerEmail") ?: "No Email"
        val name   = doc.getString("workerName") ?: "No Name"
        val status = doc.getString("status") ?: "Checked Out"
        val branch = doc.getString("branchName") ?: "No Branch"

        holder.binding.workerEmail.text    = email
        holder.binding.workerStatus.text   = status
        holder.binding.branchPosition.text = branch
        holder.binding.workerName.text     = name

        val ctx = holder.binding.root.context
        val colorRes = if (status.equals("Checked In", true)) R.color.green else R.color.red
        holder.binding.workerStatus.setTextColor(ContextCompat.getColor(ctx, colorRes))
    }

    override fun getItemCount(): Int = workerSnapshots.size

    fun getSnapshot(position: Int): DocumentSnapshot = workerSnapshots[position]

    fun updateSnapshots(newSnapshots: List<DocumentSnapshot>) {
        workerSnapshots = newSnapshots
        notifyDataSetChanged()
    }
}