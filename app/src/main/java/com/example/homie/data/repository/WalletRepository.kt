package com.example.homie.data.repository

import com.example.homie.data.model.Expense
import com.example.homie.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class WalletRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationsRepository = NotificationsRepository()

    suspend fun getApartmentId(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection("users")
            .document(uid).get().await()
            .getString("apartmentId")
    }

    suspend fun getExpenses(aptId: String): List<Expense> {
        return firestore.collection("apartments")
            .document(aptId)
            .collection("expenses")
            .get().await()
            .documents.mapNotNull { it.toObject(Expense::class.java) }
    }

    suspend fun addExpense(aptId: String, expense: Expense) {
        firestore.collection("apartments")
            .document(aptId)
            .collection("expenses")
            .document(expense.id)
            .set(expense).await()

        try {
            val currentUid = auth.currentUser?.uid
            val actor = currentUid?.let {
                firestore.collection("users").document(it).get().await().getString("name")
            }?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.email
                ?: "Someone"
            val aptDoc = firestore.collection("apartments").document(aptId).get().await()
            val memberIds = aptDoc.get("members") as? List<String> ?: emptyList()
            val recipients = memberIds.filter { it.isNotBlank() && it != currentUid }
            notificationsRepository.notifyUsers(
                recipientIds = recipients,
                title = "New expense added",
                body = "$actor added: ₪${expense.amount} - ${expense.description}"
            )
        } catch (_: Exception) { }
    }

    suspend fun deleteExpense(aptId: String, expenseId: String) {
        val user = auth.currentUser
        val expenseDoc = firestore.collection("apartments")
            .document(aptId).collection("expenses").document(expenseId).get().await()
        val description = expenseDoc.getString("description") ?: "an expense"
        val amount = expenseDoc.getDouble("amount")

        firestore.collection("apartments")
            .document(aptId).collection("expenses").document(expenseId)
            .delete().await()

        try {
            val actorName = user?.uid?.let {
                firestore.collection("users").document(it).get().await().getString("name")
            }?.takeIf { it.isNotBlank() } ?: user?.email ?: "Someone"
            val aptDoc = firestore.collection("apartments").document(aptId).get().await()
            val memberIds = aptDoc.get("members") as? List<String> ?: emptyList()
            val recipients = memberIds.filter { it.isNotBlank() && it != user?.uid }
            val amountStr = if (amount != null) "₪$amount - " else ""
            notificationsRepository.notifyUsers(
                recipientIds = recipients,
                title = "Expense deleted",
                body = "$actorName deleted expense: $amountStr$description"
            )
        } catch (_: Exception) { }
    }

    suspend fun getApartmentMembers(aptId: String): List<User> {
        val aptDoc = firestore.collection("apartments")
            .document(aptId).get().await()
        val memberIds = aptDoc.get("members") as? List<String> ?: return emptyList()
        return memberIds.mapNotNull { uid ->
            firestore.collection("users")
                .document(uid).get().await()
                .toObject(User::class.java)
        }
    }
}
