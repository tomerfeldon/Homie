package com.example.homie.ui.auth

import androidx.lifecycle.*
import com.example.homie.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _registerState = MutableLiveData<Result<Unit>>()
    val registerState: LiveData<Result<Unit>> = _registerState

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            val result = repository.registerUser(name, email, password)
            _registerState.value = result
        }
    }
}
