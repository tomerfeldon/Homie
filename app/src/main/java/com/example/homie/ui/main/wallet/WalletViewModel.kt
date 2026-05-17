package com.example.homie.ui.main.wallet

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homie.data.model.Expense
import com.example.homie.data.model.MemberBalance
import com.example.homie.data.model.User
import com.example.homie.data.repository.WalletRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class WalletViewModel(
    private val repository: WalletRepository = WalletRepository()
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _uiState = MutableLiveData<WalletUiState>()
    val uiState: LiveData<WalletUiState> = _uiState

    val expenseSaved = MutableLiveData<Boolean?>()

    private var cachedApartmentId: String? = null

    private suspend fun getApartmentId(): String? {
        if (cachedApartmentId == null) {
            cachedApartmentId = repository.getApartmentId()
        }
        return cachedApartmentId
    }

    fun loadExpenses() {
        _uiState.value = WalletUiState.Loading
        viewModelScope.launch {
            try {
                val aptId = getApartmentId() ?: throw Exception("No apartment found")
                val members = repository.getApartmentMembers(aptId)
                val expenses = repository.getExpenses(aptId)
                val balanceRows = calculateBalance(members, expenses)
                _uiState.value = WalletUiState.Success(expenses, balanceRows)
            } catch (e: Exception) {
                _uiState.value = WalletUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun calculateBalance(
        members: List<User>,
        expenses: List<Expense>
    ): List<MemberBalance> {
        if (members.isEmpty()) return emptyList()
        val balanceMap = mutableMapOf<String, Double>()
        members.forEach { balanceMap[it.userId] = 0.0 }

        expenses.forEach { expense ->
            val splitAmong = if (expense.participants.isEmpty()) {
                members
            } else {
                members.filter { it.userId in expense.participants }
            }
            if (splitAmong.isEmpty()) return@forEach

            val perPersonShare = expense.amount / splitAmong.size

            // Credit payer the full amount first
            balanceMap[expense.payerId] =
                (balanceMap[expense.payerId] ?: 0.0) + expense.amount

            // Deduct each participant's share (including payer if they are a participant)
            splitAmong.forEach { member ->
                balanceMap[member.userId] = balanceMap[member.userId]!! - perPersonShare
            }
        }

        return members.map { user ->
            MemberBalance(user.userId, user.name, balanceMap[user.userId] ?: 0.0)
        }
    }

    fun clearExpenseSaved() {
        expenseSaved.value = null
    }

    fun addExpense(amount: Double, category: String, description: String, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                val aptId = getApartmentId() ?: throw Exception("No apartment found")

                var receiptUrl: String? = null
                if (imageUri != null) {
                    val fileRef = storage.reference.child("receipts/${UUID.randomUUID()}.jpg")
                    fileRef.putFile(imageUri).await()
                    receiptUrl = fileRef.downloadUrl.await().toString()
                }

                val userDoc = firestore.collection("users")
                    .document(currentUser.uid).get().await()
                val payerName = userDoc.getString("name") ?: currentUser.email ?: ""

                val expenseId = UUID.randomUUID().toString()
                val expense = Expense(
                    id = expenseId,
                    amount = amount,
                    category = category,
                    description = description,
                    payerId = currentUser.uid,
                    payerName = payerName,
                    receiptUrl = receiptUrl,
                    timestamp = System.currentTimeMillis()
                )
                repository.addExpense(aptId, expense)
                expenseSaved.value = true
                loadExpenses()
            } catch (e: Exception) {
                expenseSaved.value = false
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            try {
                val aptId = getApartmentId() ?: return@launch
                repository.deleteExpense(aptId, expenseId)
                loadExpenses()
            } catch (e: Exception) {
                _uiState.value = WalletUiState.Error(e.message ?: "Failed to delete")
            }
        }
    }

    fun settleUp(fromUserId: String, fromName: String, amount: Double) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                val aptId = getApartmentId() ?: throw Exception("No apartment found")

                val userDoc = firestore.collection("users")
                    .document(currentUser.uid).get().await()
                val toName = userDoc.getString("name") ?: currentUser.email ?: ""

                val expenseId = UUID.randomUUID().toString()
                val settlement = Expense(
                    id = expenseId,
                    amount = amount,
                    category = "Settlement",
                    description = "$fromName → $toName",
                    payerId = fromUserId,
                    payerName = fromName,
                    participants = listOf(fromUserId, currentUser.uid),
                    timestamp = System.currentTimeMillis()
                )
                repository.addExpense(aptId, settlement)
                loadExpenses()
            } catch (e: Exception) {
                _uiState.value = WalletUiState.Error(e.message ?: "Failed to settle")
            }
        }
    }
}
