package com.example.homie.ui.main.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homie.data.model.NotificationItem
import com.example.homie.data.repository.NotificationsRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val repository: NotificationsRepository = NotificationsRepository()
) : ViewModel() {

    private val _notificationsState = MutableLiveData<NotificationsUiState>()
    val notificationsState: LiveData<NotificationsUiState> = _notificationsState

    private var listener: ListenerRegistration? = null

    fun loadNotifications() {
        _notificationsState.value = NotificationsUiState.Loading
        listener?.remove()
        listener = repository.observeNotifications { items ->
            _notificationsState.postValue(NotificationsUiState.Success(items))
        }
        if (listener == null) {
            _notificationsState.value = NotificationsUiState.Error("User not logged in")
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}

sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    data class Success(val items: List<NotificationItem>) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}
