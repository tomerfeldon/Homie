package com.example.homie.data.model

data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 1,
    val addedBy: String = "",
    val addedByName: String = "",
    val purchased: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
