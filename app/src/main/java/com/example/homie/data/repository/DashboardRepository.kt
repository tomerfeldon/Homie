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

                val raw = memberDoc.toObject(User::class.java) ?: continue
                members.add(raw.copy(
                    userId = raw.userId.ifBlank { memberId },
                    name = raw.name.ifBlank { raw.email }
                ))
            }

            // Get tasks
            val tasksSnapshot = firestore.collection("apartments")
                .document(apartmentId)
                .collection("tasks")
                .get()
                .await()

            val urgentTasks = tasksSnapshot.documents
                .mapNotNull { it.toObject(Task::class.java)?.copy(id = it.id) }
                .filter { !it.completed && it.assignedTo == userId }
                .take(3)

            // Get expenses
            val expensesSnapshot = firestore.collection("apartments")
                .document(apartmentId)
                .collection("expenses")
                .get()
                .await()

            val expenses = expensesSnapshot.documents
                .mapNotNull { it.toObject(Expense::class.java) }

            val debtText = calculateDebt(userId, members, expenses)

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
        members: List<User>,
        expenses: List<Expense>
    ): String {
        if (members.isEmpty()) return "All settled 🎉"

        var balance = 0.0

        expenses.forEach { expense ->
            val splitAmong = if (expense.participants.isEmpty()) {
                members
            } else {
                members.filter { it.userId in expense.participants }
            }
            if (splitAmong.isEmpty()) return@forEach

            val perPersonShare = expense.amount / splitAmong.size
            val isParticipant = splitAmong.any { it.userId == currentUserId }

            when {
                expense.payerId == currentUserId && isParticipant ->
                    balance += expense.amount - perPersonShare
                expense.payerId == currentUserId && !isParticipant ->
                    balance += expense.amount
                isParticipant ->
                    balance -= perPersonShare
            }
        }

        return when {
            balance > 0 -> "You are owed ₪%.2f".format(balance)
            balance < 0 -> "You owe ₪%.2f".format(-balance)
            else -> "All settled 🎉"
        }
    }
}