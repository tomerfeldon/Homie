package com.example.homie.ui.main.dashboard

import androidx.lifecycle.*
import com.example.homie.data.repository.ApartmentRepository
import com.example.homie.data.repository.DashboardRepository
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: DashboardRepository = DashboardRepository(),
    private val apartmentRepository: ApartmentRepository = ApartmentRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<DashboardUiState>()
    val uiState: LiveData<DashboardUiState> = _uiState

    private val _leaveState = MutableLiveData<Result<Unit>>()
    val leaveState: LiveData<Result<Unit>> = _leaveState

    fun leaveApartment() {
        viewModelScope.launch {
            _leaveState.value = apartmentRepository.leaveApartment()
        }
    }

    fun loadDashboard() {
        _uiState.value = DashboardUiState.Loading

        viewModelScope.launch {
            val result = repository.getFullDashboardData()

            result.fold(
                onSuccess = {
                    _uiState.value = DashboardUiState.Success(
                        apartmentName = it.apartmentName,
                        members = it.members,
                        urgentTasks = it.urgentTasks,
                        debtText = it.debtText,
                        inviteCode = it.inviteCode
                    )
                },
                onFailure = {
                    _uiState.value = DashboardUiState.Error(
                        it.message ?: "Unknown error"
                    )
                }
            )
        }
    }
}