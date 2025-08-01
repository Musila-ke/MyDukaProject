package com.example.myduka

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.myduka.databinding.FragmentBranchesDialogFragmentBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class Branches_dialog_fragment : DialogFragment() {

    private var _binding: FragmentBranchesDialogFragmentBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth

    interface BranchAddListener {
        fun onBranchAdded()
    }
    var listener: BranchAddListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBranchesDialogFragmentBinding
            .inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancelButton.setOnClickListener { dismiss() }

        binding.AddBranchDF.setOnClickListener {
            val name = binding.branchNameET.text.toString().trim()
            val location = binding.branchLocationET.text.toString().trim()
            val mpesaTill = binding.branchMpesaTillET.text.toString().trim().toInt()

            if (name.isEmpty() || location.isEmpty()) return@setOnClickListener

            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            val branchesRef = db.collection("users").document(uid).collection("branches")
            val newDoc = branchesRef.document()
            val branch = Branch(id = newDoc.id, name = name, location = location, mpesaTill = mpesaTill)

            // EARLY OFFLINE PATH
            if (!isInternetAvailable()) {
                // queue the write locally
                branchesRef.document(newDoc.id).set(branch)
                Toast.makeText(
                    context,
                    "No internet. Branch will sync when online.",
                    Toast.LENGTH_LONG
                ).show()
                dismiss()
                return@setOnClickListener
            }

            // ONLINE: show progress and do the real await()
            binding.progressBarAddBranch.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    newDoc.set(branch).await()
                    binding.progressBarAddBranch.visibility = View.GONE
                    listener?.onBranchAdded()
                    dismiss()
                } catch (e: Exception) {
                    binding.progressBarAddBranch.visibility = View.GONE
                    Toast.makeText(
                        context,
                        "Failed to add branch: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
