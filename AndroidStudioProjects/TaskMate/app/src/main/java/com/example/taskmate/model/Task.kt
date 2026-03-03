package com.example.taskmate.model

import com.google.firebase.Timestamp
import java.util.UUID

enum class Priority {
    HIGH, MEDIUM, LOW
}

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val isDone: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val dueDate: String? = null,
    val completedAt: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now()
)
