package com.minderu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minderu.data.TaskUiModel
import com.minderu.ui.components.AddTaskBottomSheet
import com.minderu.ui.components.NativeAdItem
import com.minderu.ui.components.TaskCard
import com.minderu.ui.viewmodels.TodayTasksViewModel

@Composable
fun TodayTasksScreen(
    viewModel: TodayTasksViewModel,
    contentPadding: PaddingValues,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // Bottom sheet state
    var showBottomSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskUiModel?>(null) }

    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (errorMessage != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.loadTasks() }) {
                    Text("Retry")
                }
            }
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
                // Header row with sign-out
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Minderu",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Logout,
                            contentDescription = "Sign out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Today's tasks",
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
                onTaskCompleted = { viewModel.toggleComplete(it.id) },
                onTaskDeleted = { viewModel.deleteTask(it.id) },
                onTaskEdit = { taskToEdit ->
                    editingTask = taskToEdit
                    showBottomSheet = true
                },
                modifier = Modifier.animateItem()
            )
        }

        // FAB-equivalent: "Add task" button at bottom of list
        item {
            Button(
                onClick = {
                    editingTask = null
                    showBottomSheet = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text("+ Add a micro-task", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // ── Bottom Sheet (Create / Edit) ──────────────────────────

    if (showBottomSheet) {
        AddTaskBottomSheet(
            existingTask = editingTask,
            onDismissRequest = {
                showBottomSheet = false
                editingTask = null
            },
            onSubmit = { title, subhead, sparks ->
                val existing = editingTask
                if (existing != null) {
                    viewModel.updateTask(existing.id, title, subhead, sparks)
                } else {
                    viewModel.addTask(title, subhead, sparks)
                }
                showBottomSheet = false
                editingTask = null
            }
        )
    }
}
