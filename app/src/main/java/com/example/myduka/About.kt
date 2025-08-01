package com.example.myduka

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myduka.databinding.ActivityAboutBinding
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

class About : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // handle system-bar insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // ←— Add this block to launch the license list:
        binding.licenses.setOnClickListener {
            // Optional: change the title in the license screen
            OssLicensesMenuActivity.setActivityTitle("Third-Party Licenses")
            startActivity(Intent(this, OssLicensesMenuActivity::class.java))
        }
    }
}
