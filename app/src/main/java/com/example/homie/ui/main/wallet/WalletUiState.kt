package com.example.homie.ui.main.wallet

import com.example.homie.data.model.Expense
import com.example.homie.data.model.MemberBalance

sealed class WalletUiState {
    object Loading : WalletUiState()
    data class Success(
        val expenses: List<Expense>,
        val balanceRows: List<MemberBalance>
    ) : WalletUiState()
    data class Error(val message: String) : WalletUiState()
}
