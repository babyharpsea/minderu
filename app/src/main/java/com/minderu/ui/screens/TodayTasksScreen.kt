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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.minderu.ui.components.ConfettiAnimation
import com.minderu.ui.components.ConfirmTaskCompletionDialog
import com.minderu.ui.components.NativeAdItem
import com.minderu.ui.components.TaskCard
import com.minderu.ui.components.UserProfileBottomSheet
import com.minderu.ui.viewmodels.TodayTasksViewModel

@Composable
fun TodayTasksScreen(
    viewModel: TodayTasksViewModel,
    contentPadding: PaddingValues,
    onSignOut: () -> Unit,
    userEmail: String? = null,
    userId: String? = null,
    onLinkPasskey: () -> Unit = {},
    onAddTaskFromFab: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Bottom sheet & dialog states
    var showBottomSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskUiModel?>(null) }
    var confirmingTask by remember { mutableStateOf<TaskUiModel?>(null) }
    var triggerConfetti by remember { mutableStateOf(false) }

    // Consume transient errors via Snackbar
    LaunchedEffect(Unit) {
        viewModel.transientError.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp,
                top = contentPadding.calculateTopPadding() + 24.dp,
                end = 24.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header — always visible, even during loading or error states
            item {
                Column {
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
                            onClick = { showProfileSheet = true },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.primary
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

            // Interactive state handling
            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                uiState.loadError != null -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = uiState.loadError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = { viewModel.loadTasks() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                uiState.tasks.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tasks for today yet. Tap '+' to create your first habit!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onTaskCompleted = { selectedTask ->
                                if (selectedTask.isCompleted) {
                                    viewModel.toggleComplete(selectedTask.id)
                                } else {
                                    confirmingTask = selectedTask
                                }
                            },
                            onTaskDeleted = { selectedTask ->
                                viewModel.deleteTask(selectedTask.id)
                            },
                            onTaskEdit = { selectedTask ->
                                editingTask = selectedTask
                            }
                        )
                    }
                }
            }
        }

        // Add / Edit Task Bottom Sheet
        if (showBottomSheet || editingTask != null) {
            AddTaskBottomSheet(
                existingTask = editingTask,
                onDismissRequest = {
                    showBottomSheet = false
                    editingTask = null
                },
                onSubmit = { title, subhead, sparks ->
                    if (editingTask != null) {
                        viewModel.updateTask(editingTask!!.id, title, subhead, sparks)
                    } else {
                        viewModel.addTask(title, subhead, sparks)
                    }
                    showBottomSheet = false
                    editingTask = null
                }
            )
        }

        // Hold-to-confirm Task Completion Dialog
        confirmingTask?.let { task ->
            ConfirmTaskCompletionDialog(
                task = task,
                onConfirm = {
                    confirmingTask = null
                    triggerConfetti = true
                    viewModel.completeTaskWithDelay(task.id)
                },
                onDismiss = { confirmingTask = null }
            )
        }

        // Celebratory Confetti Animation
        ConfettiAnimation(
            trigger = triggerConfetti,
            onFinished = { triggerConfetti = false },
            modifier = Modifier.fillMaxSize()
        )

        // User Profile Bottom Sheet
        if (showProfileSheet) {
            UserProfileBottomSheet(
                email = userEmail,
                userId = userId,
                onLinkPasskey = onLinkPasskey,
                onSignOut = onSignOut,
                onDismiss = { showProfileSheet = false }
            )
        }

        // Snackbar Host for errors/messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp)
        )
    }
}
