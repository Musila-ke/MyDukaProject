package com.example.myduka

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myduka.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var loginBinding: ActivityLoginBinding
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loginBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(loginBinding.root)

        loginBinding.buttonSignIn.setOnClickListener {
            loginBinding.progressBarSignIn.visibility = View.VISIBLE
            signIn()
        }

        loginBinding.textViewSignUp.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
            finish() // don’t allow back to Login once going to SignUp
        }

        loginBinding.textViewForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPassword::class.java))
            // keep Login in back‑stack so user can go back after resetting
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun signIn() {
        val email    = loginBinding.editTextEmailSignIn.text.toString().trim()
        val password = loginBinding.editTextPasswordSignIn.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email or Password cannot be empty", Toast.LENGTH_SHORT).show()
            loginBinding.progressBarSignIn.visibility = View.GONE
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                loginBinding.progressBarSignIn.visibility = View.GONE

                if (task.isSuccessful) {
                    startActivity(Intent(this, PasswordLogin::class.java))
                    finish() // remove Login from back‑stack
                } else {
                    Toast.makeText(this, task.exception?.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        auth.currentUser?.let {
            Toast.makeText(this, "Welcome Back", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Dashboard::class.java))
            finish() // don’t return to Login once Dashboard is shown
        }
    }
}
