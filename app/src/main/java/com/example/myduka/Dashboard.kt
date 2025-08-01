package com.example.myduka

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.myduka.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Dashboard : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var picListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(this.binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2️⃣ Use the insets‐controller to set bar color & icon tint
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false // false = light icons
        window.statusBarColor = "#223A59".toColorInt()
        retrieveUserData()
        listenForProfilePicture()

        binding.logOut.setOnClickListener { doLogOut() }
        binding.stock.setOnClickListener {
            startActivity(Intent(this, StockOptions::class.java))
        }
        binding.earnings.setOnClickListener {
            // TODO: Implement earnings activity
        }
        binding.help.setOnClickListener {
            startActivity(Intent(this, Help::class.java))
        }
        binding.profile.setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }
        binding.branches.setOnClickListener {
            startActivity(Intent(this, Branches::class.java))
        }
        binding.about.setOnClickListener {
            startActivity(Intent(this, About::class.java))
        }

        binding.salesHistory.setOnClickListener {
            startActivity(Intent(this,SalesHistory::class.java))
        }
        binding.earnings.setOnClickListener {
            startActivity(Intent(this,Earnings::class.java))
        }


        // 5️⃣ Handle window insets for your root view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        picListener?.remove()
    }

    private fun doLogOut() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun retrieveUserData() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(this, "Error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                } else if (snap != null && snap.exists()) {
                    binding.textViewUserName.text = snap.getString("userName")
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun listenForProfilePicture() {
        val uid = auth.currentUser?.uid ?: return
        picListener = db.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(this, "Error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                } else if (snap != null && snap.exists()) {
                    val url = snap.getString("profilePicture")
                    if (!url.isNullOrEmpty()) {
                        showPic(binding.profilepicture, url)
                    } else {
                        binding.profilepicture.setImageResource(R.drawable.profile)
                    }
                }
            }
    }

    private fun showPic(imageView: ImageView, url: String) {
        Glide.with(this)
            .load(url)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.profile)
            .error(R.drawable.profile)
            .into(imageView)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.notification, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.notifications -> {
                startActivity(Intent(this, Notifications::class.java))
                true
            }
            // Handle other menu items here if needed
            else -> super.onOptionsItemSelected(item)
        }
    }
}
