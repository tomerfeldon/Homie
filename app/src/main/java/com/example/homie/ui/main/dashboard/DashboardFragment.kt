package com.example.homie.ui.main.dashboard

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.homie.databinding.FragmentDashboardBinding
import com.example.homie.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var membersAdapter: MembersAdapter
    private lateinit var tasksAdapter: DashboardTaskAdapter
    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerViews()
        setupProgressDialog()
        observeUiState()

        viewModel.loadDashboard()

        binding.ivLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireActivity(), LoginActivity::class.java))
            requireActivity().finishAffinity()
        }
    }

    private fun setupRecyclerViews() {

        membersAdapter = MembersAdapter(emptyList())
        binding.rvMembers.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvMembers.adapter = membersAdapter

        tasksAdapter = DashboardTaskAdapter(emptyList())
        binding.rvUrgentTasks.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvUrgentTasks.adapter = tasksAdapter
    }

    private fun setupProgressDialog() {
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Loading Dashboard...")
        progressDialog.setCancelable(false)
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->

            when (state) {

                is DashboardUiState.Loading -> {
                    if (!progressDialog.isShowing)
                        progressDialog.show()
                }

                is DashboardUiState.Success -> {
                    if (progressDialog.isShowing)
                        progressDialog.dismiss()

                    binding.tvApartmentName.text =
                        "Apartment: ${state.apartmentName}"

                    binding.tvCurrentDebt.text = state.debtText

                    membersAdapter.updateData(state.members)
                    tasksAdapter.updateData(state.urgentTasks)
                    if (state.urgentTasks.isNotEmpty()){
                        binding.tvNoUrgentTask.visibility = View.GONE
                    }
                }

                is DashboardUiState.Error -> {
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
        if (::progressDialog.isInitialized && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
        super.onDestroyView()
        _binding = null
    }
}