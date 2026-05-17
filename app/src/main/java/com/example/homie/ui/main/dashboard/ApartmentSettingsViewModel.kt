package com.example.homie.ui.main.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homie.data.repository.ApartmentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ApartmentSettingsViewModel(
    private val repository: ApartmentRepository = ApartmentRepository()
) : ViewModel() {

    private val _settingsState = MutableLiveData<ApartmentSettingsUiState>()
    val settingsState: LiveData<ApartmentSettingsUiState> = _settingsState

    private val _renameState = MutableLiveData<Result<Unit>>()
    val renameState: LiveData<Result<Unit>> = _renameState

    private val _removeMemberState = MutableLiveData<Result<Unit>>()
    val removeMemberState: LiveData<Result<Unit>> = _removeMemberState

    fun loadSettings() {
        _settingsState.value = ApartmentSettingsUiState.Loading
        viewModelScope.launch {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            repository.getApartmentWithMembers().fold(
                onSuccess = { (apartment, members) ->
                    _settingsState.value = ApartmentSettingsUiState.Success(
                        apartmentName = apartment.name,
                        members = members,
                        currentUserId = currentUserId
                    )
                },
                onFailure = {
                    _settingsState.value = ApartmentSettingsUiState.Error(it.message ?: "Unknown error")
                }
            )
        }
    }

    fun renameApartment(newName: String) {
        viewModelScope.launch {
            _renameState.value = repository.renameApartment(newName)
        }
    }

    fun removeMember(memberId: String) {
        viewModelScope.launch {
            _removeMemberState.value = repository.removeMember(memberId)
        }
    }
}
