package com.example.myduka

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myduka.databinding.BranchrecyclerviewBinding
import com.google.firebase.firestore.DocumentSnapshot

/**
 * RecyclerView Adapter for Firestore branches with optional swipe-to-delete support.
 */
class BranchAdapter(
    private var branchSnapshots: List<DocumentSnapshot>
) : RecyclerView.Adapter<BranchAdapter.BranchViewHolder>() {

    inner class BranchViewHolder(val binding: BranchrecyclerviewBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BranchViewHolder {
        val binding = BranchrecyclerviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BranchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BranchViewHolder, position: Int) {
        val doc      = branchSnapshots[position]
        val name     = doc.getString("name") ?: "Unnamed Branch"
        val location = doc.getString("location") ?: "Unknown Location"
        val mpesaTill = doc.getLong("mpesaTill") ?: 0

        holder.binding.branchNameTV.text     = name
        holder.binding.branchLocationTV.text = location
        holder.binding.textViewMpesaTill.text = mpesaTill.toString()
    }

    override fun getItemCount(): Int = branchSnapshots.size

    /**
     * Returns the DocumentSnapshot for swipe-to-delete.
     */
    fun getSnapshot(position: Int): DocumentSnapshot = branchSnapshots[position]

    /**
     * Replace data and refresh.
     */
    fun updateSnapshots(newSnapshots: List<DocumentSnapshot>) {
        branchSnapshots = newSnapshots
        notifyDataSetChanged()
    }
}