package com.example.homie.ui.main.tasks

import android.app.ProgressDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.homie.data.model.User
import com.example.homie.databinding.FragmentAddTaskBinding
import com.google.firebase.auth.FirebaseAuth

class AddTaskFragment : Fragment() {

    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TasksViewModel by viewModels()

    private lateinit var progressDialog: ProgressDialog
    private var membersList: List<User> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Saving...")
        progressDialog.setCancelable(false)

        binding.btnSaveTask.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Enter title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedIndex = binding.spinnerAssignee.selectedItemPosition
            if (membersList.isEmpty() || selectedIndex < 0) {
                Toast.makeText(requireContext(), "Please wait, loading members...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedMember = membersList[selectedIndex]
            viewModel.addTask(title, description, selectedMember.userId, selectedMember.name)
        }

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        observeViewModel()
        viewModel.loadMembers()
    }

    private fun observeViewModel() {
        viewModel.addTaskState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TaskUiState.Loading -> progressDialog.show()
                is TaskUiState.Success -> {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Task Added", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is TaskUiState.Error -> {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.membersState.observe(viewLifecycleOwner) { members ->
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            val sortedMembers = members.filter { it.userId != currentUid }
            membersList = sortedMembers
            val names = sortedMembers.map { it.name.ifEmpty { it.email } }
            val spinnerAdapter = android.widget.ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                names
            )
            binding.spinnerAssignee.adapter = spinnerAdapter
        }
    }

    override fun onDestroyView() {
        progressDialog.dismiss()
        _binding = null
        super.onDestroyView()
    }
}
