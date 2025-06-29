package com.example.myduka

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myduka.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUp : AppCompatActivity() {
    private lateinit var signupBinding: ActivitySignUpBinding
    private val database: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val databaseReference = database.collection("users").document()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // for polling verification
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 5000L // 5 seconds
    private lateinit var verificationCheck: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        signupBinding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(signupBinding.root)

        signupBinding.buttonSignUp.setOnClickListener {
            signupBinding.progressBarSignUp.visibility = View.VISIBLE
            signupBinding.buttonSignUp.isEnabled = false
            saveUser()
        }

        signupBinding.textViewLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun saveUser() {
        val userName = signupBinding.editTextFullName.text.toString().trim()
        val email = signupBinding.editTextEmail.text.toString().trim()
        val password = signupBinding.editTextPassword.text.toString().trim()
        val companyName = signupBinding.editTextCompanyName.text.toString().trim()
        val phoneNumber = signupBinding.editTextPhonenumber.text.toString().trim()

        if (userName.isEmpty() || email.isEmpty() || password.isEmpty()
            || companyName.isEmpty() || phoneNumber.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            signupBinding.progressBarSignUp.visibility = View.GONE
            signupBinding.buttonSignUp.isEnabled = true
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val id = databaseReference.id
                val uid = authResult.user?.uid
                if (uid != null) {
                    val user = SignUpDTC(
                        userName,
                        companyName,
                        email,
                        id,
                        phoneNumber,
                        uid
                    )
                    database.collection("users").document(uid)
                        .set(user)
                        .addOnSuccessListener {
                            sendVerificationEmail()
                        }
                        .addOnFailureListener { e ->
                            signupBinding.progressBarSignUp.visibility = View.GONE
                            signupBinding.buttonSignUp.isEnabled = true
                            Toast.makeText(this, "Failed to save user: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    signupBinding.progressBarSignUp.visibility = View.GONE
                    signupBinding.buttonSignUp.isEnabled = true
                    Toast.makeText(this, "UID not available", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                signupBinding.progressBarSignUp.visibility = View.GONE
                signupBinding.buttonSignUp.isEnabled = true
                Toast.makeText(this, "Registration failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendVerificationEmail() {
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Verification email sent to ${user.email}. Please verify your email.",
                        Toast.LENGTH_LONG
                    ).show()
                    startVerificationPolling()
                } else {
                    signupBinding.progressBarSignUp.visibility = View.GONE
                    signupBinding.buttonSignUp.isEnabled = true
                    Toast.makeText(
                        this,
                        "Failed to send verification email: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun startVerificationPolling() {
        verificationCheck = object : Runnable {
            override fun run() {
                val user = auth.currentUser
                user?.reload()?.addOnCompleteListener { reloadTask ->
                    if (reloadTask.isSuccessful) {
                        if (user.isEmailVerified) {
                            // Email verified, proceed to Dashboard
                            handler.removeCallbacks(this)
                            Toast.makeText(
                                this@SignUp,
                                "Email verified successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(Intent(this@SignUp, Password_SignUp::class.java))
                        } else {
                            // Not verified yet, check again later
                            handler.postDelayed(this, checkInterval)
                        }
                    } else {
                        // Handle reload failure
                        signupBinding.progressBarSignUp.visibility = View.GONE
                        signupBinding.buttonSignUp.isEnabled = true
                        Toast.makeText(
                            this@SignUp,
                            "Failed to check verification status: ${reloadTask.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        handler.postDelayed(verificationCheck, checkInterval)
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::verificationCheck.isInitialized) {
            handler.removeCallbacks(verificationCheck)
        }
    }
}