package com.example.homie.ui.main.wallet

import android.net.Uri
import androidx.lifecycle.*
import com.example.homie.data.model.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class WalletViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _uiState = MutableLiveData<WalletUiState>()
    val uiState: LiveData<WalletUiState> = _uiState

    private val _expenseSaveState = MutableLiveData<WalletUiState>()
    val expenseSaveState: LiveData<WalletUiState> = _expenseSaveState

    // =========================
    // Load Expenses (Safe + MVVM)
    // =========================

    fun loadExpenses() {

        _uiState.value = WalletUiState.Loading

        viewModelScope.launch {
            try {

                val currentUser = auth.currentUser
                    ?: throw Exception("User not logged in")

                val userDoc = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val apartmentId = userDoc.getString("apartmentId")
                    ?: throw Exception("No apartment found")

                val apartmentDoc = firestore.collection("apartments")
                    .document(apartmentId)
                    .get()
                    .await()

                val members = apartmentDoc.get("members") as? List<String>
                    ?: emptyList()

                val memberNameMap = mutableMapOf<String, String>()

                for (memberId in members) {
                    val memberDoc = firestore.collection("users")
                        .document(memberId)
                        .get()
                        .await()

                    val name = memberDoc.getString("name") ?: "Unknown"
                    memberNameMap[memberId] = name
                }

                val expensesSnapshot = firestore.collection("apartments")
                    .document(apartmentId)
                    .collection("expenses")
                    .get()
                    .await()

                val expenses = expensesSnapshot.documents.mapNotNull {
                    it.toObject(Expense::class.java)
                }

                val balanceSummary =
                    calculateBalance(currentUser.uid, members, expenses, memberNameMap)

                _uiState.value =
                    WalletUiState.Success(expenses, balanceSummary)

            } catch (e: Exception) {
                _uiState.value =
                    WalletUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // =========================
    // Correct Balance Algorithm
    // =========================

    private fun calculateBalance(
        currentUserId: String,
        members: List<String>,
        expenses: List<Expense>,
        nameMap: Map<String, String>
    ): String {

        if (members.isEmpty()) return "No members found"

        val balanceMap = mutableMapOf<String, Double>()

        members.forEach { balanceMap[it] = 0.0 }

        expenses.forEach { expense ->
            val splitAmount = expense.amount / members.size

            members.forEach { memberId ->
                if (memberId == expense.payerId) {
                    balanceMap[memberId] =
                        balanceMap[memberId]!! + (expense.amount - splitAmount)
                } else {
                    balanceMap[memberId] =
                        balanceMap[memberId]!! - splitAmount
                }
            }
        }

        return balanceMap.entries.joinToString("\n") {
            val username = nameMap[it.key] ?: "Unknown"
            "$username  : ₪ ${"%.2f".format(it.value)}"
        }
    }

    // =========================
    // Add Expense (With Receipt Upload)
    // =========================

    fun addExpense(
        amount: Double,
        category: String,
        description: String,
        imageUri: Uri?
    ) {

        _expenseSaveState.value = WalletUiState.Loading

        viewModelScope.launch {
            try {

                val currentUser = auth.currentUser
                    ?: throw Exception("User not logged in")

                val userDoc = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val apartmentId = userDoc.getString("apartmentId")
                    ?: throw Exception("No apartment found")

                var receiptUrl: String? = null

                // Upload image if exists
                if (imageUri != null) {

                    val fileRef = storage.reference
                        .child("receipts/${UUID.randomUUID()}.jpg")

                    fileRef.putFile(imageUri).await()

                    receiptUrl = fileRef.downloadUrl.await().toString()
                }

                val expenseId = UUID.randomUUID().toString()

                val expense = Expense(
                    id = expenseId,
                    amount = amount,
                    category = category,
                    description = description,
                    payerId = currentUser.uid,
                    payerName = currentUser.email ?: "",
                    receiptUrl = receiptUrl,
                    timestamp = System.currentTimeMillis()
                )

                firestore.collection("apartments")
                    .document(apartmentId)
                    .collection("expenses")
                    .document(expenseId)
                    .set(expense)
                    .await()

                _expenseSaveState.value =
                    WalletUiState.Success(emptyList(), "Expense Added")

                loadExpenses()

            } catch (e: Exception) {
                _expenseSaveState.value =
                    WalletUiState.Error(e.message ?: "Failed to save")
            }
        }
    }
}