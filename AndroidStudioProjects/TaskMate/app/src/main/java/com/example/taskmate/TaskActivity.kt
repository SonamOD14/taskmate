package com.example.taskmate

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.taskmate.model.Priority
import com.example.taskmate.model.Task
import com.example.taskmate.repository.TaskRepository
import com.example.taskmate.storage.TaskStorage
import com.example.taskmate.worker.ReminderWorker
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TaskActivity : ComponentActivity() {
    private val repository = TaskRepository()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = TaskStorage(this)

        setContent {
            val scope = rememberCoroutineScope()
            var tasks by remember { mutableStateOf(emptyList<Task>()) }
            var newTaskTitle by remember { mutableStateOf("") }
            var newTaskPriority by remember { mutableStateOf(Priority.MEDIUM) }
            var newTaskDueDate by remember { mutableStateOf<String?>(null) }
            var showDialog by remember { mutableStateOf(false) }
            val context = LocalContext.current

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                repository.getTasks { updatedTasks ->
                    tasks = updatedTasks
                }
            }

            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("TaskMate") },
                            actions = {
                                IconButton(onClick = {
                                    startActivity(Intent(this@TaskActivity, StatsActivity::class.java))
                                }) {
                                    Icon(Icons.Default.Info, contentDescription = "Stats")
                                }
                                IconButton(onClick = {
                                    storage.logout()
                                    startActivity(Intent(this@TaskActivity, LoginActivity::class.java))
                                    finish()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task")
                        }
                    }
                ) { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                        if (tasks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No tasks yet. Add one!")
                            }
                        } else {
                            LazyColumn {
                                items(tasks, key = { it.id }) { task ->
                                    TaskItem(
                                        task = task,
                                        onToggle = {
                                            scope.launch {
                                                val updatedTask = task.copy(
                                                    isDone = !task.isDone,
                                                    completedAt = if (!task.isDone) Timestamp.now() else null
                                                )
                                                repository.updateTask(updatedTask)
                                            }
                                        },
                                        onDelete = {
                                            scope.launch {
                                                repository.deleteTask(task.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (showDialog) {
                        var expanded by remember { mutableStateOf(false) }
                        
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text("Add Task") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = newTaskTitle,
                                        onValueChange = { newTaskTitle = it },
                                        label = { Text("Task Title") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { expanded = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Priority: ${newTaskPriority.name}")
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            Priority.values().forEach { priority ->
                                                DropdownMenuItem(
                                                    text = { Text(priority.name) },
                                                    onClick = {
                                                        newTaskPriority = priority
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val calendar = Calendar.getInstance()
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    calendar.set(year, month, dayOfMonth)
                                                    newTaskDueDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                                                },
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.DateRange, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(newTaskDueDate ?: "Select Due Date")
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    if (newTaskTitle.isNotBlank()) {
                                        scope.launch {
                                            val newTask = Task(
                                                title = newTaskTitle,
                                                priority = newTaskPriority,
                                                dueDate = newTaskDueDate
                                            )
                                            repository.addTask(newTask)
                                            
                                            newTaskDueDate?.let { dateStr ->
                                                scheduleReminder(context, newTaskTitle, dateStr)
                                            }
                                            
                                            newTaskTitle = ""
                                            newTaskPriority = Priority.MEDIUM
                                            newTaskDueDate = null
                                            showDialog = false
                                        }
                                    }
                                }) {
                                    Text("Add")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun scheduleReminder(context: android.content.Context, title: String, dateStr: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dueDate = sdf.parse(dateStr) ?: return
        
        val reminderTime = dueDate.time - (24 * 60 * 60 * 1000) // 1 day before
        val delay = reminderTime - System.currentTimeMillis()

        if (delay > 0) {
            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putString("task_title", title).build())
                .addTag(title)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}

@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    val priorityColor = when (task.priority) {
        Priority.HIGH -> Color.Red
        Priority.MEDIUM -> Color(0xFFFFA500)
        Priority.LOW -> Color.Green
    }

    val isOverdue = task.dueDate?.let {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(it)
        date != null && date.before(Date()) && !task.isDone
    } ?: false

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 48.dp)) {
                    Surface(
                        color = priorityColor,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = task.priority.name,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    
                    task.dueDate?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Due: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) Color.Red else Color.Gray
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Task")
            }
        }
    }
}
