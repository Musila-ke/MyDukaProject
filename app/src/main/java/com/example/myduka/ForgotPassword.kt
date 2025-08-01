package com.example.myduka

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myduka.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPassword : AppCompatActivity() {
    lateinit var forgotPasswordBinding: ActivityForgotPasswordBinding
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.title = "Forgot Password"
        forgotPasswordBinding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        val view = forgotPasswordBinding.root
        setContentView(view)

        forgotPasswordBinding.buttonForgotPassword.setOnClickListener {
            forgotPassword()
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    fun forgotPassword(){
        val email = forgotPasswordBinding.editTextForgotPassword.text.toString().trim()
        if (email.isEmpty()) {
            Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email).addOnCompleteListener { resetTask ->
            if (resetTask.isSuccessful) {
                Toast.makeText(this, "Reset email sent. Please check your inbox (and spam folder).", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                val exception = resetTask.exception
                if (exception is com.google.firebase.auth.FirebaseAuthException) {
                    val errorCode = exception.errorCode
                    if (errorCode == "auth/user-not-found") {
                        Toast.makeText(this, "No account found with that email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to send email: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Failed to send email: ${exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }}