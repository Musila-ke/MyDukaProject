package com.example.myduka

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myduka.databinding.ActivityPasswordLoginBinding
import com.google.firebase.auth.FirebaseAuth

class PasswordLogin : AppCompatActivity() {
    private lateinit var binding: ActivityPasswordLoginBinding
    private val pinBuilder = StringBuilder()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is logged in and has PIN
        if (auth.currentUser == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!PinManager.hasPin(this)) {
            Toast.makeText(this, "No PIN found. Please create one.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Password_SignUp::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        setupNumberButtons()
        setupActionButtons()
    }

    private fun setupNumberButtons() {
        listOf(
            binding.zero, binding.one, binding.two,
            binding.three, binding.four, binding.five,
            binding.six, binding.seven, binding.eight,
            binding.nine
        ).forEach { btn ->
            btn.setOnClickListener { onDigitPressed(btn.text.toString()) }
        }
    }

    private fun setupActionButtons() {
        binding.backspace.setOnClickListener {
            if (pinBuilder.isNotEmpty()) {
                pinBuilder.deleteCharAt(pinBuilder.length - 1)
                updatePinField()
            }
        }

        binding.textViewforgotpin.setOnClickListener {
            startActivity(Intent(this, ForgotPin::class.java))
            // do not finish, allow user to come back after resetting
        }
    }

    private fun onDigitPressed(digit: String) {
        if (pinBuilder.length < 4) {
            pinBuilder.append(digit)
            updatePinField()

            if (pinBuilder.length == 4) {
                binding.editTextPinLogIn.postDelayed({ verifyPin() }, 100)
            }
        }
    }

    private fun updatePinField() {
        binding.editTextPinLogIn.setText(pinBuilder.toString())
    }

    private fun verifyPin() {
        try {
            if (PinManager.checkPin(this, pinBuilder.toString())) {
                Toast.makeText(this, "PIN verified", Toast.LENGTH_SHORT).show()
                // Launch Dashboard as a new task, clearing backstack
                val intent = Intent(this, Dashboard::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                pinBuilder.clear()
                updatePinField()
            }
        } catch (e: Exception) {
            // Key is invalidated (likely due to uninstall/reinstall)
            val uid = auth.currentUser?.uid
            if (uid != null) {
                getSharedPreferences("app_prefs_$uid", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
            }

            Toast.makeText(this, "PIN was reset. Please create a new one.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, Password_SignUp::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}