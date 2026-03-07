package com.example.homie.data.model

data class Apartment(
    val aptId: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val members: List<String> = emptyList()
)