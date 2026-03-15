package com.example.homie.ui.main.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.homie.data.model.MemberBalance
import com.example.homie.databinding.FragmentSettleUpBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettleUpBottomSheet(
    private val fromMember: MemberBalance,
    private val currentUserName: String,
    private val onSettle: (fromUserId: String, fromName: String, amount: Double) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: FragmentSettleUpBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettleUpBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.etFrom.setText(fromMember.name)
        binding.etTo.setText(currentUserName)
        // balance is negative (owes money), show absolute value
        binding.etAmount.setText("%.2f".format(-fromMember.balance))

        binding.btnConfirm.setOnClickListener {
            val amountText = binding.etAmount.text.toString().trim()
            if (amountText.isEmpty()) {
                Toast.makeText(requireContext(), "Enter amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onSettle(fromMember.userId, fromMember.name, amount)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
