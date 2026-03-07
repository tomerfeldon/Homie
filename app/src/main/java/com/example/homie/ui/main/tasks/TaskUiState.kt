package com.example.homie.ui.main.tasks

sealed class TaskUiState<out T> {
    object Loading : TaskUiState<Nothing>()
    data class Success<T>(val data: T) : TaskUiState<T>()
    data class Error(val message: String) : TaskUiState<Nothing>()
}