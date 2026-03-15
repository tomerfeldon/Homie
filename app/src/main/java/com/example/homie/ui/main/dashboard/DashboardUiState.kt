package com.example.homie.ui.main.dashboard

import com.example.homie.data.model.Task
import com.example.homie.data.model.User

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val apartmentName: String,
        val members: List<User>,
        val urgentTasks: List<Task>,
        val debtText: String,
        val inviteCode: String
    ) : DashboardUiState()

    data class Error(val message: String) : DashboardUiState()
}