package com.example.myduka

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.myduka.databinding.FragmentWorkerDialogFragmentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class worker_dialog_fragment : DialogFragment() {

    private var _binding: FragmentWorkerDialogFragmentBinding? = null
    private val binding get() = _binding!!

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var branchList: List<Map<String, Any>> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerDialogFragmentBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadBranchesIntoSpinner(binding.spinnerBranch)
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.AddWorkerDF.setOnClickListener { addWorker() }
    }

    private fun addWorker() {
        val workerEmail = binding.workerEmailET.text.toString().trim()
        val workerName  = binding.workerNameET.text.toString().trim()
        if (workerEmail.isEmpty() || workerName.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        val idx = binding.spinnerBranch.selectedItemPosition
        if (idx !in branchList.indices) {
            Toast.makeText(context, "Please select a branch", Toast.LENGTH_SHORT).show()
            return
        }

        val (branchId, branchName) = branchList[idx].let {
            it["branchId"] as String to it["branchName"] as String
        }
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        val workerId = db.collection("users").document(uid)
            .collection("workers").document().id

        val workerData = mapOf(
            "workerEmail" to workerEmail,
            "workerName"  to workerName,
            "status"      to "Checked Out",
            "workerId"    to workerId,
            "branchId"    to branchId,
            "branchName"  to branchName
        )

        fun writeNotification() {
            val notifRef = db.collection("users")
                .document(uid)
                .collection("notifications")
                .document()

            val notification = NotificationDC(
                id        = notifRef.id,
                title     = "New Worker Added",
                message   = "$workerName has been added to $branchName",
                timestamp = com.google.firebase.Timestamp.now(),

                type      = NotificationDC.TYPE_WORKERS
            )
            notifRef.set(notification)
        }

        if (!isInternetAvailable()) {
            // offline: Firestore will sync both writes later
            db.collection("users").document(uid)
                .collection("workers").document(workerId)
                .set(workerData)
            writeNotification()
            Toast.makeText(context, "Offline—worker & notification will sync when online.", Toast.LENGTH_LONG).show()
            dismiss()
            return
        }

        // online path
        binding.progressBarWorker.visibility = View.VISIBLE
        db.collection("users").document(uid)
            .collection("workers").document(workerId)
            .set(workerData)
            .addOnSuccessListener {
                writeNotification()
                binding.progressBarWorker.visibility = View.GONE
                Toast.makeText(context, "Worker added.", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener { e ->
                binding.progressBarWorker.visibility = View.GONE
                Toast.makeText(context, "Failed to add worker: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadBranchesIntoSpinner(spinner: Spinner) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("branches")
            .get()
            .addOnSuccessListener { snapshot ->
                branchList = snapshot.map { doc ->
                    mapOf("branchId" to doc.id, "branchName" to (doc.getString("name") ?: "Unnamed"))
                }
                val names = branchList.map { it["branchName"] as String }
                spinner.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    names
                ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Couldn't load branches: ${e.message}", Toast.LENGTH_LONG).show()
                dismiss()
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
