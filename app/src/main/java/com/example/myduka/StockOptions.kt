package com.example.myduka

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myduka.databinding.ActivityStockOptionsBinding

class StockOptions : AppCompatActivity() {
    lateinit var stockOptionsBinding: ActivityStockOptionsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        stockOptionsBinding = ActivityStockOptionsBinding.inflate(layoutInflater)
        val view = stockOptionsBinding.root
        setContentView(view)

        stockOptionsBinding.buttonAddProduct.setOnClickListener {
            val intent = Intent(this, AddStock::class.java)
            startActivity(intent)
        }


        stockOptionsBinding.productUnits.setOnClickListener {
            startActivity(Intent(this, AddProductUnits::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}