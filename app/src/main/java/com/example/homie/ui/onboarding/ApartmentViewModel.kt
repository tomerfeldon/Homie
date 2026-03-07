package com.example.homie.ui.onboarding

import androidx.lifecycle.*
import com.example.homie.data.repository.ApartmentRepository
import kotlinx.coroutines.launch

class ApartmentViewModel(
    private val repository: ApartmentRepository = ApartmentRepository()
) : ViewModel() {

    private val _apartmentState = MutableLiveData<Result<Unit>>()
    val apartmentState: LiveData<Result<Unit>> = _apartmentState

    fun createApartment(name: String) {
        viewModelScope.launch {
            _apartmentState.value = repository.createApartment(name)
        }
    }

    fun joinApartment(code: String) {
        viewModelScope.launch {
            _apartmentState.value = repository.joinApartment(code)
        }
    }
}