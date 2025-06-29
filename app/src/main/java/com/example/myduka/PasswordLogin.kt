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
            Toast.makeText(this, "No PIN found", Toast.LENGTH_SHORT).show()
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
        }
    }

    private fun onDigitPressed(digit: String) {
        if (pinBuilder.length < 4) {
            pinBuilder.append(digit)
            updatePinField()

            if (pinBuilder.length == 4) {
                binding.editTextPinLogIn.postDelayed({
                    verifyPin()
                }, 100)
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
                startActivity(Intent(this, Dashboard::class.java))
                finish()
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                pinBuilder.clear()
                updatePinField()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error verifying PIN", Toast.LENGTH_SHORT).show()
            pinBuilder.clear()
            updatePinField()
        }
    }
}