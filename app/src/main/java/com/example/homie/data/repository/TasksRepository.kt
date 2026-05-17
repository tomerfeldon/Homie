package com.example.homie.data.repository

import android.util.Log
import com.example.homie.data.model.Task
import com.example.homie.data.model.User
import com.example.homie.ui.main.tasks.TaskUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TasksRepository {

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

    suspend fun getMembers(apartmentId: String): List<User> {
        val aptDoc = firestore.collection("apartments")
            .document(apartmentId)
            .get()
            .await()
        val memberIds = aptDoc.get("members") as? List<String> ?: return emptyList()
        return memberIds.mapNotNull { uid ->
            firestore.collection("users")
                .document(uid)
                .get()
                .await()
                .toObject(User::class.java)
                ?.copy(userId = uid)
        }
    }

    fun observeTasks(
        apartmentId: String,
        onResult: (TaskUiState<List<Task>>) -> Unit
    ): ListenerRegistration {
        return firestore.collection("apartments")
            .document(apartmentId)
            .collection("tasks")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onResult(TaskUiState.Error(error.message ?: "Unknown error"))
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Task::class.java)?.apply { id = doc.id }
                } ?: emptyList()

                onResult(TaskUiState.Success(list))
            }
    }

    suspend fun addTask(
        apartmentId: String,
        title: String,
        description: String,
        assignedToId: String,
        assignedToName: String
    ) {
        val user = auth.currentUser ?: return
        val taskId = UUID.randomUUID().toString()
        val task = Task(
            id = taskId,
            title = title,
            description = description,
            assignedTo = assignedToId,
            assignedToName = assignedToName,
            createdBy = user.uid,
            completed = false,
            timestamp = System.currentTimeMillis()
        )
        firestore.collection("apartments")
            .document(apartmentId)
            .collection("tasks")
            .document(taskId)
            .set(task)
            .await()

        Log.d("HomieNotif", "addTask: assignedToId='$assignedToId', currentUid='${user.uid}'")
        if (assignedToId.isNotBlank() && assignedToId != user.uid) {
            Log.d("HomieNotif", "addTask: sending notification to $assignedToId")
            try {
                val actorName = firestore.collection("users").document(user.uid)
                    .get().await().getString("name")
                    ?.takeIf { it.isNotBlank() }
                    ?: user.email
                    ?: "Someone"
                notificationsRepository.notifyUser(
                    recipientId = assignedToId,
                    title = "New task assigned",
                    body = "$actorName assigned you: $title"
                )
            } catch (e: Exception) {
                Log.e("HomieNotif", "addTask: outer catch swallowed: ${e.message}", e)
            }
        } else {
            Log.d("HomieNotif", "addTask: SKIPPED notification (blank or self-assigned)")
        }
    }

    suspend fun deleteTask(apartmentId: String, taskId: String) {
        val user = auth.currentUser
        val taskDoc = firestore.collection("apartments")
            .document(apartmentId).collection("tasks").document(taskId).get().await()
        val taskTitle = taskDoc.getString("title") ?: "a task"

        firestore.collection("apartments")
            .document(apartmentId).collection("tasks").document(taskId)
            .delete().await()

        try {
            val actorName = user?.uid?.let {
                firestore.collection("users").document(it).get().await().getString("name")
            }?.takeIf { it.isNotBlank() } ?: user?.email ?: "Someone"
            val aptDoc = firestore.collection("apartments").document(apartmentId).get().await()
            val memberIds = aptDoc.get("members") as? List<String> ?: emptyList()
            val recipients = memberIds.filter { it.isNotBlank() && it != user?.uid }
            notificationsRepository.notifyUsers(
                recipientIds = recipients,
                title = "Task deleted",
                body = "$actorName deleted task: $taskTitle"
            )
        } catch (_: Exception) { }
    }

    suspend fun completeTask(apartmentId: String, task: Task) {
        val uid = auth.currentUser?.uid ?: return

        val taskRef = firestore.collection("apartments")
            .document(apartmentId)
            .collection("tasks")
            .document(task.id)

        val userRef = firestore.collection("users").document(uid)

        firestore.runBatch { batch ->
            batch.set(taskRef, mapOf("completed" to true), SetOptions.merge())
            batch.set(userRef, mapOf("streak" to FieldValue.increment(1)), SetOptions.merge())
        }.await()

        try {
            val actorName = firestore.collection("users").document(uid)
                .get().await().getString("name")
                ?.takeIf { it.isNotBlank() } ?: auth.currentUser?.email ?: "Someone"
            val aptDoc = firestore.collection("apartments").document(apartmentId).get().await()
            val memberIds = aptDoc.get("members") as? List<String> ?: emptyList()
            val recipients = memberIds.filter { it.isNotBlank() && it != uid }
            notificationsRepository.notifyUsers(
                recipientIds = recipients,
                title = "Task completed",
                body = "$actorName completed: ${task.title}"
            )
        } catch (_: Exception) { }
    }

    suspend fun uncompleteTask(apartmentId: String, task: Task) {
        val uid = auth.currentUser?.uid ?: return

        val taskRef = firestore.collection("apartments")
            .document(apartmentId)
            .collection("tasks")
            .document(task.id)

        val userRef = firestore.collection("users").document(uid)

        firestore.runBatch { batch ->
            batch.set(taskRef, mapOf("completed" to false), SetOptions.merge())
            batch.set(userRef, mapOf("streak" to FieldValue.increment(-1)), SetOptions.merge())
        }.await()
    }
}
