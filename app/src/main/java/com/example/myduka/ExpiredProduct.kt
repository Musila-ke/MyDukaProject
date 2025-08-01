package com.example.myduka

data class ExpiredProduct (
    val branchId: String,
    val productId: String,
    val name: String,
    val imageUrl: String?,
    val expiredUnits: List<ExpiredUnit>
)