package com.example.myduka

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.myduka.databinding.FragmentProfileDfBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class ProfileDF : DialogFragment() {
    lateinit var profileDfBinding: FragmentProfileDfBinding
    val database: FirebaseFirestore = FirebaseFirestore.getInstance()
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        profileDfBinding = FragmentProfileDfBinding.inflate(inflater, container, false)
        profileDfBinding.editCancel.setOnClickListener {
            dialog!!.dismiss()
        }
        profileDfBinding.buttonEdit.setOnClickListener {
            profileDfBinding.progressBarEditProfile.visibility = View.VISIBLE
            updateData()
        }
        dialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        return profileDfBinding.root



    }

    private fun updateData() {
        val updatedData = mutableMapOf<String, Any>()
        val userName = profileDfBinding.editUserName.text.toString().trim()
        val companyName = profileDfBinding.editCompanyName.text.toString().trim()


        if (userName.isNotEmpty()) {
            updatedData["userName"] = userName
        }
        if (companyName.isNotEmpty()) {
            updatedData["companyName"] = companyName
        }

        if (updatedData.isNotEmpty()) {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                database.collection("users").document(uid)
                    .update(updatedData)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Profile updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog!!.dismiss()
                    }.addOnFailureListener { exception ->
                        Toast.makeText(
                            requireContext(),
                            "Failed to update profile: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }else{
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            }
        }
        else{
            Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
        }


    }

}