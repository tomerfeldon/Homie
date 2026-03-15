package com.example.homie.ui.main.wallet

import android.app.ProgressDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homie.R
import com.example.homie.databinding.FragmentWalletBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class WalletFragment : Fragment() {

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WalletViewModel by viewModels()
    private lateinit var adapter: ExpenseAdapter
    private lateinit var balanceAdapter: BalanceSummaryAdapter
    private var currentUserName = ""

    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ExpenseAdapter()
        binding.rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExpenses.adapter = adapter

        balanceAdapter = BalanceSummaryAdapter { member ->
            SettleUpBottomSheet(member, currentUserName) { fromUserId, fromName, amount ->
                viewModel.settleUp(fromUserId, fromName, amount)
            }.show(childFragmentManager, "settle_up")
        }
        binding.rvBalanceSummary.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBalanceSummary.adapter = balanceAdapter

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)

        observeUiState()
        viewModel.loadExpenses()

        binding.fabAddExpense.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_wallet_to_addExpenseFragment)
        }

        setupSwipeToDelete()
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is WalletUiState.Loading -> {
                    if (!progressDialog.isShowing) progressDialog.show()
                }
                is WalletUiState.Success -> {
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    adapter.submitList(state.expenses)
                    balanceAdapter.submitList(state.balanceRows)
                    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                    currentUserName = state.balanceRows
                        .firstOrNull { it.userId == currentUid }?.name ?: ""
                }
                is WalletUiState.Error -> {
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
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
                val expense = adapter.currentList[position]
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                if (expense.payerId != currentUid) {
                    adapter.notifyItemChanged(position)
                    Toast.makeText(
                        requireContext(),
                        "You can only delete items you created",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete expense?")
                    .setMessage("This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteExpense(expense.id)
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
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvExpenses)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
