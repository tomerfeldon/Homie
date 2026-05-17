package com.example.homie.ui.main.inventory

import android.app.ProgressDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.homie.R
import com.example.homie.databinding.FragmentAddInventoryBinding
import com.google.android.material.textfield.TextInputEditText

class AddInventoryFragment : Fragment() {

    private var _binding: FragmentAddInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels()
    private lateinit var progressDialog: ProgressDialog

    private val rowViews = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Saving...")
        progressDialog.setCancelable(false)

        addRow()

        binding.btnAddRow.setOnClickListener { addRow() }

        binding.btnSaveItem.setOnClickListener { saveAllItems() }

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        viewModel.addItemState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is InventoryUiState.Loading -> progressDialog.show()
                is InventoryUiState.Success -> {
                    progressDialog.dismiss()
                    val count = rowViews.size
                    val msg = if (count == 1) "Item added" else "$count items added"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is InventoryUiState.Error -> {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addRow() {
        val row = layoutInflater.inflate(R.layout.item_add_inventory_row, binding.llItemsContainer, false)
        rowViews.add(row)
        binding.llItemsContainer.addView(row)

        row.findViewById<View>(R.id.ibRemoveRow).setOnClickListener {
            if (rowViews.size > 1) {
                rowViews.remove(row)
                binding.llItemsContainer.removeView(row)
            } else {
                Toast.makeText(requireContext(), "At least one item required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAllItems() {
        val items = mutableListOf<Pair<String, Int>>()

        for (row in rowViews) {
            val name = row.findViewById<TextInputEditText>(R.id.etRowName).text.toString().trim()
            val qtyText = row.findViewById<TextInputEditText>(R.id.etRowQuantity).text.toString().trim()
            val qty = qtyText.toIntOrNull()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Fill in all item names", Toast.LENGTH_SHORT).show()
                return
            }
            if (qty == null || qty < 1) {
                Toast.makeText(requireContext(), "Enter a valid quantity for \"$name\"", Toast.LENGTH_SHORT).show()
                return
            }
            items.add(name to qty)
        }

        viewModel.addItems(items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
