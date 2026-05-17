package com.example.homie.data.repository

import com.example.homie.data.model.Apartment
import com.example.homie.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ApartmentRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun generateInviteCode(): String {
        return (100000..999999).random().toString()
    }

    suspend fun createApartment(name: String): Result<Unit> {
        return try {

            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            val aptRef = firestore.collection("apartments").document()
            val inviteCode = generateInviteCode()

            val apartment = Apartment(
                aptId = aptRef.id,
                name = name,
                inviteCode = inviteCode,
                members = listOf(userId)
            )

            aptRef.set(apartment).await()

            // Update user with apartmentId
            firestore.collection("users")
                .document(userId)
                .set(mapOf("apartmentId" to aptRef.id), com.google.firebase.firestore.SetOptions.merge())
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinApartment(inviteCode: String): Result<Unit> {
        return try {

            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            val query = firestore.collection("apartments")
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .await()

            if (query.isEmpty) {
                return Result.failure(Exception("Invalid Invite Code"))
            }

            val apartmentDoc = query.documents.first()
            val aptId = apartmentDoc.id

            firestore.collection("apartments")
                .document(aptId)
                .update("members", FieldValue.arrayUnion(userId))
                .await()

            firestore.collection("users")
                .document(userId)
                .set(mapOf("apartmentId" to aptId), com.google.firebase.firestore.SetOptions.merge())
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApartmentWithMembers(): Result<Pair<Apartment, List<User>>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val userDoc = firestore.collection("users").document(userId).get().await()
            val apartmentId = userDoc.getString("apartmentId")
                ?: return Result.failure(Exception("No apartment found"))

            val aptDoc = firestore.collection("apartments").document(apartmentId).get().await()
            val apartment = aptDoc.toObject(Apartment::class.java)
                ?: return Result.failure(Exception("Apartment missing"))

            val members = mutableListOf<User>()
            for (memberId in apartment.members) {
                val memberDoc = firestore.collection("users").document(memberId).get().await()
                val raw = memberDoc.toObject(User::class.java) ?: continue
                members.add(raw.copy(userId = raw.userId.ifBlank { memberId }))
            }

            Result.success(Pair(apartment, members))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameApartment(newName: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val userDoc = firestore.collection("users").document(userId).get().await()
            val apartmentId = userDoc.getString("apartmentId")
                ?: return Result.failure(Exception("No apartment found"))

            firestore.collection("apartments").document(apartmentId)
                .update("name", newName)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMember(memberId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val userDoc = firestore.collection("users").document(userId).get().await()
            val apartmentId = userDoc.getString("apartmentId")
                ?: return Result.failure(Exception("No apartment found"))

            firestore.collection("apartments").document(apartmentId)
                .update("members", FieldValue.arrayRemove(memberId))
                .await()

            firestore.collection("users").document(memberId)
                .update("apartmentId", null)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveApartment(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val apartmentId = userDoc.getString("apartmentId")
                ?: return Result.failure(Exception("No apartment found"))

            firestore.collection("apartments")
                .document(apartmentId)
                .update("members", FieldValue.arrayRemove(userId))
                .await()

            firestore.collection("users")
                .document(userId)
                .set(mapOf("apartmentId" to null), com.google.firebase.firestore.SetOptions.merge())
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
