package com.minderu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minderu.data.RoutineDto
import com.minderu.data.SupabaseProvider
import com.minderu.data.Task
import com.minderu.data.TaskDto
import com.minderu.ui.components.NativeAdItem
import com.minderu.ui.components.TaskCard
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun TodayTasksScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val tasks = remember { mutableStateListOf<Task>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            SupabaseProvider.ensureAuthenticated()
            val user = SupabaseProvider.client.auth.currentUserOrNull() ?: throw Exception("Auth failed")

            // 1. Fetch routines
            val routines = SupabaseProvider.client.postgrest["routines"]
                .select()
                .decodeList<RoutineDto>()

            val activeRoutine = if (routines.isEmpty()) {
                // Seed default routine
                val newRoutine = RoutineDto(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    title = "Daily Focus",
                    isActive = true
                )
                SupabaseProvider.client.postgrest["routines"].insert(newRoutine)

                // Seed default tasks
                val defaultTasks = listOf(
                    TaskDto(UUID.randomUUID().toString(), newRoutine.id, 0, "Put 3 clothes in your closet", "One tiny step", 3),
                    TaskDto(UUID.randomUUID().toString(), newRoutine.id, 1, "Drink a glass of water", "One tiny step", 2),
                    TaskDto(UUID.randomUUID().toString(), newRoutine.id, 2, "Clear one surface", "One tiny step", 3)
                )
                SupabaseProvider.client.postgrest["tasks"].insert(defaultTasks)
                newRoutine
            } else {
                routines.first { it.isActive }
            }

            // 2. Fetch tasks for routine
            val dbTasks = SupabaseProvider.client.postgrest["tasks"]
                .select {
                    filter {
                        eq("routine_id", activeRoutine.id)
                    }
                }
                .decodeList<TaskDto>()

            tasks.clear()
            tasks.addAll(dbTasks.map { dto ->
                Task(dto.id, dto.title, dto.subhead, dto.sparksReward, false)
            })

            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (errorMessage != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Error: $errorMessage", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            top = 32.dp,
            end = 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Minderu",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Today’s tasks",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 40.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Thin,
                        lineHeight = 44.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Small steps make a real difference.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            NativeAdItem(adId = "ca-app-pub-7003079941696391/7018656297")
        }

        items(tasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onTaskCompleted = { updatedTask ->
                    val index = tasks.indexOfFirst { it.id == updatedTask.id }
                    if (index != -1) {
                        val newCompletedStatus = !tasks[index].isCompleted
                        tasks[index] = tasks[index].copy(isCompleted = newCompletedStatus)
                        
                        // Persist to Supabase (e.g., updating a profile or a 'task_completions' table)
                        scope.launch {
                            try {
                                // For this example, let's assume we update user's sparks in a 'profiles' table
                                // and maybe log the completion.
                                // SupabaseProvider.client.postgrest["profiles"].update(...)
                            } catch (e: Exception) {
                                // Handle persistence error
                            }
                        }
                    }
                },
                onTaskDeleted = { taskToDelete ->
                    tasks.removeIf { it.id == taskToDelete.id }
                    scope.launch {
                        try {
                            SupabaseProvider.client.postgrest["tasks"].delete {
                                filter { eq("id", taskToDelete.id) }
                            }
                        } catch (e: Exception) {
                            // Handle deletion error
                        }
                    }
                },
                modifier = Modifier.animateItem()
            )
        }
    }
}
