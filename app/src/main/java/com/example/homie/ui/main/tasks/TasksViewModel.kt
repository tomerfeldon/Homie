package com.example.homie.ui.main.tasks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homie.data.model.Task
import com.example.homie.data.model.User
import com.example.homie.data.repository.TasksRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class TasksViewModel(
    private val repository: TasksRepository = TasksRepository()
) : ViewModel() {

    private val _tasksState = MutableLiveData<TaskUiState<List<Task>>>()
    val tasksState: LiveData<TaskUiState<List<Task>>> = _tasksState

    private var apartmentId: String? = null

    private val _addTaskState = MutableLiveData<TaskUiState<Unit>>()
    val addTaskState: LiveData<TaskUiState<Unit>> = _addTaskState

    private val _membersState = MutableLiveData<List<User>>()
    val membersState: LiveData<List<User>> = _membersState

    private var tasksListener: ListenerRegistration? = null

    private var allTasks: List<Task> = emptyList()
    private var selectedAssigneeIds: Set<String> = emptySet()
    private var statusFilter: StatusFilter = StatusFilter.ALL

    enum class StatusFilter { ALL, PENDING, COMPLETED }

    fun setFilter(ids: Set<String>) {
        selectedAssigneeIds = ids
        applyFilter()
    }

    fun setStatusFilter(status: StatusFilter) {
        statusFilter = status
        applyFilter()
    }

    private fun applyFilter() {
        var filtered = allTasks
        if (selectedAssigneeIds.isNotEmpty()) {
            filtered = filtered.filter { it.assignedTo in selectedAssigneeIds }
        }
        filtered = when (statusFilter) {
            StatusFilter.PENDING -> filtered.filter { !it.completed }
            StatusFilter.COMPLETED -> filtered.filter { it.completed }
            StatusFilter.ALL -> filtered
        }
        _tasksState.postValue(TaskUiState.Success(filtered))
    }

    suspend fun ensureApartmentLoaded(): Boolean {
        if (apartmentId == null) {
            apartmentId = repository.getApartmentId()
        }
        return apartmentId != null
    }

    fun addTask(title: String, description: String, assignedToId: String, assignedToName: String) {
        viewModelScope.launch {
            _addTaskState.value = TaskUiState.Loading
            try {
                if (ensureApartmentLoaded()) {
                    repository.addTask(apartmentId!!, title, description, assignedToId, assignedToName)
                    _addTaskState.value = TaskUiState.Success(Unit)
                } else {
                    _addTaskState.value = TaskUiState.Error("Apartment not found")
                }
            } catch (e: Exception) {
                _addTaskState.value = TaskUiState.Error(e.message ?: "Failed to add task")
            }
        }
    }

    fun loadMembers() {
        viewModelScope.launch {
            try {
                if (ensureApartmentLoaded()) {
                    val members = repository.getMembers(apartmentId!!)
                    _membersState.value = members
                }
            } catch (e: Exception) {
                // fail silently; spinner will remain empty
            }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            _tasksState.value = TaskUiState.Loading

            apartmentId = repository.getApartmentId()

            apartmentId?.let { aptId ->
                tasksListener?.remove()
                tasksListener = repository.observeTasks(aptId) { state ->
                    if (state is TaskUiState.Success) {
                        allTasks = state.data
                        applyFilter()
                    } else {
                        _tasksState.postValue(state)
                    }
                }
            } ?: run {
                _tasksState.value = TaskUiState.Error("Apartment not found")
            }
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            try {
                apartmentId?.let { repository.completeTask(it, task) }
            } catch (e: Exception) {
                _tasksState.value = TaskUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun uncompleteTask(task: Task) {
        viewModelScope.launch {
            try {
                apartmentId?.let { repository.uncompleteTask(it, task) }
            } catch (e: Exception) {
                _tasksState.value = TaskUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                apartmentId?.let { repository.deleteTask(it, task.id) }
            } catch (e: Exception) {
                _tasksState.value = TaskUiState.Error(e.message ?: "Failed to delete task")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tasksListener?.remove()
    }
}
