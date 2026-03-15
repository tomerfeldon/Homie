package com.example.homie.data.model

data class Task(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var assignedTo: String = "",
    var assignedToName: String = "",
    var createdBy: String = "",
    var completed: Boolean = false,
    var timestamp: Long = System.currentTimeMillis()
)
