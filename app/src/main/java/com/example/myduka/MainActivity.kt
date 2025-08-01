package com.example.myduka

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myduka.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val NOTIFICATION_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            } else {
                initFCM()
            }
        } else {
            initFCM()
        }
    }

    private fun initFCM() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            setupAuthListener()
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "FCM Token: $token")

                // ✅ Save token to Firestore
                val data = mapOf("uid" to user.uid)
                FirebaseFirestore.getInstance()
                    .collection("fcmTokens")
                    .document(token)
                    .set(data)
                    .addOnSuccessListener {
                        Log.d("FCM", "Token saved to Firestore")
                    }
                    .addOnFailureListener {
                        Log.w("FCM", "Failed to save token", it)
                    }

            } else {
                Log.w("FCM", "Token fetch failed", task.exception)
            }
        }

        // 🔹 Subscribe to topic and log status
        FirebaseMessaging.getInstance().subscribeToTopic("admin-notifications")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Subscribed to admin-notifications")
                    Toast.makeText(this, "Subscribed to admin notifications", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w("FCM", "Subscription failed", task.exception)
                    Toast.makeText(this, "Subscription to notifications failed", Toast.LENGTH_SHORT).show()
                }
            }

        setupAuthListener()
    }

    private fun setupAuthListener() {
        auth.addAuthStateListener { firebaseAuth ->
            firebaseAuth.currentUser?.let { user ->
                checkUserAndPin(user)
            } ?: run {
                startActivity(Intent(this, SignUp::class.java))
                finish()
            }
        }
    }

    private fun checkUserAndPin(user: FirebaseUser) {
        if (!user.isEmailVerified) {
            Toast.makeText(this, "Please verify your email first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignUp::class.java))
            finish()
            return
        }

        try {
            if (PinManager.hasPin(this)) {
                startActivity(Intent(this, PasswordLogin::class.java))
            } else {
                startActivity(Intent(this, Password_SignUp::class.java))
            }
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error checking PIN status", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignUp::class.java))
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initFCM()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                setupAuthListener()
            }
        }
    }
}
