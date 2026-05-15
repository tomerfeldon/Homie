package com.example.homie.ui.main.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.homie.data.model.Task
import com.example.homie.databinding.ItemTaskBinding

class TasksAdapter(
    private val onTaskChecked: (Task) -> Unit
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

        with(holder.binding) {

            // IMPORTANT: Remove old listener before setting state
            checkComplete.setOnCheckedChangeListener(null)

            checkComplete.text = task.title
            checkComplete.isChecked = task.completed
            tvDescription.text = task.description
            tvAssignedTo.text = "Assigned to ${task.assignedToName}"

            if (task.completed) {
                checkComplete.isEnabled = false
            } else {
                checkComplete.isEnabled = true

                checkComplete.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        onTaskChecked(task)
                    }
                }
            }
        }
    }
}
