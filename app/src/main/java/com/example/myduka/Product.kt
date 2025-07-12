package com.example.myduka

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val discountPercent: Double? = null,
    val discountedPrice: Double? = null,
    val hasExpiredUnits: Boolean = false,
    val type: String = ""
)
