package com.example.homie.data.model

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val description: String = "",
    val payerId: String = "",
    val payerName: String = "",
    val receiptUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)