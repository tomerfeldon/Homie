package com.example.homie.ui.main.inventory

import androidx.lifecycle.*
import com.example.homie.data.model.InventoryItem
import com.example.homie.data.repository.InventoryRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val repository: InventoryRepository = InventoryRepository()
) : ViewModel() {

    private val _inventoryState =
        MutableLiveData<InventoryUiState<List<InventoryItem>>>()
    val inventoryState: LiveData<InventoryUiState<List<InventoryItem>>> =
        _inventoryState

    private val _addItemState =
        MutableLiveData<InventoryUiState<Unit>>()
    val addItemState: LiveData<InventoryUiState<Unit>> =
        _addItemState

    private val _purchaseState =
        MutableLiveData<InventoryUiState<Unit>>()
    val purchaseState: LiveData<InventoryUiState<Unit>> =
        _purchaseState

    private var apartmentId: String? = null
    private var inventoryListener: ListenerRegistration? = null

    fun loadInventory() {

        viewModelScope.launch {

            _inventoryState.value = InventoryUiState.Loading

            try {
                apartmentId = repository.getApartmentId()

                apartmentId?.let { aptId ->
                    inventoryListener?.remove()
                    inventoryListener = repository.observeInventory(aptId) { state ->
                        _inventoryState.postValue(state)
                    }
                } ?: run {
                    _inventoryState.value =
                        InventoryUiState.Error("Apartment not found")
                }

            } catch (e: Exception) {
                _inventoryState.value =
                    InventoryUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun addItem(name: String, quantity: Int) {

        viewModelScope.launch {

            _addItemState.value = InventoryUiState.Loading
            apartmentId = repository.getApartmentId()

            try {
                apartmentId?.let {
                    repository.addItem(it, name, quantity)
                    _addItemState.value = InventoryUiState.Success(Unit)
                } ?: run {
                    _addItemState.value =
                        InventoryUiState.Error("Apartment not found")
                }

            } catch (e: Exception) {
                _addItemState.value =
                    InventoryUiState.Error(e.message ?: "Failed to add item")
            }
        }
    }

    fun markAsPurchased(item: InventoryItem) {

        viewModelScope.launch {

            _purchaseState.value = InventoryUiState.Loading

            try {
                apartmentId?.let {
                    repository.markAsPurchased(it, item.id)
                    _purchaseState.value = InventoryUiState.Success(Unit)
                }

            } catch (e: Exception) {
                _purchaseState.value =
                    InventoryUiState.Error(e.message ?: "Failed")
            }
        }
    }

    fun updateItem(item: InventoryItem, newName: String, newQuantity: Int) {
        viewModelScope.launch {
            try {
                apartmentId?.let { repository.updateItem(it, item.id, newName, newQuantity) }
            } catch (e: Exception) {
                _inventoryState.value = InventoryUiState.Error(e.message ?: "Failed to update item")
            }
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            try {
                apartmentId?.let { repository.deleteInventoryItem(it, item.id) }
            } catch (e: Exception) {
                _inventoryState.value = InventoryUiState.Error(e.message ?: "Failed to delete")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        inventoryListener?.remove()
    }

}