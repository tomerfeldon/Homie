package com.example.homie.data.repository

import com.example.homie.data.model.Apartment
import com.example.homie.data.model.DashboardData
import com.example.homie.data.model.Expense
import com.example.homie.data.model.Task
import com.example.homie.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DashboardRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getFullDashboardData(): Result<DashboardData> {
        return try {

            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            // Get user
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val apartmentId = userDoc.getString("apartmentId")
                ?: return Result.failure(Exception("No apartment found"))

            // Get apartment
            val aptDoc = firestore.collection("apartments")
                .document(apartmentId)
                .get()
                .await()

            val apartment = aptDoc.toObject(Apartment::class.java)
                ?: return Result.failure(Exception("Apartment missing"))

            // Get members
            val members = mutableListOf<User>()
            for (memberId in apartment.members) {
                val memberDoc = firestore.collection("users")
                    .document(memberId)
                    .get()
                    .await()

                memberDoc.toObject(User::class.java)?.let {
                    members.add(it)
                }
            }

            // Get tasks
            val tasksSnapshot = firestore.collection("apartments")
                .document(apartmentId)
                .collection("tasks")
                .get()
                .await()

            val urgentTasks = tasksSnapshot.documents
                .mapNotNull { it.toObject(Task::class.java)?.copy(id = it.id) }
                .filter { !it.completed }
                .take(3)

            // Get expenses
            val expensesSnapshot = firestore.collection("apartments")
                .document(apartmentId)
                .collection("expenses")
                .get()
                .await()

            val expenses = expensesSnapshot.documents
                .mapNotNull { it.toObject(Expense::class.java) }

            val debtText = calculateDebt(userId, members.size, expenses)

            Result.success(
                DashboardData(
                    apartment.name,
                    members,
                    urgentTasks,
                    debtText,
                    apartment.inviteCode
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateDebt(
        currentUserId: String,
        memberCount: Int,
        expenses: List<Expense>
    ): String {

        if (memberCount == 0) return "All settled 🎉"

        val totalAmount = expenses.sumOf { it.amount }

        val perPersonShare = totalAmount / memberCount

        val userPaid = expenses
            .filter { it.payerId == currentUserId }
            .sumOf { it.amount }

        val balance = userPaid - perPersonShare

        return when {
            balance > 0 -> "You are owed ₪%.2f".format(balance)
            balance < 0 -> "You owe ₪%.2f".format(-balance)
            else -> "All settled 🎉"
        }
    }
}