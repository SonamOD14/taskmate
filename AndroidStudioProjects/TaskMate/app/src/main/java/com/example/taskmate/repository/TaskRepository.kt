package com.example.taskmate.repository

import com.example.taskmate.model.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class TaskRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserTasksCollection() = auth.currentUser?.uid?.let {
        firestore.collection("users").document(it).collection("tasks")
    }

    suspend fun addTask(task: Task) {
        getUserTasksCollection()?.document(task.id)?.set(task)?.await()
    }

    suspend fun updateTask(task: Task) {
        getUserTasksCollection()?.document(task.id)?.set(task)?.await()
    }

    suspend fun deleteTask(taskId: String) {
        getUserTasksCollection()?.document(taskId)?.delete()?.await()
    }

    fun getTasks(callback: (List<Task>) -> Unit) {
        getUserTasksCollection()?.orderBy("priority", Query.Direction.ASCENDING)
            ?.addSnapshotListener { snapshot, _ ->
                val tasks = snapshot?.toObjects(Task::class.java) ?: emptyList()
                // Manual sorting because Firestore doesn't support custom enum ordering easily without mapping
                val priorityOrder = listOf("HIGH", "MEDIUM", "LOW")
                val sortedTasks = tasks.sortedWith(compareBy({ priorityOrder.indexOf(it.priority.name) }, { it.createdAt }))
                callback(sortedTasks)
            }
    }

    suspend fun getAllTasksOnce(): List<Task> {
        return getUserTasksCollection()?.get()?.await()?.toObjects(Task::class.java) ?: emptyList()
    }
}
