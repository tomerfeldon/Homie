package com.example.homie.ui.main.inventory

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homie.R
import com.example.homie.data.model.InventoryItem
import com.example.homie.databinding.FragmentInventoryBinding

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels()
    private lateinit var adapter: InventoryAdapter

    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)

        adapter = InventoryAdapter { item, _ ->
            showAddToWalletDialog(item)
        }

        binding.rvInventory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInventory.adapter = adapter

        binding.fabAddItem.setOnClickListener {
            findNavController().navigate(
                R.id.action_navigation_inventory_to_addInventoryFragment
            )
        }

        viewModel.inventoryState.observe(viewLifecycleOwner) { state ->

            when (state) {

                is InventoryUiState.Loading -> {
                    progressDialog.show()
                }

                is InventoryUiState.Success -> {
                    progressDialog.dismiss()
                    adapter.submitList(state.data)
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

        viewModel.loadInventory()
    }

    private fun showAddToWalletDialog(item: InventoryItem) {

        AlertDialog.Builder(requireContext())
            .setTitle("Add to Wallet?")
            .setMessage("Do you want to add this purchase cost to Wallet?")
            .setPositiveButton("Yes") { _, _ ->

                viewModel.markAsPurchased(item)

                val action =
                    InventoryFragmentDirections
                        .actionNavigationInventoryToAddExpenseFragment(
                            prefilledTitle = item.name,
                            prefilledAmount = 0.0f
                        )

                findNavController().navigate(action)
            }
            .setNegativeButton("No") { _, _ ->
                viewModel.markAsPurchased(item)
            }
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
