package com.example.homie.ui.main.wallet

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.homie.databinding.FragmentAddExpenseBinding

class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WalletViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private lateinit var progressDialog: ProgressDialog

    private val args: AddExpenseFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        if (args.prefilledTitle.isNotEmpty()) {
            binding.etDescription.setText(args.prefilledTitle)
        }

        if (args.prefilledAmount > 0f) {
            binding.etAmount.setText(args.prefilledAmount.toString())
        }

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Saving...")
        progressDialog.setCancelable(false)

        setupCategorySpinner()

        binding.btnSaveExpense.setOnClickListener {
            validateAndSave()
        }

        observeSaveState()

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupCategorySpinner() {
        val categories = listOf(
            "Rent",
            "Groceries",
            "Electricity",
            "Internet",
            "Maintenance",
            "Other"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
        binding.spinnerCategory.adapter = adapter
    }

    private fun validateAndSave() {

        val amountText = binding.etAmount.text.toString()

        if (amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Enter amount", Toast.LENGTH_SHORT).show()
            return
        }

        val description = binding.etDescription.text.toString()
        val category = binding.spinnerCategory.selectedItem.toString()

        viewModel.addExpense(
            amount = amountText.toDouble(),
            category = category,
            description = description,
            imageUri = selectedImageUri
        )
    }

    private fun observeSaveState() {
        viewModel.expenseSaveState.observe(viewLifecycleOwner) { state ->

            when (state) {

                is WalletUiState.Loading -> progressDialog.show()

                is WalletUiState.Success -> {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(),
                        "Expense Added", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }

                is WalletUiState.Error -> {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(),
                        state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}