package com.minderu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.minderu.data.MinderuDestination
import com.minderu.data.Screen
import com.minderu.data.Task
import com.minderu.data.mockBinderCards
import com.minderu.data.mockPlantings
import com.minderu.data.mockTasks
import com.minderu.ui.components.AddTaskBottomSheet
import com.minderu.ui.components.FloatingNavDock
import com.minderu.ui.screens.CardBinderScreen
import com.minderu.ui.screens.ImpactTrackerScreen
import com.minderu.ui.screens.TodayTasksScreen
import com.minderu.ui.theme.MinderuTheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
        setContent {
            MinderuTheme {
                MinderuApp(firebaseAnalytics = firebaseAnalytics)
            }
        }
    }
}

@Composable
fun MinderuApp(
    initialTasks: List<Task> = mockTasks,
    modifier: Modifier = Modifier,
    firebaseAnalytics: FirebaseAnalytics? = null
) {
    val navController = rememberNavController()
    val tasksState = remember { mutableStateOf(initialTasks) }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Task Creation State
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FloatingNavDock(
                selectedDestination = MinderuDestination.entries.find { it.screen.route == currentRoute }
                    ?: MinderuDestination.Dashboard,
                onDestinationSelected = { destination ->
                    navController.navigate(destination.screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onFabClick = { showBottomSheet = true }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route
        ) {
            composable(Screen.Dashboard.route) {
                TodayTasksScreen(
                    tasks = tasksState.value,
                    contentPadding = innerPadding,
                    onTaskCompleted = { task ->
                        firebaseAnalytics?.logEvent("task_completed") {
                            param("task_id", task.id)
                            param("task_title", task.title)
                        }
                        tasksState.value = tasksState.value.map { currentTask ->
                            if (currentTask.id == task.id) {
                                currentTask.copy(isCompleted = !currentTask.isCompleted)
                            } else {
                                currentTask
                            }
                        }
                    },
                    onTaskDeleted = { task ->
                        firebaseAnalytics?.logEvent("task_deleted") {
                            param("task_id", task.id)
                            param("task_title", task.title)
                        }
                        tasksState.value = tasksState.value.filter { it.id != task.id }
                    }
                )
            }
            composable(Screen.Binder.route) {
                CardBinderScreen(
                    cards = mockBinderCards,
                    sparksBalance = 350,
                    contentPadding = innerPadding
                )
            }
            composable(Screen.Impact.route) {
                ImpactTrackerScreen(
                    treesPlanted = 12,
                    plantings = mockPlantings,
                    contentPadding = innerPadding
                )
            }
        }

        if (showBottomSheet) {
            AddTaskBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                onAddTask = { title, sparks ->
                    val newTask = Task(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        subtitle = "One tiny step",
                        sparks = sparks
                    )
                    tasksState.value = listOf(newTask) + tasksState.value
                    showBottomSheet = false
                }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun MinderuPreview() {
    MinderuTheme {
        MinderuApp()
    }
}
