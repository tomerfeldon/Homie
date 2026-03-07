package com.example.homie.ui.main.inventory

sealed class InventoryUiState<out T> {
    object Loading : InventoryUiState<Nothing>()
    data class Success<T>(val data: T) : InventoryUiState<T>()
    data class Error(val message: String) : InventoryUiState<Nothing>()
}