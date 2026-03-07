package com.example.homie.data.repository

import com.example.homie.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun registerUser(
        name: String,
        email: String,
        password: String
    ): Result<Unit> {

        return try {
            val authResult = auth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val userId = authResult.user?.uid ?: ""

            val user = User(
                userId = userId,
                name = name,
                email = email,
                apartmentId = null,
                streak = 0
            )

            firestore.collection("users")
                .document(userId)
                .set(user)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}