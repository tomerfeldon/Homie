package com.example.homie.ui.main.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.homie.data.model.Task
import com.example.homie.databinding.ItemTaskBinding
import com.google.firebase.auth.FirebaseAuth

class TasksAdapter(
    private val onTaskToggled: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    private val taskList = mutableListOf<Task>()
    val currentList: List<Task> get() = taskList

    fun submitList(list: List<Task>) {
        taskList.clear()
        taskList.addAll(list)
        notifyDataSetChanged()
    }

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun getItemCount() = taskList.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        with(holder.binding) {
            checkComplete.setOnCheckedChangeListener(null)

            checkComplete.text = task.title
            checkComplete.isChecked = task.completed
            tvDescription.text = task.description
            tvAssignedTo.text = "Assigned to ${task.assignedToName}"

            // Can complete any pending task; can only uncheck if you're the assignee
            val canToggle = !task.completed || task.assignedTo == currentUid
            checkComplete.isEnabled = canToggle

            checkComplete.setOnCheckedChangeListener { _, isChecked ->
                onTaskToggled(task, isChecked)
            }
        }
    }
}
