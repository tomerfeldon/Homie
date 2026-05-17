package com.example.homie.ui.main.dashboard

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
import com.example.homie.data.model.User
import com.example.homie.databinding.FragmentApartmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ApartmentSettingsFragment : Fragment() {

    private var _binding: FragmentApartmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ApartmentSettingsViewModel by viewModels()
    private lateinit var membersAdapter: ApartmentMembersAdapter
    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApartmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupProgressDialog()
        setupToolbar()
        observeViewModel()
        viewModel.loadSettings()

        binding.btnSaveName.setOnClickListener {
            val newName = binding.etApartmentName.text?.toString()?.trim() ?: ""
            if (newName.isBlank()) {
                binding.tilApartmentName.error = "Name cannot be empty"
                return@setOnClickListener
            }
            binding.tilApartmentName.error = null
            showRenameConfirmation(newName)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupProgressDialog() {
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setCancelable(false)
    }

    private fun observeViewModel() {
        viewModel.settingsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ApartmentSettingsUiState.Loading -> {
                    progressDialog.setMessage("Loading...")
                    progressDialog.show()
                }
                is ApartmentSettingsUiState.Success -> {
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    binding.etApartmentName.setText(state.apartmentName)
                    setupMembersAdapter(state.currentUserId)
                    membersAdapter.submitList(state.members)
                }
                is ApartmentSettingsUiState.Error -> {
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.renameState.observe(viewLifecycleOwner) { result ->
            if (progressDialog.isShowing) progressDialog.dismiss()
            result.onSuccess {
                Toast.makeText(requireContext(), "Apartment name updated", Toast.LENGTH_SHORT).show()
            }
            result.onFailure {
                Toast.makeText(requireContext(), it.message ?: "Failed to rename", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.removeMemberState.observe(viewLifecycleOwner) { result ->
            if (progressDialog.isShowing) progressDialog.dismiss()
            result.onSuccess {
                Toast.makeText(requireContext(), "Member removed", Toast.LENGTH_SHORT).show()
                viewModel.loadSettings()
            }
            result.onFailure {
                Toast.makeText(requireContext(), it.message ?: "Failed to remove member", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupMembersAdapter(currentUserId: String) {
        if (!::membersAdapter.isInitialized) {
            membersAdapter = ApartmentMembersAdapter(currentUserId) { user ->
                showRemoveMemberConfirmation(user)
            }
            binding.rvMembers.layoutManager = LinearLayoutManager(requireContext())
            binding.rvMembers.adapter = membersAdapter
        }
    }

    private fun showRenameConfirmation(newName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename Apartment")
            .setMessage("Rename to \"$newName\"?")
            .setPositiveButton("Rename") { _, _ ->
                progressDialog.setMessage("Saving...")
                progressDialog.show()
                viewModel.renameApartment(newName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveMemberConfirmation(user: User) {
        val name = user.name.ifBlank { user.email }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Member")
            .setMessage("Remove $name from the apartment?")
            .setPositiveButton("Remove") { _, _ ->
                progressDialog.setMessage("Removing...")
                progressDialog.show()
                viewModel.removeMember(user.userId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        if (::progressDialog.isInitialized && progressDialog.isShowing) progressDialog.dismiss()
        super.onDestroyView()
        _binding = null
    }
}
