package com.example.myduka

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myduka.databinding.ActivityForgotPinBinding
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.util.concurrent.TimeUnit

class ForgotPin : AppCompatActivity() {
    lateinit var forgotPinBinding: ActivityForgotPinBinding
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val database: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var verificationId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        forgotPinBinding = ActivityForgotPinBinding.inflate(layoutInflater)
        val view = forgotPinBinding.root
        setContentView(view)
        setPhoneNumber()


        forgotPinBinding.buttonSubmitOTP.setOnClickListener {
            val otp = forgotPinBinding.editTextOTP.text.toString().trim()
            if (otp.isEmpty() || otp.length < 6) {
                Toast.makeText(this, "Enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            } else {
                verificationId?.let { id ->
                    verifyOTP(id, otp)
                } ?: run {
                    Toast.makeText(this, "Verification ID not available", Toast.LENGTH_SHORT).show()
                }
            }

        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setPhoneNumber() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("ForgotPin", "Current UID: $uid")
        database.collection("users").document(uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                Log.d("ForgotPin", "Document data: ${document.data}")
                val phoneNumber = document.getString("phoneNumber")
                if (!phoneNumber.isNullOrEmpty()) {
                    Log.d("ForgotPin", "Retrieved phone number: $phoneNumber")
                    forgotPinBinding.OTPPhone.text = phoneNumber
                    sendOTP(phoneNumber)
                } else {
                    Toast.makeText(this, "No phone number found.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendOTP(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                    Toast.makeText(this@ForgotPin, "OTP sent", Toast.LENGTH_SHORT).show()
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                    Toast.makeText(
                        this@ForgotPin,
                        "Verification failed ${p0.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                    super.onCodeSent(p0, p1)
                    this@ForgotPin.verificationId = p0
                    Toast.makeText(this@ForgotPin, "OTP sent", Toast.LENGTH_SHORT).show()
                }

            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)


    }
    private fun verifyOTP(verificationId: String, otp: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        auth.signInWithCredential(credential).addOnSuccessListener {
            Toast.makeText(this, "OTP verified", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Password_SignUp::class.java))
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "OTP verification failed", Toast.LENGTH_SHORT).show()
        }
    }
}



