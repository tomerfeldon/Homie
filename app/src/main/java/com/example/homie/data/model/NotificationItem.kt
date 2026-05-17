package com.example.homie.data.model

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Long = 0L,
    val read: Boolean = false
)
