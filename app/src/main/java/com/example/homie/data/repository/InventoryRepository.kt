package com.example.homie.data.repository

import com.example.homie.data.model.InventoryItem
import com.example.homie.ui.main.inventory.InventoryUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class InventoryRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationsRepository = NotificationsRepository()

    suspend fun getApartmentId(): String? {
        val uid = auth.currentUser?.uid ?: return null

        val doc = firestore.collection("users")
            .document(uid)
            .get()
            .await()

        return doc.getString("apartmentId")
    }

    fun observeInventory(
        apartmentId: String,
        onResult: (InventoryUiState<List<InventoryItem>>) -> Unit
    ): ListenerRegistration {

        return firestore.collection("apartments")
            .document(apartmentId)
            .collection("inventory")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onResult(InventoryUiState.Error(error.message ?: "Error loading inventory"))
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InventoryItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                onResult(InventoryUiState.Success(list))
            }
    }

    suspend fun addItem(
        apartmentId: String,
        name: String,
        quantity: Int
    ) {

        val user = auth.currentUser ?: throw Exception("User not logged in")

        val itemId = UUID.randomUUID().toString()

        val item = InventoryItem(
            id = itemId,
            name = name,
            quantity = quantity,
            addedBy = user.uid,
            addedByName = user.email ?: ""
        )

        firestore.collection("apartments")
            .document(apartmentId)
            .collection("inventory")
            .document(itemId)
            .set(item)
            .await()

        try {
            val aptDoc = firestore.collection("apartments").document(apartmentId).get().await()
            val memberIds = aptDoc.get("members") as? List<String> ?: emptyList()
            val recipients = memberIds.filter { it.isNotBlank() && it != user.uid }
            val actorName = firestore.collection("users").document(user.uid)
                .get().await().getString("name")
                ?.takeIf { it.isNotBlank() }
                ?: user.email
                ?: "Someone"
            notificationsRepository.notifyUsers(
                recipientIds = recipients,
                title = "New inventory item",
                body = "$actorName added: $name (x$quantity)"
            )
        } catch (_: Exception) { }
    }

    suspend fun markAsPurchased(
        apartmentId: String,
        itemId: String
    ) {

        firestore.collection("apartments")
            .document(apartmentId)
            .collection("inventory")
            .document(itemId)
            .set(mapOf("purchased" to true), SetOptions.merge())
            .await()
    }

    suspend fun deleteInventoryItem(apartmentId: String, itemId: String) {
        firestore.collection("apartments")
            .document(apartmentId)
            .collection("inventory")
            .document(itemId)
            .delete()
            .await()
    }
}