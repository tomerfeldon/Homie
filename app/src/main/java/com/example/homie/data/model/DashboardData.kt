package com.example.homie.data.model

data class DashboardData(
    val apartmentName: String,
    val members: List<User>,
    val urgentTasks: List<Task>,
    val debtText: String
)
