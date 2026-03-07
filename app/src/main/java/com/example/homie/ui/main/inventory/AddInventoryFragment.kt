package com.example.homie.ui.main.inventory

import android.app.ProgressDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.homie.databinding.FragmentAddInventoryBinding

class AddInventoryFragment : Fragment() {

    private var _binding: FragmentAddInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels()

    private lateinit var progressDialog: ProgressDialog

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

        binding.btnSaveItem.setOnClickListener {

            val name = binding.etItemName.text.toString()
            val quantityText = binding.etQuantity.text.toString()

            if (name.isEmpty() || quantityText.isEmpty()) {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.addItem(name, quantityText.toInt())
        }

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.addItemState.observe(viewLifecycleOwner) { state ->

            when (state) {

                is InventoryUiState.Loading -> {
                    progressDialog.show()
                }

                is InventoryUiState.Success -> {
                    progressDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Item Added",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().popBackStack()
                }

                is InventoryUiState.Error -> {
                    progressDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        state.message,
                        Toast.LENGTH_SHORT
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
