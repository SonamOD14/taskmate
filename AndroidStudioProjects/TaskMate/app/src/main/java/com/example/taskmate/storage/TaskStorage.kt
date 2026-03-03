package com.example.taskmate.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.taskmate.model.Task
import org.json.JSONArray
import org.json.JSONObject

class TaskStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("taskmate_prefs", Context.MODE_PRIVATE)

    fun saveLoginState(isLoggedIn: Boolean) {
        prefs.edit().putBoolean("is_logged_in", isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)

    fun saveTasks(tasks: List<Task>) {
        val jsonArray = JSONArray()
        tasks.forEach { task ->
            val jsonObject = JSONObject()
            jsonObject.put("id", task.id)
            jsonObject.put("title", task.title)
            jsonObject.put("isDone", task.isDone)
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString("tasks", jsonArray.toString()).apply()
    }

    fun getTasks(): List<Task> {
        val tasksString = prefs.getString("tasks", null) ?: return emptyList()
        val tasks = mutableListOf<Task>()
        val jsonArray = JSONArray(tasksString)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            tasks.add(Task(
                id = jsonObject.getString("id"),
                title = jsonObject.getString("title"),
                isDone = jsonObject.getBoolean("isDone")
            ))
        }
        return tasks
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }
}
