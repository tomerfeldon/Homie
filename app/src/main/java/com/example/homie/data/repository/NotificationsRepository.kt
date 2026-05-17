package com.example.homie.data.repository

import com.example.homie.data.model.NotificationItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class NotificationsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun notificationsRef(userId: String) =
        firestore.collection("users").document(userId).collection("notifications")

    fun observeNotifications(
        onUpdate: (List<NotificationItem>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val userId = auth.currentUser?.uid ?: return null
        return notificationsRef(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Failed to load notifications")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val items = snapshot.documents
                    .mapNotNull { doc -> doc.toObject(NotificationItem::class.java)?.copy(id = doc.id) }
                    .sortedByDescending { it.timestamp }
                onUpdate(items)
            }
    }

    suspend fun saveNotification(title: String, body: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))
            notifyUser(userId, title, body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun notifyUser(recipientId: String, title: String, body: String): Result<Unit> {
        return try {
            val ref = notificationsRef(recipientId).document()
            val item = NotificationItem(
                id = ref.id,
                title = title,
                body = body,
                timestamp = System.currentTimeMillis(),
                read = false
            )
            ref.set(item).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun notifyUsers(recipientIds: List<String>, title: String, body: String) {
        recipientIds.forEach { notifyUser(it, title, body) }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))
            notificationsRef(userId).document(notificationId)
                .update("read", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveFcmToken(token: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))
            firestore.collection("users").document(userId)
                .update("fcmToken", token)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
