package com.example.homie.ui.main.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homie.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var adapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = NotificationsAdapter { item ->
            if (!item.read) viewModel.markAsRead(item.id)
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter

        viewModel.notificationsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is NotificationsUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
                is NotificationsUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(state.items)
                    binding.tvEmpty.visibility = if (state.items.isEmpty()) View.VISIBLE else View.GONE
                }
                is NotificationsUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.loadNotifications()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
