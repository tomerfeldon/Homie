package com.example.homie.ui.main.dashboard

import com.example.homie.data.model.User

sealed class ApartmentSettingsUiState {
    object Loading : ApartmentSettingsUiState()
    data class Success(
        val apartmentName: String,
        val members: List<User>,
        val currentUserId: String
    ) : ApartmentSettingsUiState()
    data class Error(val message: String) : ApartmentSettingsUiState()
}
