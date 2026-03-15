package com.example.homie.data.repository

import com.example.homie.data.model.Expense
import com.example.homie.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class WalletRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
    }

    suspend fun deleteExpense(aptId: String, expenseId: String) {
        firestore.collection("apartments")
            .document(aptId)
            .collection("expenses")
            .document(expenseId)
            .delete().await()
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
