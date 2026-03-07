package com.example.homie.ui.main.wallet

import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homie.R
import com.example.homie.databinding.FragmentWalletBinding

class WalletFragment : Fragment() {

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WalletViewModel by viewModels()
    private lateinit var adapter: ExpenseAdapter

    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ExpenseAdapter()

        binding.rvExpenses.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.rvExpenses.adapter = adapter

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)

        observeUiState()

        viewModel.loadExpenses()


        binding.fabAddExpense.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_wallet_to_addExpenseFragment)
        }
    }

    private fun observeUiState() {

        viewModel.uiState.observe(viewLifecycleOwner) { state ->

            when (state) {

                is WalletUiState.Loading -> {
                    if (!progressDialog.isShowing)
                        progressDialog.show()
                }

                is WalletUiState.Success -> {
                    if (progressDialog.isShowing)
                        progressDialog.dismiss()

                    adapter.submitList(state.expenses)
                    binding.tvBalanceSummary.text = state.balanceSummary
                }

                is WalletUiState.Error -> {
                    if (progressDialog.isShowing)
                        progressDialog.dismiss()

                    Toast.makeText(
                        requireContext(),
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}