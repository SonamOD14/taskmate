package com.example.taskmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskmate.model.Task
import com.example.taskmate.repository.TaskRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Calendar

class StatsActivity : ComponentActivity() {
    private val repository = TaskRepository()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val scope = rememberCoroutineScope()
            var totalTasks by remember { mutableLongStateOf(0L) }
            var completedToday by remember { mutableLongStateOf(0L) }
            var completedThisWeek by remember { mutableLongStateOf(0L) }
            var currentStreak by remember { mutableIntStateOf(0) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                scope.launch {
                    val allTasks = repository.getAllTasksOnce()
                    totalTasks = allTasks.size.toLong()
                    completedToday = calculateToday(allTasks)
                    completedThisWeek = calculateThisWeek(allTasks)
                    currentStreak = calculateStreak(allTasks)
                    isLoading = false
                }
            }

            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Task Statistics") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatCard("Total Tasks", totalTasks.toString())
                            StatCard("Completed Today", completedToday.toString())
                            StatCard("Completed This Week", completedThisWeek.toString())
                            StatCard("Daily Streak", "$currentStreak Days")

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Today's Progress", style = MaterialTheme.typography.titleMedium)
                            val progress = if (totalTasks > 0) completedToday.toFloat() / totalTasks.toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                            )
                            Text("${(progress * 100).toInt()}%", modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
            }
        }
    }

    private fun calculateToday(tasks: List<Task>): Long {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return tasks.count { it.isDone && it.completedAt != null && it.completedAt.toDate().time >= today }.toLong()
    }

    private fun calculateThisWeek(tasks: List<Task>): Long {
        val weekStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return tasks.count { it.isDone && it.completedAt != null && it.completedAt.toDate().time >= weekStart }.toLong()
    }

    private fun calculateStreak(tasks: List<Task>): Int {
        val completedDates = tasks.filter { it.isDone && it.completedAt != null }
            .map {
                val cal = Calendar.getInstance()
                cal.time = it.completedAt!!.toDate()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .distinct()
            .sortedDescending()

        if (completedDates.isEmpty()) return 0

        var streak = 0
        val current = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If no task today, check if streak continued until yesterday
        if (completedDates.first() < current.timeInMillis) {
            current.add(Calendar.DAY_OF_YEAR, -1)
            if (completedDates.first() < current.timeInMillis) return 0
        }

        for (date in completedDates) {
            if (date == current.timeInMillis) {
                streak++
                current.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
