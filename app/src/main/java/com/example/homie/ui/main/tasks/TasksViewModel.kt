package com.example.homie.ui.main.tasks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homie.data.model.Task
import com.example.homie.data.repository.TasksRepository
import kotlinx.coroutines.launch

class TasksViewModel(
    private val repository: TasksRepository = TasksRepository()
) : ViewModel() {

    private val _tasksState = MutableLiveData<TaskUiState<List<Task>>>()
    val tasksState: LiveData<TaskUiState<List<Task>>> = _tasksState

    private var apartmentId: String? = null

    private val _addTaskState = MutableLiveData<TaskUiState<Unit>>()
    val addTaskState: LiveData<TaskUiState<Unit>> = _addTaskState

    suspend fun ensureApartmentLoaded(): Boolean {
        if (apartmentId == null) {
            apartmentId = repository.getApartmentId()
        }
        return apartmentId != null
    }

    fun addTask(title: String, description: String) {
        viewModelScope.launch {
            _addTaskState.value = TaskUiState.Loading
            try {
                if (ensureApartmentLoaded()) {
                    repository.addTask(apartmentId!!, title, description)
                    _addTaskState.value = TaskUiState.Success(Unit)
                } else {
                    _addTaskState.value =
                        TaskUiState.Error("Apartment not found")
                }
            } catch (e: Exception) {
                _addTaskState.value =
                    TaskUiState.Error(e.message ?: "Failed to add task")
            }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            _tasksState.value = TaskUiState.Loading

            apartmentId = repository.getApartmentId()

            apartmentId?.let { aptId ->
                repository.observeTasks(aptId) { state ->
                    _tasksState.postValue(state)
                }
            } ?: run {
                _tasksState.value = TaskUiState.Error("Apartment not found")
            }
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            try {
                apartmentId?.let {
                    repository.completeTask(it, task)
                }
            } catch (e: Exception) {
                _tasksState.value = TaskUiState.Error(e.message ?: "Error")
            }
        }
    }
}
