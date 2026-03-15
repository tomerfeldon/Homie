package com.example.homie.ui.main.inventory

import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homie.R
import com.example.homie.data.model.InventoryItem
import com.example.homie.databinding.FragmentInventoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

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
        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                t: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                if (item.addedBy != currentUid) {
                    adapter.notifyItemChanged(position)
                    Toast.makeText(
                        requireContext(),
                        "You can only delete items you added",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete item?")
                    .setMessage("This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteItem(item)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .setOnCancelListener {
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val background = ColorDrawable(Color.parseColor("#E53935"))
                background.setBounds(
                    itemView.right + dX.toInt(), itemView.top,
                    itemView.right, itemView.bottom
                )
                background.draw(c)
                ContextCompat.getDrawable(recyclerView.context, R.drawable.baseline_delete_24)
                    ?.let { icon ->
                        val margin = (itemView.height - icon.intrinsicHeight) / 2
                        val top = itemView.top + margin
                        val right = itemView.right - margin
                        icon.setBounds(right - icon.intrinsicWidth, top, right, top + icon.intrinsicHeight)
                        icon.setTint(Color.WHITE)
                        icon.draw(c)
                    }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvInventory)
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
