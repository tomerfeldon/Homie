package com.example.homie.data.model

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val apartmentId: String? = null,
    val streak: Int = 0
)