package com.example.myduka

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myduka.databinding.ActivityPasswordSignUpBinding
import com.google.firebase.auth.FirebaseAuth

class Password_SignUp : AppCompatActivity() {
    private lateinit var binding: ActivityPasswordSignUpBinding
    private val pinBuilder = StringBuilder()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordSignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is logged in
        if (auth.currentUser == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
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

        binding.buttonOk.setOnClickListener {
            if (pinBuilder.length == 4) {
                savePin()
            } else {
                Toast.makeText(this, "Please enter 4 digits", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onDigitPressed(digit: String) {
        if (pinBuilder.length < 4) {
            pinBuilder.append(digit)
            updatePinField()
        }
    }

    private fun updatePinField() {
        binding.editTextPinLogIn.setText(pinBuilder.toString())
    }

    private fun savePin() {
        try {
            PinManager.setPin(this, pinBuilder.toString())
            Toast.makeText(this, "PIN created successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Dashboard::class.java))
            finish() // prevent returning to PIN setup
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save PIN. Please try again.", Toast.LENGTH_SHORT).show()
            pinBuilder.clear()
            updatePinField()
        }
    }
}