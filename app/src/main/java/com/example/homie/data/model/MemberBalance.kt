package com.example.homie.data.model

data class MemberBalance(
    val userId: String,
    val name: String,
    val balance: Double  // positive = owed money, negative = owes money
)
