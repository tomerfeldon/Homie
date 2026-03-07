package com.example.homie.ui.main.wallet

import com.example.homie.data.model.Expense

sealed class WalletUiState {

    object Loading : WalletUiState()

    data class Success(
        val expenses: List<Expense>,
        val balanceSummary: String
    ) : WalletUiState()

    data class Error(val message: String) : WalletUiState()
}
