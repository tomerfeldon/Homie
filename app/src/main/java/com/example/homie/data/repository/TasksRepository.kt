package com.example.homie.data.repository

import com.example.homie.data.model.Task
import com.example.homie.ui.main.tasks.TaskUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TasksRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getApartmentId(): String? {
        val uid = auth.currentUser?.uid ?: return null
        val doc = firestore.collection("users")
            .document(uid)
            .get()
            .await()

        return doc.getString("apartmentId")
    }

    fun observeTasks(
        apartmentId: String,
        onResult: (TaskUiState<List<Task>>) -> Unit
    ) {
        firestore.collection("apartments")
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

    suspend fun addTask(apartmentId: String, title: String, description: String) {
        val user = auth.currentUser ?: return

        val taskId = UUID.randomUUID().toString()

        val task = Task(
            id = taskId,
            title = title,
            description = description,
            assignedTo = user.uid,
            assignedToName = user.email ?: "",
            completed = false,
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("apartments")
            .document(apartmentId)
            .collection("tasks")
            .document(taskId)
            .set(task)
            .await()
    }

    suspend fun completeTask(apartmentId: String, task: Task) {
        val uid = auth.currentUser?.uid ?: return

        val taskRef = firestore.collection("apartments")
            .document(apartmentId)
            .collection("tasks")
            .document(task.id)

        val userRef = firestore.collection("users").document(uid)

        firestore.runBatch { batch ->
            batch.update(taskRef, "completed", true)
            batch.update(userRef, "streak", FieldValue.increment(1))
        }.await()
    }
}
