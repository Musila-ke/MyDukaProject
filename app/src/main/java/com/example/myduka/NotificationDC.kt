package com.example.myduka

import com.google.firebase.Timestamp
import java.util.UUID

data class NotificationDC(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val message: String = "",
    val timestamp: Timestamp? = null,
    val type: String = TYPE_ALL,
    val seen: Boolean = false      // ← new field
) {
    companion object {
        const val TYPE_ALL     = "all"
        const val TYPE_WORKERS = "workers"
        const val TYPE_EXPIRY  = "expiry"
        const val TYPE_SALES   = "sales"
    }
}
